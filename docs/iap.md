# PairShot Android — In-App Purchase 설계서

본 문서는 PairShot Android (현재 v1.1.6 출시 중) 에 **구독형 IAP** 를 도입하기 위한 설계 명세입니다. iOS 측 결정과 정렬됐으나 본 문서는 **Android 한정**이며, 기존에 출시된 Play Store v1.1.6 사용자 마이그레이션까지 포함합니다.

작성: 2026-05-13
대상 Library: Google Play Billing Library **8.3.0** (2025-12 release)

---

## 1. 비즈니스 모델 (확정)

| 티어 | 사용 조건 | 페어 한도 | 광고 |
|---|---|---|---|
| **Trial** | 신규 설치 후 자동 시작, 14일 (1회만) | 무제한 | 없음 |
| **Free** | trial 종료 후, 또는 trial 없이 진입 | **동시 보유 5개 슬롯** | 표시 (현행 유지) |
| **Paid** | 구독 활성 | 무제한 | 없음 |

### 핵심 규칙
- "5개 슬롯" = 앱에 동시 보관 가능한 페어카드 최대치. 초과 시 export·삭제로 슬롯 비워야 새로 만들 수 있음
- 월·누적 카운터 X. 슬롯 비우면 즉시 다시 만들 수 있음
- Play Store 정책상 trial 은 Google 계정당 **앱 전체 1회**
- Platform-isolated — iOS / Android 구독 별도. 크로스플랫폼 동기화 없음 (단일 device 사용자 가정)

### 가격대 제안 (확정은 별도)
| SKU | 가격대 | 비고 |
|---|---|---|
| `pairshot_pro_monthly` | ₩4,900 ~ ₩7,900 / 월 | 현장 작업자 결제 부담 임계점 |
| `pairshot_pro_yearly` | ₩39,000 ~ ₩59,000 / 년 | 월 단위 대비 ~40% 할인 |

월·연 둘 다 같은 base subscription 의 별도 base plan 으로 구성. 14일 trial 은 yearly 에만 또는 양쪽에 부여.

---

## 2. 현재 PairShot Premium 아키텍처 (As-Is)

### 2.1 광고 비표시 상태 (`AdFree`) 의 단일 소스

**`AdFreeStatusProvider`** 인터페이스 — `core/domain/.../coupon/AdFreeStatusProvider.kt`

```kotlin
interface AdFreeStatusProvider {
    fun observeIsAdFree(): Flow<Boolean>
    suspend fun currentIsAdFree(): Boolean
}
```

**유일한 구현체**: `CouponAdFreeStatusProvider` (`:core:coupon`)
- DataStore `"coupon_state"` 에 저장된 `StoredCouponState.expiresAtEpochMillis` 기준
- `null` (Unlimited) 또는 `nowMillis < expiresAtEpochMillis` → `isAdFree = true`
- Hilt `@Binds` 단일 등록: `CouponModule.kt:34`

### 2.2 광고 위치 (모든 placement)

| 광고 유형 | 위치 |
|---|---|
| Banner | `CameraScreen`, `AfterCameraScreen`, `HomeScreen`, `PairPreviewScreen`, `ExportSettingsScreen`, `Settings`/`WatermarkSettings`/`CombineSettings`/`LicenseScreen` |
| Native | `HomePairGridSection`, `AlbumPairGridSection` — 2열 그리드 2행마다, 페어 5개 이상일 때 |
| Interstitial | `MainActivity` (기기 저장), `ExportShareEffect` (공유), `SaveZipToDocumentEffect` (ZIP 저장). 쿨다운 5초 |
| AppOpen | `AppOpenAdLifecycleObserver` — 앱 포그라운드 복귀, 쿨다운 60초 |

모든 광고 컴포넌트가 `adFreeStatusProvider.currentIsAdFree()` 를 먼저 평가하고 `true` 면 skip.

### 2.3 추가 premium 흐름 — `SettingsPremiumGate` + `RewardedAdController`

- `SettingsPremiumGate` (`:core:ads`) — 인메모리 set. `unlock(feature: PremiumFeature)` 시 열림, 앱 재시작 시 초기화
- `RewardedAdController.show()` 흐름:
  1. `gate.isUnlocked(feature)` → 즉시 reward
  2. `adFreeStatusProvider.currentIsAdFree()` → `gate.unlock()` 후 즉시 reward
  3. 그 외 → 리워드 광고 시청 → unlock + reward
