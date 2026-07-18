# KAI Board

KAI Board là bàn phím Android viết bằng Kotlin, tập trung vào cảm giác bấm nhanh, chính xác và xử lý Telex tiếng Việt tự nhiên.

## Tính năng chính

- Gõ Telex kiểu UniKey, tự sửa, viết hoa đầu câu và gợi ý từ/cụm từ cá nhân.
- Học cục bộ từ, email và hashtag thường dùng theo tần suất.
- Hàng số, ký hiệu giữ phím, emoji, clipboard và ghi chú.
- Chế độ một tay, preset kích thước/vị trí và kéo chỉnh trực tiếp trên bàn phím.
- Dịch offline theo từng model ngôn ngữ bằng ML Kit.
- Mic AI/Dịch hiển thị lời nói theo thời gian thực, hỗ trợ tạm dừng, sửa và dịch tự động.
- KAI AI hỗ trợ nhiều nhà cung cấp, nhiều API key, quét model và thống kê trạng thái từng key.
- Theme sáng, tối, theo hệ thống với màu nhấn mint đồng bộ.
- Cài đặt dùng nhóm button nhỏ thay dropdown; ô thử gõ sticky không tự mở bàn phím.

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

Build release tối ưu R8/resource shrinking sau khi cấu hình bộ ký theo `khaiez/RELEASE.md`:

```powershell
.\build-release.cmd
```

API key không nằm trong source hoặc file sao lưu. Bộ ký release cục bộ nằm trong `Res/`, đã bị `.gitignore` loại khỏi Git; xem `khaiez/RELEASE.md` trước khi phát hành.
