# KAI Board

KAI Board là bàn phím Android viết bằng Kotlin, tập trung vào cảm giác bấm nhanh, chính xác và xử lý Telex tiếng Việt tự nhiên.

## Tính năng chính

- Gõ Telex kiểu UniKey trong cả ô URL, tự sửa, viết hoa khi bắt đầu nhập và gợi ý từ/cụm từ cá nhân; dấu câu không tự thêm Space hoặc bật Shift.
- Gợi ý ưu tiên dữ liệu AI, từ điển cá nhân có decay 14 ngày rồi từ điển Việt/Anh tải tùy chọn; tự nhận biết ngôn ngữ bằng luật nhẹ, offline.
- Học cục bộ từ, email, hashtag và câu lệnh AI thường dùng.
- Hàng số, ký hiệu giữ phím, tìm kiếm emoji, clipboard và ghi chú.
- Vuốt Space để di chuyển con trỏ; giữ Backspace để xóa tăng tốc; giữ hoặc chạm hai lần Shift để khóa viết hoa.
- Chế độ một tay, preset kích thước/vị trí và kéo chỉnh trực tiếp trên bàn phím.
- Dịch offline theo từng model ngôn ngữ bằng ML Kit.
- Mic AI/Dịch hiển thị lời nói theo thời gian thực, hỗ trợ tạm dừng, sửa và dịch tự động.
- KAI AI hỗ trợ DS2API (`khaids-`), OpenRouter, Gemini, OpenAI, Groq và NVIDIA NIM; nhiều API key, quét model, thống kê trạng thái từng key và fallback theo thứ tự key do người dùng kéo thả khi lỗi/quota.
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
