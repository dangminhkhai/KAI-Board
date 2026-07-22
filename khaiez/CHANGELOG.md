# Changelog

Lịch sử mốc phát triển nội bộ (không còn pipeline ship).

## Debug / đang phát triển

### Clipboard panel UI — scroll + card cao + không tràn chữ (2026-07-21)

- Card clipboard cao **gấp đôi** hàng phím cũ (span 2 row); text wrap/ellipsis theo **measure** (`ClipboardTextLayout`), không cắt theo số ký tự → hết tràn bo góc.
- Hiển thị **toàn bộ** lịch sử (≤12) / ghi chú: **vuốt dọc** trong vùng card; tab + nút xóa/Quản lý cố định; thanh scroll mỏng khi còn nội dung.
- Phân biệt chạm: vuốt dọc ≥ slop → **scroll** (không dán, không mở pin/xóa); chỉ tap sạch cùng card mới paste.
- Scroll chỉ đổi offset + `invalidate` — **không** `rebuildKeys` mỗi frame (tránh bàn phím bị mờ/nháy).
- Unit: `ClipboardTextLayoutTest`.

### AI output — gỡ ngoặc kép tiêu đề + tone (2026-07-21)

- System prompt: không bọc tiêu đề/câu ngắn trong `""` / `“”`; không code fence nếu không cần code.
- `AiOutputSanitizer` trước khi dán: strip quote bọc cả chuỗi / từng dòng list; giữ quote giữa câu; gỡ fence wrapper.
- Tone: user chọn trong Cài đặt → mỗi request dùng đúng tone; **RANDOM** = model tự chọn giọng (app **không** round-robin).
- Unit: `AiOutputSanitizerTest`.

### AI providers Groq / NVIDIA (413 / 410)

- **Không phải prompt ô AI quá dài:** body chỉ system (giọng văn) + `aiPrompt`; 413/410 chủ yếu do **model sai**.
- Lọc model chat (bỏ whisper/tts/guard/embed/rerank); ưu tiên model chat ổn định.
- NVIDIA: **không** còn prepend model hardcode đã gỡ (HTTP **410 Gone**).
- HTTP **400/404/410/413/422** → thử model/key tiếp (trước đây ném `IllegalStateException` chặn fallback).
- Sau khi pull: **quét lại API key** trong Quản lý API để làm mới danh sách model.

### Học vị trí chạm (touch adaptation) (2026-07-22)

- Offline: mỗi phím chữ/space/⌫/Shift/Enter lưu offset chạm (phân số bề rộng/cao phím), **không** lưu nội dung gõ.
- Hit-test dùng tâm đã dịch (`TouchTargetPolicy` + `TouchAdaptationStore`); **không** dời phím vẽ.
- Strength tăng dần (~12 lần chạm/phím); cap ±32% để không “nuốt” phím kề.
- Tắt trên private/password; tắt panel emoji/clipboard/symbols; setting **Học vị trí chạm**; xóa kèm “Xóa từ đã học”.
- Unit `TouchAdaptationStoreTest` / `TouchTargetPolicyTest`; cài Vivo USB 2026-07-22.

### AI / Dịch panel + gợi ý command (2026-07-21)

- **Clipboard → AI / Dịch:** mở AI hoặc Dịch từ panel clipboard không kẹt body clipboard / không “về ABC” oan; `setAiState` / `setTranslationState` xóa `panel` media; `rebuildKeys` ưu tiên feature body; offline AI check **trước** `finishComposing`.
- **Ô input AI & Dịch** giống text field: caret, chạm/kéo đặt con trỏ, vuốt Space dịch cursor, gõ/⌫/Space **tại cursor** (Dịch có `translationCursor` + `SetTranslationCursor`).
- **Gợi ý AI command — next-word only:**
  - Không gộp cụm 2 từ (`đề ngắn`); học/transition 1 từ.
  - Gõ `Tiêu` → `đề` (không `Đề` / `Tiêu` / starter global `Limo` từ lệnh khác).
  - Khi đã có next-word AI: **không** merge lexicon/dictionary (tránh chip trùng từ đang gõ).
  - Context depth ≥ 1: không trộn empty-context starters; fallback global chỉ khi không có contextual hit.
