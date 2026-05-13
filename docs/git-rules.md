# PairShot Git 운영 규칙

이 문서는 PairShot Android 프로젝트의 git 브랜치 · 태그 · 릴리즈 운영 표준을 정의합니다. 모든 인간 협업자와 AI 에이전트(특히 `/cut-release` Skill) 가 이 규칙을 따라 작업합니다.

---

## 1. 브랜치 전략 (Model B)

### 핵심 모델

```
feature/<name>  ──► PR ──► release/v1.x   (CI 검증 → 머지)
                              │
                              └──► main 은 ff 로 따라감 (별도 PR X)
```

- **`release/v1.x`** = v1.x 라인의 통합 브랜치. 새 commit 은 모두 여기로 PR 머지됨.
- **`main`** = `release/v1.x` 의 미러. 자동 ff 만 받음. 직접 push 금지.
- **`feature/<name>`** = 작업 브랜치. 머지 후 삭제.

### PR target 규칙

| 작업 종류 | PR base |
|---|---|
| 일반 기능 / 버그 수정 / 리팩토링 / 핫픽스 | **`release/v1.x`** |
| v2.x 도입 시 | `release/v2.x` (별도 라인) |

`--base main` 으로 PR 만들지 말 것. 어떤 경우에도 안 함.

### 브랜치 명명

- `feature/<짧은-주제>` — 신규 기능
- `fix/<짧은-주제>` — 버그 수정
- `refactor/<짧은-주제>` — 리팩토링
- `chore/<짧은-주제>` — 인프라·설정

### 정리

PR 머지 후 source 브랜치는 즉시 삭제 — GitHub UI 의 "Delete branch" 또는:
```bash
git branch -d <branch>
git push origin --delete <branch>
```

---

## 2. 태그 운용

### 형식

`v<major>.<minor>.<patch>` — semver. 예: `v1.1.6`.

### 언제 만드나

`/cut-release v<version>` Skill 이 release notes commit 에 annotated tag 를 자동 생성. 사람·AI 가 수동으로 태깅하지 않음.

### 불변성

- 태그는 한 번 생성 후 **절대 이동·삭제·덮어쓰기 금지**
- v1.1.6 출시 후 v1.1.6 의 의미가 영구히 그 commit
- 핫픽스 필요 시 다음 버전 (v1.1.7) 으로 진행

### Push

```bash
git push origin v<version>
```

태그 push 도 사용자 명시 지시 후에만 (memory `feedback_no_autonomous_push.md`).

---

## 3. 릴리즈 운용

### GitHub Release

매 버전 출시 시 GitHub Releases 페이지에 등록:

```bash
gh release create v<version> \
  --title "v<version>" \
  --notes-file docs/releases/v<version>.md \
  --latest
```

- `--latest`: "Latest" 배지 부착 (가장 최근 출시 1 개)
- `--notes-file`: `docs/releases/v<version>.md` 본문이 그대로 release notes 가 됨

### Play Store 업로드

- AAB 빌드: `./gradlew :app:bundleRelease` (로컬, 키스토어 보유)
- AAB 위치: `app/build/outputs/bundle/release/app-release.aab`
- Play Console UI 에서 직접 업로드 (자동화 안 함)
- Staged rollout 권장: 5% → 20% → 100%

### Release notes 위치

`docs/releases/v<version>.md` — 양식의 단일 진실 소스는 **`docs/releases/_template.md`**. 모든 신규 릴리즈는 이 템플릿을 그대로 복사해서 시작.

섹션 구조 (Keep-a-Changelog 기반):
- 헤더: `# v<version> — YYYY-MM-DD`, `Build` / `Range` / `Compare` 메타
- `## Highlights` — 2~3 줄 요약. README 표 "주요 내용" 칼럼에도 동일 첫 항목
- `## Added` (feat)
- `## Changed` (UX 영향 있는 refactor / 동작 변경)
- `## Fixed` (fix)
- `## Removed` (기능 제거. breaking 명시)
- `## Internal` (chore / refactor / perf / build / test / docs)
- `## Compatibility` — Android API 레벨, 디바이스, Room migration
- `## Known Issues` — 없으면 `None`
- `## Commits` — `git log <prev>..HEAD --format='- %h %s'` 결과

본문 언어: 한국어. 섹션 헤더는 영어 그대로 유지.

빈 버킷(Added/Changed/Fixed/Removed/Internal)은 섹션 전체 생략. 빈 헤더 남기지 말 것.

자동화: `/cut-release` Skill 이 `_template.md` 를 읽어 placeholder 를 채움. 사용자는 작성된 초안을 검토·수정.

`README.md` 의 표에도 한 줄 추가 (버전 / 날짜 / `## Highlights` 첫 줄 압축).

기존 v1.0.0 ~ v1.1.6 의 release notes 는 구 양식(사용자 체감 / 적용 화면 / 내부 변경 / 참고) 으로 작성되어 있으며 그대로 보존 — 신규 양식은 다음 버전(v1.1.7+) 부터 적용.

---

## 4. 새 버전 릴리즈 절차

### 4.1 작업 단계

1. `release/v1.x` 에서 feature 브랜치 분기:
   ```bash
   git checkout release/v1.x
   git pull
   git checkout -b feature/<name>
   ```

