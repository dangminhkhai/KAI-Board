# Kiến trúc

## Thành phần chính

- `ime/KaiBoardImeService`: vòng đời IME, composing text, Telex, AI, dịch và voice routing.
- `ui/KeyboardView`: vẽ bàn phím, geometry và chuyển action; không chứa mạng hay model.
- `touch/`: hit testing, pointer ownership, slide, giữ phím và repeat.
- `input/` và `telex/`: chính sách nhập, Telex, gợi ý, học từ, clipboard và emoji.
- `ai/`: nhận diện provider/model, request AI và fallback theo thứ tự API key người dùng sắp xếp, lưu key bằng Android Keystore và lưu metadata/lỗi gần nhất không chứa secret theo fingerprint SHA-256.
- `translation/`: ngôn ngữ, model ML Kit và tùy chọn dịch.
- `voice/`: nhận dạng giọng nói hệ thống cho mic thường và panel inline AI/Dịch.
- `settings/`: tùy chọn bàn phím, Theme Extension JSON và xuất/nhập cấu hình.
- `CustomThemesActivity`: trang cài, xem trước, áp dụng, xuất/chia sẻ và xóa giao diện tải riêng.

## Luồng nhập

`MotionEvent → TouchDispatcher → KeyAction → KaiBoardImeService → InputConnection`.

Core typing phải hoạt động khi AI, mạng, micro hoặc model dịch lỗi. Dictionary tải ngoài luồng UI; request AI chạy nền và có cancellation/timeout.

## Dữ liệu

Tùy chọn dùng SharedPreferences. API key được mã hóa AES-GCM với khóa trong Android Keystore; card quản lý chỉ hiện đầu/cuối key đã che bớt. Metadata provider/số model/hạn mức được lưu riêng theo fingerprint, không lưu lại key dạng rõ. Từ, cụm từ, email và hashtag đã học cùng clipboard đều ở cục bộ. File sao lưu có thể chứa dữ liệu học, Theme Extensions và thứ tự Smartbar nhưng không chứa API key, clipboard hoặc ghi chú.

`TelexWordComposer` giữ song song chuỗi phím gốc và kết quả Telex theo từng từ. Khi kết quả có dấu nhưng cấu trúc âm tiết không hợp lệ, composer trả lại chuỗi Latin gốc; Backspace phát lại phần chuỗi phím còn lại. `ComposingCursorPolicy` kết thúc composing khi con trỏ rời mép cuối để chỉnh sửa giữa từ không xóa nhầm ký tự cuối. `SentenceAutomationPolicy` chỉ bật viết hoa khi bắt đầu nhập hoặc xuống dòng mới, không tự thêm Space/bật Shift sau dấu câu. Email và hashtag được xếp hạng bằng bộ đếm tần suất có giới hạn.

`PhraseLearningStore` chỉ học bigram/trigram cá nhân và ưu tiên trigram đúng hai từ ngữ cảnh. `WordDictionaryPack` tải riêng gói Việt/Anh vào `filesDir`; `SuggestionLanguageDetector` dùng luật ký tự/prefix nhẹ để ưu tiên nguồn phù hợp mà không dùng model hoặc I/O trên đường gõ.

`AiCommandSuggestionStore` chỉ học chuyển tiếp từ câu lệnh AI người dùng thực sự gửi. Thanh AI gộp gợi ý theo thứ tự AI → cá nhân → offline. `SpaceCursorGesturePolicy`, `ShiftGesturePolicy` và `RepeatKeyState` giữ logic cử chỉ thuần, có unit test và không đọc đĩa/mạng.

`SmartClipboardClassifier` chỉ dùng regex cục bộ khi mở bảng Clipboard; OTP chỉ được tách khi văn bản có ngữ cảnh xác thực. `ClipboardHistoryStore` dọn mục hết hạn khi đọc/thêm thay vì chạy timer nền và không xóa mục ghim. `UnicodeDeletionPolicy` tính số UTF-16 code unit cần xóa cho emoji ghép trước khi gọi `InputConnection`.

`ThemeExtensionStore` giới hạn mỗi gói JSON 64 KB, xác thực ID/palette/font và lưu trong `filesDir/theme_extensions`. Preset màu dựng sẵn chỉ còn Mặc định; các theme khác được phân phối ngoài APK. Backup đóng gói nội dung theme đã xác thực, còn chia sẻ file dùng `FileProvider` với quyền đọc tạm thời.

Mỗi API key có profile provider/model riêng trong `AiKeyStatsStore`. Thứ tự trong `SecureApiKeyStore` là thứ tự fallback thực tế và được chỉnh bằng RecyclerView/ItemTouchHelper trong Quản lý API; thứ tự chỉ lưu khi thả card. Lỗi xác thực được ghi trạng thái; quota/rate-limit, timeout, model không tương thích hoặc lỗi máy chủ cho phép chuyển sang key tiếp theo. Lỗi yêu cầu `400` không tự chuyển để tránh lặp một yêu cầu sai và tốn quota.

Hình học bàn phím gồm chiều cao, khoảng nâng đáy, phần trăm chiều rộng và lệch trái. Preset chỉ ghi bốn giá trị này qua `KeyboardPreferences`; đường xử lý chạm và timing commit không thay đổi.

