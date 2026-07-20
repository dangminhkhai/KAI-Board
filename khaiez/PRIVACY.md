# Quyền riêng tư

## Dữ liệu cục bộ

KAI Board có thể lưu tùy chọn, từ/cặp từ, email, hashtag đã học, emoji gần đây, clipboard và ghi chú trên thiết bị. Email/hashtag được xếp hạng bằng số lần sử dụng. Người dùng có thể xóa dữ liệu học trong ứng dụng.

File sao lưu cài đặt có thể chứa từ/cụm từ, email, hashtag đã học, Theme Extensions và thứ tự Smartbar; không chứa API key, clipboard hoặc ghi chú.

Nhận diện OTP, email, URL và số điện thoại trong Clipboard dùng luật cục bộ, không gửi nội dung ra mạng. Lịch sử có thể tự xóa sau 1 giờ hoặc 1 ngày; mục người dùng ghim được giữ cho tới khi xóa thủ công.

Theme tải bằng URL chỉ kết nối HTTPS sau thao tác của người dùng. Gói JSON bị giới hạn 64 KB, không chứa mã thực thi; chia sẻ theme chỉ cấp quyền đọc tạm thời cho file được chọn.

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

## Gói từ điển mở rộng

- Chỉ tải khi người dùng bấm tải và chế độ offline đang tắt.
- Gồm tối đa 40.000 từ Việt và 15.000 từ Anh từ FrequencyWords/OpenSubtitles.
- Sau khi tải, gợi ý chạy cục bộ; nội dung người dùng gõ không được gửi khi tải gói.
- Từ điển cá nhân dùng điểm gần đây với chu kỳ bán rã 14 ngày; không có tác vụ mạng hoặc decay chạy nền.

