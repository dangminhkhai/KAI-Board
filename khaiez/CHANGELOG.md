# Changelog

Định dạng [Keep a Changelog](https://keepachangelog.com/); phiên bản theo Semantic Versioning khi phù hợp.

## Chưa phát hành

### Sửa

- «Xóa từ đã học» / xóa từng từ / giữ gợi ý trên smartbar: xóa luôn cặp cụm trong `PhraseLearningStore` (trước chỉ xóa lexicon → cụm vẫn hiện).
- Chọn gợi ý từ: thay prefix đang gõ bằng từ gợi ý qua `applyComposingText` (tránh `T`+`tôi` → `Ttôi` trên OEM direct-commit).
- Backspace `Safe`→`Saff` (và `Cafe`→`Caff`) trên Vivo OriginOS: co chuỗi chỉ **xóa đuôi** trên editor, không xóa cả từ rồi `commitText` dạng ngắn (OEM nhân đôi tone Latin `s/f/r/x/j`).
- `commitText` từ kết thúc bằng modifier Telex `sfrxjaeowd` (tone + shape) tách body/tail và gỡ chữ kép nếu còn.
- Latin lock khi mất lock; suppress `onUpdateSelection` finish khi IME đang ghi `InputConnection`.
- Backspace sau escape shape (`aaa`→`aa`, `eee`/`ooo`/`aww`/`oww`/`uww`/`ddd`/`uoww`) xóa đúng 1 ký tự **hiển thị** (raw dài hơn display không làm BS no-op).
- Hoa/thường khi hoàn tác modifier theo phím vừa bấm (`A+s+i+s` → `Ais`); kết thúc composing khi con trỏ vào giữa từ.
- Giữ khóa Latin khi BS qua từ Anh có modifier (`Google`, `case`, `safe`, `care`, `pixel`, `object`).

### Thêm / cải thiện Telex

- Telex theo chuỗi phím gốc: modifier sai thứ tự; restore Latin khi âm tiết không hợp lệ; bao phủ `s f r x j`, `dd`, `aa ee oo`, `aw ow uw`.
- Unit: `TelexModifierBackspaceMatrixTest`, `ComposingEditorSync*`, `SafeTraceTest`; checklist thiết bị đầy đủ trong `TESTING.md`.
- Docs: `ARCHITECTURE.md` (luồng ghi editor OEM), `ROADMAP`, `CONTRIBUTING`, README Telex/BS.

### Gợi ý cụm từ (P1)

- Mid-word blend: cụm theo ngữ cảnh (personal + seed) khớp prefix đang gõ, ưu tiên trước completion từ điển.
- Seed offline ~100 bigram xã giao (`PhraseSeedCatalog`), bật/tắt cài đặt **Gợi ý cụm có sẵn**.
- Khi tắt setting: ẩn seed **và** cụm nhiều từ hard-code trong từ điển (`xin chào`, `cảm ơn`…) + boost contextPairs; vẫn giữ cụm user đã học.
- Phrase store v2: recency + decay nửa đời 21 ngày; migrate từ `pairs` cũ; personal luôn xếp trên seed.

### UI & tính năng (gom unreleased trước 1.2.x)

- Gallery theme hai cột (preview 400:225); Theme Extension JSON file/HTTPS; chỉ Mặc định trong APK; packs `pastel-forest` / `pastel-pink` / `retro-japanese-manga` tải riêng.
- Quản lý từ cá nhân (decay, badge ưu tiên); clipboard OTP/email/URL/SĐT + TTL + ghim; privacy mode; Inline Autofill; numpad; dictionary packs offline; gợi ý AI command; vuốt Space; Telex URL; Smartbar reorder; kéo thả API key; backup theme+Smartbar không secret; xóa emoji grapheme; bỏ N-gram dựng sẵn.

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

