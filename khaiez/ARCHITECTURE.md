# Kiến trúc

## Thành phần chính

- `ime/KaiBoardImeService`: vòng đời IME, composing text, Telex, AI, dịch và voice routing.
- `ui/KeyboardView`: vẽ bàn phím, geometry và chuyển action; không chứa mạng hay model.
- `touch/`: hit testing, pointer ownership, slide, giữ phím và repeat.
- `input/` và `telex/`: chính sách nhập, Telex, gợi ý, học từ, clipboard và emoji.
- `ai/`: nhận diện provider/model, request AI, xoay API key và lưu key bằng Android Keystore.
- `translation/`: ngôn ngữ, model ML Kit và tùy chọn dịch.
- `voice/`: bridge tới dịch vụ nhận dạng giọng nói của hệ thống.
- `settings/`: tùy chọn bàn phím và xuất/nhập cấu hình.

## Luồng nhập

`MotionEvent → TouchDispatcher → KeyAction → KaiBoardImeService → InputConnection`.

Core typing phải hoạt động khi AI, mạng, micro hoặc model dịch lỗi. Dictionary tải ngoài luồng UI; request AI chạy nền và có cancellation/timeout.

## Dữ liệu

Tùy chọn dùng SharedPreferences. API key được mã hóa AES-GCM với khóa trong Android Keystore. Dữ liệu học và clipboard ở cục bộ; file sao lưu cấu hình không chứa secret, clipboard hoặc dữ liệu học.