- 현재 `PremiumFeature` enum: `WATERMARK_DETAIL`, `COMBINE_DETAIL` (2개)
- 호출 위치: `SettingsRoute.kt:185-196` — 워터마크/합성 상세 진입 시

### 2.4 페어 생성 진입점

`PhotoPairRepository.saveBeforePhoto()` — `:core:data/.../PhotoPairRepositoryImpl.kt`. 현재 한도 검증 없음.

---

## 3. To-Be 설계 — 한눈에

```
┌──────────────────────────────────────────────────────────────────┐
│                    :core:domain/entitlement                      │
│  ProEntitlement (data class)                                     │
│  EntitlementSource (enum: NONE | COUPON | TRIAL | SUBSCRIPTION)  │
│  ProEntitlementProvider (interface)                              │
│  CanCreatePairUseCase (5개 한도 + ProEntitlement 통합)            │
└──────────────────────────────────────────────────────────────────┘
                              ▲           ▲
                              │           │
        ┌─────────────────────┘           └─────────────────────┐
        │                                                       │
┌───────────────────────┐                       ┌───────────────────────┐
│   :core:coupon        │                       │   :core:billing       │  ← 신규
│ CouponAdFreeProvider  │                       │ BillingClient wrapper │
│ (기존 그대로 유지)     │                       │ SubscriptionProvider  │
└───────────────────────┘                       └───────────────────────┘
        │                                                       │
        └────────────────┬──────────────────────────────────────┘
                         ▼
        ┌─────────────────────────────────────────┐
        │   :core:data                            │
        │ CompositeProEntitlementProvider         │  ← 신규
        │  = coupon.observe() OR billing.observe()│
        │ AdFreeStatusProvider 도 위임             │
        └─────────────────────────────────────────┘
                         ▲
                         │
        ┌────────────────┴──────────────────────┐
        │                                       │
┌───────────────────┐               ┌──────────────────────┐
│  Existing ads UI  │               │ :feature:paywall     │  ← 신규
│ (no code change)  │               │ Paywall sheet        │
└───────────────────┘               │ ManageSubscription   │
                                    │ RestorePurchases     │
                                    └──────────────────────┘
                                            ▲
                                            │
                                ┌───────────────────────────┐
                                │ Pair limit gate           │
                                │ (camera ShutterClick)     │
                                └───────────────────────────┘
```

---

## 4. 신규 모듈 — `:core:billing`

`:core:ads` 옆에 별도 모듈로 분리. 광고와 결제는 별 도메인이므로 분리가 깔끔.

### 4.1 Gradle 설정

```kotlin
// core/billing/build.gradle.kts
plugins {
    id("pairshot.android.library")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pairshot.core.billing"
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":core:domain"))
    implementation("com.android.billingclient:billing-ktx:8.3.0")
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.timber)
}
```

`libs.versions.toml` 에 `billing-ktx = "com.android.billingclient:billing-ktx:8.3.0"` 추가.

**Manifest 권한** (`:app/AndroidManifest.xml`):
```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

### 4.2 파일 구조

```
core/billing/
├── build.gradle.kts
├── src/main/AndroidManifest.xml   (BILLING permission만)
└── src/main/java/com/pairshot/core/billing/
    ├── BillingClientHolder.kt        — Singleton 연결 관리
    ├── BillingProductCatalog.kt      — productId / basePlan / offerId 상수
    ├── BillingRepository.kt          — Public API
    ├── BillingRepositoryImpl.kt      — 구현
    ├── di/BillingModule.kt
    ├── domain/
    │   ├── SubscriptionStatus.kt     — sealed: Inactive / Trial / Active / Pending / OnHold
    │   └── PurchaseError.kt
    └── internal/
        ├── PurchaseStateMachine.kt   — Purchase → SubscriptionStatus 매핑
        └── AcknowledgeWorker.kt      — 3일 acknowledge window 관리
