# Bảo mật

## Báo cáo lỗ hổng

Không đăng công khai API key, dữ liệu người dùng hoặc chi tiết khai thác chưa được sửa. Gửi báo cáo riêng cho chủ dự án, kèm phiên bản, thiết bị, bước tái hiện và mức ảnh hưởng.

## Quy tắc secret

- API key chỉ lưu qua `SecureApiKeyStore`.
- Release signing lấy từ các biến môi trường `KAI_RELEASE_*`.
- Không ghi authorization header, prompt, response hoặc nội dung gõ vào log/crash report.
- Không theo redirect mạng có thể làm lộ header xác thực.
- Khi key lộ, thu hồi ngay và tạo key mới.

## Phạm vi ưu tiên

Rò rỉ dữ liệu gõ/clipboard/key, bypass quyền IME, export component ngoài ý muốn, truyền dữ liệu không mã hóa và dependency có lỗ hổng nghiêm trọng.

