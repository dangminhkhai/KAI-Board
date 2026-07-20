# Lộ trình

## Đã xong gần đây (2026-07)

- [x] Telex raw/display/lock; restore Latin; escape UniKey tone + shape.
- [x] Backspace Latin/OEM Vivo: delete-only prefix-shrink (`Safe`→`Saf`, không `Saff`).
- [x] Ma trận modifier `s f r x j`, `aa ee oo aw ow uw dd`; escape shape BS visible.
- [x] Checklist thiết bị đầy đủ trong [TESTING.md](TESTING.md).
- [x] Theme Extension gallery; clipboard classifier; privacy mode; dictionary packs (trong nhánh unreleased / 1.2.x docs).

## Gần nhất

- [ ] Hoàn thiện lỗi AI, micro và dịch còn lại trên Samsung/Vivo (ngoài Telex).
- [ ] Đo cold/warm IME show và key-down-to-commit (kịch bản lặp).
- [ ] Chạy full checklist [TESTING.md](TESTING.md) trên Notes / Chrome / Zalo / Messenger / email / password.
- [ ] Hoàn thiện thông báo quyền riêng tư trong UI nếu còn thiếu so với [PRIVACY.md](PRIVACY.md).

## Trước beta

- [ ] Ma trận app: Zalo, Messenger, Chrome, Notes, email, ô mật khẩu — tick trong TESTING.md.
- [ ] Accessibility, dark mode, xoay, one-hand, nhiều mật độ màn hình.
- [ ] Xác minh ký release trên máy thứ hai; sao lưu keystore + mapping R8 ngoài Git.
- [ ] Tag phiên bản sau unreleased (ví dụ 1.2.1 hoặc 1.3.0) khi gom đủ fix Telex/OEM + UI.

## Sau beta

- [ ] Cải thiện xếp hạng gợi ý và quản lý từ học.
- [ ] Benchmark/Perfetto tự động; ANR/crash **không** thu nội dung gõ.
- [ ] Listing, ảnh chụp, chính sách Play Store.
- [ ] (Tùy chọn) EditorSession tách khỏi IME service — xem hướng “học từ Floris” trong thảo luận kiến trúc.
