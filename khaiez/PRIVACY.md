# Quyền riêng tư

Tóm tắt người dùng cũng nằm trong [README gốc](../README.md#quyền-riêng-tư). Tài liệu này là bản chi tiết cho review nội bộ.

## Dữ liệu cục bộ

KAI Board có thể lưu tùy chọn, từ/cặp từ, email, hashtag đã học, emoji gần đây, clipboard (text/HTML/ảnh thu nhỏ local) và ghi chú trên thiết bị. Email/hashtag được xếp hạng bằng số lần sử dụng. Người dùng có thể xóa từng mục clipboard, xóa hết chưa ghim, hoặc xóa dữ liệu học trong ứng dụng.

File sao lưu cài đặt có thể chứa từ/cụm từ, email, hashtag đã học, Theme Extensions và thứ tự Smartbar; không chứa API key, clipboard hoặc ghi chú.

Nhận diện OTP, email, URL và số điện thoại trong Clipboard dùng luật cục bộ, không gửi nội dung ra mạng. Ảnh clipboard chỉ lưu file JPEG trong thư mục app; không upload. Lịch sử có thể tự xóa sau 1 giờ hoặc 1 ngày; mục ghim được giữ cho tới khi xóa thủ công.

Theme tải bằng URL chỉ kết nối HTTPS sau thao tác của người dùng. Gói JSON bị giới hạn 64 KB, không chứa mã thực thi; chia sẻ theme chỉ cấp quyền đọc tạm thời cho file được chọn.

## AI và dịch

- Nội dung chỉ được gửi tới nhà cung cấp AI khi người dùng kích hoạt tính năng AI.
- Provider/model đã chọn và phần văn bản yêu cầu là dữ liệu tối thiểu được gửi.
- API key được mã hóa bằng Android Keystore và không nằm trong file sao lưu.
- Màn Quản lý API chỉ hiển thị key đã che bớt. Thống kê provider/model và lỗi gần nhất phục vụ fallback được liên kết bằng fingerprint SHA-256, không chứa key dạng rõ.
- Khi API key ưu tiên không dùng được, cùng một nội dung yêu cầu có thể được gửi sang key/provider tiếp theo theo thứ tự người dùng đã kéo thả; clipboard, lịch sử gõ và dữ liệu học không được đính kèm.
- Key mới chỉ được lưu sau khi người dùng bấm lưu/quét và provider chấp nhận key; quá trình quét không chạy nền khi rời trang.
- Dịch ML Kit ưu tiên model trên thiết bị; tải model có thể cần mạng.
- Nhập giọng nói phụ thuộc dịch vụ speech recognition được cài trên thiết bị; KAI Board không ghi âm nền và panel inline dừng khi rời IME.

KAI Board không được âm thầm gửi clipboard, lịch sử gõ, từ đã học, định danh thiết bị hoặc dữ liệu tài khoản.

## Gói từ điển / cụm từ mở rộng

- Chỉ tải khi người dùng bấm tải (từ điển: HTTPS; gói cụm: HTTPS hoặc asset bundled).
- Từ điển: tối đa 40.000 từ Việt và 15.000 từ Anh từ FrequencyWords/OpenSubtitles.
- Gói cụm (`PhrasePack`): ~720 000 bigram offline (~8 MB; collocation + Viet74K + OpenSubtitles); preload 1 lần trên worker; **không** backup; **không** xóa khi clear học. Không seed cụm trong APK. Cụm cá nhân (decay 21 ngày) vẫn #1.
- Thống kê chọn gợi ý cụm (`PhraseStats`): chỉ đếm số lần hiện/chọn theo nguồn (personal/pack), **không** lưu nội dung gõ hay cặp từ.
- Sau khi cài, gợi ý chạy cục bộ; nội dung người dùng gõ không được gửi khi tải gói.
- Từ điển cá nhân dùng điểm gần đây với chu kỳ bán rã 14 ngày; không có tác vụ mạng hoặc decay chạy nền.

