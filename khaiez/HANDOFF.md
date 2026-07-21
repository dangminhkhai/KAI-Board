# Handoff — làm việc tiếp

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**Cập nhật:** 2026-07-21 — clipboard rich, keystore debug chung, settings **không** tự mở IME  

```powershell
git pull origin main
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd
# hoặc: adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

**Trạng thái:** debug-only `0.1.0-debug` (`versionCode 1`). Không pipeline release/signing.

**Chữ ký debug dùng chung:** `keystore/android-debug.keystore` (xem `keystore/README.md`). Máy khác `git pull` + build → cùng cert, cài đè không cần gỡ app.

---

## Clipboard (mới nhất)

| Khả năng | Chi tiết |
| --- | --- |
| Lưu | Text, HTML (plain+html), ảnh local JPEG |
| Panel | Tab lịch sử + ghi chú; badge OTP/email/URL/SĐT/HTML/ẢNH |
| Giữ mục | Popup pill **Ghim / Xóa** (không dialog hệ thống) |
| Thùng rác | Xóa hết chưa ghim + xác nhận pill; ★ giữ |
| Dán ảnh | `commitContent` nếu `image/*`; URL/search → toast, không setPrimaryClip |
| Ô thử | Sticky **Ô thử (thay Messages)** — nhận ảnh; **không** auto-focus khi mở Settings |
| Privacy | Private session ẩn clipboard; backup **không** clipboard/notes |
| Debug sign | `keystore/android-debug.keystore` — cùng chữ ký mọi máy |

**Files:** `ClipboardHistoryStore.kt`, `KeyboardView` (popup/draw), `KaiBoardImeService` (paste), `RichClipboardTestEditText.kt`, `MainActivity` (no auto IME), `keystore/`

---

## Gợi ý cụm từ

| Tầng | Nguồn | Ghi chú |
| --- | --- | --- |
| **1 Personal** | `PhraseLearningStore` (`pairs_v2`) | Decay **21 ngày**, max 512 — **luôn #1** |
| **2 Pack** | `PhrasePack` / `vi_social.tsv` | ~**720k** cặp (~**8 MB**); 5–20 MB OK |
| ~~Seed APK~~ | **đã xóa** | Không còn `PhraseSeedCatalog` |

**Rank:** personal → pack.

**Nguồn build pack (full):**

1. `phrase-packs/collocations_vi.txt` (tiêu đề, hoàng hôn, …)  
2. Viet74K multi-token (duyet/vietnamese-wordlist)  
3. OPUS OpenSubtitles VI mono (bigram tần suất)  

```powershell
# raw (gitignored): phrase-packs/raw/vi_opensub.txt.gz, Viet74K.txt
py -3 tools\build_phrase_pack.py
# → cập nhật PhrasePack.EXPECTED_SHA256
```

**Hiệu năng / RAM**

- Gõ: **chỉ lookup RAM** — không parse file trên hot path.  
- Cache trống: `warmUpAsync` nền, frame đó pack rỗng (personal vẫn chạy).  
- Preload: IME `onCreate` + sau cài gói.  
- RAM sau warm: ~**30–40 MB** (máy 16 GB không vấn đề).  

**Cài trên máy:** Settings → Gói cụm từ → Xóa gói cũ → Tải lại.  
**Setting `phrase_seed`:** chỉ bật/tắt cụm nhiều từ trong **từ điển word** (không phải seed APK).  
**FrequencyWords:** gợi ý **từ** (card tải VI/EN), không nằm trong gói cụm.

---

## P2 cụm personal (đã code)

- Tab **Từ | Cụm** trong `LearnedWordsActivity`
- `listEntries` / `removeExact` / xóa hết cụm
- `PhraseStats`: shown/accepted personal|pack — **không** lưu text; long-press dòng stats để xóa số liệu

## Next

- [x] Checklist **auto** 2026-07-21: 331 unit tests OK; Telex regression unit OK; SM-N986N IME enabled  
- [x] Smoke **tay** 1–2–3 (user): Safe/Cafe/case BS; P2 tab Cụm; Zalo/Chrome Telex+BS  
- [x] Clipboard rich + popup Gboard + clear/pin/delete + ô thử settings (user OK)  
- [x] Settings **không** tự mở bàn phím; shared debug keystore  
- [ ] (Tùy) ma trận app rộng / AI-mic-dịch / `Mí`+BS  
- [ ] (Tùy) unit test `ClipboardHistoryStore` suppress-fingerprint / TTL + ghim  

---

## Files chính

```text
app/.../input/PhraseLearningStore.kt
app/.../input/PhrasePack.kt
app/.../input/ClipboardHistoryStore.kt   # text/HTML/image + suppress re-import
app/.../ui/KeyboardView.kt               # clipboard panel + Gboard popups
app/.../ui/RichClipboardTestEditText.kt  # settings test field (image/*)
app/.../ime/KaiBoardImeService.kt        # warmUpAsync + paste rich
tools/build_phrase_pack.py
phrase-packs/collocations_vi.txt
phrase-packs/vi_social.tsv
phrase-packs/README.md
app/src/main/assets/phrase_packs/vi_social.tsv
```