```

### 4.3 `BillingClientHolder` — Singleton 연결

Play Billing 8 의 `enableAutoServiceReconnection()` 으로 disconnect 재시도 boilerplate 제거됨.

```kotlin
@Singleton
class BillingClientHolder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val purchasesListener: PurchasesUpdatedListener,
    ) {
        private val pendingParams =
            PendingPurchasesParams
                .newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()

        val client: BillingClient =
            BillingClient
                .newBuilder(context)
                .setListener(purchasesListener)
                .enablePendingPurchases(pendingParams)
                .enableAutoServiceReconnection()
                .build()

        private val readiness = MutableStateFlow(false)
        val isReady: StateFlow<Boolean> = readiness.asStateFlow()

        fun ensureConnected() {
            if (client.isReady) {
                readiness.value = true
                return
            }
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        readiness.value = result.responseCode == BillingClient.BillingResponseCode.OK
                    }
                    override fun onBillingServiceDisconnected() {
                        readiness.value = false
                    }
                },
            )
        }
    }
```

- **수명**: 앱 프로세스 = Singleton. `endConnection()` 호출 안 함 (Google 권장: "long-lived")
- **재연결**: v8 auto-reconnect 가 처리하지만 `ensureConnected()` 를 cold-start 시 호출

### 4.4 `BillingRepository` — Public API

```kotlin
interface BillingRepository {
    val subscriptionStatus: StateFlow<SubscriptionStatus>

    suspend fun refresh()

    suspend fun loadOffers(): Result<List<BillingOffer>>

    suspend fun launchPurchaseFlow(
        activity: Activity,
        offerToken: String,
        productId: String,
    ): Result<Unit>

    fun deepLinkToManageSubscriptions(productId: String? = null): Intent
}
```

**구독 상태 sealed class**:
```kotlin
sealed interface SubscriptionStatus {
    data object Inactive : SubscriptionStatus
    data class Pending(val productId: String) : SubscriptionStatus
    data class InTrial(val productId: String, val expiryEpochMs: Long) : SubscriptionStatus
    data class Active(val productId: String, val expiryEpochMs: Long, val autoRenew: Boolean) : SubscriptionStatus
    data class OnHold(val productId: String) : SubscriptionStatus
    data class GracePeriod(val productId: String, val gracePeriodEnd: Long) : SubscriptionStatus
}

val SubscriptionStatus.isPro: Boolean
    get() = this is InTrial || this is Active || this is GracePeriod
