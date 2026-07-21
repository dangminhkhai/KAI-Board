# Shared debug keystore

Dùng chung **một** key debug trên mọi máy → `adb install -r` không bị `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

| | |
| --- | --- |
| File | `android-debug.keystore` |
| Alias | `androiddebugkey` |
| Store / key password | `android` |
| Gradle | `app/build.gradle.kts` → `signingConfigs.sharedDebug` |

## Máy mới

```powershell
git pull origin main
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

Không cần copy `~/.android/debug.keystore` — project tự ký bằng file trong thư mục này.

## Lưu ý

- **Chỉ debug / cài tay.** Không dùng key này cho Google Play.
- Ai clone repo cũng ký cùng cert (chấp nhận được vì debug-only).
- Release thật sau này: tạo keystore riêng, **không** commit, backup offline.
