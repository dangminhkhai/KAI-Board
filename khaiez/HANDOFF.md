# Handoff — làm việc tiếp

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**Cập nhật:** 2026-07-21 (AI/Dịch field + gợi ý next-word + clipboard→feature)

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

## Session 2026-07-21 (mới) — AI / Dịch / Settings ô thử

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

### E. HTTP 413 (ghi chú)

- **413** = Payload Too Large (server). Prompt ngắn *không* gây 413 trên Groq/NIM.
- App map lỗi: `AI lỗi HTTP $status` (`AiProviderClient.post`).

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

- [ ] Smoke tay AI: `Tiêu` → chip `đề`; chọn → `ngắn`; không `Limo`/`Tiêu`/`đề ngắn`
- [ ] Smoke: clipboard → AI / Dịch mở đủ chrome + body phím chữ
- [ ] Smoke: ô Dịch — chạm giữa, gõ/BS tại cursor
- [ ] (Tùy) unit ClipboardHistoryStore
- [ ] (Tùy) `versionCode` bump nếu máy có bản lạ
- [ ] Release: keystore riêng, không dùng android-debug

---

## Files chính (session mới)

```text
app/.../ai/AiCommandSuggestionEngine.kt     # next-word only, no global mix
app/.../ai/AiCommandSuggestionEngineTest.kt
app/.../ime/KaiBoardImeService.kt            # openAi/Translator, translationCursor, buildAiSuggestions
app/.../input/KeyAction.kt                   # SetTranslationCursor
app/.../ui/KeyboardView.kt                   # feature field draw/touch, panel priority
app/.../MainActivity.kt                      # compact ô thử
app/.../res/values/strings.xml
khaiez/CHANGELOG.md · HANDOFF.md · TESTING.md
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