```

`Pending` / `OnHold` 는 pro 아님 — 결제 미완료/실패 상태이므로 entitlement 부여 X.

### 4.5 핵심 흐름

#### 앱 시작 시 동기화 (`refresh()`)

```kotlin
override suspend fun refresh() {
    billingClient.ensureConnected()
    awaitReady()
    val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
    val result = client.queryPurchasesAsync(params)
    if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
        Timber.w("queryPurchases failed: ${result.billingResult.debugMessage}")
        return
    }
    val active = result.purchasesList.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    val status = if (active != null) mapToStatus(active) else SubscriptionStatus.Inactive
    _status.value = status
    if (active != null && !active.isAcknowledged) {
        acknowledgeAndCacheToken(active)
    }
}
```

`queryPurchasesAsync` 는 **Play 서버에서 fresh 데이터** 를 가져옴 (cache X). `onPurchasesUpdated` 콜백은 결제 즉시에만 발화하므로 cold-start 마다 query 가 필수.

#### Offer 로딩 (Paywall 진입 시)

```kotlin
override suspend fun loadOffers(): Result<List<BillingOffer>> = runCatching {
    awaitReady()
    val params = QueryProductDetailsParams.newBuilder()
        .setProductList(
            BillingProductCatalog.subscriptionIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            },
        ).build()
    val result = client.queryProductDetails(params)
    val products = result.productDetailsList.orEmpty()
    products.flatMap { product ->
        product.subscriptionOfferDetails.orEmpty().map { offer ->
            BillingOffer(
                productId = product.productId,
                basePlanId = offer.basePlanId,
                offerId = offer.offerId,
                offerToken = offer.offerToken,
                priceFormatted = offer.pricingPhases.pricingPhaseList.last().formattedPrice,
                trialDays = offer.pricingPhases.pricingPhaseList
                    .firstOrNull { it.priceAmountMicros == 0L }
                    ?.billingPeriod
                    ?.let(::parseIsoDays),
            )
        }
    }
}
```

**중요**: `offerToken` 은 `(productId, basePlanId, offerId)` 조합마다 다르며 매번 fresh query 필요. 캐싱 금지. 호출 측은 trial 이 포함된 offer 만 강조 표시.

#### 결제 시트 띄우기

```kotlin
override suspend fun launchPurchaseFlow(
    activity: Activity,
    offerToken: String,
    productId: String,
): Result<Unit> = runCatching {
    awaitReady()
    val productDetails = cachedProductDetails(productId)
        ?: error("ProductDetails not loaded; call loadOffers() first")
    val params = BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build(),
            ),
        ).setObfuscatedAccountId(deviceHashProvider.deviceHash())
        .build()
    val result = client.launchBillingFlow(activity, params)
    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
        error("launchBillingFlow failed: ${result.debugMessage}")
    }
}
```

`setObfuscatedAccountId` = 기기별 hash. Play 가 fraud 신호로 사용 + RTDN payload 에 포함됨. PairShot 의 기존 `DeviceHashProvider` (`:core:coupon`) 재활용 가능.

#### Acknowledge — 3일 한도 절대 준수

```kotlin
private suspend fun acknowledgeAndCacheToken(purchase: Purchase) {
    val params = AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchase.purchaseToken)
        .build()
    val result = client.acknowledgePurchase(params)
    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
        Timber.i("Acknowledged ${purchase.products.firstOrNull()}")
    } else {
        Timber.e("Acknowledge failed: ${result.debugMessage}")
    }
}
```

**Acknowledge 안 하면 Google 이 3일 후 자동 환불**. Trial 의 경우 3일 ≪ 14일 trial 기간이므로 잊으면 즉시 망함. `refresh()` 마다 `!isAcknowledged` 체크 + 재시도.

### 4.6 `PurchasesUpdatedListener` 의 위치

콜백은 **Singleton** 으로 두고 `BillingRepository` 가 listener 의 결과를 채널/Flow 로 받아 처리. Compose 화면이 listener 를 직접 구현하면 화면 재구성 시 listener 중복 발생.

```kotlin
@Singleton
class BillingPurchaseUpdatesListener
    @Inject
    constructor() : PurchasesUpdatedListener {
        private val _updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 1)
        val updates: SharedFlow<PurchaseUpdate> = _updates.asSharedFlow()

        override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
            _updates.tryEmit(PurchaseUpdate(result, purchases.orEmpty()))
        }
    }
```

`BillingRepositoryImpl` 가 `init` 또는 `Application.onCreate` 호출 후 `updates` flow 를 collect 해서 상태 갱신.

---

## 5. Entitlement 통합 — `:core:domain/entitlement`

기존 `AdFreeStatusProvider` 만으로는 부족 (페어 한도 도 entitlement 의 일부). 새 추상화 추가.

### 5.1 새 도메인 모델

```kotlin
// :core:domain/entitlement/ProEntitlement.kt
data class ProEntitlement(
    val isActive: Boolean,
    val source: EntitlementSource,
    val expiresAtEpochMs: Long? = null,
)

enum class EntitlementSource { NONE, COUPON, TRIAL, SUBSCRIPTION }

// :core:domain/entitlement/ProEntitlementProvider.kt
interface ProEntitlementProvider {
    fun observe(): Flow<ProEntitlement>
    suspend fun current(): ProEntitlement
}
```

`ProEntitlement.isActive == true` → 광고 X + 페어 무제한.

### 5.2 Composite 구현

`:core:data/entitlement/CompositeProEntitlementProvider.kt`:

```kotlin
@Singleton
class CompositeProEntitlementProvider
    @Inject
    constructor(
        private val couponStatus: CouponAdFreeStatusProvider,
        private val billingRepository: BillingRepository,
    ) : ProEntitlementProvider {

        override fun observe(): Flow<ProEntitlement> =
            combine(
                couponStatus.observeIsAdFree(),
                billingRepository.subscriptionStatus,
            ) { couponAdFree, subStatus ->
                when {
                    subStatus.isPro && subStatus is SubscriptionStatus.InTrial ->
                        ProEntitlement(true, EntitlementSource.TRIAL, subStatus.expiryEpochMs)
                    subStatus.isPro ->
                        ProEntitlement(true, EntitlementSource.SUBSCRIPTION, /* expiry */)
                    couponAdFree ->
                        ProEntitlement(true, EntitlementSource.COUPON, /* expiry from couponPrefs */)
                    else ->
                        ProEntitlement(false, EntitlementSource.NONE)
                }
            }
    }
