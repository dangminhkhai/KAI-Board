# Handoff — làm việc tiếp

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**Cập nhật:** 2026-07-21 (clipboard scroll/UI + AI quote sanitize + model filter)

```powershell
git pull origin main
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd
# WiFi (Vivo gần đây):
adb connect 192.168.10.217:37121
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
adb shell am force-stop vn.kai.board
```

| | |
| --- | --- |
| **Build** | Debug-only `0.1.0-debug` · `versionCode 1` · không release pipeline |
| **Package** | `vn.kai.board` |
| **ABI** | splits: `arm64-v8a`, `x86_64` (cài arm64 trên Vivo/Samsung) |
| **Signing** | **Chung mọi máy:** `keystore/android-debug.keystore` — xem `keystore/README.md` |
| **Cài đè** | `adb install -r` OK giữa PC sau pull keystore |

---

## Session 2026-07-21 (mới) — Clipboard UI + AI sanitize

### 0. Clipboard panel (scroll / card / tràn chữ) — đã vá + cài Vivo

| Vấn đề | Vá |
| --- | --- |
| Chữ dài tràn bo góc card | `ClipboardTextLayout` measure + ellipsis + `clipRect`; pad chừa pin |
| Chỉ hiện ~4 card | Card cao 2× row; **scroll dọc** toàn history; chrome cố định |
| Vuốt bị dán nhầm | Scroll slop dọc → không paste / không long-press pin |
| Scroll làm bàn phím mờ | **Không** `rebuildKeys` khi scroll — chỉ `clipboardScrollY` + draw offset |

Files: `ui/ClipboardTextLayout.kt`, `ui/KeyboardView.kt` (clipboard draw/touch), tests `ClipboardTextLayoutTest`.

### 0b. AI tiêu đề có `""` — đã vá + cài Vivo

- Prompt: cấm bọc quote; `AiOutputSanitizer` strip trước `commitText`.
- Tone: Cài đặt chọn 1 giọng; không round-robin phía app.
- Files: `ai/AiOutputSanitizer.kt`, `ai/AiProviderClient.kt`, `AiOutputSanitizerTest`.

### A. Clipboard → AI / Dịch (đã vá)

| Hiện tượng | Vá |
| --- | --- |
| Đang clipboard, bấm AI/Dịch → về phím chữ / kẹt body clipboard | `openAi`/`openTranslator` gọi `closeMediaPanel`; `setAiState`/`setTranslationState(enabled)` clear panel/symbols/voice; `rebuildKeys` ưu tiên `aiMode`/`translationMode` body |
| Offline AI đóng clipboard rồi toast | Check offline **trước** `finishComposing` |
| Mở AI khi đang Dịch (hoặc ngược lại) | `stopAi` / `stopTranslation` trước khi bật mode kia |

### B. Ô input AI & Dịch (thao tác như text field)

- Vẽ chung `drawFeatureInputField`: placeholder, caret, window quanh cursor.
- Chạm / kéo trên ô → `SetAiCursor` / `SetTranslationCursor`.
- Vuốt **Space** trong AI/Dịch → `MoveCursor` nội bộ (không còn tắt gesture).
- Dịch: `translationCursor`; Character/Space/Backspace tại cursor (Telex word trước cursor).
- Cursor-only UI update: `invalidate` không `rebuildKeys` (kéo mượt).

### C. Gợi ý AI command (next-word)

- `AiCommandSuggestionEngine`: candidate size **1**; filter multi-word legacy; không titlecase next-word theo từ trước (`đề` không thành `Đề`).
- Context depth ≥ 1: **không** trộn empty-context starters (`Limo` từ lệnh khác).
- `buildAiSuggestions`: nếu AI đã next-word sau token xong → **chỉ** AI chips (không merge lexicon → hết chip `Tiêu` trùng).
- Unit: `AiCommandSuggestionEngineTest` (Tiêu→đề, no multi-word, no global starter mix).

