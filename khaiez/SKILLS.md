# Bộ skill phát triển KAI Board

## Skill cốt lõi

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

1. Đọc `kai-board-ime-development` để xác định guardrail.
2. Thêm skill chuyên môn: AI, Material UI hoặc performance.
3. Implement và chạy unit test/build.
4. Dùng `android-adb-fast-loop` để xác minh thiết bị.
5. Chỉ dùng GitHub publishing skill khi người dùng yêu cầu commit/push/PR.

Không dùng skill để mở rộng phạm vi ngoài yêu cầu; không đưa API key, nội dung gõ hoặc clipboard vào log, ảnh hay tài liệu.