```

### 5.3 `AdFreeStatusProvider` 와의 관계

기존 `AdFreeStatusProvider` 호출처가 다수 (모든 광고 컴포넌트). 안전한 마이그레이션:

```kotlin
// :core:data/entitlement/EntitlementBackedAdFreeStatusProvider.kt
class EntitlementBackedAdFreeStatusProvider
    @Inject
    constructor(
        private val provider: ProEntitlementProvider,
    ) : AdFreeStatusProvider {
        override fun observeIsAdFree(): Flow<Boolean> = provider.observe().map { it.isActive }
        override suspend fun currentIsAdFree(): Boolean = provider.current().isActive
    }
```

Hilt `@Binds` 를 `CouponAdFreeStatusProvider` → `EntitlementBackedAdFreeStatusProvider` 로 교체.

→ **광고 측 코드 (`PairShotBannerAd`, `PairShotNativeAdCard`, `AppOpenAdLifecycleObserver` 등) 는 한 줄도 안 바뀜**. 기존 `observeIsAdFree()` 가 새 entitlement 흐름을 자동 반영.

---

## 6. 페어 한도 Gate

### 6.1 UseCase

```kotlin
// :core:domain/pair/CanCreatePairUseCase.kt
class CanCreatePairUseCase
    @Inject
    constructor(
        private val photoPairRepository: PhotoPairRepository,
        private val entitlementProvider: ProEntitlementProvider,
    ) {
        suspend operator fun invoke(): Result {
            val entitlement = entitlementProvider.current()
            if (entitlement.isActive) return Result.Allowed
            val activeCount = photoPairRepository.countActive().first()
            return if (activeCount < FREE_PAIR_LIMIT) {
                Result.Allowed
            } else {
                Result.LimitReached(activeCount, FREE_PAIR_LIMIT)
            }
        }

        sealed interface Result {
            data object Allowed : Result
            data class LimitReached(val current: Int, val limit: Int) : Result
        }

        companion object { const val FREE_PAIR_LIMIT = 5 }
    }
```

`countActive()` 는 `PhotoPairRepository` 에 신규 메서드 — `status != EXPORTED_AND_DELETED` 같은 정의 필요. 현재 `PhotoPair.status` 가 `BEFORE_ONLY` / `PAIRED` / `AFTER_ONLY` 만 있으므로 단순히 row count.

### 6.2 호출 시점

`CameraScreen.kt` 의 shutter onClick (Before 촬영 직전):

```kotlin
onShutter = {
    scope.launch {
        when (val r = canCreatePairUseCase()) {
            is CanCreatePairUseCase.Result.Allowed -> {
                /* 기존 capture 로직 */
            }
            is CanCreatePairUseCase.Result.LimitReached -> {
                viewModel.requestPaywall(reason = PaywallReason.PairLimit(r.current))
            }
        }
    }
}
```

After 촬영은 기존 페어에 사진을 추가하는 것이므로 gate 불필요.

### 6.3 Album/Home 화면의 친절한 UX

빈 슬롯 임박 시(예: 4/5 페어 보유) 카메라 진입 전에 hint 표시 — "1개 더 만들면 한도. 결제로 무제한 해제" 같은 inline banner. 결제 압력은 자연스러워야 함.

---

## 7. Paywall UI — `:feature:paywall` (신규 모듈)

### 7.1 화면 구성

- **Hero**: "PairShot Pro" 타이틀 + 핵심 가치 3줄 (페어 무제한 / 광고 없음 / 작업 흐름 끊김 없음)
- **CTA**:
  - "14일 무료 체험 시작" — `pairshot_pro_yearly` 의 trial offer
  - "월 결제로 시작" — `pairshot_pro_monthly` (trial 없는 base plan)
- **Footer**:
  - "구독 복원" 버튼 → `billingRepository.refresh()`
  - "이용약관 / 개인정보처리방침" 링크 (Play 정책 필수)
  - 가격 + 자동갱신 명시 (Play 정책 필수)

### 7.2 Compose + Activity 액세스

`launchBillingFlow` 가 `Activity` 필요. Compose 1.8+ 의 `LocalActivity` 사용 (이 프로젝트 BOM 2026.03 에 포함):

```kotlin
@Composable
fun PaywallScreen(viewModel: PaywallViewModel = hiltViewModel()) {
    val activity = LocalActivity.current ?: return
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    Column { /* ... */
        Button(onClick = { viewModel.startPurchase(activity, offers.trialOffer) }) {
            Text("14일 무료 체험 시작")
        }
    }
}
```

**중요**: `Activity` 를 ViewModel 안에 절대 저장하지 말 것. 매 호출마다 인자로 받음.

### 7.3 Paywall 진입 트리거

| Trigger | Reason |
|---|---|
| 페어 5개 한도 도달 시 shutter 탭 | `PaywallReason.PairLimit` |
| Settings → "Pro 구독" 메뉴 | `PaywallReason.UserInitiated` |
| WATERMARK_DETAIL / COMBINE_DETAIL 진입 (Rewarded gate 대안 경로) | `PaywallReason.PremiumFeature` |
| 첫 실행 안내 (선택) | `PaywallReason.Onboarding` — 정책상 dismiss 가능해야 함 |

---

## 8. Play Console 설정

### 8.1 Subscription 구조

```
PRODUCT: pairshot_pro
├── Base Plan: monthly  (auto-renewing, ₩X/월)
│   └── (offers 없음 — 기본 가격으로 시작)
└── Base Plan: yearly  (auto-renewing, ₩Y/년)
    └── Offer: trial14
        ├── Phase 1: Free trial, P14D
        └── Phase 2: Base plan 가격으로 자동 갱신
