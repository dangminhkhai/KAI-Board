# KAI Board

KAI Board là bàn phím Android viết bằng Kotlin, tập trung vào cảm giác bấm nhanh, chính xác, xử lý Telex tiếng Việt tự nhiên và khả năng mở rộng mà không đưa công việc nặng vào đường gõ phím.

Ứng dụng hoạt động từ Android 8.0, có gợi ý và học từ trên thiết bị, clipboard, emoji, dịch ML Kit, nhập giọng nói, trợ lý AI nhiều nhà cung cấp, tùy chỉnh hình học bàn phím và Theme Extension cài độc lập với APK.

> Trạng thái: dự án đang được phát triển và kiểm thử thực tế. Phiên bản hiện tại là **1.2.0** (`versionCode 120`).

## Mục lục

- [Tính năng](#tính-năng)
- [AI và dịch](#ai-và-dịch)
- [Giao diện và tùy chỉnh](#giao-diện-và-tùy-chỉnh)
- [Quyền riêng tư](#quyền-riêng-tư)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Build và kiểm thử](#build-và-kiểm-thử)
- [Build release](#build-release)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Tài liệu](#tài-liệu)

## Tính năng

### Gõ tiếng Việt và Telex

- Telex theo thói quen UniKey, gồm đặt dấu, biến âm, hoàn tác dấu và xử lý chuỗi Latin/tiếng Anh trong chế độ Telex.
- Cho phép Telex trong ô URL nhưng vẫn áp dụng chính sách riêng cho email, mật khẩu, số, OTP và các loại input đặc biệt.
- Tự viết hoa khi bắt đầu nhập hoặc xuống dòng mới; không tự thêm Space hay bật Shift sau dấu câu.
- Chọn từ gợi ý sẽ hoàn thiện từ và chuyển sang từ mới.
- Backspace hỗ trợ sửa lại từ trước đó, xóa vùng chọn và xóa emoji ghép nhiều code point theo cụm hiển thị.
- Giữ Backspace để xóa tăng tốc; giữ hoặc chạm hai lần Shift để bật Caps Lock.

### Gợi ý, từ điển và học cá nhân

- Gợi ý từ và cụm từ cá nhân được học cục bộ từ quá trình sử dụng.
- Học email, hashtag và câu lệnh AI thường dùng; xếp hạng theo tần suất, độ mới và ngữ cảnh.
- Từ điển cá nhân dùng cơ chế giảm ưu tiên theo thời gian để từ lâu không dùng không chiếm vị trí gợi ý mãi mãi.
- Tự nhận biết tiếng Việt/Anh bằng luật nhẹ để ưu tiên đúng nguồn gợi ý.
- Có thể tải riêng gói từ điển mở rộng khoảng 40.000 từ Việt và 15.000 từ Anh; sau khi tải, gói hoạt động offline và không làm tăng dung lượng APK mặc định.
- Quản lý từ cá nhân: tìm kiếm, xem nguồn học, số lần sử dụng, lần dùng gần nhất, điểm ưu tiên, sửa, xóa hoặc đặt lại ưu tiên.

### Bố cục và thao tác gõ

- QWERTY, hàng số tùy chọn, màn ký hiệu `?123` và gợi ý ký hiệu khi giữ phím.
- Numpad phù hợp cho ô số, OTP và số điện thoại; bố cục thích ứng cho email và URL.
- Vuốt trên Space để di chuyển con trỏ.
- Chế độ một tay và giới hạn chiều cao khi xoay ngang.
- Preset kích thước/vị trí: Mặc định, Nâng lên, Cao dễ bấm và Tùy chỉnh.
- Chỉnh trực tiếp chiều cao, chiều rộng, độ lệch và khoảng nâng đáy mà không thay đổi timing commit hay hit-test cốt lõi.
- Rung, âm thanh, viền phím và độ dày viền có thể điều chỉnh trong cài đặt.

### Smartbar, emoji và clipboard

- Smartbar có menu, gợi ý từ và thứ tự nút tùy chỉnh.
- Emoji có tìm kiếm bằng từ khóa tiếng Việt không dấu hoặc tiếng Anh, tab vừa sử dụng và xóa đúng emoji ghép.
- Clipboard có lịch sử, ghim và thời hạn tự xóa 1 giờ, 1 ngày hoặc không tự xóa; nội dung ghim luôn được giữ.
- Nhận diện offline OTP, email, URL và số điện thoại để chèn nhanh phần hữu ích.
- Ghi chú clipboard có quản lý thêm, sửa và xóa riêng.
- Inline Autofill hỗ trợ OTP và trình quản lý mật khẩu trên Android 11 trở lên khi hệ thống cung cấp dữ liệu.

## AI và dịch

### KAI AI

- Nhập yêu cầu trực tiếp trên bàn phím, gửi thủ công hoặc tự gửi sau khoảng dừng có thể cấu hình.
- Các giọng văn: Hầm hố giật gân, Hài hước, Tò mò bí ẩn, Thời sự tin tức, Truyền động lực, Tối giản tinh tế và Tự động ngẫu nhiên.
- Hỗ trợ nhiều API key, quét model, lưu trạng thái/model theo từng key và tự chuyển khi gặp quota, timeout hoặc lỗi máy chủ phù hợp.
- Chạm giữ và kéo card API key để đặt thứ tự ưu tiên; thứ tự chỉ được lưu sau khi thả.
- API key được mã hóa bằng Android Keystore và không được ghi vào source, log hoặc file sao lưu.

Nhà cung cấp hiện hỗ trợ:

| Nhà cung cấp | Nhận diện tự động |
| --- | --- |
| DS2API | Prefix `khaids-`; có thể chọn DS2API thủ công với prefix khác |
| OpenRouter | `sk-or-` |
| Gemini | `AIza` |
| OpenAI | `sk-` |
| Groq | `gsk_` |
| NVIDIA NIM | `nvapi-` |

DS2API sử dụng endpoint OpenAI-compatible HTTPS đã cấu hình, gồm `/v1/models` và `/v1/chat/completions`. Key DS2API không được gửi sang endpoint OpenAI khi đã nhận diện hoặc chọn provider thủ công.

### Dịch và giọng nói

- Dịch bằng Google ML Kit; người dùng chọn và tải model theo từng ngôn ngữ để sử dụng offline.
- Model dịch không được đóng gói sẵn vào APK, giúp giảm dung lượng tải ban đầu.
- Mic cho AI và Dịch hiển thị lời nói trực tiếp trong vùng bàn phím, có tạm dừng/tiếp tục, Backspace và chỉnh sửa trước khi xử lý.
- Khi tạm dừng mic Dịch, văn bản được đưa vào ô dịch và quá trình dịch tự chạy nếu model sẵn sàng.

## Giao diện và tùy chỉnh

- Theme Theo hệ thống, Sáng và Tối; bảng màu mặc định đồng bộ giữa bàn phím và ứng dụng cài đặt.
- Card, button, toggle, slider và trạng thái dùng chung palette Material 3 trong ứng dụng; hình học và cảm giác bấm của bàn phím vẫn dùng renderer riêng.
- Chỉ giữ theme mặc định trong APK để giảm dung lượng.
- Theme Extension dạng JSON có thể cài từ file hoặc URL HTTPS, xem trước, áp dụng, xuất, chia sẻ và xóa độc lập với APK.
- Gallery theme hai cột với preview đúng tỷ lệ và trạng thái theme đang dùng.
- Các gói mẫu nằm trong [`theme-packs/`](theme-packs/), gồm Pastel Forest, Pastel Pink và Retro Japanese Manga.
- Ô “Chạm vào đây để thử KAI Board” dạng sticky trong cài đặt và không tự bật bàn phím khi mở ứng dụng.

## Quyền riêng tư

- Gõ phím cốt lõi hoạt động không cần AI, mạng, clipboard, micro hoặc model dịch.
- Chế độ riêng tư tự áp dụng cho ô mật khẩu và `IME_FLAG_NO_PERSONALIZED_LEARNING`: không học từ, không ghi clipboard và không chạy KAI AI.
- Từ/cụm từ, email, hashtag, emoji gần đây và dữ liệu học được lưu cục bộ trên thiết bị.
- Chỉ gửi prompt, provider/model và chỉ dẫn giọng văn cần thiết khi người dùng kích hoạt AI.
- Không tự gửi clipboard, lịch sử gõ, từ đã học, định danh thiết bị hoặc dữ liệu tài khoản tới AI.
- File sao lưu có thể chứa cài đặt, dữ liệu học, Theme Extensions và thứ tự Smartbar; không chứa API key, clipboard hoặc ghi chú.
- KAI Board không ghi typed text, clipboard, prompt, AI output, key hay Authorization header vào log.

Xem chi tiết tại [khaiez/PRIVACY.md](khaiez/PRIVACY.md).

## Yêu cầu hệ thống

- Android 8.0 trở lên (`minSdk 26`).
- JDK 17.
- Android SDK với `compileSdk 37`; ứng dụng đặt `targetSdk 35`.
- Thiết bị thật hoặc giả lập Android.
- APK hiện được tách cho `arm64-v8a` và `x86_64`.

## Build và kiểm thử

Clone dự án và chạy tại thư mục gốc:

```powershell
git clone https://github.com/dangminhkhai/KAI-Board.git
cd KAI-Board
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK debug nằm trong `app/build/outputs/apk/debug/`.

Cài nhanh lên thiết bị đang kết nối ADB:

```powershell
.\dev-install.cmd -WithTests
```

Các nhóm unit test bao phủ Telex, composing/cursor, chính sách input, gợi ý, từ điển cá nhân, emoji Unicode, clipboard, touch/repeat, AI provider/fallback, cài đặt và palette.

Thay đổi liên quan đến touch, IME lifecycle, AI, mic hoặc dịch vẫn cần kiểm thử trên thiết bị thật. Ma trận kiểm thử chi tiết nằm tại [khaiez/TESTING.md](khaiez/TESTING.md).

## Build release

Release bật R8, resource shrinking và dùng APK tách theo ABI. Chuẩn bị keystore/credential cục bộ theo [khaiez/RELEASE.md](khaiez/RELEASE.md), sau đó chạy:

```powershell
.\build-release.cmd
```

Không commit keystore, credential, API key hoặc file secret. Thư mục `Res/` dành cho bộ ký local và đã được loại khỏi Git.

## Cấu trúc dự án

```text
app/src/main/java/vn/kai/board/
├── ai/            Provider, model discovery, key pool và AI request
├── ime/           Vòng đời IME, composing và điều phối InputConnection
├── input/         Telex policy, gợi ý, từ điển, clipboard và emoji
├── settings/      Preferences, backup và Theme Extensions
├── touch/         Hit-test, pointer, slide, giữ phím và repeat
├── translation/   Quản lý model và luồng dịch ML Kit
├── ui/            Renderer bàn phím, geometry và preview theme
└── voice/         Speech recognition cho mic thường, AI và Dịch

theme-packs/        Các gói theme JSON cài riêng
khaiez/             Kiến trúc, kiểm thử, riêng tư và phát hành
```

Đường gõ chính:

```text
MotionEvent → TouchDispatcher → KeyAction → KaiBoardImeService → InputConnection
```

Đường này không thực hiện network, tải model hoặc parse dữ liệu lớn để giữ độ trễ gõ ổn định.

## Tài liệu

- [Kiến trúc](khaiez/ARCHITECTURE.md)
- [Changelog](khaiez/CHANGELOG.md)
- [Kiểm thử](khaiez/TESTING.md)
- [Quyền riêng tư](khaiez/PRIVACY.md)
- [Phát hành](khaiez/RELEASE.md)
- [Lộ trình](khaiez/ROADMAP.md)
- [Đóng góp](khaiez/CONTRIBUTING.md)
- [Bảo mật](khaiez/SECURITY.md)
- [Theme Extension](theme-packs/README.md)
- [Thông báo bên thứ ba](THIRD_PARTY_NOTICES.md)

## Lưu ý

KAI Board là dự án đang phát triển. Trước khi phân phối rộng, cần kiểm thử thực tế trên nhiều ứng dụng nhập liệu, rà soát chính sách quyền riêng tư và tuân thủ điều khoản của từng dịch vụ AI/dịch được tích hợp.
