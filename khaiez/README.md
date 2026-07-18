# KAI Board

KAI Board là bàn phím Android viết bằng Kotlin, ưu tiên cảm giác gõ nhanh, chính xác, riêng tư và ổn định. Ứng dụng hỗ trợ Telex tiếng Việt trong cả ô URL, từ điển Việt/Anh offline tùy chọn, từ điển cá nhân có decay, gợi ý câu lệnh AI tự học, tìm kiếm emoji, vuốt Space điều khiển con trỏ, clipboard, dịch ML Kit, nhập giọng nói và trợ lý AI nhiều nhà cung cấp gồm DS2API. Cài đặt có sticky input, nhóm button lựa chọn nhanh, preset hình học bàn phím và kéo thả thứ tự ưu tiên API key.

## Yêu cầu

- JDK 17.
- Android SDK; `compileSdk 37`, `targetSdk 35`, `minSdk 26`.
- Thiết bị hoặc giả lập Android; APK hiện tối ưu cho `arm64-v8a`.

## Build

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK debug nằm trong `app/build/outputs/apk/debug/`. Cài nhanh bằng `dev-install.cmd` hoặc ADB. Không đặt API key trong source, tài nguyên hay lệnh được chia sẻ.

Release dùng `build-release.cmd`, ABI `arm64-v8a`, R8 và resource shrinking. Keystore/credential cục bộ trong `Res/` không được commit; chi tiết ở [Phát hành](RELEASE.md).

## Tài liệu

- [Kiến trúc](ARCHITECTURE.md)
- [Kiểm thử](TESTING.md)
- [Riêng tư](PRIVACY.md)
- [Phát hành](RELEASE.md)
- [Lộ trình](ROADMAP.md)
- [Bộ skill](SKILLS.md)

