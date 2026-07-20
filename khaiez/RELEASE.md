# Phát hành

## Chuẩn bị

1. Cập nhật `versionCode`, `versionName` và `CHANGELOG.md` (chuyển mục *Chưa phát hành* thành version mới nếu ship).
2. Chạy test, build debug và kiểm thử thiết bị theo [TESTING.md](TESTING.md) (ít nhất **smoke 5 phút** + regression Telex trên một máy thật; Vivo nếu có thay đổi composing/Backspace).
3. Chuẩn bị bộ ký. Cấu hình Gradle nhận bốn biến môi trường:
   - `KAI_RELEASE_STORE_FILE`
   - `KAI_RELEASE_STORE_PASSWORD`
   - `KAI_RELEASE_KEY_ALIAS`
   - `KAI_RELEASE_KEY_PASSWORD`
   Máy phát triển hiện lưu bộ ký gốc cục bộ tại `Res/kai-board-release.jks` và `Res/signing-secret.xml`. XML là `PSCredential` được DPAPI bảo vệ, chỉ giải mã được với đúng tài khoản/máy Windows đã tạo. Cả hai loại file đã bị `.gitignore` loại khỏi Git.

   `build-release.cmd` ưu tiên hai file đang hoạt động trong `Res/`; nếu không có thì mới đọc `%USERPROFILE%\.kai-board\`. Không đặt hai bộ ký khác nhau ở hai nơi nếu chưa đối chiếu certificate fingerprint.
4. Build release:

   ```powershell
   .\gradlew.bat testDebugUnitTest assembleRelease lintRelease
   # hoặc, sau khi đã chuẩn bị %USERPROFILE%\.kai-board\
   .\build-release.cmd
   ```

5. Xác minh bằng `apksigner verify --verbose --print-certs`, kiểm tra package/version/ABI bằng `aapt2 dump badging` và lưu SHA-256.
6. APK hiện phát hành cho `arm64-v8a`, bật R8, resource shrinking và nén thư viện ML Kit trong APK.

## Quy tắc

- Không commit keystore hoặc mật khẩu.
- Lưu keystore ở nơi được sao lưu an toàn; mất keystore có thể chặn cập nhật ứng dụng.
- Không thay keystore sau khi đã phân phối APK. APK dùng chữ ký mới không cập nhật đè được bản ký cũ.
- Khi tạo lại `signing-secret.xml`, phải dùng đúng tài khoản Windows sẽ build release; luôn thử `Import-Clixml` ở tiến trình mới.
- Xác minh chính sách riêng tư, third-party notices và listing trước khi tải lên store.
- Phát hành theo staged rollout; giữ APK/AAB, mapping và commit tương ứng.

## Artifact hiện tại

- Package: `vn.kai.board`.
- Version: `1.2.0` (`versionCode 120`).
- ABI: `arm64-v8a`.
- Tên artifact: `KAI-Board-1.2.0.apk`; mọi bản sau phải kèm `versionName` trong tên file.

