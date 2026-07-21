# Kiến trúc

## Thành phần chính

| Package / lớp | Vai trò |
| --- | --- |
| `ime/KaiBoardImeService` | Vòng đời IME; điều phối Telex → editor; AI, dịch, voice |
| `ui/KeyboardView` | Vẽ bàn phím, geometry, action; không mạng/model |
| `touch/` | Hit-test, pointer, slide, giữ phím, repeat |
| `telex/TelexEngine` | Biến đổi Telex thuần trạng thái (tone/shape, kiểu mới) |
| `input/TelexWordComposer` | Chuỗi hiển thị + raw keys + literal lock; append/backspace |
| `input/ComposingEditorSync` | Quyết định xóa suffix / prefix-shrink / OEM hostile |
| `input/ComposingRewritePolicy` | Chọn chế độ ghi editor (setComposing / direct / shrink) |
| `input/ComposingCursorPolicy` | Kết thúc composing khi con trỏ rời mép cuối |
| `input/*` | Policy nhập, gợi ý, từ điển, clipboard, emoji |
| `ai/` | Provider, key pool, Keystore, fallback, AI suggestions |
| `translation/` | ML Kit translate + quản lý model |
| `voice/` | Speech system + panel inline AI/Dịch |
| `settings/` | Preferences, backup, Theme Extension JSON |
| `*Activity` | Cài đặt, API, theme, clipboard, từ đã học, dịch |

## Luồng nhập

```text
MotionEvent → TouchDispatcher → KeyAction → KaiBoardImeService → InputConnection
```

Core typing phải chạy khi AI, mạng, micro hoặc model dịch lỗi. Dictionary tải ngoài UI; request AI nền có cancellation/timeout.

### Luồng Telex (một từ)

```text
KeyAction.Character
  → TelexWordComposer.append(display, key, lock, raw)
  → TelexEngine.apply (tone/shape/escape/restore Latin)
  → applyComposingText(previous, next)   // xem dưới
```

```text
KeyAction.Backspace (đang có buffer từ)
  → TelexWordComposer.backspace(display, raw, lock)
  → applyComposingText — ưu tiên prefix-shrink (chỉ xóa đuôi)
```

`TelexWordComposer` giữ:

- **display** (`text`): chuỗi user thấy (`Sà`, `Safe`, `má`…)
- **raw** (`rawText`): chuỗi phím gốc (`Saf`, `Safe`, `mas`…)
- **literalLockLength**: sau escape UniKey / restore Latin, phần còn lại không Telex lại

Backspace:

- Có dấu, không lock → xóa **một ký tự hiển thị** (`Mí`→`M`, không `Mi`)
- Latin / lock → xóa một ký tự hiển thị; raw dài hơn display (escape `aaa`→`aa`) được cắt đồng bộ
- Không recompose `Saf`→`Sà` khi buffer đã Latin

## Ghi editor (`applyComposingText`)

| Tình huống | Hành vi |
| --- | --- |
| **Prefix-shrink** (`Safe`→`Saf`, mọi BS co chuỗi) | Chỉ `deleteSurroundingText` phần đuôi — **không** `commitText` từ ngắn |
| OEM Vivo/iQOO/BBK (gõ mới / đổi độ dài tăng) | Direct: finish → xóa suffix khớp → `commitText` |
| Editor hỗ trợ composing tốt | `setComposingText` / finish+delete+set khi rewrite |
| `commitText` kết thúc bằng modifier Telex `sfrxjaeowd` | Tách body + tail; strip nếu OEM nhân đôi chữ cuối |
| Đã hỏng thành `Saff` | `ComposingEditorSync` nhận diện và xóa đuôi thừa |

**Bài học Vivo (2026-07):** xóa cả từ rồi `commitText("Saf")` có thể thành `Saff` (nhân đôi tone Latin). Backspace phải delete-only khi `next` là prefix của `previous`.

`imeWriteDepth` chặn `onUpdateSelection` finish composing trong lúc IME đang ghi. Trên direct-commit OEM, nếu không còn span composing, state nội bộ Telex được xóa khi text trước con trỏ không còn khớp buffer.

`ComposingCursorPolicy` kết thúc composing khi con trỏ rời mép cuối (selection / outside / inside word), trừ khi đang `suppressForImeWrite`.

## Dữ liệu

Tùy chọn: SharedPreferences. API key: AES-GCM + Android Keystore; UI chỉ hiện key đã che. Metadata provider/model/quota theo fingerprint SHA-256, không lưu key rõ. Từ/cụm/email/hashtag/clipboard: cục bộ. Backup: học + Theme Extensions + thứ tự Smartbar; **không** API key, clipboard, notes.

| Thành phần | Ghi chú |
| --- | --- |
| `SentenceAutomationPolicy` | Hoa đầu ô/dòng; không Space/Shift sau dấu câu |
| `PhraseLearningStore` | Bigram/trigram cá nhân (decay 21 ngày, max 512); **rank #1** |
| `PhrasePack` | Gói ~720k offline (collocation + Viet74K + OpenSubtitles); `warmUpAsync`; gõ chỉ lookup RAM; không seed APK |
| `SuggestionPriority.mergePhraseAndCompletions` | Mid-word: phrase-prefix rồi dictionary |
| `WordDictionaryPack` | Gói VI/EN offline trong `filesDir` |
| `SuggestionLanguageDetector` | Luật nhẹ, không I/O trên hot path |
| `AiCommandSuggestionStore` | Học lệnh AI đã gửi; gợi ý AI → cá nhân → offline |
| `SpaceCursorGesturePolicy` / `ShiftGesturePolicy` / `RepeatKeyState` | Cử chỉ thuần, unit test |
| `SmartClipboardClassifier` | Regex offline; OTP có ngữ cảnh |
| `ClipboardHistoryStore` | TTL khi đọc/thêm; ghim không xóa |
| `UnicodeDeletionPolicy` | Xóa emoji/grapheme đúng số code unit |
| `ThemeExtensionStore` | JSON ≤ 64 KB, validate, `filesDir/theme_extensions` |
| `SecureApiKeyStore` / `AiKeyStatsStore` | Thứ tự fallback = thứ tự card; `400` không nhảy key |

Hình học bàn phím (cao, nâng đáy, % rộng, lệch) chỉ qua `KeyboardPreferences`/preset; không đổi hit-test/timing commit cốt lõi.

## Kiểm thử kiến trúc

- Unit: `telex/*`, `TelexWordComposer*`, `TelexModifierBackspaceMatrixTest`, `ComposingEditorSync*`, `ComposingRewritePolicy*`, `ComposingCursorPolicy*`
- Thiết bị: [TESTING.md](TESTING.md) — smoke 5 phút + ma trận app/OEM
- Guardrail: không I/O/mạng trên hot path gõ; không log nội dung gõ/secret