```

**Trial eligibility**:
- "Allow one free trial per app" (default ON) → Google 계정당 앱 전체 1회
- 회원가입 가입자만 부여하는 추가 조건 X (PairShot 은 계정 없음)

### 8.2 출시 전 필수 메타데이터

- 상품 설명 (각 SKU)
- 가격 (국가별. 한국 우선)
- 약관·개인정보처리방침 URL (`https://pairshot.kangkyeonggu.com/privacy` 등)
- 구독 상품을 앱 내 어디서 cancel·manage 할 수 있는지 명시

---

## 9. 테스트 전략

### 9.1 License Testers

Play Console → Setup → License testing → 본인 Google 계정 등록. 등록된 계정으로만 sandbox 결제 가능.

### 9.2 Test Cards (Play Billing Lab 앱)

별도 Play Store 설치. 다음 카드 시나리오 전환 가능:
- Approves
- Declines
- Always approves with slow delay (PENDING 케이스)
- Always declines with slow delay

### 9.3 가속 시간

- 1개월 = 5분
- **14일 trial = 3분**
- 1년 = 30분
- 자동 갱신 최대 6회 후 자동 만료

### 9.4 시나리오 매트릭스 (최소 커버)

| # | 시나리오 | 검증 포인트 |
|---|---|---|
| 1 | 신규 trial 시작 → 14일(가속 3분) 내 cancel | trial 기간 동안 pro, cancel 후 만료 시점에 pro 해제 |
| 2 | Trial → 자동 갱신 첫 결제 성공 | Active 로 전이, isAcknowledged 검증 |
| 3 | Trial → 결제 실패 (declined card) | OnHold 진입, pro 해제, 재결제 prompt |
| 4 | Slow approval (PENDING) | Pending 동안 pro 부여 X, 승인 후 Active |
| 5 | 앱 강제 종료 후 재진입 | `queryPurchasesAsync` 가 정확한 상태 복원 |
| 6 | Trial 중 다른 device 에서 같은 계정 로그인 | 양쪽 자동 활성 (Play 계정 기반) |
| 7 | 구독 활성 상태에서 5개 페어 만들기 | 한도 미적용, 무제한 생성 |
| 8 | Free 사용자가 5개 만든 후 shutter | Paywall sheet 노출 |
| 9 | 구독 cancel 후 갱신 만료 시점 | Active → Inactive 전이, 광고 다시 표시 |
| 10 | 환불 (Play Console manual refund) → 다음 cold-start | `refresh()` 가 Inactive 로 갱신 |

### 9.5 Instrumented test (Hilt + Robolectric 가능)

