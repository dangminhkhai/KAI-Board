---
name: kai-board-continue
description: >
  Continue or resume KAI Board work safely after switching machines, pulling main,
  or finishing a feature — pull, shared debug keystore, build/install, and mandatory
  IME regression smoke so old fixes (panels, Settings IME, clipboard, Telex) do not
  break when changing other code. Use when the user says đổi máy, tiếp tục, resume,
  handoff, regression, smoke sau cài, /kai-board-continue, or asks how to keep
  previous fixes working across PCs.
---

# KAI Board — continue / switch machine / anti-regression

Project skill (lives in repo). After `git pull` on any PC, Grok loads this with the project.

## When invoked

Do **all** of the following that apply — do not only implement the new feature.

1. **Orient** from `khaiez/HANDOFF.md` (current HEAD, signed keystore, last session, Next).
2. **Sync & verify toolchain** (switch machine / first message of the day).
3. **Before claiming done:** unit tests + device smoke from [references/smoke-ime.md](references/smoke-ime.md).
4. **After a bugfix:** add or tick a regression line (test or smoke item) so it cannot silently die.

## 1. Switch machine / first open of day

```powershell
cd <repo-root>
git pull origin main
git status -sb
git log -3 --oneline
```

Confirm shared debug signing exists:

- File: `keystore/android-debug.keystore`
- Gradle: `app/build.gradle.kts` → `signingConfigs.sharedDebug`
- Doc: `keystore/README.md`  
  Alias `androiddebugkey` / password `android`.  
  **Do not** rely on `~/.android/debug.keystore` for device installs.

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug
adb devices -l
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
adb shell am force-stop vn.kai.board
```

If `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: uninstall once then reinstall (data loss), or ensure shared keystore was used on the build.

If `INSTALL_FAILED_VERSION_DOWNGRADE`: `adb install -r -d` or bump `versionCode` intentionally.

## 2. Guardrails (do not re-break)

These are **known regressions**. Any edit to `KeyboardView`, `KaiBoardImeService`, Settings, or clipboard must re-check them.

| ID | Rule | Where |
| --- | --- | --- |
| G1 | Hide IME then show again → **letter keyboard (ABC)**, not emoji/AI/dịch/mic/clipboard/?123 | `resetToLetterKeyboard()` on `onStartInputView` + `onFinishInputView` |
| G2 | Open Settings → **do not** auto `requestFocus` / show IME on test field | `MainActivity` |
| G3 | Clipboard long-press popup: **UP after long-press does not dismiss** (`suppressClipboardPopupUp`) | `KeyboardView` |
| G4 | Delete/clear clipboard must **not** resurrect system clip on next panel open (suppress fingerprint) | `ClipboardHistoryStore` |
| G5 | Image paste on URL/search/password → soft toast only, **no** `setPrimaryClip` FileProvider | `KaiBoardImeService` |
| G6 | Clipboard draw must **restore** `textPaint` size (or reset each `onDraw`) | `KeyboardView` |
| G7 | Private password session: no AI/clipboard/mic smartbar | `KeyboardView` / privacy policy |
| G8 | Telex BS OEM (Vivo Safe/Cafe): never delete-all + short `commitText` for tone shrink | `KaiBoardImeService` / ARCHITECTURE |
| G9 | Debug APK always signed with **shared** keystore | `app/build.gradle.kts` |

If a change touches these areas and smoke is skipped, **say so explicitly** in the reply.

## 3. Implementation workflow

1. Read relevant slice of `khaiez/HANDOFF.md` + this skill’s guardrails.
2. For Telex/composing/BS: also `khaiez/ARCHITECTURE.md`.
3. Prefer **small commits**; do not mix unrelated IME modes in one huge diff without smoke.
4. Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

5. Install arm64 on device; run **IME smoke** in [references/smoke-ime.md](references/smoke-ime.md).
6. If user asked for docs/push: update `khaiez/CHANGELOG.md` / `HANDOFF.md` / `TESTING.md` as needed, then commit + push.

## 4. After fixing a regression

1. Prefer a **unit test** if logic is pure (store, policy, classifier).
2. Else add a **numbered row** under `khaiez/TESTING.md` (clipboard 7.x, IME 6.x, settings 9.x).
3. Mention the guardrail ID (G1–G9) in the commit message or HANDOFF “Next” if still flaky.

## 5. What not to do

- Do not commit real release keystores or API keys.
- Do not log typed text, clipboard contents, or secrets.
- Do not “fix A by rewriting half of KeyboardView” without smoke G1–G6.
- Do not use machine-local `debug.keystore` for install instructions in docs.

## 6. Quick user prompts this skill owns

- “đổi máy / pull / tiếp tục KAI Board”
- “cài adb / install debug”
- “đừng để tính năng cũ lỗi lại”
- “smoke / checklist sau khi sửa”
- “chữ ký debug / INSTALL_FAILED_UPDATE”

## References

- [references/smoke-ime.md](references/smoke-ime.md) — mandatory short device smoke  
- Repo: `khaiez/HANDOFF.md`, `khaiez/TESTING.md`, `khaiez/CHANGELOG.md`, `keystore/README.md`
