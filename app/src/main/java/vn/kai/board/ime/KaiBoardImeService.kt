package vn.kai.board.ime

import android.inputmethodservice.InputMethodService
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.os.Build
import java.util.Locale
import vn.kai.board.input.KeyAction
import vn.kai.board.input.TelexInputPolicy
import vn.kai.board.input.WordCursorContext
import vn.kai.board.input.WordRecomposer
import vn.kai.board.input.VietnameseSuggestionEngine
import vn.kai.board.input.EmailSuggestionEngine
import vn.kai.board.input.EmailSuggestionStore
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.LearnSource
import vn.kai.board.input.PhraseLearningStore
import vn.kai.board.input.AutoCorrectionStatsStore
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.telex.TelexEngine
import vn.kai.board.ui.KeyboardView
import vn.kai.board.MainActivity
import vn.kai.board.ClipboardManagerActivity
import vn.kai.board.voice.VoiceInputActivity
import vn.kai.board.translation.TranslationActivity
import vn.kai.board.translation.TranslationLanguages
import vn.kai.board.translation.TranslationPreferences
import vn.kai.board.ai.AiPreferences
import vn.kai.board.ai.AiProviderClient
import vn.kai.board.ai.SecureApiKeyStore
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class KaiBoardImeService : InputMethodService() {
    private var shifted = false
    private var keyboardView: KeyboardView? = null
    private var composing = ""
    private var telexEnabled = true
    private var selectionActive = false
    private var lastAutoCorrection: AutoCorrection? = null
    private var translationMode = false
    private var translationSource = ""
    private var translationResult = ""
    private var translationGeneration = 0
    private val translationHandler = Handler(Looper.getMainLooper())
    private val translationRunnable = Runnable { translateSourceNow() }
    private var aiMode = false
    private var aiPrompt = ""
    private var aiCursor = 0
    private var aiGeneration = 0
    private val aiHandler = Handler(Looper.getMainLooper())
    private val aiRunnable = Runnable { sendAiNow() }

    override fun onCreate() {
        super.onCreate()
        // Asset parsing and index construction stay off both startup rendering and key dispatch.
        Thread({
            runCatching {
                assets.open("suggestions.tsv").reader(Charsets.UTF_8).use(VietnameseSuggestionEngine::load)
            }
        }, "kai-dictionary-loader").start()
    }

    override fun onCreateInputView(): View = KeyboardView(this).also { view ->
        keyboardView = view
        view.onKeyAction = ::handleAction
        view.setShifted(shifted)
        view.setSpaceLabel(resolveSpaceLabel(currentInputEditorInfo))
        view.setSuggestionsEnabled(suggestionsAllowed(currentInputEditorInfo))
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composing = ""
        telexEnabled = info?.let { TelexInputPolicy.isEnabled(it.inputType) } ?: true
        selectionActive = false
        lastAutoCorrection = null
        shifted = false
        keyboardView?.setShifted(false)
        keyboardView?.setSpaceLabel(resolveSpaceLabel(info))
        keyboardView?.setSuggestionsEnabled(suggestionsAllowed(info))
        updateSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        confirmPendingCorrection()
        rememberCurrentEmail()
        keyboardView?.cancelActiveGesture()
        keyboardView?.setSuggestions(emptyList())
        stopTranslation(commit = true)
        stopAi(commit = true)
        composing = ""
        selectionActive = false
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        keyboardView?.closeMediaPanel()
        selectionActive = newSelStart != newSelEnd
        if (composing.isNotEmpty() && candidatesStart >= 0) {
            val cursorOutside = newSelStart < candidatesStart || newSelEnd > candidatesEnd
            val hasSelection = newSelStart != newSelEnd
            if (cursorOutside || hasSelection) {
                composing = ""
                currentInputConnection?.finishComposingText()
                updateSuggestions()
            }
        }
    }

    private fun handleAction(action: KeyAction) {
        val connection = currentInputConnection ?: return
        if (translationMode && handleTranslationAction(action)) return
        if (aiMode && handleAiAction(action)) return
        if (action != KeyAction.Backspace) confirmPendingCorrection()
        when (action) {
            is KeyAction.CommitText -> {
                finishComposing()
                connection.commitText(action.value, 1)
                updateSuggestions()
            }
            is KeyAction.SelectSuggestion -> {
                if (isEmailInput(currentInputEditorInfo)) {
                    val before = connection.getTextBeforeCursor(120, 0) ?: ""
                    val token = EmailSuggestionEngine.currentToken(before)
                    if (token.isNotEmpty()) connection.deleteSurroundingText(token.length, 0)
                    connection.commitText(action.value, 1)
                    EmailSuggestionStore.remember(this, action.value)
                    updateSuggestions()
                } else if (composing.isNotEmpty()) {
                    composing = action.value
                    connection.setComposingText(composing, 1)
                    UserLexiconStore.record(this, composing, LearnSource.SUGGESTION, 3)
                    updateSuggestions()
                } else {
                    val previous = previousWord()
                    connection.commitText("${action.value} ", 1)
                    UserLexiconStore.record(this, action.value, LearnSource.SUGGESTION, 3)
                    PhraseLearningStore.record(this, previous, action.value)
                    updateSuggestions()
                }
            }
            is KeyAction.ForgetSuggestion -> {
                UserLexiconStore.forget(this, action.value)
                updateSuggestions()
            }
            is KeyAction.Character -> {
                val value = if (shifted) action.value.uppercaseChar() else action.value
                if (value.isLetter() && telexEnabled) {
                    if (composing.isEmpty() && !selectionActive && isTelexModifier(value) && transformWordAtCursor(value)) {
                        Unit
                    } else {
                        composing = TelexEngine.apply(composing, value) ?: "$composing$value"
                        connection.setComposingText(composing, 1)
                    }
                } else {
                    finishComposing()
                    connection.commitText(value.toString(), 1)
                }
                if (shifted) {
                    shifted = false
                    keyboardView?.setShifted(false)
                }
                updateSuggestions()
            }
            KeyAction.Backspace -> {
                val correction = lastAutoCorrection
                if (correction != null) {
                    val before = connection.getTextBeforeCursor(correction.corrected.length + 1, 0)?.toString().orEmpty()
                    if (before == "${correction.corrected} ") {
                        connection.deleteSurroundingText(correction.corrected.length + 1, 0)
                        composing = correction.original
                        connection.setComposingText(composing, 1)
                        // Undo means the original spelling was intentional. Learn it
                        // immediately so the next Space does not apply the same fix again.
                        UserLexiconStore.decrement(this, correction.corrected, 2)
                        UserLexiconStore.record(this, correction.original, LearnSource.TYPED, 4)
                        AutoCorrectionStatsStore.rejected(this, correction.original, correction.corrected)
                        lastAutoCorrection = null
                        updateSuggestions()
                        return
                    }
                    lastAutoCorrection = null
                }
                if (composing.isNotEmpty()) {
                    composing = composing.dropLast(1)
                    if (composing.isEmpty()) connection.finishComposingText()
                    else connection.setComposingText(composing, 1)
                } else {
                    val beforeCursor = connection.getTextBeforeCursor(80, 0) ?: ""
                    val previousWord = WordRecomposer.beforeSingleWhitespace(beforeCursor)
                    if (previousWord != null) {
                        connection.deleteSurroundingText(previousWord.length + 1, 0)
                        composing = previousWord
                        connection.setComposingText(composing, 1)
                    } else {
                        connection.deleteSurroundingText(1, 0)
                    }
                }
                updateSuggestions()
            }
            KeyAction.Space -> {
                if (isEmailInput(currentInputEditorInfo)) {
                    rememberCurrentEmail(); finishComposing(); connection.commitText(" ", 1)
                } else {
                    val original = composing
                    val previous = previousWord()
                    val learned = UserLexiconStore.read(this)
                    val corrected = if (KeyboardPreferences.autoCorrect(this) && original.isNotEmpty()) {
                        VietnameseSuggestionEngine.bestAutoCorrection(original, learned, previous)
                    } else null
                    if (corrected != null) {
                        composing = corrected
                        connection.setComposingText(corrected, 1)
                    }
                    finishComposing()
                    connection.commitText(" ", 1)
                    if (original.isNotEmpty()) {
                        if (corrected != null) UserLexiconStore.record(this, corrected, LearnSource.AUTO_CORRECT, 2)
                        else UserLexiconStore.learn(this, original)
                        PhraseLearningStore.record(this, previous, corrected ?: original)
                    }
                    lastAutoCorrection = corrected?.let { AutoCorrection(original, it) }
                    updateSuggestions()
                }
            }
            KeyAction.Enter -> {
                lastAutoCorrection = null
                rememberCurrentEmail()
                finishComposing()
                val info = currentInputEditorInfo
                val multiline = ((info?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
                val actionId = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
                if (multiline || actionId == EditorInfo.IME_ACTION_NONE) connection.commitText("\n", 1)
                else connection.performEditorAction(actionId)
            }
            KeyAction.OpenSettings -> {
                finishComposing()
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            KeyAction.VoiceInput -> openVoiceInput()
            KeyAction.OpenTranslator -> openTranslator()
            KeyAction.OpenAi -> openAi()
            KeyAction.CloseTranslator,
            KeyAction.CycleTranslationSource,
            KeyAction.CycleTranslationTarget,
            KeyAction.SwapTranslationLanguages,
            KeyAction.CloseAi,
            KeyAction.SendAi -> Unit
            is KeyAction.SetAiCursor -> Unit
            KeyAction.ToggleEmoji,
            KeyAction.ToggleClipboard -> Unit
            is KeyAction.SelectEmojiGroup -> Unit
            is KeyAction.SelectClipboardTab -> Unit
            KeyAction.HideKeyboard -> requestHideSelf(0)
            KeyAction.OpenClipboardManager -> {
                startActivity(Intent(this, ClipboardManagerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            KeyAction.ToggleSymbols -> Unit
            KeyAction.ToggleSymbolPage -> Unit
            KeyAction.MoveKeyboardUp,
            KeyAction.MoveKeyboardDown,
            KeyAction.DecreaseKeyboardHeight,
            KeyAction.IncreaseKeyboardHeight,
            KeyAction.FinishKeyboardAdjustment -> Unit
            KeyAction.Shift -> {
                shifted = !shifted
                keyboardView?.setShifted(shifted)
            }
        }
    }

    private fun finishComposing() {
        if (composing.isEmpty()) return
        composing = ""
        currentInputConnection?.finishComposingText()
        updateSuggestions()
    }

    private fun updateSuggestions() {
        val info = currentInputEditorInfo
        val values = when {
            isEmailInput(info) -> {
                val before = currentInputConnection?.getTextBeforeCursor(120, 0) ?: ""
                EmailSuggestionEngine.suggest(
                    EmailSuggestionEngine.currentToken(before),
                    EmailSuggestionStore.read(this),
                )
            }
            info?.let { TelexInputPolicy.isEnabled(it.inputType) } == true ->
                if (composing.isEmpty()) PhraseLearningStore.suggest(this, previousWord())
                else VietnameseSuggestionEngine.suggest(composing, learned = UserLexiconStore.read(this), previousWord = previousWord())
            else -> emptyList()
        }
        keyboardView?.setSuggestions(values)
    }

    private fun suggestionsAllowed(info: EditorInfo?): Boolean =
        KeyboardPreferences.wordSuggestions(this) &&
            (isEmailInput(info) || (info?.let { TelexInputPolicy.isEnabled(it.inputType) } ?: false))

    private fun isEmailInput(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> true
            else -> false
        }
    }

    private fun rememberCurrentEmail() {
        if (!isEmailInput(currentInputEditorInfo)) return
        val before = currentInputConnection?.getTextBeforeCursor(120, 0) ?: ""
        EmailSuggestionStore.remember(this, EmailSuggestionEngine.currentToken(before))
    }

    private fun previousWord(): String? {
        val text = currentInputConnection?.getTextBeforeCursor(160, 0)?.toString().orEmpty()
        val words = text.trimEnd().split(Regex("\\s+")).filter(String::isNotBlank)
        return when {
            words.isEmpty() -> null
            composing.isNotEmpty() && words.last() == composing -> words.dropLast(1).lastOrNull()
            else -> words.lastOrNull()
        }
    }

    private data class AutoCorrection(val original: String, val corrected: String)

    private fun confirmPendingCorrection() {
        val correction = lastAutoCorrection ?: return
        AutoCorrectionStatsStore.accepted(this, correction.original, correction.corrected)
        lastAutoCorrection = null
    }

    private fun openVoiceInput() {
        finishComposing()
        val manager = getSystemService(InputMethodManager::class.java)
        val googleVoice = manager?.enabledInputMethodList?.firstOrNull { info ->
            info.packageName == "com.google.android.tts" &&
                info.serviceName.contains("VoiceInputMethodService", ignoreCase = true)
        }
        if (Build.VERSION.SDK_INT >= 28 && googleVoice != null) {
            switchInputMethod(googleVoice.id)
            return
        }
        startVoiceRecognitionActivity()
    }

    private fun openTranslator() {
        finishComposing()
        translationMode = true
        translationSource = currentInputConnection?.getSelectedText(0)?.toString().orEmpty().take(500)
        translationResult = ""
        updateTranslationUi()
        if (translationSource.isNotEmpty()) scheduleTranslation()
    }

    private fun openAi() {
        finishComposing()
        aiMode = true
        aiPrompt = currentInputConnection?.getSelectedText(0)?.toString().orEmpty().take(2_000)
        aiCursor = aiPrompt.length
        updateAiUi()
        if (aiPrompt.isNotBlank()) scheduleAi()
    }

    private fun handleAiAction(action: KeyAction): Boolean {
        when (action) {
            KeyAction.CloseAi -> stopAi(commit = true)
            KeyAction.OpenAi -> Unit
            KeyAction.SendAi -> sendAiNow()
            is KeyAction.SetAiCursor -> {
                aiCursor = action.index.coerceIn(0, aiPrompt.length)
                updateAiUi()
            }
            is KeyAction.Character -> {
                val value = if (shifted) action.value.uppercaseChar() else action.value
                val before = aiPrompt.substring(0, aiCursor)
                val changed = appendAiInput(before, value)
                aiPrompt = (changed + aiPrompt.substring(aiCursor)).take(2_000)
                aiCursor = changed.length.coerceAtMost(aiPrompt.length)
                if (shifted) { shifted = false; keyboardView?.setShifted(false) }
                updateAiUi(); scheduleAi()
            }
            KeyAction.Space -> {
                if (aiCursor == 0 || aiPrompt.getOrNull(aiCursor - 1) != ' ') {
                    aiPrompt = (aiPrompt.substring(0, aiCursor) + " " + aiPrompt.substring(aiCursor)).take(2_000)
                    aiCursor = (aiCursor + 1).coerceAtMost(aiPrompt.length)
                }
                updateAiUi(); scheduleAi()
            }
            KeyAction.Backspace -> {
                if (aiCursor > 0) {
                    aiPrompt = aiPrompt.removeRange(aiCursor - 1, aiCursor)
                    aiCursor--
                }
                if (aiPrompt.isEmpty()) currentInputConnection?.setComposingText("", 1)
                updateAiUi(); scheduleAi()
            }
            KeyAction.Enter -> sendAiNow()
            KeyAction.Shift -> { shifted = !shifted; keyboardView?.setShifted(shifted) }
            KeyAction.HideKeyboard -> { stopAi(commit = true); requestHideSelf(0) }
            else -> return false
        }
        return true
    }

    private fun appendAiInput(text: String, value: Char): String {
        if (!value.isLetter()) return text + value
        val split = text.lastIndexOf(' ')
        val prefix = if (split >= 0) text.substring(0, split + 1) else ""
        val word = if (split >= 0) text.substring(split + 1) else text
        return prefix + (TelexEngine.apply(word, value) ?: "$word$value")
    }

    private fun scheduleAi() {
        aiHandler.removeCallbacks(aiRunnable)
        if (!AiPreferences.autoSend(this) || aiPrompt.isBlank()) return
        aiHandler.postDelayed(aiRunnable, AiPreferences.delaySeconds(this) * 1_000L)
    }

    private fun sendAiNow() {
        aiHandler.removeCallbacks(aiRunnable)
        val prompt = aiPrompt.trim()
        if (prompt.isEmpty()) return
        val key = SecureApiKeyStore.read(this)
        val provider = AiPreferences.provider(this)
        val models = AiPreferences.models(this)
        if (key.isBlank() || provider.isBlank() || models.isEmpty()) {
            updateAiUi("Hãy thiết lập API key trong Cài đặt")
            return
        }
        val generation = ++aiGeneration
        updateAiUi("AI đang xử lý…")
        Thread({
            val result = runCatching { AiProviderClient.generate(provider, key, models, AiPreferences.tone(this), prompt) }
            aiHandler.post {
                if (generation != aiGeneration || !aiMode) return@post
                result.onSuccess { (model, output) ->
                    currentInputConnection?.setComposingText(output, 1)
                    updateAiUi("Đã xử lý • $model")
                }.onFailure { updateAiUi(it.message ?: "AI không thể xử lý") }
            }
        }, "kai-ai-request").start()
    }

    private fun updateAiUi(status: String = "") {
        keyboardView?.setAiState(aiMode, aiPrompt, aiCursor, status, AiPreferences.tone(this).label)
    }

    private fun stopAi(commit: Boolean) {
        aiHandler.removeCallbacks(aiRunnable)
        aiGeneration++
        if (commit) currentInputConnection?.finishComposingText()
        aiMode = false; aiPrompt = ""; aiCursor = 0
        updateAiUi()
    }

    private fun handleTranslationAction(action: KeyAction): Boolean {
        when (action) {
            KeyAction.CloseTranslator -> stopTranslation(commit = true)
            KeyAction.OpenTranslator -> Unit
            KeyAction.CycleTranslationSource -> cycleTranslationLanguage(source = true)
            KeyAction.CycleTranslationTarget -> cycleTranslationLanguage(source = false)
            KeyAction.SwapTranslationLanguages -> {
                val source = TranslationPreferences.source(this)
                val target = TranslationPreferences.target(this)
                TranslationPreferences.save(this, target, source, TranslationPreferences.autoDownload(this))
                translationSource = translationResult.ifEmpty { translationSource }
                translationResult = ""
                updateTranslationUi(); scheduleTranslation()
            }
            is KeyAction.Character -> {
                val value = if (shifted) action.value.uppercaseChar() else action.value
                translationSource = appendTranslatedInput(translationSource, value).takeLast(500)
                if (shifted) { shifted = false; keyboardView?.setShifted(false) }
                updateTranslationUi(); scheduleTranslation()
            }
            KeyAction.Space -> {
                if (translationSource.isNotEmpty() && !translationSource.endsWith(' ')) translationSource += " "
                updateTranslationUi(); scheduleTranslation()
            }
            KeyAction.Backspace -> {
                translationSource = translationSource.dropLast(1)
                if (translationSource.isEmpty()) {
                    translationResult = ""
                    currentInputConnection?.setComposingText("", 1)
                }
                updateTranslationUi(); scheduleTranslation()
            }
            KeyAction.Enter -> {
                currentInputConnection?.finishComposingText()
                translationSource = ""; translationResult = ""; updateTranslationUi()
            }
            KeyAction.Shift -> {
                shifted = !shifted; keyboardView?.setShifted(shifted)
            }
            KeyAction.HideKeyboard -> { stopTranslation(commit = true); requestHideSelf(0) }
            else -> return false
        }
        return true
    }

    private fun appendTranslatedInput(text: String, value: Char): String {
        if (!value.isLetter() || TranslationPreferences.source(this) != "vi") return text + value
        val split = text.lastIndexOf(' ')
        val prefix = if (split >= 0) text.substring(0, split + 1) else ""
        val word = if (split >= 0) text.substring(split + 1) else text
        return prefix + (TelexEngine.apply(word, value) ?: "$word$value")
    }

    private fun scheduleTranslation() {
        translationHandler.removeCallbacks(translationRunnable)
        if (translationSource.isBlank()) return
        translationHandler.postDelayed(translationRunnable, 350L)
    }

    private fun translateSourceNow() {
        val sourceTag = TranslationPreferences.source(this)
        val targetTag = TranslationPreferences.target(this)
        val sourceCode = TranslateLanguage.fromLanguageTag(sourceTag) ?: return
        val targetCode = TranslateLanguage.fromLanguageTag(targetTag) ?: return
        if (sourceCode == targetCode || translationSource.isBlank()) return
        val generation = ++translationGeneration
        val input = translationSource.trim()
        updateTranslationUi("Đang dịch…")
        val client = Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(sourceCode).setTargetLanguage(targetCode).build())
        val run = {
            client.translate(input).addOnSuccessListener { translated ->
                if (generation == translationGeneration && translationMode) {
                    translationResult = translated
                    currentInputConnection?.setComposingText(translated, 1)
                    updateTranslationUi()
                }
                client.close()
            }.addOnFailureListener {
                if (generation == translationGeneration) updateTranslationUi("Chưa có model")
                client.close()
            }
        }
        if (TranslationPreferences.autoDownload(this)) {
            client.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnSuccessListener { run() }
                .addOnFailureListener { updateTranslationUi("Lỗi tải model"); client.close() }
        } else run()
    }

    private fun cycleTranslationLanguage(source: Boolean) {
        val languages = TranslationLanguages.all
        val current = if (source) TranslationPreferences.source(this) else TranslationPreferences.target(this)
        var next = languages[(languages.indexOfFirst { it.tag == current }.coerceAtLeast(0) + 1) % languages.size].tag
        val other = if (source) TranslationPreferences.target(this) else TranslationPreferences.source(this)
        if (next == other) next = languages[(languages.indexOfFirst { it.tag == next } + 1) % languages.size].tag
        TranslationPreferences.save(
            this,
            if (source) next else TranslationPreferences.source(this),
            if (source) TranslationPreferences.target(this) else next,
            TranslationPreferences.autoDownload(this),
        )
        translationResult = ""; updateTranslationUi(); scheduleTranslation()
    }

    private fun updateTranslationUi(status: String = "") {
        val source = TranslationLanguages.all.firstOrNull { it.tag == TranslationPreferences.source(this) }?.name
            ?: TranslationPreferences.source(this).uppercase(Locale.ROOT)
        val target = TranslationLanguages.all.firstOrNull { it.tag == TranslationPreferences.target(this) }?.name
            ?: TranslationPreferences.target(this).uppercase(Locale.ROOT)
        keyboardView?.setTranslationState(
            translationMode,
            source,
            target,
            translationSource,
            status,
        )
    }

    private fun stopTranslation(commit: Boolean) {
        translationHandler.removeCallbacks(translationRunnable)
        translationGeneration++
        if (commit) currentInputConnection?.finishComposingText()
        translationMode = false; translationSource = ""; translationResult = ""
        updateTranslationUi()
    }

    private fun startVoiceRecognitionActivity() {
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode != VoiceInputActivity.RESULT_SPEECH) return
                val text = resultData?.getString(VoiceInputActivity.EXTRA_TEXT)?.trim().orEmpty()
                if (text.isNotEmpty()) currentInputConnection?.commitText(text, 1)
            }
        }
        startActivity(
            Intent(this, VoiceInputActivity::class.java)
                .putExtra(VoiceInputActivity.EXTRA_RECEIVER, receiver)
                .putExtra(VoiceInputActivity.EXTRA_LANGUAGE, resolveSpeechLanguage(currentInputEditorInfo))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun resolveSpeechLanguage(info: EditorInfo?): String {
        val locale = (if (Build.VERSION.SDK_INT >= 24) info?.hintLocales?.get(0) else null) ?: Locale("vi", "VN")
        return if (locale.language == "en") locale.toLanguageTag() else "vi-VN"
    }

    private fun transformWordAtCursor(key: Char): Boolean {
        val connection = currentInputConnection ?: return false
        val before = connection.getTextBeforeCursor(80, 0) ?: return false
        val after = connection.getTextAfterCursor(80, 0) ?: ""
        val context = WordCursorContext.read(before, after) ?: return false
        val transformed = TelexEngine.apply(context.word, key) ?: return false
        connection.deleteSurroundingText(context.before.length, context.after.length)
        composing = transformed
        connection.setComposingText(composing, 1)
        return true
    }

    private fun isTelexModifier(char: Char) = char.lowercaseChar() in "sfrxjzaeowd"

    private fun resolveSpaceLabel(info: EditorInfo?): String {
        val locale: Locale = (if (Build.VERSION.SDK_INT >= 24) info?.hintLocales?.get(0) else null)
            ?: Locale("vi")
        return when (locale.language) {
            "vi" -> "Tiếng Việt"
            "en" -> "English"
            else -> locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }.ifBlank { "Tiếng Việt" }
        }
    }

}
