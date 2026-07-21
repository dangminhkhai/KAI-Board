# Handoff — làm việc tiếp

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**HEAD (gần nhất):** `b827879` — Settings không tự mở IME  
**Cập nhật:** 2026-07-21  

```powershell
git pull origin main
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd
# hoặc USB:
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

| | |
| --- | --- |
| **Build** | Debug-only `0.1.0-debug` · `versionCode 1` · không release pipeline |
| **Package** | `vn.kai.board` |
| **ABI** | splits: `arm64-v8a`, `x86_64` (cài arm64 trên Vivo/Samsung) |
| **Signing** | **Chung mọi máy:** `keystore/android-debug.keystore` (alias `androiddebugkey` / pass `android`) — xem `keystore/README.md` |
| **Cài đè** | `adb install -r` OK giữa các PC sau khi pull keystore; mismatch → gỡ app hoặc dùng đúng shared key |

---

## Session 2026-07-21 — đã xong (user OK)

### 1. Private / mật khẩu
- Smartbar private: banner **«Riêng tư · mật khẩu»** + icon khóa.
- Ẩn AI / clipboard / mic / emoji / settings; Telex + gợi ý + học tắt.
- `KeyAction.NoOp` cho chrome không bấm được.

### 2. Resize Gboard-style
- Adjustment mode: viewport max một lần; kéo = rebuildKeys + invalidate (throttle layout khi cần).
- Apply geometry khi thả / ✓; không scaleY méo phím.
- Insets transparent khi resize (app phía trên chạm được).

### 3. Theme màu
- **System / Sáng / Tối** giữ đủ 3 nút.
- Bộ màu mặc định **neutral grayscale** (bỏ accent mint xanh) — `KeyboardThemePalette.neutralLight/Dark`.

### 4. Clipboard rich
| | |
| --- | --- |
| Lưu | TEXT / HTML / IMAGE (JPEG local `filesDir/clipboard_images/`) |
| Smart | OTP, email, URL, SĐT (offline) |
| Panel | Tab lịch sử + ghi chú; badge + thumbnail ảnh |
| Giữ mục | Popup pill **Ghim · Xóa** (vẽ trên keyboard, không Material dialog) |
| Nhả tay | Long-press mở popup → UP **không** đóng (`suppressClipboardPopupUp`) |
| Thùng rác | Xóa hết **chưa ghim** + popup xác nhận gọn; ★ giữ |
| Xóa 1 mục | Menu Ghim/Xóa; suppress re-import clip hệ thống |
| Dán ảnh | `commitContent` chỉ khi field `image/*`; URL/search/password → toast, **không** `setPrimaryClip` |
| HTML | Spanned trên text thường; plain trên URI/search |
| Ô thử | Sticky **Ô thử (thay Messages)** — `RichClipboardTestEditText` |
| Settings IME | **Không** `requestFocus` khi mở app — phím chỉ khi user chạm ô / Tùy chỉnh / Xóa ô thử |
| Privacy | Private ẩn clipboard; backup **không** clipboard/notes |

### 5. Debug keystore (máy khác)
- File trong git: `keystore/android-debug.keystore`
- Gradle: `signingConfigs.sharedDebug` (debug + release debug-only)
- Máy mới: `git pull` → build → `install -r` cùng cert

### 6. Thiết bị đã cài gần đây
| Máy | Serial / model | Ghi chú |
| --- | --- | --- |
| Samsung | `R3CN80C8Y7L` (trước) | Checklist auto / smoke Telex |
| Vivo | `10AE5U24S0000TQ` · **V2366GA** / PD2366 | USB install OK sau gỡ bản versionCode 120 |

---

## Phrase / gợi ý (ổn định trước session)

| Tầng | Nguồn | Ghi chú |
| --- | --- | --- |
| **1 Personal** | `PhraseLearningStore` (`pairs_v2`) | Decay **21 ngày**, max 512 — **luôn #1** |
| **2 Pack** | `PhrasePack` / `vi_social.tsv` | ~**720k** cặp (~**8 MB**) |
| ~~Seed APK~~ | **đã xóa** | Không còn `PhraseSeedCatalog` |

- Rank: personal → pack. Preload: `PhrasePack.warmUpAsync` (IME start).  
- P2: tab **Từ | Cụm** + `PhraseStats` (không lưu text).  
- Build pack: `py -3 tools\build_phrase_pack.py` → cập nhật `EXPECTED_SHA256`.

---

## Next (gợi ý)

- [ ] (Tùy) unit test `ClipboardHistoryStore` (TTL, ghim, suppress fingerprint, clearUnpinned)
- [ ] (Tùy) ma trận app rộng / AI-mic-dịch / Telex `Mí`+BS
- [ ] (Tùy) tăng `versionCode` debug khi cần (tránh downgrade trên máy có bản lạ)
- [ ] Release thật: keystore **riêng**, không dùng `android-debug.keystore`

---

## Files chính (session + phrase)

```text
app/.../input/ClipboardHistoryStore.kt   # text/HTML/image + suppress re-import
app/.../input/KeyAction.kt               # CommitClipboard rich, ClearClipboardUnpinned
app/.../ui/KeyboardView.kt               # panel, popups Gboard, resize, private chrome
app/.../ui/RichClipboardTestEditText.kt  # settings test field image/*
app/.../ime/KaiBoardImeService.kt        # paste rich, insets resize, warmUpAsync
app/.../MainActivity.kt                  # sticky test field, no auto IME
app/.../settings/KeyboardThemePalette.kt # neutral light/dark
app/build.gradle.kts                     # sharedDebug signing
keystore/android-debug.keystore
keystore/README.md
app/.../input/PhraseLearningStore.kt
app/.../input/PhrasePack.kt
tools/build_phrase_pack.py
phrase-packs/vi_social.tsv
khaiez/CHANGELOG.md · TESTING.md · PRIVACY.md · ARCHITECTURE.md
```

---

## Lệnh hay dùng

```powershell
# Test + APK
.\gradlew.bat testDebugUnitTest :app:assembleDebug

# Cài arm64 (Samsung/Vivo)
adb devices -l
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
adb shell am force-stop vn.kai.board

# Nếu chữ ký lệch (máy cài bản khác):
adb uninstall vn.kai.board
adb install app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
```

**Docs chi tiết:** `CHANGELOG.md` (mốc), `TESTING.md` (7.8–7.12 clipboard, 9.4b no-auto-IME), `PRIVACY.md` (ảnh local).

---

## Skill chống regression (đổi máy vẫn dùng)

Nằm **trong git** (mọi máy `git pull` là có):

```text
.grok/skills/kai-board-continue/SKILL.md
.grok/skills/kai-board-continue/references/smoke-ime.md
```

| Cách gọi | |
| --- | --- |
| Slash | `/kai-board-continue` |
| Chat | “đổi máy”, “tiếp tục KAI Board”, “smoke sau cài”, “đừng để tính năng cũ lỗi” |

Agent sẽ: pull/orient HANDOFF → shared keystore → build/install → **smoke G1–G9** (IME panel reset, Settings no auto-IME, clipboard, Telex tối thiểu).
