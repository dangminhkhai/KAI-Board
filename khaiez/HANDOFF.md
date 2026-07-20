# Handoff — làm việc tiếp trên máy khác

**Repo:** https://github.com/dangminhkhai/KAI-Board  
**Nhánh:** `main`  
**Cập nhật handoff:** 2026-07-21  

```powershell
git clone https://github.com/dangminhkhai/KAI-Board.git
cd KAI-Board
git pull origin main
.\gradlew.bat testDebugUnitTest assembleDebug
# Cài máy: JDK 17 + Android SDK; ADB
.\dev-install.cmd
# hoặc
.\dev-install.cmd -Serial <serial>
```

`dev-install.ps1` dò JDK: `JAVA_HOME` → Microsoft JDK 17 → Eclipse Temurin.  
ADB: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

---

## 1. Đã xong gần đây (theo chủ đề)

### 1.1 Telex / Backspace OEM (Vivo OriginOS)

| Vấn đề | Sửa |
| --- | --- |
| `Safe` + BS → `Saff` (4→4) | **Prefix-shrink:** chỉ xóa đuôi trên `InputConnection`, không xóa cả từ rồi `commitText("Saf")` (OEM nhân đôi tone Latin) |
| Escape shape `aaa→aa` BS no-op | `TelexWordComposer.backspace` xóa 1 ký tự **hiển thị**, đồng bộ raw dài hơn display |
| Modifier | Tone `s f r x j` + shape `aa ee oo aw ow uw dd` — ma trận unit test |

**File chính:**

- `ime/KaiBoardImeService.kt` — `applyComposingText`, `applyPrefixShrink`, `commitTextAvoidingToneDouble`, `preferDirectTelexCommit` (Vivo/iQOO/BBK)
- `input/TelexWordComposer.kt` — display / raw / lock / backspace
- `input/ComposingEditorSync.kt`, `ComposingRewritePolicy.kt`, `ComposingCursorPolicy.kt`
- Tests: `TelexModifierBackspaceMatrixTest`, `ComposingEditorSyncTest`, `SafeTraceTest`, `TelexWordComposerTest`

**Xác nhận máy:** Vivo V2366GA — Telex BS + modifier **pass** (xem `TESTING.md` nhật ký).

### 1.2 Gợi ý từ — chọn suggestion

| Vấn đề | Sửa |
| --- | --- |
| Gõ `T` + chọn `tôi` → `Ttôi` | Direct-commit OEM: prefix đã plain text; chọn gợi ý dùng `applyComposingText(prefix, suggestion)` thay `setComposingText` đơn |

### 1.3 Gợi ý cụm từ — P0 + P1

| Tầng | Nguồn | Ghi chú |
| --- | --- | --- |
| **Personal** | `PhraseLearningStore` (bigram/trigram) | Decay **21 ngày**, migrate `pairs` → `pairs_v2`, max 512 |
| **Seed APK** | `PhraseSeedCatalog` (~100 bigram) | Setting `phrase_seed` |
| **Từ điển gọn** | Multi-word trong `VietnameseSuggestionEngine.fallbackWords` + `contextPairs` | Cũng gắn với **cùng setting** seed |

**Hành vi:**

- `composing` rỗng → next-word: personal → seed (nếu bật)
- `composing` có chữ → mid-word blend: `suggestMatchingPrefix` rồi dictionary completion (`SuggestionPriority.mergePhraseAndCompletions`)
- Tắt **Gợi ý cụm có sẵn:** không seed, không multi-word dictionary, không `contextPairs`; **vẫn** personal đã học

**Xóa dữ liệu học (đã sửa bug):**

| Trước | Sau |
| --- | --- |
| «Xóa từ đã học» chỉ clear lexicon/email/hashtag | + `PhraseLearningStore.clear()` |
| Xóa từng từ / ForgetSuggestion trên smartbar | + `PhraseLearningStore.removeInvolving(word)` |

Seed APK **không** bị xóa khi clear học (tắt setting nếu muốn ẩn).

### 1.4 Docs đã refresh