- HTTP **413** = payload too large (server từ chối body); prompt ngắn kiểu *“Viết tiêu đề thảm taplo…”* **không** đủ dài để gây 413 trên Groq/NVIDIA NIM — xem status khác (key/model/mạng).
- Ô thử Cài đặt: compact sticky, placeholder ngắn, bỏ gạch chân (`setBackgroundResource(0)`), icon X nhỏ; giữ stroke card.

### Clipboard rich + UI Gboard-style (2026-07-21)

- Lịch sử **text / HTML / ảnh** local (`ClipboardHistoryStore`); ảnh JPEG trong `filesDir/clipboard_images/` (FileProvider).
- Smart paste OTP/email/URL/SĐT giữ nguyên; badge **HTML** / **ẢNH** + thumbnail.
- Dán ảnh qua `commitContent` chỉ khi ô khai báo `image/*`; URL/tìm kiếm/mật khẩu → toast nhẹ, **không** copy hệ thống (tránh lỗi sao chép).
- HTML: Spanned khi ô text thường; plain text trên URI/search/password.
- **Giữ** mục: popup pill **Ghim · Xóa** (không AlertDialog); nhả tay sau long-press **không** đóng popup.
- Thùng rác: xóa hết **chưa ghim** (popup xác nhận gọn); mục ★ được giữ; suppress re-import clip hệ thống sau xóa.
- Ô thử sticky trong Cài đặt (`RichClipboardTestEditText`) nhận text/HTML/ảnh — không cần Messages.
- **Không** `requestFocus` ô thử khi mở Cài đặt → bàn phím không tự bật; chỉ mở khi user chạm ô / Tùy chỉnh layout / Xóa ô thử.
- Palette mặc định **neutral grayscale** (bỏ accent mint xanh); theme System/Sáng/Tối giữ nguyên.
- Resize: viewport max + preview 60fps; sửa text phím bị nhỏ sau clipboard (reset `textPaint`).
- **Debug keystore dùng chung:** `keystore/android-debug.keystore` + Gradle `sharedDebug` — mọi máy ký cùng cert (`adb install -r` không mismatch).
- *(Scroll / card 2× / measure-wrap: xem mục “Clipboard panel UI” phía trên.)*

### UI chế độ mật khẩu / riêng tư

- Smartbar private: banner **«Riêng tư · mật khẩu»** + icon khóa; ẩn AI/clipboard/gợi ý; Telex tắt (logic cũ).
- Gợi ý không hiện khi private; chạm banner/khóa = NoOp.

### P2 quản lý cụm cá nhân

- Tab **Cụm** trong Quản lý từ & cụm: list/tìm/xóa từng cụm (`removeExact`), xóa hết cụm personal.
- `PhraseStats`: đếm hiện/chọn gợi ý theo nguồn personal/pack, không lưu nội dung gõ; hiện trên màn quản lý (long-press để xóa số liệu).

### Gói cụm từ mở rộng

- Thêm `PhrasePack` lớn ~720k bigram (~8 MB, cap 5–20 MB): collocation + Viet74K (~99%) + OpenSubtitles; lookup chỉ RAM sau preload (không parse trên hot path).
- **Xóa** `PhraseSeedCatalog` (~100 bigram trong APK). Rank: **personal (decay 21 ngày) → pack**.
- `PhrasePack.warmUpAsync`: preload 1 lần trên worker (IME start / sau cài gói); gõ khi cold không block UI.
- UI card Tải/Xóa gói; meta count; không backup; không xóa khi clear học.
- Build: `tools/build_phrase_pack.py`; raw corpus gitignored trong `phrase-packs/raw/`.

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

- Khi tắt setting: ẩn seed **và** cụm nhiều từ hard-code trong từ điển (`xin chào`, `cảm ơn`…) + boost contextPairs; vẫn giữ cụm user đã học.
- Phrase store v2: recency + decay nửa đời 21 ngày; migrate từ `pairs` cũ; personal luôn xếp trên seed.

### UI & tính năng

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

- Mốc cấu hình dự án ban đầu.
