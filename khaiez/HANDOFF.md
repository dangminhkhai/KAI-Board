# Handoff — làm việc tiếp

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**Cập nhật:** 2026-07-22 (touch adaptation shipped; cài Vivo USB)

```powershell
git pull origin main
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd
# USB (Vivo V2366GA gần đây):
adb devices -l
adb install -r app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
adb shell am force-stop vn.kai.board
# WiFi: port wireless đổi mỗi lần pair — xem màn Wireless debugging
```

| | |
| --- | --- |
| **Build** | Debug-only `0.1.0-debug` · `versionCode 1` · không release pipeline |
| **Package** | `vn.kai.board` |
| **ABI** | splits: `arm64-v8a`, `x86_64` (cài arm64 trên Vivo/Samsung) |
| **Signing** | **Chung mọi máy:** `keystore/android-debug.keystore` — xem `keystore/README.md` |
| **Cài đè** | `adb install -r` OK giữa PC sau pull keystore |

---

## Session 2026-07-22 — học vị trí chạm (đã cài Vivo USB)

- `TouchAdaptationStore` + bias trong `TouchTargetPolicy` (hit-test theo tâm học, **không** dời phím vẽ)
- Gõ letter/space/⌫/Shift/Enter → học offset; private / tắt setting → không học
- Cài đặt **Học vị trí chạm**; clear cùng “Xóa từ đã học”
- Unit: `TouchAdaptationStoreTest`, `TouchTargetPolicyTest` — pass
- Cài: Vivo **V2366GA** USB `10AE5U24S0000TQ` — `adb install -r` Success (2026-07-22)
- Files: `touch/TouchAdaptationStore.kt`, `touch/TouchTargetPolicy.kt`, `KeyboardView` learn/apply, `KeyboardPreferences.TOUCH_ADAPTATION`

---

## Session 2026-07-21 — Clipboard UI + AI sanitize

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
- [x] Touch adaptation — unit + **cài Vivo USB** 2026-07-22
- [ ] Smoke tay: gõ lệch → sau vài chục lần hit-test “theo tay”; private không học
- [ ] Smoke tay: scroll clipboard; AI “viết tiêu đề” không `""`
- [ ] Smoke: clipboard → AI / Dịch; ô Dịch cursor
- [ ] Release: keystore riêng, không dùng android-debug

---

## Files chính (touch adaptation + clipboard/AI UI)

```text
app/.../touch/TouchAdaptationStore.kt        # offline per-key bias
app/.../touch/TouchTargetPolicy.kt           # center bias scoring
app/.../ui/KeyboardView.kt                   # learn on tap + apply bias
app/.../settings/KeyboardPreferences.kt      # TOUCH_ADAPTATION
app/.../ui/ClipboardTextLayout.kt
app/.../ai/AiOutputSanitizer.kt
app/.../ai/AiProviderClient.kt
app/.../test/.../TouchAdaptationStoreTest.kt
app/.../test/.../TouchTargetPolicyTest.kt
khaiez/CHANGELOG · HANDOFF · TESTING · PRIVACY · ARCHITECTURE · README
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
