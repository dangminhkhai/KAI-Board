# IME smoke (5–10 phút) — chạy sau mỗi `adb install`

Máy: USB arm64 (`app-arm64-v8a-debug.apk`). Package: `vn.kai.board`.

## Bắt buộc (G1–G9)

| # | Thử | Pass |
| --- | --- | --- |
| S1 | Mở ô gõ → QWERTY chữ | [ ] |
| S2 | Emoji → ẩn bàn phím → mở lại → **chữ**, không emoji | [ ] |
| S3 | Clipboard → ẩn → mở lại → **chữ** | [ ] |
| S4 | AI (nếu có key) hoặc mở panel AI → ẩn → mở lại → **chữ** | [ ] |
| S5 | Dịch → ẩn → mở lại → **chữ** | [ ] |
| S6 | Mic/voice panel (nếu bật) → ẩn → mở lại → **chữ** | [ ] |
| S7 | `?123` → ẩn → mở lại → **chữ** (không kẹt symbols) | [ ] |
| S8 | Settings KAI Board: **không** tự bật IME; chạm ô thử mới mở | [ ] |
| S9 | Clipboard: copy text → panel thấy; giữ mục → popup Ghim/Xóa; nhả tay popup **còn** | [ ] |
| S10 | Xóa 1 mục; thùng rác xóa chưa ghim (xác nhận); mục ghim giữ; không tự mọc lại clip cũ | [ ] |
| S11 | Ô URL hoặc Google: dán ảnh clipboard → toast nhẹ, **không** crash / lỗi sao chép hệ thống | [ ] |
| S12 | Ô thử Settings: dán ảnh OK nếu đã copy ảnh | [ ] |
| S13 | Chữ phím **không** bị nhỏ sau khi mở/đóng clipboard | [ ] |
| S14 | Ô mật khẩu: private chrome, không AI/clipboard | [ ] |

## Telex tối thiểu (Vivo / OEM direct-commit)

| # | Thử | Pass |
| --- | --- | --- |
| T1 | `Safe` + BS → không `Saff` | [ ] |
| T2 | `Cafe` + BS — hành vi đúng policy hiện tại | [ ] |
| T3 | Từ Latin có s/f (`case`, `Google`) BS ổn | [ ] |

## Ghi nhật ký (khi fail)

`khaiez/TESTING.md` → Nhật ký thiết bị: ngày, model/serial, build, S#/T# fail, 1 dòng nguyên nhân nếu biết.

## Agent checklist trước khi báo “xong”

- [ ] `testDebugUnitTest` xanh (hoặc nói rõ chưa chạy)
- [ ] APK arm64 cài được
- [ ] Í nhất S2–S3 + S8 + S9 đã verify (hoặc user xác nhận)
- [ ] Nếu đụng Telex: T1