```
:core:billing/src/androidTest/
├── BillingRepositoryImplTest.kt    — fake BillingClient
├── PurchaseStateMachineTest.kt
└── AcknowledgeWorkerTest.kt
```

`BillingClient` 자체는 mock 어려움 — Google 권장은 **fake repository** 를 정의하고 UI/UseCase 레벨에서 검증, BillingClient 통합은 manual smoke test.

---

## 10. v1.1.6 사용자 마이그레이션

이미 출시된 Android 사용자 → 새 모델 전환 시 정책:

### 10.1 기존 6+ 페어 보유 사용자
**Grandfather** — 기존 페어는 유지. `CanCreatePairUseCase` 가 `< 5` 가 아닌 `< max(5, existingCount)` 체크... 아니, 더 깨끗한 방식:

> 기존 페어는 한도 카운트에 포함하되 *초과 상태에서 새로 만들 수 없게만* 차단. 사용자가 export·삭제로 5 아래로 내리면 정상 동작.

```kotlin
val activeCount = repository.countActive().first()
// 8개 보유한 v1.1.6 유저 → activeCount=8, limit=5 → LimitReached(8, 5)
// 페어 3개 삭제 → activeCount=5 → LimitReached 동일
// 페어 4개 삭제 → activeCount=4 → Allowed
```

기존 페어 강제 삭제 안 함. 강제 시 1성 리뷰 폭격 확실.

### 10.2 기존 쿠폰 AdFree 사용자
영구 grandfather. `CouponAdFreeStatusProvider` 가 그대로 작동 → `CompositeProEntitlementProvider` 가 자동으로 `EntitlementSource.COUPON` 으로 분류. 광고 안 보임 + 페어 무제한. 구독 안 사도 됨.

쿠폰 신규 등록은 deprecate 권장 — Settings 의 쿠폰 등록 버튼을 숨김 또는 "이전 등록 쿠폰만 유효" 안내.

### 10.3 마이그레이션 릴리즈 노트

```
## Added
- Pro 구독 도입 — 14일 무료 체험 후 월/연 결제로 페어 무제한 + 광고 없음
- 페어 5개 동시 보관 한도 (Free) — 기존 보유 페어는 그대로 유지

## Notes
- 쿠폰으로 광고 제거 받으신 분들은 그대로 유지됩니다
- 한도 도달 시 export 후 삭제하거나 Pro 구독으로 해제 가능
```

---

## 11. 단계별 구현 Phase

| Phase | 산출 | DB / Domain / UI |
|---|---|---|
| **0** | 현재 비율 PR 마무리 | 영향 없음 |
| **1** | `:core:billing` 모듈 신설 + `:core:domain/entitlement` 추상화 + `CompositeProEntitlementProvider` | Domain enum + repository skeleton |
| **2** | `BillingRepositoryImpl` 풀 구현 + Singleton lifecycle + Listener wiring | `:core:billing` 완성 |
| **3** | Play Console — 상품·offer·테스트 계정 셋업 (코드 변경 없음) | 외부 |
| **4** | `CanCreatePairUseCase` + `PhotoPairRepository.countActive()` + 카메라 진입 gate | Domain + ViewModel |
| **5** | `:feature:paywall` — Paywall, Restore, ManageSubscription deep link | Compose UI |
| **6** | `AdFreeStatusProvider` Hilt binding 교체 → `EntitlementBackedAdFreeStatusProvider` | 광고 측 코드 무변경 검증 |
| **7** | 쿠폰 등록 UI deprecate (등록 버튼 숨김, 활성 쿠폰만 표시) | Settings 화면 정리 |
| **8** | 마이그레이션 instrumented test (6+ 페어 보유 시나리오) | androidTest |
| **9** | 내부 테스트 트랙 출시 → 14일 trial 흐름 실기기 검증 | Play Console |
| **10** | 프로덕션 staged rollout 5% → 20% → 100% | 출시 |

각 phase 는 별도 PR. Phase 1·2 는 외부 동작 없이 합칠 수 있음 (kill switch 로 entire flow off 가능).

---

## 12. 알려진 함정 / 체크리스트

### Play Billing 흔한 실수

