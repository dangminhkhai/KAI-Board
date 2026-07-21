# Lộ trình

## Đã xong gần đây (2026-07)

- [x] Telex raw/display/lock; restore Latin; escape UniKey tone + shape.
- [x] Backspace Latin/OEM Vivo: delete-only prefix-shrink (`Safe`→`Saf`, không `Saff`).
- [x] Ma trận modifier `s f r x j`, `aa ee oo aw ow uw dd`; escape shape BS visible.
- [x] Checklist thiết bị đầy đủ trong [TESTING.md](TESTING.md).
- [x] Theme Extension gallery; clipboard classifier; privacy mode; dictionary packs.
- [x] Gợi ý từ: thay prefix khi chọn suggestion (không `Ttôi` trên direct-commit OEM).
- [x] **P0 gợi ý cụm từ** — smoke user: tạm ổn, chưa thấy lỗi (2026-07-21).
- [x] **P1 gợi ý cụm từ** — mid-word blend, decay 21 ngày, seed ~100 cặp + setting.
- [x] **Gói cụm full** — ~720k (~8 MB) collocation+Viet74K+OpenSubtitles; warmUp; không parse khi gõ; xóa seed APK; personal+decay #1.
- [x] Gỡ pipeline release / signing; dự án debug-only (`0.1.0-debug`).

## Gần nhất

- [x] Smoke máy: P1 seed + PhrasePack + clear học không đụng pack (user, 2026-07-21).
- [x] (P2 cụm từ) UI tab Cụm đã học + xóa từng/hết cụm; accept-rate counters không thu text.
- [ ] Hoàn thiện lỗi AI, micro và dịch còn lại trên Samsung/Vivo (ngoài Telex).
- [ ] Đo cold/warm IME show và key-down-to-commit (kịch bản lặp).
- [ ] Chạy full checklist [TESTING.md](TESTING.md) trên Notes / Chrome / Zalo / Messenger / email / password.
- [ ] Hoàn thiện thông báo quyền riêng tư trong UI nếu còn thiếu so với [PRIVACY.md](PRIVACY.md).

## Ổn định debug

- [ ] Ma trận app: Zalo, Messenger, Chrome, Notes, email, ô mật khẩu — tick trong TESTING.md.
- [ ] Accessibility, dark mode, xoay, one-hand, nhiều mật độ màn hình.
- [ ] Giữ vòng lặp build–install–test chỉ với APK debug.

## Tiếp theo (kỹ thuật)

- [ ] Cải thiện xếp hạng gợi ý và quản lý từ học.
- [ ] Benchmark/Perfetto tự động; ANR/crash **không** thu nội dung gõ.
- [ ] (Tùy chọn) EditorSession tách khỏi IME service — xem hướng “học từ Floris” trong thảo luận kiến trúc.
