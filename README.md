<div align="center">

<img src="./docs/readme/ic_readme.png" alt="PairShot" width="120" style="border-radius: 24px;" />

# PairShot

**Before·After 촬영 및 관리 애플리케이션**

**[🌐 웹사이트 바로가기](https://pairshot.kangkyeonggu.com)** &nbsp;·&nbsp; **[<img src="https://cdn.simpleicons.org/googleplay/34A853" height="14" alt="Google Play"/> 다운로드](https://play.google.com/store/apps/details?id=com.pairshot&pcampaignid=web_share)**

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM_2026.03-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-1.6.0-34A853?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/training/camerax)
[![License](https://img.shields.io/badge/License-Private-red?style=flat-square)]()

<br/>

</div>

<br/>

---

## 목차

- [개요](#개요)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [주요 기능](#주요-기능)
- [릴리즈 노트](#릴리즈-노트)

---

## 개요

PairShot은 Before·After 사진 촬영·관리·합성·워터마크 삽입·내보내기 등 관리 편의기능을 제공하는 Android 애플리케이션입니다. 각 Before·After 촬영본 페어를 자동으로 묶어 관리하며, 오버레이 가이드를 통한 손쉬운 After 촬영 지원, 합성·워터마크·압축파일 내보내기까지 전·후 사진 촬영에 필요한 전체 워크플로우를 제공합니다.


<br/>

---

## 기술 스택

**`Core`**

![Kotlin](https://img.shields.io/badge/Kotlin_2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Compose_BOM_2026.03-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

**`Camera & Image`**

![CameraX](https://img.shields.io/badge/CameraX_1.6.0-34A853?style=for-the-badge&logo=android&logoColor=white)
![Glide](https://img.shields.io/badge/Glide_4.16.0-2C9B44?style=for-the-badge&logoColor=white)

**`Data & DI`**

![Room](https://img.shields.io/badge/Room_2.8.4-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt_2.59.2-34A853?style=for-the-badge&logo=android&logoColor=white)
![DataStore](https://img.shields.io/badge/DataStore-4285F4?style=for-the-badge&logo=android&logoColor=white)

**`Navigation & Serialization`**

![Navigation Compose](https://img.shields.io/badge/Navigation_2.9.0-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![kotlinx.serialization](https://img.shields.io/badge/kotlinx.serialization-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)

**`Build & Quality`**

![AGP](https://img.shields.io/badge/AGP_9.1.0-34A853?style=for-the-badge&logo=gradle&logoColor=white)
![KSP](https://img.shields.io/badge/KSP_2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Detekt](https://img.shields.io/badge/Detekt-EC5B56?style=for-the-badge&logoColor=white)

<br/>

---

## 주요 기능

### `01` · 촬영

<table>
<tr>
<td width="50%" valign="top">

#### 프로젝트 기반 촬영
생성한 각 프로젝트 별 Before·After 페어를 자동으로 분류합니다. After 사진 촬영 시 기존 Before 사진과의 자동 페어링 기능을 제공합니다.

</td>
<td width="50%" valign="top">

#### 오버레이 가이드
After 촬영 시 Before 사진을 반투명 오버레이로 뷰파인더에 표시합니다. 동일한 구도·앵글을 손쉽게 재현할 수 있습니다.

</td>
</tr>
</table>

<br/>

---

### `02` · 관리 및 내보내기

<table>
<tr>
<td width="50%" valign="top">

#### 페어 카드 관리
프로젝트별 갤러리에서 Before·After 페어 카드를 관리할 수 있습니다. 비교 뷰 미리보기, 프로젝트 이름 변경·삭제 등 기본 관리 기능을 제공합니다.

</td>
<td width="50%" valign="top">

#### 이미지 합성
선택한 Before·After 페어 원본 비트맵을 합성하여 단일 비교 이미지를 생성합니다. 또한, 합성 이미지에 적용할 테두리 및 레이블 사용자 커스텀 설정을 지원합니다.

</td>
</tr>
<tr>
<td valign="top">

#### 워터마크 자동 삽입
이미지 텍스트·로고 워터마크 삽입 기능을 지원합니다. 텍스트·로고 설정, 위치·크기 커스터마이징을 통해 원하는 워터마크를 자유롭게 삽입할 수 있습니다. 

</td>
<td valign="top">

#### 내보내기 및 공유
개별 이미지 갤러리 저장·압축 파일 생성 기능을 제공합니다. Before·After·합성본 원본 또는 워터마크 삽입 버전을 자유롭게 선택하여 기기, 메신저앱, 공유드라이브 등 원하는 곳으로 즉시 전송할 수 있습니다.

</td>
</tr>
</table>

<br/>

---

## 릴리즈 노트

| 버전 | 날짜 | 주요 내용 |
|------|------|-----------|
| [v1.3.0](./docs/releases/v1.3.0.md) | 2026-05-23 | 합성 레이블 "테두리 내부" 모드 신설 — 이미지 가리지 않고 BEFORE/AFTER 각각 6 칸 자유 배치 · AFTER 촬영 중 BEFORE 길게 누르기 미리보기 · PRO 구독 옵션 진입점 + paywall 자동 닫힘 방지 |
| [v1.2.2](./docs/releases/v1.2.2.md) | 2026-05-17 | 튜토리얼 UX 다듬기 — 프리뷰 가시성 · 흰색 spotlight 테두리 · AFTER 스트립 안내 · 내보내기 재진입 아이콘 · 마지막 모달 중앙 배치 · 문구 일관화 |
| [v1.2.1](./docs/releases/v1.2.1.md) | 2026-05-17 | v1.2.0 과 코드 동일 · Play Console 재업로드용 versionCode bump |
| [v1.2.0](./docs/releases/v1.2.0.md) | 2026-05-17 | 인터랙티브 튜토리얼 + Google Play 구독·프로모션 통합 + 카메라 비율·내보내기 파이프라인 보강 |
| [v1.1.6](./docs/releases/v1.1.6.md) | 2026-04-29 | v1.1.5 업데이트 크래시 핫픽스 + 마이그레이션 회귀 자동 차단 |
| [v1.1.3](./docs/releases/v1.1.3.md) | 2026-04-25 | AdMob App Open·Native·Rewarded 프로덕션 ID 교체 |
| [v1.1.2](./docs/releases/v1.1.2.md) | 2026-04-25 | 쿠폰 사용자의 리워드 다이얼로그 노출 제거 |
| [v1.1.1](./docs/releases/v1.1.1.md) | 2026-04-25 | QR 스캔 R8 minify 크래시 핫픽스 |
| [v1.1.0](./docs/releases/v1.1.0.md) | 2026-04-24 | 쿠폰 시스템 · 광고 통합·배치 · 카메라 회전 가이드 (내부 릴리스) |
| [v1.0.0](./docs/releases/v1.0.0.md) | 2026-04-24 | 최초 출시 |

<br/>

---

<div align="center">

© 2026 NomadLabs. All rights reserved.

</div>
