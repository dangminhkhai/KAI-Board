# KAI Board

KAI Board là bàn phím Android viết bằng Kotlin, tập trung vào cảm giác bấm nhanh, chính xác, xử lý Telex tiếng Việt tự nhiên và khả năng mở rộng mà không đưa công việc nặng vào đường gõ phím.

Ứng dụng hoạt động từ Android 8.0, có gợi ý và học từ trên thiết bị, clipboard, emoji, dịch ML Kit, nhập giọng nói, trợ lý AI nhiều nhà cung cấp, tùy chỉnh hình học bàn phím và Theme Extension cài độc lập với APK.

> Trạng thái: **debug / phát triển nội bộ**. Chỉ build và cài `assembleDebug`. Phiên bản hiện tại: **0.1.0-debug** (`versionCode 1`).

## Mục lục

- [Tính năng](#tính-năng)
- [AI và dịch](#ai-và-dịch)
- [Giao diện và tùy chỉnh](#giao-diện-và-tùy-chỉnh)
- [Quyền riêng tư](#quyền-riêng-tư)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Build và kiểm thử](#build-và-kiểm-thử)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Tài liệu](#tài-liệu)

## Tính năng

### Gõ tiếng Việt và Telex

KAI Board xử lý Telex theo từng từ đang composing trước con trỏ. `TelexWordComposer` giữ song song chữ đang hiển thị và chuỗi phím gốc; engine biến đổi ngay khi nhận phím, kiểm tra cấu trúc âm tiết và có thể dựng lại đúng chuỗi Latin khi từ không còn hợp lệ như tiếng Việt. `KaiBoardImeService` chịu trách nhiệm đồng bộ composing text với ứng dụng. Không có network, tải model hoặc đọc dữ liệu lớn trên đường xử lý này.

#### Bảng phím Telex

| Phím | Kết quả | Ví dụ |
| --- | --- | --- |
| `aa`, `ee`, `oo` | `â`, `ê`, `ô` | `tieng + e + s` → `tiếng` |
| `aw`, `ow`, `uw` | `ă`, `ơ`, `ư` | `duong + w` → `dương` |
| `uow` | `ươ` | `tuong + w` → `tương` |
| `dd` | `đ` | `ddang` → `đang` |
| `s`, `f`, `r`, `x`, `j` | sắc, huyền, hỏi, ngã, nặng | `chao + f` → `chào` |
| `z` | xóa dấu thanh đang có | `á + z` → `a` |

Modifier có thể đặt ngay sau nguyên âm hoặc ở cuối từ theo thói quen UniKey:

- `tieengs` và `tieng + e + s` đều tạo `tiếng`.
- `tuaans` và `tuan + a + s` đều tạo `tuấn`.
- `dang + d` → `đang`.
- `duong + w` → `dương`.

Khi modifier nằm sau phụ âm cuối, engine chỉ tìm ngược qua các phụ âm cuối hợp lệ của tiếng Việt như `c`, `ch`, `m`, `n`, `ng`, `nh`, `p`, `t`. Cách này tránh biến đổi bừa các chuỗi Latin không giống âm tiết Việt.

#### Vị trí đặt dấu kiểu mới

- `gi` và `qu` được xem là phụ âm kép khi còn nguyên âm phía sau: `gias` → `giá`, `quas` → `quá`, `quys` → `quý`.
- Nguyên âm đã biến đổi `ă â ê ô ơ ư` được ưu tiên mang dấu.
- Âm tiết mở `oa`, `oe`, `uy` đặt dấu trên nguyên âm đầu: `hoaf` → `hòa`.
- Âm tiết mở `ia`, `ya`, `ua`, `ưa` cũng đặt dấu trên nguyên âm đầu: `tias` → `tía`.
- Âm tiết đóng bằng phụ âm đặt dấu trên nguyên âm cuối của cụm nguyên âm: `toans` → `toán`, `tuan + a + s` → `tuấn`.
- Chữ hoa/thường của nguyên âm gốc được giữ khi thêm hoặc hoàn tác dấu.

#### Hoàn tác và gõ tiếng Anh trong chế độ Telex

Nhấn lặp lại đúng modifier sẽ trả về chuỗi phím Latin thay vì ép người dùng chuyển chế độ:

| Đang có | Nhấn | Kết quả Latin |
| --- | --- | --- |
| `á`, `à`, `ả`, `ã`, `ạ` | lặp `s`, `f`, `r`, `x`, `j` tương ứng | `as`, `af`, `ar`, `ax`, `aj` |
| `â`, `ê`, `ô` | lặp `a`, `e`, `o` | `aa`, `ee`, `oo` |
| `ă`, `ơ`, `ư`, `ươ` | lặp `w` | `aw`, `ow`, `uw`, `uow` |
| `đ` | lặp `d` | `dd` |

Sau khi người dùng chủ động hoàn tác một modifier, phần còn lại của từ được khóa ở dạng Latin. Ví dụ `Vin + f` tạm thành `Vìn`; nhấn `f` lần nữa trở thành `Vinf`, sau đó gõ tiếp `ast` vẫn giữ `Vinfast`, không biến `s` thành dấu sắc. Nếu Backspace xóa lùi qua điểm thoát này, Telex được mở lại cho phần từ còn lại.

Các bảo vệ cho tiếng Anh và tên riêng:

- `z` là chữ thường nếu từ chưa có dấu; `zalo`, `zero`, `amazon`, `mazda`, `pizza` giữ nguyên. Nó chỉ là lệnh xóa khi đang tồn tại dấu thanh.
- Những onset không thể bắt đầu âm tiết Việt không tiêu thụ modifier: `free`, `smart`, `javascript`, `zoom`, `jazz`, `frozen` giữ nguyên.
- Nếu một phím dấu đã tạm bị tiêu thụ nhưng phần sau chứng minh từ là Latin, engine khôi phục phím thô; `Vinfast` không bị giữ thành `Vínfast`.
- Việc bảo vệ không dựa trên danh sách từ cố định: `Router`, `user`, `order`, `server`, `address`, `google`, `power` chỉ là các ca kiểm thử cho cơ chế chuỗi phím gốc và kiểm tra âm tiết.
- Khi một từ đã được khôi phục về Latin, Backspace giữ trạng thái đó cho tới khi xóa qua modifier đầu tiên: `Google → Googl → Goog → Goo → Go`, `pixel → pixe → pix → pi`; phần còn lại không bị đổi ngược thành `Gô` hay `pĩ`.
- Hoa/thường của modifier được lấy từ phím người dùng thực sự bấm. Ví dụ `A+s+i+s` tạo `Ais`; chỉ `A+s+i+S` mới tạo `AiS`.
- Nhầm phím `s` cạnh `d` được sửa có điều kiện: `ds` đứng riêng vẫn giữ nguyên, nhưng khi có nguyên âm theo sau thì `dsa...` được hiểu như `dda...`; ví dụ `dsangwj` → `đặng`.
- **Backspace trên editor:** khi chuỗi co lại (prefix-shrink), IME chỉ xóa phần đuôi trên `InputConnection`, không xóa cả từ rồi `commitText` lại dạng ngắn. Trên một số máy (ví dụ Vivo OriginOS) cách ghi lại `Saf` sau `Safe` có thể nhân đôi chữ tone Latin thành `Saff`. Cùng cơ chế áp dụng cho tone `s f r x j` và shape `aa ee oo aw ow uw dd`.
- Escape shape (`aaa`→`aa`, `ddd`→`dd`, …): Backspace xóa đúng một ký tự **hiển thị**, kể cả khi buffer phím gốc dài hơn chuỗi đang thấy.

#### Phạm vi bật Telex và chỉnh sửa

- Bật trong ô text thường, multiline, URL và web editor/ô tìm kiếm web.
- Tắt trong email, web email, mật khẩu, visible password và các ô số để không sửa địa chỉ hoặc dữ liệu nhạy cảm ngoài ý muốn.
- Tương thích các ô tìm kiếm tùy biến làm mất `TYPE_CLASS_TEXT` hoặc không giữ composing span: KAI Board nhận diện các cờ text an toàn và dùng cơ chế thay thế trực tiếp. Trường số, điện thoại và dữ liệu nhạy cảm vẫn không bật Telex.
- Phím Enter ưu tiên action do ứng dụng khai báo (`Search`, `Go`, `Send`, `Next`, `Done`); chỉ xuống dòng khi editor không cung cấp action hoặc yêu cầu Enter thuần.
- Khi kéo con trỏ vào giữa từ, KAI Board kết thúc composing trước khi chỉnh sửa; Backspace vì vậy xóa đúng ký tự ngay trước con trỏ thay vì xóa ký tự cuối của từ. Backspace ở cuối từ vẫn xóa và dựng lại Telex từ chuỗi phím gốc.
- Sau khi đã Space, người dùng có thể Backspace về từ trước và thêm dấu/biến âm; trạng thái composing được dựng lại từ nội dung trước con trỏ.
- Chọn từ gợi ý hoàn thiện từ đó, thêm Space và chuyển sang từ mới.
- Tự viết hoa khi bắt đầu nhập hoặc xuống dòng mới; không tự thêm Space hay bật Shift sau dấu câu.
- Giữ Backspace để xóa tăng tốc; giữ hoặc chạm hai lần Shift để bật Caps Lock. Shift/Caps Lock được trả về trạng thái thường khi mở lại bàn phím cho một phiên nhập mới.

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
- Panel clipboard: card cao (2 hàng phím), vuốt dọc xem thêm mục, text dài wrap/ellipsis theo độ rộng thật (không tràn bo góc); vuốt không dán nhầm.
- Nhận diện offline OTP, email, URL và số điện thoại để chèn nhanh phần hữu ích.
- Ghi chú clipboard có quản lý thêm, sửa và xóa riêng.
- Inline Autofill hỗ trợ OTP và trình quản lý mật khẩu trên Android 11 trở lên khi hệ thống cung cấp dữ liệu.

## AI và dịch

### KAI AI

- Nhập yêu cầu trực tiếp trên bàn phím (ô có caret, chạm/kéo đặt con trỏ, vuốt Space dịch cursor), gửi thủ công hoặc tự gửi sau khoảng dừng có thể cấu hình.
- Gợi ý lệnh offline **từng từ tiếp theo** (học từ prompt đã gửi): ví dụ gõ \Tiêu\ → \đề\, chọn xong mới gợi ý từ sau — không gộp cụm \đề ngắn\ trên một chip.
- Các giọng văn: Hầm hố giật gân, Hài hước, Tò mò bí ẩn, Thời sự tin tức, Truyền động lực, Tối giản tinh tế và Tự động ngẫu nhiên (model tự chọn; app không xoay vòng tone).
- Làm sạch output: gỡ ngoặc kép bọc tiêu đề/`""` và code fence thừa trước khi dán vào ô nhập.
- Hỗ trợ nhiều API key, quét model, lưu trạng thái/model theo từng key và tự chuyển khi gặp quota, timeout hoặc lỗi máy chủ phù hợp.
- Chạm giữ và kéo card API key để đặt thứ tự ưu tiên; thứ tự chỉ được lưu sau khi thả.
- API key được mã hóa bằng Android Keystore và không được ghi vào source, log hoặc file sao lưu.
- Lỗi **HTTP 413** từ provider = payload quá lớn (không phải do prompt một dòng ngắn); kiểm tra key/model/mạng.

Nhà cung cấp hiện hỗ trợ:

| Nhà cung cấp | Nhận diện tự động |
| --- | --- |
| OpenRouter | `sk-or-` |
| Gemini | `AIza` |
| OpenAI | `sk-` |
| Groq | `gsk_` |
| NVIDIA NIM | `nvapi-` |

### Dịch và giọng nói

- Dịch bằng Google ML Kit; người dùng chọn và tải model theo từng ngôn ngữ để sử dụng offline.
- Ô nguồn dịch trên bàn phím chỉnh sửa như text field (cursor, chạm/kéo, Space vuốt, Telex tại cursor).
- Mở AI/Dịch từ panel clipboard không kẹt clipboard hay rơi về chỉ phím chữ.
- Model dịch không được đóng gói sẵn vào APK, giúp giảm dung lượng tải ban đầu.
- Mic cho AI và Dịch hiển thị lời nói trực tiếp trong vùng bàn phím, có tạm dừng/tiếp tục, Backspace và chỉnh sửa trước khi xử lý.
- Khi tạm dừng mic Dịch, văn bản được đưa vào ô dịch và quá trình dịch tự chạy nếu model sẵn sàng.
- Ô thử trong Cài đặt dạng sticky gọn (không tự bật IME khi mở app).

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

Cài nhanh lên thiết bị đang kết nối ADB (một máy, hoặc chỉ định serial):

```powershell
.\dev-install.cmd -WithTests
.\dev-install.cmd -Serial <adb-serial>
```

`dev-install` dò JDK 17 từ `JAVA_HOME` hoặc Microsoft/Eclipse Temurin; ADB từ Android SDK mặc định.

Mọi thay đổi Telex bắt buộc chạy ma trận `s f r x j`, `dd`, `aa ee oo`, `aw ow uw` (gõ đúng/sai thứ tự, hoa/thường, hoàn tác, Backspace, con trỏ giữa từ, va chạm Latin/Anh). Unit test còn cover composer/OEM editor sync, policy input, gợi ý, emoji, clipboard, touch, AI, palette.

Thay đổi touch, IME lifecycle, Telex/Backspace, AI, mic hoặc dịch cần thiết bị thật. Checklist tick (smoke 5 phút, app, Telex, AI, debug): [khaiez/TESTING.md](khaiez/TESTING.md).

Dự án **không** có pipeline ký/phát hành store. Không commit API key hoặc dữ liệu người dùng.

## Cấu trúc dự án

```text
app/src/main/java/vn/kai/board/
├── ai/            Provider, model discovery, key pool và AI request
├── ime/           Vòng đời IME; applyComposingText / InputConnection
├── input/         TelexWordComposer, ComposingEditorSync, gợi ý, clipboard…
├── telex/         TelexEngine (tone/shape thuần)
├── settings/      Preferences, backup và Theme Extensions
├── touch/         Hit-test, pointer, slide, giữ phím và repeat
├── translation/   Quản lý model và luồng dịch ML Kit
├── ui/            Renderer bàn phím, geometry và preview theme
└── voice/         Speech recognition cho mic thường, AI và Dịch

theme-packs/        Các gói theme JSON cài riêng
khaiez/             Kiến trúc, kiểm thử (checklist), riêng tư, debug
scripts/            dev-install, adb-shot
```

Đường gõ chính:

```text
MotionEvent → TouchDispatcher → KeyAction → KaiBoardImeService → InputConnection
```

Telex: `TelexWordComposer` + `TelexEngine` → `applyComposingText` (prefix-shrink / direct commit OEM).  
Đường gõ không network, không tải model, không parse dữ liệu lớn trên hot path.

## Tài liệu

Chỉ mục đầy đủ: [khaiez/README.md](khaiez/README.md).

| Tài liệu | Mô tả |
| --- | --- |
| [Handoff / máy khác](khaiez/HANDOFF.md) | Việc gần đây + tiếp theo |
| [Kiến trúc](khaiez/ARCHITECTURE.md) | Module, Telex, ghi editor OEM |
| [Kiểm thử](khaiez/TESTING.md) | Unit + checklist tick thiết bị |
| [Changelog](khaiez/CHANGELOG.md) | Lịch sử phát triển |
| [Quyền riêng tư](khaiez/PRIVACY.md) | Dữ liệu cục bộ, AI, backup |
| [Bảo mật](khaiez/SECURITY.md) | Secret, báo cáo lỗ hổng |
| [Lộ trình](khaiez/ROADMAP.md) | Gần nhất / ổn định debug |
| [Đóng góp](khaiez/CONTRIBUTING.md) | Guardrail PR |
| [Theme Extension](theme-packs/README.md) | Schema theme JSON |
| [Thông báo bên thứ ba](THIRD_PARTY_NOTICES.md) | License phụ thuộc |

## Lưu ý

KAI Board đang ở chế độ **debug nội bộ**. Cần kiểm thử thực tế trên nhiều ứng dụng nhập liệu và tuân thủ điều khoản của từng dịch vụ AI/dịch khi dùng API key cá nhân.
