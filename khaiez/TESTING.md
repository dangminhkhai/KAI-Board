# Kiểm thử

## Tự động

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Test cần bao phủ đủ `s f r x j`, `dd`, `aa ee oo`, `aw ow uw`; modifier đúng/sai thứ tự; hoàn tác kiểu UniKey; khôi phục chuỗi phím gốc cho từ Latin/Anh; hoa/thường của phím modifier; con trỏ giữa composing; viết hoa đầu ô/đầu dòng nhưng không sau dấu câu; hashtag/email, input policy, selection deletion, touch dispatcher, repeat, gợi ý, voice routing, AI key pool/cancellation và sao lưu không chứa secret.

## Thiết bị

- Cold/warm show IME; gõ thường và Telex nhanh.
- Backspace bấm/giữ, selection, con trỏ giữa từ, long-press và slide; xóa emoji có variation selector, skin tone, ZWJ và cờ mà không để lại ô vuông.
- AI: thành công, hủy, timeout, 401/403, 429, 5xx, offline và nhiều key; xác minh nhận diện provider, fallback theo đúng thứ tự key đã lưu và `400` phải dừng.
- Gợi ý: thứ tự AI/cá nhân/offline, decay, nhận diện Việt/Anh, tải/xóa từng gói từ điển, tìm emoji Việt không dấu/Anh và fallback về dữ liệu cá nhân.
- Clipboard: phân loại OTP/email/URL/số điện thoại, chèn đúng phần được tách, không nhầm số thường thành OTP, ghim và tự hết hạn 1 giờ/1 ngày.
- Cử chỉ: vuốt Space trái/phải, Shift giữ/chạm đôi, Backspace bấm/giữ tăng tốc và không chèn nhầm ký tự khi kết thúc gesture.
- Quản lý API: ô key không tự điền, chỉ lưu key quét thành công, thống kê đúng từng key, xóa kèm xác nhận; chạm giữ card để kéo, card nổi/viền accent, card khác dịch chuyển, số thứ tự cập nhật và thứ tự chỉ lưu khi thả.
- Micro: bình thường, text trực tiếp, tạm dừng/tiếp tục, Backspace, hủy, im lặng, thiếu dịch vụ và ngôn ngữ Việt/Anh.
- Dịch: model có/không có, swap ngôn ngữ và offline.
- Giao diện: sáng/tối/theo hệ thống, Mặc định/Tùy chỉnh, gallery theme hai cột cùng chiều cao, preview đúng tỷ lệ, nhập/HTTPS/áp dụng/xuất/chia sẻ/xóa theme, card từ cá nhân trên màn hình hẹp, preset geometry, sticky input, one-hand, xoay, font lớn và insets.
- Sao lưu: khôi phục Theme Extensions và thứ tự Smartbar; xác nhận không chứa API key, clipboard hoặc ghi chú.
- Release: `assembleRelease`, `lintRelease`, R8/resource shrinking, chữ ký v2/v3, package/version/ABI và SHA-256.

Ứng dụng mục tiêu: Samsung Notes, Chrome, Zalo, Messenger, email và ô mật khẩu. Không chụp/log nội dung nhạy cảm.

