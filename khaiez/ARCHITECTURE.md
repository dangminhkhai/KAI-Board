# Kiến trúc

## Thành phần chính

- `ime/KaiBoardImeService`: vòng đời IME, composing text, Telex, AI, dịch và voice routing.
- `ui/KeyboardView`: vẽ bàn phím, geometry và chuyển action; không chứa mạng hay model.
- `touch/`: hit testing, pointer ownership, slide, giữ phím và repeat.
- `input/` và `telex/`: chính sách nhập, Telex, gợi ý, học từ, clipboard và emoji.
- `ai/`: nhận diện provider/model, request AI, fallback theo thứ tự model → key cùng provider → provider khác, lưu key bằng Android Keystore và lưu metadata/lỗi gần nhất không chứa secret theo fingerprint SHA-256.
- `translation/`: ngôn ngữ, model ML Kit và tùy chọn dịch.
- `voice/`: nhận dạng giọng nói hệ thống cho mic thường và panel inline AI/Dịch.
- `settings/`: tùy chọn bàn phím và xuất/nhập cấu hình.

## Luồng nhập

`MotionEvent → TouchDispatcher → KeyAction → KaiBoardImeService → InputConnection`.

Core typing phải hoạt động khi AI, mạng, micro hoặc model dịch lỗi. Dictionary tải ngoài luồng UI; request AI chạy nền và có cancellation/timeout.

## Dữ liệu

Tùy chọn dùng SharedPreferences. API key được mã hóa AES-GCM với khóa trong Android Keystore; card quản lý chỉ hiện đầu/cuối key đã che bớt. Metadata provider/số model/hạn mức được lưu riêng theo fingerprint, không lưu lại key dạng rõ. Từ, cụm từ, email và hashtag đã học cùng clipboard đều ở cục bộ. File sao lưu có thể chứa dữ liệu học nhưng không chứa API key, clipboard hoặc ghi chú.

`TelexWordComposer` giữ trạng thái hoàn tác phím theo từng từ; `SentenceAutomationPolicy` chỉ bật viết hoa khi bắt đầu nhập hoặc xuống dòng mới, không tự thêm Space/bật Shift sau dấu câu. Email và hashtag được xếp hạng bằng bộ đếm tần suất có giới hạn.

Mỗi API key có profile provider/model riêng trong `AiKeyStatsStore`. Khi gửi yêu cầu, provider đang chọn được ưu tiên; chỉ các lỗi xác thực đã được ghi trạng thái, quota/rate-limit, timeout, model không tương thích hoặc lỗi máy chủ mới cho phép chuyển ứng viên. Lỗi yêu cầu `400` không tự chuyển để tránh lặp một yêu cầu sai và tốn quota.

Hình học bàn phím gồm chiều cao, khoảng nâng đáy, phần trăm chiều rộng và lệch trái. Preset chỉ ghi bốn giá trị này qua `KeyboardPreferences`; đường xử lý chạm và timing commit không thay đổi.

