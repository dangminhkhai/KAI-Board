# Bảo mật

Liên quan: [PRIVACY.md](PRIVACY.md), [RELEASE.md](RELEASE.md) (ký bản phát hành).

## Báo cáo lỗ hổng

Không đăng công khai API key, dữ liệu người dùng hoặc chi tiết khai thác chưa được sửa. Gửi báo cáo riêng cho chủ dự án, kèm phiên bản, thiết bị, bước tái hiện và mức ảnh hưởng.

## Quy tắc secret

- API key chỉ lưu qua `SecureApiKeyStore`.
- Release signing lấy từ các biến môi trường `KAI_RELEASE_*`.
- `Res/signing-secret.xml` dùng DPAPI cục bộ và `Res/*.jks` phải luôn nằm ngoài Git; sao lưu keystore ở nơi bảo mật riêng.
- Không ghi authorization header, prompt, response hoặc nội dung gõ vào log/crash report.
- Không theo redirect mạng có thể làm lộ header xác thực.
- Khi key lộ, thu hồi ngay và tạo key mới.
- Không ghi toàn bộ API key trong ảnh, docs, log hoặc trạng thái UI; chỉ dùng dạng che bớt.
- Fallback phải giữ key đúng endpoint/provider. Lỗi key được lưu dưới dạng trạng thái không chứa secret; không chuyển provider cho yêu cầu sai `400`.

## Phạm vi ưu tiên

Rò rỉ dữ liệu gõ/clipboard/key, bypass quyền IME, export component ngoài ý muốn, truyền dữ liệu không mã hóa và dependency có lỗ hổng nghiêm trọng.

