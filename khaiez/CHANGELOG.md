# Changelog

## Chưa phát hành

- Thêm gói từ điển mở rộng tùy chọn gồm 40.000 từ Việt và 15.000 từ Anh; tải một lần và dùng offline, không tăng dung lượng APK.
- Từ điển cá nhân dùng decay 14 ngày để từ lâu không dùng tự giảm ưu tiên.
- Gợi ý trong ô AI tự học từ câu lệnh người dùng và dùng chung thanh gợi ý theo thứ tự AI → cá nhân → từ điển offline.
- Tự nhận biết tiếng Việt/Anh bằng luật nhẹ để ưu tiên đúng nguồn từ điển; thêm tìm kiếm emoji bằng từ khóa Việt không dấu hoặc tiếng Anh.
- Vuốt Space để di chuyển con trỏ; giữ Backspace xóa tăng tốc và xóa ngay ký tự composing cuối cùng; giữ/chạm hai lần Shift để bật Caps Lock.
- Cho phép Telex trong ô URL và đồng bộ palette/gradient cho Quản lý API, từ cá nhân và model dịch.
- Thêm DS2API OpenAI-compatible qua deployment HTTPS, tự nhận diện key prefix `khaids-`, quét model và tham gia failover nhiều key.
- Quản lý API cho phép chạm giữ trực tiếp card để kéo thả thứ tự ưu tiên; card đang kéo nổi viền accent và các card khác tự tạo vị trí chèn.
- Thứ tự API key được dùng trực tiếp khi gửi AI và chỉ được lưu sau khi thả card.
- Loại bỏ gói N-gram/cụm từ dựng sẵn; gợi ý cụm từ chỉ học từ dữ liệu cá nhân trên thiết bị.

Định dạng theo Keep a Changelog; phiên bản theo Semantic Versioning khi phù hợp.

## 1.2.0 - 2026-07-18

### Thêm

- Preset bàn phím dạng button: Mặc định, Nâng lên, Cao dễ bấm và Tùy chỉnh.
- Trạng thái API key theo từng key, gồm provider, số model, hạn mức và hiệu ứng quét/thành công/lỗi.
- Card danh sách API key; ô nhập chỉ dùng cho key mới và chỉ lưu sau khi quét thành công.
- Nhập giọng nói trực tiếp trong vùng bàn phím cho AI và Dịch, có text thời gian thực, tạm dừng/tiếp tục và Backspace.
- Tự động viết hoa khi bắt đầu nhập hoặc xuống dòng mới; dấu câu không tự thêm Space hoặc bật Shift.
- Học email và hashtag thường dùng, xếp hạng gợi ý theo tần suất trên thiết bị.
- Tùy chọn bật/tắt viền phím và chỉnh độ dày.

### Sửa

- AI tách key/model theo đúng provider; tự thử model khác, key khác rồi provider khác khi key lỗi, hết quota, timeout hoặc lỗi máy chủ. Card API hiển thị lỗi gần nhất theo từng key.
- NVIDIA NIM ưu tiên model chat tương thích và bỏ qua model dùng endpoint khác thay vì kết luận nhầm key hỏng.
- Tối giản và ghim ô thử KAI Board khi cuộn cài đặt; không tự bật bàn phím khi mở ứng dụng.
- Thay dropdown cài đặt bằng nhóm button nhỏ cho theme, một tay, giọng văn AI và provider API.
- Đồng bộ nền sáng/tối và màu nhấn mint trong cài đặt và bàn phím.
- Dịch tự chạy khi tạm dừng mic và đưa kết quả vào ô đang nhập.
- Telex tương thích thao tác hoàn tác kiểu UniKey: sau khi bấm lặp dấu/biến âm, phần còn lại của từ giữ dạng Latin.
- Sửa màu phím trắng/đen theo chế độ sáng/tối và hoàn thiện unit test palette thuần JVM.

## 1.1.0

### Thêm

- Màn hình **Quản lý API** riêng: nhiều key, quét model, thống kê key, xóa key an toàn.
- Bộ màu bàn phím: **Mint**, **Gradient AI**, **Ocean** (sáng/tối).
- Thông báo quyền riêng tư ngắn trong mục Thiết lập.
- Ô thử gõ sticky trên màn cài đặt.

### Sửa

- Khôi phục Gradient AI sau khi palette bị rút gọn về một tone.
- Legacy `gemini_ai_gradient` map lại sang Gradient AI.

### Bao gồm từ 1.0.x

- Nhập giọng nói cho AI và dịch.
- Nhiều API key với tự chuyển khi hết quota.
- Xuất/nhập cấu hình không chứa secret.
- Cache từ/cặp từ đã học trên đường gõ.

## 1.0.0

- Mốc phát hành cấu hình; build release có sẵn.

