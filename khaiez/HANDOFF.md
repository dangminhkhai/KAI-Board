# Handoff — làm việc tiếp

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**Cập nhật:** 2026-07-21 — PhrasePack full (~720k), không seed APK, debug-only  

```powershell
git pull origin main
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd
```

**Trạng thái:** debug-only `0.1.0-debug` (`versionCode 1`). Không pipeline release/signing.

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

## Next

- [ ] Smoke máy pack ~720k: `tiêu`/`hoàng`/`xin` + Space; personal vẫn thắng  
- [ ] P2: UI xem/xóa từng cụm personal  
- AI/mic/dịch Samsung/Vivo; full checklist TESTING.md  

---

## Files chính

```text
app/.../input/PhraseLearningStore.kt
app/.../input/PhrasePack.kt
app/.../ime/KaiBoardImeService.kt   # warmUpAsync
tools/build_phrase_pack.py
phrase-packs/collocations_vi.txt
phrase-packs/vi_social.tsv
phrase-packs/README.md
app/src/main/assets/phrase_packs/vi_social.tsv
```