### D. Ô thử Settings

- Compact sticky: hint `Thử gõ / dán…`, icon X nhỏ, bỏ underline (`setBackgroundResource(0)`), giữ stroke card.

### E. HTTP 413 (Groq) / 410 (NVIDIA) — đã vá + **pass máy**

- **413** / **410** thường do **model** (non-chat / đã gỡ), không phải prompt ô AI thừa dữ liệu.
- Body gửi: system (giọng văn Cài đặt) + `aiPrompt.trim()` only.
- Vá: lọc chat models; NVIDIA không prepend ID chết; 410/413 retry model+key.
- **Xác nhận 2026-07-21 (Vivo):** quét lại key → **Groq pass**, **NVIDIA NIM pass**.

### F. Thiết bị cài gần đây

| Máy | Kết nối | Ghi chú |
| --- | --- | --- |
| Vivo **V2366GA** / PD2366 | WiFi `192.168.10.217:37121` (port wireless đổi khi pair lại) | Install OK 2026-07-21 |
| Samsung SM-N986N | USB `R3CN80C8Y7L` | Smoke Telex trước đó |

---

## Session trước — clipboard / private / keystore (vẫn giữ)

### Private / mật khẩu
- Banner **«Riêng tư · mật khẩu»** + khóa; ẩn AI/clipboard/mic/emoji; Telex + gợi ý tắt.

### Clipboard rich
- TEXT/HTML/IMAGE; popup Ghim·Xóa; clear unpinned; dán ảnh `commitContent`; Settings **không** auto IME.

### Debug keystore
- `keystore/android-debug.keystore` + `sharedDebug` — mọi máy cùng cert.

### Panel reset (G1)
- `resetToLetterKeyboard` khi hide/show IME — không kẹt emoji/clipboard/AI.

---

## Phrase / gợi ý (ổn định)

| Tầng | Nguồn |
| --- | --- |
| **1 Personal** | `PhraseLearningStore` decay 21 ngày |
| **2 Pack** | `PhrasePack` ~720k bigram |

AI bar: **next-word command** tách khỏi phrase chat (xem mục C).

---

## Next (gợi ý)

- [x] Groq + NVIDIA NIM generate sau quét lại model — **pass** (Vivo 2026-07-21)
- [x] Clipboard scroll + card 2× + no overflow + no dim — **cài Vivo**
- [x] AI strip outer quotes on title — **cài Vivo**
- [ ] Smoke tay: scroll clipboard nhiều item; tap vs scroll; AI “viết tiêu đề” không `""`
- [ ] Smoke: clipboard → AI / Dịch; ô Dịch cursor
- [ ] Release: keystore riêng, không dùng android-debug

---

## Files chính (session clipboard/AI UI)

```text
app/.../ui/ClipboardTextLayout.kt            # measure wrap/ellipsis
app/.../ui/KeyboardView.kt                   # clipboard cards, scroll, touch
app/.../ai/AiOutputSanitizer.kt              # strip quotes / fences
app/.../ai/AiProviderClient.kt               # systemInstruction + sanitize + chat filter
app/.../test/.../ClipboardTextLayoutTest.kt
app/.../test/.../AiOutputSanitizerTest.kt
khaiez/CHANGELOG.md · HANDOFF.md · TESTING.md · ARCHITECTURE.md
```

---

## Lệnh hay dùng

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebug
adb devices -l
adb connect <ip>:<port>   # Wireless debugging
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
adb shell am force-stop vn.kai.board
```

**Docs:** `CHANGELOG.md` · `TESTING.md` (AI smoke) · `PRIVACY.md` · skill `/kai-board-continue`.

---

## Skill chống regression

```text
.grok/skills/kai-board-continue/SKILL.md
.grok/skills/kai-board-continue/references/smoke-ime.md
```

Gọi: `/kai-board-continue` · “đổi máy” · “smoke sau cài”.