2. 작업 + commit (Korean conventional commits, no Co-authored-by):
   ```
   feat(scope): "한국어 설명"
   fix(scope): "한국어 설명"
   refactor(scope): "한국어 설명"
   ```

3. push 후 PR 생성:
   ```bash
   git push -u origin feature/<name>
   gh pr create --base release/v1.x --head feature/<name>
   ```

4. CI 통과 확인 후 머지 (GitHub UI).

5. 로컬 정리:
   ```bash
   git checkout release/v1.x
   git pull
   git branch -d feature/<name>
   git push origin --delete feature/<name>
   ```

### 4.2 릴리즈 단계 (`/cut-release` Skill)

작업이 모여 새 버전 출시 시점:

1. **`/cut-release v<version>`** Skill 호출. 다음 HARD GATE 자동 검증 (모두 통과해야 진행):

   | # | 검사 | 도구 |
   |---|---|---|
   | 1 | Connected device (emulator) 가용 | `adb devices` |
   | 2 | Migration coverage (정적) | grep `MIGRATION_X_Y` constants vs MigrationTest |
   | 3 | JVM functional tests | `./gradlew testDebugUnitTest` (ArchUnit + 일반 unit) |
   | 4 | Instrumented migration tests | `./gradlew :core:database:connectedDebugAndroidTest` |
   | 5 | Release assembly (R8 / signing) | `./gradlew :app:assembleRelease` |
   | 6 | Co-authored-by 검사 | `git log <prev>..HEAD` 의 메시지 본문 grep |

2. 통과 시 Skill 이:
   - `docs/releases/v<version>.md` 초안 작성
   - `README.md` 표에 한 줄 추가
   - 사용자 검토 대기

3. 사용자 검토·승인 후 Skill 이:
   - docs(release) commit 생성 (`docs/releases/v<version>.md` + `README.md` 함께)
   - annotated tag `v<version>` 생성

4. **Skill 종료** — 이후는 사용자 명시 지시.

### 4.3 출시 단계 (사용자 지시)

1. push (작업 브랜치 + tag):
   ```bash
   git push origin feature/<name>
   git push origin v<version>
   ```

2. PR (`feature/<name>` → `release/v1.x`) 생성, CI 통과 후 머지.

3. main ff:
   ```bash
   git checkout main
   git pull
   git merge --ff-only origin/release/v1.x
   git push origin main
   ```

4. GitHub Release 등록:
   ```bash
   gh release create v<version> --title "v<version>" \
     --notes-file docs/releases/v<version>.md --latest
   ```

5. AAB 빌드:
   ```bash
   ./gradlew :app:bundleRelease
   ```

6. Play Console 업로드 (사용자 직접).

---

## 5. CI 게이트 (`.github/workflows/ci.yml`)

### 트리거

`pull_request` 만 (push 트리거 없음).

→ PR 시점에 1 회 검증. 머지 후 main / release/v1.x ff push 시 CI 안 돎 (중복 차단).

### Job 구성

- **`jvm-tests`**: `testDebugUnitTest` (~3–5분, ArchUnit 포함)
- **`migration-test`**: `:core:database:connectedDebugAndroidTest` 위에서 emulator 부팅 후 실행 (~10–15분)

### 실패 시

PR 의 status check 가 빨간불. 머지 버튼 비활성 (Branch Protection 설정 시).

### Branch Protection (GitHub UI 설정 필요)

Settings → Branches → main / release/v1.x:
- Require pull request before merging
- Require status checks to pass (jvm-tests, migration-test)
- Require branches to be up to date

---

## 6. 핫픽스 절차

이미 출시된 v1.x.y 에서 긴급 수정 필요 시:

1. `release/v1.x` 에서 핫픽스 브랜치 분기:
   ```bash
   git checkout release/v1.x
   git pull
   git checkout -b fix/<urgent-issue>
   ```

2. 수정 + commit + push + PR (target = release/v1.x).

3. CI 통과 → 머지.

4. `/cut-release v1.x.<y+1>` 으로 패치 버전 출시.

5. 위 4.3 의 출시 단계 반복.

---

## 7. 절대 하지 말아야 할 것

- ❌ `main` 직접 push 또는 `--base main` PR
- ❌ 태그 이동·삭제·덮어쓰기 (`git tag -f`, `git tag -d` + 재생성)
- ❌ Force push to `main` 또는 `release/v1.x`
- ❌ Co-authored-by 트레일러를 commit 메시지에 포함
- ❌ `--no-verify` 로 hook 우회
- ❌ 사용자 명시 지시 전 push / merge / GitHub Release / AAB 빌드

---

## 8. 참고 메모리·문서

- `.claude/skills/cut-release/SKILL.md` — 릴리즈 자동화 절차
- `~/.claude/projects/.../memory/feedback_pr_branching_model.md` — Model B 정책
- `~/.claude/projects/.../memory/feedback_no_autonomous_push.md` — 자동 push 금지
- `~/.claude/projects/.../memory/feedback_no_coauthored.md` — Co-authored-by 금지
- `~/.claude/projects/.../memory/feedback_commit_format.md` — Commit 메시지 형식
- `CLAUDE.md` — Commit policy 섹션
