# Kiểm thử

## Tự động

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Test cần bao phủ Telex/hoàn tác kiểu UniKey, viết hoa đầu ô/đầu dòng nhưng không sau dấu câu, hashtag/email, input policy, cursor/recomposition, selection deletion, touch dispatcher, repeat, gợi ý, voice routing, AI key pool/cancellation và sao lưu không chứa secret.

## Thiết bị

- Cold/warm show IME; gõ thường và Telex nhanh.
- Backspace bấm/giữ, selection, con trỏ giữa từ, long-press và slide.
- AI: thành công, hủy, timeout, 401/403, 429, 5xx, offline và nhiều key; xác minh thứ tự fallback model → key cùng provider → provider khác, đồng thời `400` phải dừng.
- Quản lý API: ô key không tự điền, chỉ lưu key quét thành công, thống kê đúng từng key, xóa kèm xác nhận và animation dừng khi rời trang.
- Micro: bình thường, text trực tiếp, tạm dừng/tiếp tục, Backspace, hủy, im lặng, thiếu dịch vụ và ngôn ngữ Việt/Anh.
- Dịch: model có/không có, swap ngôn ngữ và offline.
- Giao diện: sáng/tối/theo hệ thống, nhóm button lựa chọn, preset/tùy chỉnh geometry, sticky input, one-hand, xoay, font lớn và insets.
- Release: `assembleRelease`, `lintRelease`, R8/resource shrinking, chữ ký v2/v3, package/version/ABI và SHA-256.

Ứng dụng mục tiêu: Samsung Notes, Chrome, Zalo, Messenger, email và ô mật khẩu. Không chụp/log nội dung nhạy cảm.

