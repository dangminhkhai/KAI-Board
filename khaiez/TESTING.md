# Kiểm thử

Đánh dấu `[x]` khi đạt trên thiết bị; để `[ ]` nếu chưa / fail. Ghi máy + app + ngày ở mục [Nhật ký](#nhật-ký-thiết-bị).

**Không** chụp/log nội dung nhạy cảm (mật khẩu, OTP thật, API key, tin nhắn riêng).

Liên quan: [ARCHITECTURE.md](ARCHITECTURE.md) (ghi editor), [CONTRIBUTING.md](CONTRIBUTING.md), [RELEASE.md](RELEASE.md).

---

## Lỗi Telex/Backspace (Vivo) — đã vá (delete-only shrink)

- Hiện tượng: `Safe` + Backspace → `Saff`; `Cafe` → `Caff` (OEM nhân đôi tone Latin khi `commitText` từ ngắn).
- Vá: prefix-shrink **chỉ xóa đuôi**; `commitText` từ kết thúc bằng modifier `sfrxjaeowd` tách body/tail; Latin lock; escape shape `aaa→aa` BS xóa đúng 1 ký tự hiển thị.
- Unit: `SafeTraceTest`, `ComposingEditorSyncTest`, `TelexModifierBackspaceMatrixTest`, `TelexWordComposerTest`, `TelexEngineTest`.
- Xác nhận máy (2026-07-20, Vivo V2366GA): modifier + Safe→Saf **pass** — xem nhật ký.

### Xác nhận regression (bắt buộc sau mỗi đổi Telex/IME)

- [x] Gõ `T` → chọn gợi ý `tôi` → ra `tôi ` (không `Ttôi`) — pass (user, ~2026-07-21)
- [x] `Safe` → BS → `Saf` (không `Saff` / `Sà`) — pass Vivo
- [x] Gợi ý cụm (P0): học bigram sau Space/chọn gợi ý; privacy tắt học; chưa thấy lỗi — smoke pass (user)
- [ ] Gợi ý cụm (P1): sau `xin`+Space thấy seed `chào`; gõ `xin ` rồi `c` thấy `chào` ưu tiên; tắt “Gợi ý cụm cơ bản” thì mất seed
- [ ] `Cafe` → BS → `Caf`
- [ ] `case` → BS → `cas`
- [ ] `care` → BS → `car`
- [ ] `Google` xóa dần: `Googl` … `Go` (không `Gô`)
- [ ] `Mí` (từ `Mis`) → BS một lần → `M`
- [ ] `aaa` (escape `aa`) → BS → `a`
- [ ] `ddd` (escape `dd`) → BS → `d`
- [ ] Tone `s f r x j` + shape `aa ee oo aw ow uw dd` (mẫu trong ma trận dưới)

---

## Tự động

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Mọi thay đổi Telex bắt buộc bao phủ đủ `s f r x j`, `dd`, `aa ee oo`, `aw ow uw`; modifier đúng/sai thứ tự; hoàn tác kiểu UniKey; Backspace từng bước trên từ Việt và Latin/Anh; khôi phục chuỗi phím gốc; hoa/thường của phím modifier; con trỏ giữa composing. Các test còn lại bao phủ viết hoa đầu ô/đầu dòng nhưng không sau dấu câu; hashtag/email, input policy, selection deletion, touch dispatcher, repeat, gợi ý, voice routing, AI key pool/cancellation và sao lưu không chứa secret.

---

## Checklist 5 phút (smoke)

Chạy trước khi cài bản debug/release lên máy chính.

- [ ] `Safe` / `case` / `care` / `Google` + BS
- [ ] `mas` → `má` → BS → `m`
- [ ] `aaa` → `aa` → BS → `a`
- [ ] Zalo **hoặc** Messenger: gõ Telex + BS
- [ ] Chrome URL/search: gõ + BS
- [ ] Ô mật khẩu: không Telex / không gợi ý cá nhân
- [ ] Emoji cờ (🇻🇳) hoặc ZWJ + BS: không ô vuông

---

## 1. Ứng dụng & loại ô

| # | Hạng mục | Pass |
| --- | --- | --- |
| 1.1 | Ô thử trong app KAI Board | [ ] |
| 1.2 | Samsung Notes / Notes hệ thống | [ ] |
| 1.3 | Chrome — ô thường | [ ] |
| 1.4 | Chrome — URL / search | [ ] |
| 1.5 | Zalo | [ ] |
| 1.6 | Messenger | [ ] |
| 1.7 | Gmail / email | [ ] |
| 1.8 | Ô mật khẩu / visible password | [ ] |
| 1.9 | Ô số / OTP / điện thoại (numpad) | [ ] |
| 1.10 | Ô email (`@`, Telex tắt) | [ ] |

---

## 2. Telex — Latin / Anh (OEM + lock)

| # | Thử | Kỳ vọng | Pass |
| --- | --- | --- | --- |
| 2.1 | `Safe` + BS | `Saf` | [ ] |
| 2.2 | `Cafe` + BS | `Caf` | [ ] |
| 2.3 | `case` + BS | `cas` | [ ] |
| 2.4 | `care` + BS | `car` | [ ] |
| 2.5 | `pixel` + BS lần lượt | `pixe` → … giữ Latin | [ ] |
| 2.6 | `object` + BS | giữ Latin | [ ] |
| 2.7 | `Safe` → BS → `Saf` → gõ `e` | lại `Safe`, không `Sà`/`Saff` | [ ] |
| 2.8 | `Vinfast` / `Vin`+`f`+`f`+`ast` | Latin, không `Vín…` | [ ] |
| 2.9 | `free`, `zoom`, `javascript` | không nuốt modifier | [ ] |
| 2.10 | `Google` xóa dần | không tái tạo `ô` | [ ] |
| 2.11 | `power`, `awesome`, `address` | BS giữ Latin | [ ] |
| 2.12 | Hoa: `Safe`, `Case`, `Care` | BS đúng hoa/thường | [ ] |

---

## 3. Telex — tone `s f r x j`

| # | Thử | Kỳ vọng | Pass |
| --- | --- | --- | --- |
| 3.1 | `mas` → `má` | sắc | [ ] |
| 3.2 | `maf` → `mà` | huyền | [ ] |
| 3.3 | `mar` → `mả` | hỏi | [ ] |
| 3.4 | `max` → `mã` | ngã | [ ] |
| 3.5 | `maj` → `mạ` | nặng | [ ] |
| 3.6 | Escape: `á`+`s` → `as` (tương tự `f r x j`) | hoàn tác UniKey | [ ] |
| 3.7 | `Vin`+tone+tone → `Vins`/`Vinf`/… rồi gõ thêm | lock Latin | [ ] |
| 3.8 | BS sau escape tone | xóa 1 ký tự hiển thị | [ ] |

---

## 4. Telex — shape `aa ee oo aw ow uw dd` (+ `uow`)

| # | Thử | Kỳ vọng | Pass |
| --- | --- | --- | --- |
| 4.1 | `aa` → `â` | | [ ] |
| 4.2 | `ee` → `ê` | | [ ] |
| 4.3 | `oo` → `ô` | | [ ] |
| 4.4 | `aw` → `ă` | | [ ] |
| 4.5 | `ow` → `ơ` | | [ ] |
| 4.6 | `uw` → `ư` | | [ ] |
| 4.7 | `dd` → `đ` | | [ ] |
| 4.8 | `uow` → `ươ` | | [ ] |
| 4.9 | Escape: `aaa`→`aa`, BS→`a` | không no-op | [ ] |
| 4.10 | Escape: `eee`/`ooo`/`aww`/`oww`/`uww`/`ddd`/`uoww` | BS xóa 1 visible | [ ] |
| 4.11 | `tieengs` → `tiếng`; `tuaans` → `tuấn` | | [ ] |
| 4.12 | `duongw` → `dương`; `tuongw` → `tương` | | [ ] |
| 4.13 | `dangd` → `đang` | trailing `d` | [ ] |

---

## 5. Telex — đặt dấu & biên

| # | Thử | Kỳ vọng | Pass |
| --- | --- | --- | --- |
| 5.1 | `gias` → `giá`; `quas` → `quá` | `gi`/`qu` phụ âm kép | [ ] |
| 5.2 | `hoaf` → `hòa`; `toans` → `toán` | kiểu mới | [ ] |
| 5.3 | `dsangwj` → `đặng` | typo `ds`→`đ` | [ ] |
| 5.4 | `zalo`, `zero`, `amazon` | `z` literal khi chưa có dấu | [ ] |
| 5.5 | `Mis` → `Mí` → BS → `M` | xóa visible | [ ] |
| 5.6 | `Goo` → `Gô` → BS → `G` | xóa visible | [ ] |
| 5.7 | `tieengs` → `tiếng` → BS | `tiến` (một grapheme cuối) | [ ] |
| 5.8 | Con trỏ giữa từ + BS / gõ | finish composing, xóa đúng chỗ | [ ] |
| 5.9 | Selection + BS | xóa vùng chọn | [ ] |
| 5.10 | Hoa modifier: `Asis`→`Ais`, `AsiS`→`AiS` | | [ ] |

---

## 6. Cử chỉ & vòng đời IME

| # | Thử | Pass |
| --- | --- | --- |
| 6.1 | Cold show IME (tắt app → mở ô gõ) | [ ] |
| 6.2 | Warm show (chuyển app rồi quay lại) | [ ] |
| 6.3 | Giữ Backspace — xóa tăng tốc, không chèn chữ | [ ] |
| 6.4 | Vuốt Space trái/phải — con trỏ | [ ] |
| 6.5 | Shift giữ / double-tap Caps; mở lại IME về thường | [ ] |
| 6.6 | Long-press ký hiệu trên phím chữ | [ ] |
| 6.7 | Xoay ngang | [ ] |
| 6.8 | One-hand / preset cao–thấp / nâng bàn phím | [ ] |
| 6.9 | Font lớn + insets | [ ] |

---

## 7. Emoji, clipboard, gợi ý

| # | Thử | Pass |
| --- | --- | --- |
| 7.1 | Emoji skin tone / ZWJ / cờ + BS — không ô vuông | [ ] |
| 7.2 | Tìm emoji từ khóa Việt không dấu / Anh | [ ] |
| 7.3 | Clipboard: OTP / email / URL / SĐT — chèn đúng phần tách | [ ] |
| 7.4 | Không nhầm số thường thành OTP | [ ] |
| 7.5 | Ghim + TTL 1 giờ / 1 ngày / không xóa | [ ] |
| 7.6 | Gợi ý: AI → cá nhân → offline; decay | [ ] |
| 7.7 | Tải / xóa gói từ điển mở rộng | [ ] |

---

## 8. AI, micro, dịch

| # | Thử | Pass |
| --- | --- | --- |
| 8.1 | AI thành công | [ ] |
| 8.2 | AI hủy giữa chừng | [ ] |
| 8.3 | Timeout / offline | [ ] |
| 8.4 | 401/403, 429, 5xx — fallback đúng thứ tự key | [ ] |
| 8.5 | `400` **không** nhảy key | [ ] |
| 8.6 | Kéo thả thứ tự API key — chỉ lưu khi thả | [ ] |
| 8.7 | Micro: live text, pause/resume, BS, hủy, im lặng | [ ] |
| 8.8 | Micro thiếu speech service | [ ] |
| 8.9 | Dịch: có/không model, swap ngôn ngữ, offline | [ ] |

---

## 9. Giao diện, sao lưu, release

| # | Thử | Pass |
| --- | --- | --- |
| 9.1 | Theme sáng / tối / theo hệ thống | [ ] |
| 9.2 | Gallery theme 2 cột, preview, áp dụng / xuất / xóa | [ ] |
| 9.3 | Import theme file / HTTPS | [ ] |
| 9.4 | Sticky ô thử — không tự bật IME khi mở app | [ ] |
| 9.5 | Backup/restore: theme + Smartbar; **không** key/clipboard/notes | [ ] |
| 9.6 | `assembleRelease`, `lintRelease`, R8/shrink | [ ] |
| 9.7 | Chữ ký v2/v3, package/version/ABI, SHA-256 | [ ] |

---

## Nhật ký thiết bị

| Ngày | Máy / OS | Bản APK | Người thử | Ghi chú |
| --- | --- | --- | --- | --- |
| 2026-07-20 | Vivo V2366GA / OriginOS 6 | debug 1.2.0 (Telex BS fix) | | Telex modifier + Safe→Saf: **pass** |
| 2026-07-21 | Vivo (cùng máy) | debug (gợi ý + Telex) | | Chọn gợi ý `T`→`tôi` OK; **P0 cụm từ** smoke: tạm ổn, chưa thấy lỗi |
| | | | | |

---

## Thiết bị (tóm tắt nhanh)

- Cold/warm IME; Telex nhanh; BS bấm/giữ; selection; emoji ZWJ/cờ.
- AI multi-key + fallback; gợi ý; clipboard; cử chỉ Space/Shift; mic; dịch; theme; backup; release.

Ứng dụng mục tiêu: Samsung Notes, Chrome, Zalo, Messenger, email, ô mật khẩu.
