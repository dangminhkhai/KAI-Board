# Quyền riêng tư

## Dữ liệu cục bộ

KAI Board có thể lưu tùy chọn, từ/cặp từ, email, hashtag đã học, emoji gần đây, clipboard và ghi chú trên thiết bị. Email/hashtag được xếp hạng bằng số lần sử dụng. Người dùng có thể xóa dữ liệu học trong ứng dụng.

File sao lưu cài đặt có thể chứa từ/cụm từ, email và hashtag đã học; không chứa API key, clipboard hoặc ghi chú.

## AI và dịch

- Nội dung chỉ được gửi tới nhà cung cấp AI khi người dùng kích hoạt tính năng AI.
- Provider/model đã chọn và phần văn bản yêu cầu là dữ liệu tối thiểu được gửi.
- API key được mã hóa bằng Android Keystore và không nằm trong file sao lưu.
- Màn Quản lý API chỉ hiển thị key đã che bớt. Thống kê provider/model và lỗi gần nhất phục vụ fallback được liên kết bằng fingerprint SHA-256, không chứa key dạng rõ.
- Khi API key ưu tiên không dùng được, cùng một nội dung yêu cầu có thể được gửi sang key/provider tiếp theo theo thứ tự người dùng đã kéo thả; clipboard, lịch sử gõ và dữ liệu học không được đính kèm.
- Key có prefix `khaids-` được gửi riêng tới deployment DS2API HTTPS đã cấu hình; không được gửi tới endpoint OpenAI.
- Key mới chỉ được lưu sau khi người dùng bấm lưu/quét và provider chấp nhận key; quá trình quét không chạy nền khi rời trang.
- Dịch ML Kit ưu tiên model trên thiết bị; tải model có thể cần mạng.
- Nhập giọng nói phụ thuộc dịch vụ speech recognition được cài trên thiết bị; KAI Board không ghi âm nền và panel inline dừng khi rời IME.

KAI Board không được âm thầm gửi clipboard, lịch sử gõ, từ đã học, định danh thiết bị hoặc dữ liệu tài khoản. Trước khi phát hành công khai, nội dung này cần được rà soát thành chính sách pháp lý phù hợp khu vực phân phối.

## Gói gợi ý cụm từ offline

- Chỉ tải khi người dùng bấm **Tải gói tiếng Việt** và chế độ offline đang tắt.
- Ứng dụng tải corpus công khai UD Vietnamese VTB từ commit cố định, tạo N-gram trên thiết bị rồi dùng offline.
- Nội dung người dùng gõ và dữ liệu cá nhân đã học không được gửi khi tải hoặc tạo model.
- Người dùng có thể xóa gói bất kỳ lúc nào mà không xóa dữ liệu cụm từ cá nhân.

