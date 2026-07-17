# KAI Board

KAI Board là bàn phím Android viết bằng Kotlin, tập trung vào cảm giác bấm nhanh, chính xác và xử lý Telex tiếng Việt tự nhiên.

## Tính năng chính

- Gõ Telex tiếng Việt, tự sửa và gợi ý từ/cụm từ cá nhân.
- Hàng số, ký hiệu giữ phím, emoji, clipboard và ghi chú.
- Chế độ một tay và điều chỉnh trực tiếp kích thước/vị trí bàn phím.
- Dịch offline theo từng model ngôn ngữ bằng ML Kit.
- KAI AI hỗ trợ nhiều nhà cung cấp API; khóa API chỉ được lưu cục bộ trên thiết bị.
- Theme sáng, tối, theo hệ thống và Gradient AI.

## Yêu cầu

- Android Studio hoặc JDK 17.
- Android SDK với `compileSdk 37`.
- Android 8.0 (API 26) trở lên.

## Build và kiểm thử

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Cài nhanh bản debug lên thiết bị đang kết nối ADB:

```powershell
.\dev-install.cmd -WithTests
```

Khóa ký release và API key không nằm trong repository.