- [ ] `offerToken` 을 잘못 전달 — `(productId, basePlanId, offerId)` 매칭 확인
- [ ] `purchaseState == PENDING` 인데 entitlement 부여 (3일 후 cancel 되면 사용자 광고 차단된 채로 cancel 됨)
- [ ] `acknowledgePurchase` 누락 — 3일 후 자동 환불
- [ ] Cold start 마다 `queryPurchasesAsync` 안 호출 — 다른 device 결제 / 환불 반영 안 됨
- [ ] `BillingClient` 를 ViewModel scope 으로 두어 중복 listener
- [ ] `ProductDetails` 를 메모리 캐시 — 가격 변경 / 사용자별 eligibility 반영 안 됨
- [ ] Test 시 License Tester 미등록 → 실제 결제 발생

### Play 정책 (출시 차단 가능)

- [ ] 약관 / 개인정보처리방침 URL 앱 내 노출
- [ ] 구독 cancel·manage 진입점 앱 내 명시
- [ ] 가격 + 자동갱신 사실 paywall 에 명시
- [ ] "구매 복원" UI 노출 (정책 필수는 아니지만 권장)

### PairShot 특수 항목

- [ ] 쿠폰 grandfather 시나리오 instrumented test
- [ ] 6+ 페어 보유 마이그레이션 시나리오 test
- [ ] `AdFreeStatusProvider` binding 교체 후 모든 광고 placement 재검증 (banner/native/interstitial/app-open)
- [ ] Pair limit reached 시 Paywall 진입 동선 (back stack 처리)
- [ ] Trial 만료 시점 backbone 알림 — `WorkManager` 로 trial 만료 24시간 전 local notification? (선택)

---

## 13. 참고 자료

### Google 공식
- [Play Billing Library release notes](https://developer.android.com/google/play/billing/release-notes) — v8.3.0
- [Integrate the Play Billing Library](https://developer.android.com/google/play/billing/integrate)
- [About subscriptions](https://developer.android.com/google/play/billing/subscriptions)
- [Security and purchase verification](https://developer.android.com/google/play/billing/security)
- [Test Play Billing integration](https://developer.android.com/google/play/billing/test)
- [Create and manage subscriptions (Play Console)](https://support.google.com/googleplay/android-developer/answer/140504)

### 베스트 프랙티스
- [Play Billing 8 migration guide (RevenueCat)](https://www.revenuecat.com/blog/engineering/play-billing-8-migration/)
- [Hilt SDK lifecycle (RevenueCat)](https://www.revenuecat.com/blog/engineering/hilt-sdk-lifecycle/)
- [Play Billing edge cases (RevenueCat)](https://www.revenuecat.com/blog/engineering/google-play-edge-cases/)
- [Test strategy for free trials (Brickit Engineering)](https://medium.com/brickit-engineering/test-strategy-for-free-trials-and-introductory-prices-in-google-play-8e0e6a0fdf41)

### 사내 (이 프로젝트)
- `CLAUDE.md` — 아키텍처 invariants, 커밋 정책
- `docs/git-rules.md` — Model B 브랜치 / cut-release 절차
- `docs/releases/_template.md` — 릴리즈 노트 양식 (v1.2 출시 시 사용)
- `.claude/refs/hilt-patterns.md` — Hilt 모듈 설계 컨벤션
- `.claude/refs/compose-patterns.md` — Compose 상태/UI 설계
- `.claude/refs/room-patterns.md` — Room migration 패턴 (`AspectRatio` 도입 시 참고했던 동일 패턴)

---

## 14. Open Questions (구현 전 확정 필요)

1. **가격 확정** — `pairshot_pro_monthly` / `_yearly` 의 KRW 가격
2. **Trial 부여 범위** — yearly 만 vs monthly + yearly 양쪽
3. **첫 실행 paywall** — 첫 실행 시 자동 노출 vs 사용자가 한도 도달해야 노출 (후자가 conversion 낮지만 리뷰 우호적)
4. **`RewardedGateDialog` 운명** — 제거 (모든 premium 은 구독으로) vs 유지 (free 사용자 광고 시청 후 임시 unlock)
5. **쿠폰 등록 UI** — 완전 숨김 vs "이전 쿠폰만 유효" 안내 노출

이 5가지가 정해지면 Phase 1 부터 PR 단위로 작업 가능합니다.
