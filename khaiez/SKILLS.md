# Bộ skill phát triển KAI Board

## Skill cốt lõi

- **`kai-board-continue`** (trong repo `.grok/skills/`): **đổi máy / tiếp tục / chống regression** — pull, shared debug keystore, build–install, smoke IME bắt buộc (G1–G9), không để tính năng cũ vỡ khi sửa mới. Slash: `/kai-board-continue`.
- `kai-board-ime-development`: mọi thay đổi IME, Telex, touch, gợi ý, clipboard, emoji, dịch và AI; bảo vệ cảm giác gõ.
- `android-adb-fast-loop`: build–install–launch, chọn IME, screenshot, logcat và kiểm tra crash trên thiết bị.
- `android-material3-ui`: màn hình cài đặt, theme, accessibility và UI không đổi geometry bàn phím.
- `android-ime-performance`: đo startup, key latency, frame, memory, GC và tối ưu có baseline.
- `kai-board-ai-provider-development`: provider/model, API key, timeout, cancellation, quota và quyền riêng tư AI.
- `concise-responses`: báo cáo kết quả ngắn, nêu test và blocker quan trọng.

## Skill hỗ trợ

- `imagegen`: tạo/chỉnh icon, ảnh store và tài sản raster; không dùng thay vector có nguồn chỉnh sửa.
- `github:github`: đọc repo, issue và PR.
- `github:gh-fix-ci`: xử lý GitHub Actions thất bại.
- `github:gh-address-comments`: sửa review comments.
- `github:yeet`: commit, push và mở draft PR khi được yêu cầu.
- `skill-creator`: tạo hoặc cập nhật workflow chuyên biệt mới.

## Thứ tự thường dùng

1. **Đổi máy / session mới:** `/kai-board-continue` (hoặc nói “tiếp tục KAI Board”) — pull + keystore + smoke.
2. Đọc `kai-board-ime-development` để xác định guardrail khi sửa IME.
3. Thêm skill chuyên môn: AI, Material UI hoặc performance.
4. Implement và chạy unit test/build (`testDebugUnitTest assembleDebug`).
5. Đổi Telex/IME/Backspace: đọc [ARCHITECTURE.md](ARCHITECTURE.md) và tick [TESTING.md](TESTING.md) + smoke trong skill `kai-board-continue/references/smoke-ime.md`.
6. Dùng `android-adb-fast-loop` / `dev-install.cmd` / `adb install -r` arm64 để xác minh thiết bị (Vivo: Safe/Cafe BS).
7. Chỉ commit/push khi người dùng yêu cầu.

Không dùng skill để mở rộng phạm vi ngoài yêu cầu; không đưa API key, nội dung gõ hoặc clipboard vào log, ảnh hay tài liệu.

