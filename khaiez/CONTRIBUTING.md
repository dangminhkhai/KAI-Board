# Đóng góp

## Nguyên tắc

- Giữ nguyên hình học phím, vùng chạm, repeat, long-press, rung, âm thanh và thời điểm commit nếu yêu cầu không chủ đích thay đổi chúng.
- Không chạy I/O, mạng, tải model hoặc phân tích dữ liệu lớn trên luồng nhập liệu.
- Không log nội dung gõ, clipboard, từ đã học, prompt, phản hồi AI hoặc API key.
- Thay đổi hành vi phải có test hồi quy; thay đổi giao diện phải kiểm tra sáng/tối và kích thước màn hình.

## Quy trình

1. Tạo nhánh ngắn theo tính năng hoặc lỗi.
2. Chỉ sửa phạm vi cần thiết; không trộn refactor không liên quan.
3. Chạy `testDebugUnitTest assembleDebug`.
4. Kiểm tra trên ít nhất một ô nhập thường và một ứng dụng thực tế.
5. PR mô tả trước/sau, cách test, rủi ro và ảnh nếu thay đổi UI.

Không commit keystore, mật khẩu ký, `local.properties`, API key hoặc dữ liệu người dùng.

