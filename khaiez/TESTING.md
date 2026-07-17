# Kiểm thử

## Tự động

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Test cần bao phủ Telex, input policy, cursor/recomposition, selection deletion, touch dispatcher, repeat, gợi ý, voice routing, AI key pool/cancellation và sao lưu không chứa secret.

## Thiết bị

- Cold/warm show IME; gõ thường và Telex nhanh.
- Backspace bấm/giữ, selection, con trỏ giữa từ, long-press và slide.
- AI: thành công, hủy, timeout, 401/403, 429, 5xx, offline và nhiều key.
- Micro: bình thường, hủy, im lặng, thiếu dịch vụ và ngôn ngữ Việt/Anh.
- Dịch: model có/không có, swap ngôn ngữ và offline.
- Giao diện: sáng/tối, one-hand, xoay, font lớn và insets.

Ứng dụng mục tiêu: Samsung Notes, Chrome, Zalo, Messenger, email và ô mật khẩu. Không chụp/log nội dung nhạy cảm.