| File | Nội dung |
| --- | --- |
| `khaiez/ARCHITECTURE.md` | Telex path, OEM editor, phrase layers |
| `khaiez/TESTING.md` | Checklist tick + nhật ký thiết bị |
| `khaiez/CHANGELOG.md` | Unreleased: Telex OEM, phrase P1, clear phrases |
| `khaiez/ROADMAP.md` | Done / P2 phrase / beta |
| `khaiez/CONTRIBUTING.md` | Guardrail Telex/BS |
| `khaiez/README.md` | Chỉ mục docs |
| `README.md` (root) | Telex BS OEM, cấu trúc, bảng tài liệu |
| **`khaiez/HANDOFF.md`** | File này |

---

## 2. Cài đặt liên quan suggestions

| Preference key | UI | Mặc định |
| --- | --- | --- |
| `word_suggestions` | Hiện gợi ý từ | true |
| `phrase_seed` | Gợi ý cụm có sẵn (seed + cụm từ điển) | true |

Prefs file: `keyboard_preferences`.  
Phrase personal: SharedPreferences `phrase_learning` (`pairs_v2`).  
Lexicon: `user_lexicon`.

---

## 3. Chưa làm (next)

### 3.1 Gói cụm từ tải tùy chọn (đã chốt hướng, **chưa code**)

Spec đầy đủ đã thảo luận:

- Mirror `WordDictionaryPack`: tải HTTPS → `filesDir/phrase_packs/vi_social.tsv`
- Quy mô phase 1: **~2 000–3 000** bigram VI (~50–100 KB)
- Rank: **personal → pack đã tải → seed APK**
- UI card dưới từ điển mở rộng: Tải / Xóa gói
- Offline mode chặn tải; privacy không suggest
- Pack **không** vào backup; **không** xóa khi clear học (nút xóa gói riêng)
- Host file versioned + SHA-256

PR gợi ý: `PhrasePack` API → file TSV + URL → UI → docs.

### 3.2 P2 cụm từ

- UI xem/xóa từng cụm đã học  
- Accept-rate nội bộ **không** thu text  
- (Tùy) gói EN / pack lớn hơn  

### 3.3 Khác (roadmap)

- AI/mic/dịch còn sót trên Samsung/Vivo  
- Latency IME measure  
- Full checklist app: Zalo, Chrome, Notes, password  
- Release tag sau khi gom unreleased  

---

## 4. File quan trọng (map nhanh)

```text
app/src/main/java/vn/kai/board/
  ime/KaiBoardImeService.kt     # IME, suggestions, Telex apply, clear/forget
  input/TelexWordComposer.kt
  input/telex/TelexEngine.kt    # (package telex/)
  input/ComposingEditorSync.kt
  input/ComposingRewritePolicy.kt
  input/PhraseLearningStore.kt  # personal + seed gate + clear/removeInvolving
  input/PhraseSeedCatalog.kt    # ~100 bigrams
  input/SuggestionPriority.kt
  input/VietnameseSuggestionEngine.kt  # allowBuiltInPhrases
  input/WordDictionaryPack.kt   # mẫu cho PhrasePack tương lai
  settings/KeyboardPreferences.kt
  MainActivity.kt / LearnedWordsActivity.kt
```

---

## 5. Kiểm thử nhanh trên máy mới

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\dev-install.cmd -WithTests
```

| Case | Kỳ vọng |
| --- | --- |
| `Safe` + BS | `Saf` không `Saff` |
| `T` + chọn gợi ý `tôi` | `tôi ` không `Ttôi` |
| `xin` + Space (seed bật) | có `chào` |
| Tắt cụm có sẵn + clear học | không personal phrase; không `xin chào` multi-word dict |
| Xóa từ đã học | xóa cả `phrase_learning` |

Chi tiết: [TESTING.md](TESTING.md).

---

## 6. Commit / remote

Trước handoff này, `origin/main` có ít nhất:

- `a399cdc` — Fix Vivo Telex backspace + docs refresh  

Các thay đổi phrase P1, suggestion replace, clear phrases, seed gate dictionary **cần commit + push** cùng commit chứa `HANDOFF.md` (xem lịch sử `git log` sau khi pull).

---

## 7. Lưu ý vận hành

- Không commit `local.properties`, keystore, API key.  
- Không log nội dung gõ / secret.  
- Telex/IME: unit **không** thay cho test Vivo.  
- Workspace Codex cũ:  
  `Documents\Codex\2026-07-18\dangminhkhai-kai-board-https-github-com\work\KAI-Board`  
  Trên máy mới: clone repo GitHub là đủ.
