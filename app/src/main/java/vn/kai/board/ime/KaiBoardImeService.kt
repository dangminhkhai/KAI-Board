package vn.kai.board.ime

import android.inputmethodservice.InputMethodService
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InlineSuggestionsRequest
import android.view.inputmethod.InlineSuggestionsResponse
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.inline.InlinePresentationSpec
import android.util.Size
import android.widget.Toast
import android.os.Build
import java.util.Locale
import vn.kai.board.input.KeyAction
import vn.kai.board.input.TelexInputPolicy
import vn.kai.board.input.EditorActionPolicy
import vn.kai.board.input.WordCursorContext
import vn.kai.board.input.WordRecomposer
import vn.kai.board.input.VietnameseSuggestionEngine
import vn.kai.board.input.EmailSuggestionEngine
import vn.kai.board.input.EmailSuggestionStore
import vn.kai.board.input.HashtagSuggestionEngine
import vn.kai.board.input.HashtagSuggestionStore
import vn.kai.board.input.UserLexiconStore
import vn.kai.board.input.LearnSource
import vn.kai.board.input.PhraseLearningStore
import vn.kai.board.input.WordDictionaryPack
import vn.kai.board.input.SuggestionPriority
import vn.kai.board.input.AutoCorrectionStatsStore
import vn.kai.board.input.SelectionDeletionPolicy
import vn.kai.board.input.InputPrivacyPolicy
import vn.kai.board.input.InputPunctuationPolicy
import vn.kai.board.input.NumericInputPolicy
import vn.kai.board.input.UnicodeDeletionPolicy
import vn.kai.board.input.ComposingCursorPolicy
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.telex.TelexEngine
import vn.kai.board.input.TelexWordComposer
import vn.kai.board.input.SentenceAutomationPolicy
import vn.kai.board.ui.KeyboardView
import vn.kai.board.MainActivity
import vn.kai.board.R
import vn.kai.board.ClipboardManagerActivity
import vn.kai.board.voice.VoiceInputActivity
import vn.kai.board.voice.InlineVoiceRecognizer
import vn.kai.board.voice.VoiceLanguageResolver
import vn.kai.board.voice.VoiceResultRouter
import vn.kai.board.voice.VoiceTarget
import vn.kai.board.translation.TranslationActivity
import vn.kai.board.translation.TranslationLanguages
import vn.kai.board.translation.TranslationPreferences
import vn.kai.board.ai.AiPreferences
import vn.kai.board.ai.AiProviderClient
import vn.kai.board.ai.AiProviderCandidate
import vn.kai.board.ai.AiKeyStatsStore
import vn.kai.board.ai.AiRequestCancellation
import vn.kai.board.ai.SecureApiKeyStore
import vn.kai.board.ai.AiCommandSuggestionEngine
import vn.kai.board.ai.AiCommandSuggestionStore
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class KaiBoardImeService : InputMethodService() {
    private var shifted = false
    private var capsLocked = false
    private var keyboardView: KeyboardView? = null
    private var inlineAutofillStrip: LinearLayout? = null
    private var inlineAutofillGeneration = 0
    private var composing = ""
    private var rawComposing = ""
    private var literalTelexLockLength = 0
    private var telexEnabled = true
    private var directCommitTelex = false
    private var selectionActive = false
    private var privateSession = false
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
    private var aiCancellation: AiRequestCancellation? = null
    private val aiHandler = Handler(Looper.getMainLooper())
    private val aiRunnable = Runnable { sendAiNow() }
    private val aiSuggestionTokenRegex = Regex("[\\p{L}\\p{N}._+-]+")
    private var inlineVoiceRecognizer: InlineVoiceRecognizer? = null
    private var voiceGeneration = 0
    private var activeVoiceTarget: VoiceTarget? = null
    private var activeVoiceLanguage = ""
    private var activeVoicePrompt = ""
    private var activeVoicePartial = ""
    private var voicePaused = false

    override fun onCreate() {
        super.onCreate()
        // Asset parsing and index construction stay off both startup rendering and key dispatch.
        Thread({
            runCatching { WordDictionaryPack.load(this) }
        }, "kai-dictionary-loader").start()
    }

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            setPadding((4 * density).toInt(), (3 * density).toInt(), (4 * density).toInt(), (3 * density).toInt())
        }
        inlineAutofillStrip = strip
        val view = KeyboardView(this)
        keyboardView = view
        view.onKeyAction = ::handleAction
        view.setShifted(shifted)
        view.setCapsLocked(capsLocked)
        view.setSpaceLabel(resolveSpaceLabel(currentInputEditorInfo))
        view.setLeadingPunctuation(InputPunctuationPolicy.leadingKey(currentInputEditorInfo?.inputType ?: 0))
        view.setSuggestionsEnabled(suggestionsAllowed(currentInputEditorInfo))
        view.setPrivateSession(privateSession)
        syncNumericMode(view, currentInputEditorInfo)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(HorizontalScrollView(this@KaiBoardImeService).apply {
                isHorizontalScrollBarEnabled = false
                visibility = View.GONE
                addView(strip, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
                strip.tag = this
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (50 * density).toInt()))
            addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    override fun onCreateInlineSuggestionsRequest(uiExtras: Bundle): InlineSuggestionsRequest? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val density = resources.displayMetrics.density
        val spec = InlinePresentationSpec.Builder(
            Size((72 * density).toInt(), (40 * density).toInt()),
            Size((240 * density).toInt(), (44 * density).toInt()),
        ).build()
        return InlineSuggestionsRequest.Builder(listOf(spec))
            .setMaxSuggestionCount(4)
            .build()
    }

    override fun onInlineSuggestionsResponse(response: InlineSuggestionsResponse): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val strip = inlineAutofillStrip ?: return false
        val host = strip.tag as? View ?: return false
        val suggestions = response.inlineSuggestions.take(4)
        val generation = ++inlineAutofillGeneration
        strip.removeAllViews()
        strip.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
        host.visibility = strip.visibility
        if (suggestions.isEmpty()) return true
        val density = resources.displayMetrics.density
        suggestions.forEach { suggestion ->
            suggestion.inflate(this, Size((220 * density).toInt(), (44 * density).toInt()), mainExecutor) { content ->
                if (generation != inlineAutofillGeneration || content == null) return@inflate
                strip.addView(content, LinearLayout.LayoutParams((220 * density).toInt(), (44 * density).toInt()).apply {
                    marginEnd = (4 * density).toInt()
                })
            }
        }
        return true
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        capsLocked = false
        shifted = false
        keyboardView?.setCapsLocked(false)
        keyboardView?.setShifted(false)
        composing = ""
        rawComposing = ""
        literalTelexLockLength = 0
        telexEnabled = info?.let { TelexInputPolicy.isEnabled(it.inputType) } ?: true
        directCommitTelex = info?.let { TelexInputPolicy.requiresDirectCommit(it.inputType, it.imeOptions) } ?: false
        privateSession = InputPrivacyPolicy.isPrivateSession(info)
        selectionActive = false
        lastAutoCorrection = null
        updateAutomaticShift()
        keyboardView?.setSpaceLabel(resolveSpaceLabel(info))
        keyboardView?.setLeadingPunctuation(InputPunctuationPolicy.leadingKey(info?.inputType ?: 0))
        keyboardView?.setSuggestionsEnabled(suggestionsAllowed(info))
        keyboardView?.setPrivateSession(privateSession)
        keyboardView?.let { syncNumericMode(it, info) }
        updateSuggestions()
    }

    /** Keep the host app visible in landscape instead of Android's full-screen extract UI. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun syncNumericMode(view: KeyboardView, info: EditorInfo?) {
        val mode = NumericInputPolicy.resolve(info?.inputType ?: 0)
        view.setNumericMode(
            enabled = mode.enabled,
            decimal = mode.decimal,
            signed = mode.signed,
            phone = mode.phone,
        )
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        inlineAutofillGeneration++
        inlineAutofillStrip?.removeAllViews()
        inlineAutofillStrip?.visibility = View.GONE
        (inlineAutofillStrip?.tag as? View)?.visibility = View.GONE
        confirmPendingCorrection()
        rememberCurrentEmail()
        rememberCurrentHashtag()
        keyboardView?.cancelActiveGesture()
        keyboardView?.setSuggestions(emptyList())
        cancelInlineVoice()
        stopTranslation(commit = true)
        stopAi(commit = true)
        composing = ""
        rawComposing = ""
        literalTelexLockLength = 0
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
        if (ComposingCursorPolicy.shouldFinish(
                composing.isNotEmpty(), directCommitTelex,
                newSelStart, newSelEnd, candidatesStart, candidatesEnd,
            )) {
                composing = ""
                rawComposing = ""
                literalTelexLockLength = 0
                currentInputConnection?.finishComposingText()
                updateSuggestions()
        }
    }

    private fun handleAction(action: KeyAction) {
        val connection = currentInputConnection ?: return
        if (privateSession && (action == KeyAction.OpenAi || action == KeyAction.ToggleClipboard)) return
        if (voicePaused && action == KeyAction.Backspace && activeVoiceTarget != null) {
            activeVoicePartial = activeVoicePartial.dropLast(1)
            syncVoiceTextToFeatureInput(activeVoicePartial)
            keyboardView?.setVoicePanel(true, "Đã tạm dừng", activeVoicePartial, paused = true)
            return
        }
        if (translationMode && handleTranslationAction(action)) return
        if (aiMode && handleAiAction(action)) return
        if (action != KeyAction.Backspace) confirmPendingCorrection()
        when (action) {
            is KeyAction.CommitText -> {
                finishComposing()
                connection.commitText(action.value, 1)
                updateSuggestions()
            }
            is KeyAction.CommitClipboard -> {
                finishComposing()
                connection.commitText(action.value, 1)
                updateSuggestions()
            }
            is KeyAction.SelectSuggestion -> {
                val history = previousWords()
                val previous = history.lastOrNull()
                val hashtagToken = currentHashtagToken()
                if (action.value.startsWith('#') && hashtagToken.isNotEmpty()) {
                    finishComposing()
                    connection.deleteSurroundingText(hashtagToken.length, 0)
                    connection.commitText("${action.value} ", 1)
                    if (learningAllowed()) HashtagSuggestionStore.remember(this, action.value)
                    showNextWordSuggestions(history + action.value)
                } else if (isEmailInput(currentInputEditorInfo)) {
                    val before = connection.getTextBeforeCursor(120, 0) ?: ""
                    val token = EmailSuggestionEngine.currentToken(before)
                    if (token.isNotEmpty()) connection.deleteSurroundingText(token.length, 0)
                    connection.commitText("${action.value} ", 1)
                    if (learningAllowed()) EmailSuggestionStore.remember(this, action.value)
                    showNextWordSuggestions(history + action.value)
                } else if (composing.isNotEmpty()) {
                    composing = action.value
                    rawComposing = action.value
                    literalTelexLockLength = 0
                    connection.setComposingText(composing, 1)
                    if (learningAllowed()) {
                        UserLexiconStore.record(this, composing, LearnSource.SUGGESTION, 3)
                        PhraseLearningStore.record(this, history, composing)
                    }
                    finishComposing()
                    connection.commitText(" ", 1)
                    showNextWordSuggestions(history + action.value)
                } else {
                    connection.commitText("${action.value} ", 1)
                    if (learningAllowed()) {
                        UserLexiconStore.record(this, action.value, LearnSource.SUGGESTION, 3)
                        PhraseLearningStore.record(this, history, action.value)
                    }
                    showNextWordSuggestions(history + action.value)
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
                        val result = TelexWordComposer.append(composing, value, literalTelexLockLength, rawComposing)
                        val previousComposing = composing
                        composing = result.text
                        rawComposing = result.rawText
                        literalTelexLockLength = result.literalLockLength
                        if (directCommitTelex) {
                            if (previousComposing.isNotEmpty()) connection.deleteSurroundingText(previousComposing.length, 0)
                            connection.commitText(composing, 1)
                        } else connection.setComposingText(composing, 1)
                    }
                } else {
                    rememberCurrentHashtag()
                    finishComposing()
                    connection.commitText(value.toString(), 1)
                }
                if (shifted && !capsLocked) {
                    shifted = false
                    keyboardView?.setShifted(false)
                }
                updateSuggestions()
            }
            KeyAction.Backspace -> {
                val selectedText = connection.getSelectedText(0)
                if (SelectionDeletionPolicy.shouldDeleteSelection(selectionActive, selectedText)) {
                    composing = ""
                    rawComposing = ""
                    connection.finishComposingText()
                    connection.commitText("", 1)
                    selectionActive = false
                    lastAutoCorrection = null
                    updateSuggestions()
                    return
                }
                val correction = lastAutoCorrection
                if (correction != null) {
                    val before = connection.getTextBeforeCursor(correction.corrected.length + 1, 0)?.toString().orEmpty()
                    if (before == "${correction.corrected} ") {
                        connection.deleteSurroundingText(correction.corrected.length + 1, 0)
                        composing = correction.original
                        rawComposing = correction.original
                        literalTelexLockLength = 0
                        connection.setComposingText(composing, 1)
                        // Undo means the original spelling was intentional. Learn it
                        // immediately so the next Space does not apply the same fix again.
                        if (learningAllowed()) {
                            UserLexiconStore.decrement(this, correction.corrected, 2)
                            UserLexiconStore.record(this, correction.original, LearnSource.TYPED, 4)
                            AutoCorrectionStatsStore.rejected(this, correction.original, correction.corrected)
                        }
                        lastAutoCorrection = null
                        updateSuggestions()
                        return
                    }
                    lastAutoCorrection = null
                }
                if (composing.isNotEmpty()) {
                    val previousComposing = composing
                    val restored = TelexWordComposer.backspace(rawComposing, literalTelexLockLength)
                    composing = restored.text
                    rawComposing = restored.rawText
                    literalTelexLockLength = restored.literalLockLength
                    if (directCommitTelex) {
                        connection.deleteSurroundingText(previousComposing.length, 0)
                        if (composing.isNotEmpty()) connection.commitText(composing, 1)
                    } else if (composing.isEmpty()) {
                        connection.setComposingText("", 1)
                        connection.finishComposingText()
                    } else connection.setComposingText(composing, 1)
                } else {
                    val beforeCursor = connection.getTextBeforeCursor(80, 0) ?: ""
                    val previousWord = WordRecomposer.beforeSingleWhitespace(beforeCursor)
                    if (previousWord != null) {
                        connection.deleteSurroundingText(previousWord.length + 1, 0)
                        composing = previousWord
                        rawComposing = previousWord
                        literalTelexLockLength = 0
                        connection.setComposingText(composing, 1)
                    } else {
                        val deleteCount = UnicodeDeletionPolicy.charactersToDeleteBeforeCursor(beforeCursor)
                        if (deleteCount > 0) connection.deleteSurroundingText(deleteCount, 0)
                        else connection.deleteSurroundingTextInCodePoints(1, 0)
                    }
                }
                updateSuggestions()
            }
            is KeyAction.MoveCursor -> {
                finishComposing()
                val keyCode = if (action.characters < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                repeat(kotlin.math.abs(action.characters)) {
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                }
                updateSuggestions()
            }
            KeyAction.Space -> {
                val hashtag = currentHashtagToken()
                if (HashtagSuggestionEngine.isComplete(hashtag) && learningAllowed()) {
                    HashtagSuggestionStore.remember(this, hashtag)
                    finishComposing()
                    connection.commitText(" ", 1)
                    updateSuggestions()
                    return
                }
                if (isEmailInput(currentInputEditorInfo)) {
                    rememberCurrentEmail(); finishComposing(); connection.commitText(" ", 1)
                } else {
                    val original = composing
                    val history = previousWords()
                    val previous = history.lastOrNull()
                    val learned = UserLexiconStore.read(this)
                    val corrected = if (!directCommitTelex && KeyboardPreferences.autoCorrect(this) && original.isNotEmpty()) {
                        VietnameseSuggestionEngine.bestAutoCorrection(original, learned, previous)
                    } else null
                    if (corrected != null) {
                        composing = corrected
                        connection.setComposingText(corrected, 1)
                    }
                    finishComposing()
                    connection.commitText(" ", 1)
                    if (original.isNotEmpty() && learningAllowed()) {
                        if (corrected != null) UserLexiconStore.record(this, corrected, LearnSource.AUTO_CORRECT, 2)
                        else UserLexiconStore.learn(this, original)
                        PhraseLearningStore.record(this, history, corrected ?: original)
                    }
                    lastAutoCorrection = corrected?.let { AutoCorrection(original, it) }
                    showNextWordSuggestions(history + (corrected ?: original))
                }
            }
            KeyAction.Enter -> {
                lastAutoCorrection = null
                rememberCurrentEmail()
                rememberCurrentHashtag()
                finishComposing()
                val info = currentInputEditorInfo
                val actionId = EditorActionPolicy.resolve(info?.imeOptions ?: EditorInfo.IME_ACTION_NONE)
                if (actionId == null) connection.commitText("\n", 1)
                else connection.performEditorAction(actionId)
                if (sentenceAutomationAllowed()) updateAutomaticShift(forceCapital = true)
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
            KeyAction.SendAi,
            KeyAction.VoiceTranslation,
            KeyAction.VoiceAi,
            KeyAction.CancelVoice,
            KeyAction.ToggleVoicePause -> Unit
            is KeyAction.SetAiCursor,
            is KeyAction.SelectAiSuggestion -> Unit
            KeyAction.ToggleEmoji,
            KeyAction.ToggleClipboard -> Unit
            KeyAction.ToggleEmojiSearch,
            KeyAction.EmojiSearchBackspace,
            is KeyAction.EmojiSearchCharacter -> Unit
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
            KeyAction.Shift -> handleShiftTap()
            KeyAction.CapsLock -> toggleCapsLock()
        }
    }

    private fun finishComposing() {
        literalTelexLockLength = 0
        rawComposing = ""
        if (composing.isEmpty()) return
        composing = ""
        currentInputConnection?.finishComposingText()
        updateSuggestions()
    }

    private fun sentenceAutomationAllowed(): Boolean = telexEnabled && !selectionActive

    private fun updateAutomaticShift(forceCapital: Boolean = false) {
        if (capsLocked) {
            shifted = true
            keyboardView?.setShifted(true)
            return
        }
        if (!KeyboardPreferences.autoCapitalization(this) || !sentenceAutomationAllowed()) {
            shifted = false
            keyboardView?.setShifted(false)
            return
        }
        val shouldShift = forceCapital || SentenceAutomationPolicy.shouldCapitalize(
            currentInputConnection?.getTextBeforeCursor(120, 0),
        )
        shifted = shouldShift
        keyboardView?.setShifted(shouldShift)
    }

    private fun handleShiftTap() {
        if (capsLocked) {
            capsLocked = false
            keyboardView?.setCapsLocked(false)
            shifted = false
        } else {
            shifted = !shifted
        }
        keyboardView?.setShifted(shifted)
    }

    private fun toggleCapsLock() {
        capsLocked = !capsLocked
        shifted = capsLocked
        keyboardView?.setCapsLocked(capsLocked)
        keyboardView?.setShifted(shifted)
    }

    private fun updateSuggestions() {
        val info = currentInputEditorInfo
        val hashtagToken = currentHashtagToken()
        val values = when {
            hashtagToken.isNotEmpty() && learningAllowed() ->
                HashtagSuggestionEngine.suggest(hashtagToken, HashtagSuggestionStore.read(this))
            isEmailInput(info) -> {
                val before = currentInputConnection?.getTextBeforeCursor(120, 0) ?: ""
                EmailSuggestionEngine.suggest(
                    EmailSuggestionEngine.currentToken(before),
                    EmailSuggestionStore.read(this),
                )
            }
            info?.let { TelexInputPolicy.isEnabled(it.inputType) } == true ->
                if (composing.isEmpty()) PhraseLearningStore.suggest(this, previousWords())
                else VietnameseSuggestionEngine.suggest(composing, learned = UserLexiconStore.read(this), previousWord = previousWord())
            else -> emptyList()
        }
        keyboardView?.setSuggestions(values)
    }

    private fun showNextWordSuggestions(history: List<String>) {
        val info = currentInputEditorInfo
        if (!suggestionsAllowed(info) || info?.let { TelexInputPolicy.isEnabled(it.inputType) } != true) {
            updateSuggestions()
            return
        }
        val next = PhraseLearningStore.suggest(this, history)
        if (next.isEmpty()) updateSuggestions() else keyboardView?.setSuggestions(next)
    }

    private fun suggestionsAllowed(info: EditorInfo?): Boolean =
        KeyboardPreferences.wordSuggestions(this) &&
            !InputPrivacyPolicy.isPrivateSession(info) &&
            (isEmailInput(info) || (info?.let { TelexInputPolicy.isEnabled(it.inputType) } ?: false))

    private fun isSensitiveInput(info: EditorInfo?): Boolean =
        info?.let { InputPrivacyPolicy.isSensitive(it.inputType) } == true

    private fun learningAllowed(): Boolean = !InputPrivacyPolicy.isPrivateSession(currentInputEditorInfo)

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
        if (!learningAllowed() || !isEmailInput(currentInputEditorInfo)) return
        val before = currentInputConnection?.getTextBeforeCursor(120, 0) ?: ""
        EmailSuggestionStore.remember(this, EmailSuggestionEngine.currentToken(before))
    }

    private fun currentHashtagToken(): String {
        if (isEmailInput(currentInputEditorInfo)) return ""
        val before = currentInputConnection?.getTextBeforeCursor(120, 0) ?: return ""
        return HashtagSuggestionEngine.currentToken(before)
    }

    private fun rememberCurrentHashtag() {
        if (!learningAllowed()) return
        val hashtag = currentHashtagToken()
        if (HashtagSuggestionEngine.isComplete(hashtag)) HashtagSuggestionStore.remember(this, hashtag)
    }

    private fun previousWords(limit: Int = 2): List<String> {
        val text = currentInputConnection?.getTextBeforeCursor(160, 0)?.toString().orEmpty()
        val words = text.trimEnd().split(Regex("\\s+")).filter(String::isNotBlank)
        val committed = if (composing.isNotEmpty() && words.lastOrNull() == composing) words.dropLast(1) else words
        return committed.takeLast(limit.coerceAtLeast(1))
    }

    private fun previousWord(): String? = previousWords(1).lastOrNull()

    private data class AutoCorrection(val original: String, val corrected: String)

    private fun confirmPendingCorrection() {
        val correction = lastAutoCorrection ?: return
        if (learningAllowed()) AutoCorrectionStatsStore.accepted(this, correction.original, correction.corrected)
        lastAutoCorrection = null
    }

    private fun openVoiceInput() {
        if (KeyboardPreferences.offlineMode(this)) {
            Toast.makeText(this, "Chế độ offline đang bật", Toast.LENGTH_SHORT).show()
            return
        }
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
        startVoiceRecognitionActivity(
            VoiceTarget.NORMAL,
            resolveSpeechLanguage(currentInputEditorInfo),
            getString(R.string.voice_prompt),
        )
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
        if (KeyboardPreferences.offlineMode(this)) {
            Toast.makeText(this, "AI bị tắt trong chế độ offline", Toast.LENGTH_SHORT).show()
            return
        }
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
            KeyAction.VoiceAi -> startVoiceRecognitionActivity(
                VoiceTarget.AI_COMMAND,
                resolveSpeechLanguage(currentInputEditorInfo),
                getString(R.string.voice_prompt_ai),
            )
            KeyAction.CancelVoice -> cancelInlineVoice()
            KeyAction.ToggleVoicePause -> toggleInlineVoicePause()
            is KeyAction.SetAiCursor -> {
                aiCursor = action.index.coerceIn(0, aiPrompt.length)
                updateAiUi()
            }
            is KeyAction.SelectAiSuggestion -> {
                val edit = AiCommandSuggestionEngine.applySuggestion(aiPrompt, aiCursor, action.value)
                aiPrompt = edit.prompt
                aiCursor = edit.cursor
                updateAiUi()
                scheduleAi()
            }
            is KeyAction.Character -> {
                val value = if (shifted) action.value.uppercaseChar() else action.value
                val before = aiPrompt.substring(0, aiCursor)
                val changed = appendAiInput(before, value)
                aiPrompt = (changed + aiPrompt.substring(aiCursor)).take(2_000)
                aiCursor = changed.length.coerceAtMost(aiPrompt.length)
                if (shifted && !capsLocked) { shifted = false; keyboardView?.setShifted(false) }
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
                    val before = aiPrompt.substring(0, aiCursor)
                    val deleteCount = UnicodeDeletionPolicy.charactersToDeleteBeforeCursor(before)
                    aiPrompt = aiPrompt.removeRange(aiCursor - deleteCount, aiCursor)
                    aiCursor -= deleteCount
                }
                updateAiUi(); scheduleAi()
            }
            KeyAction.Enter -> sendAiNow()
            KeyAction.Shift -> handleShiftTap()
            KeyAction.CapsLock -> toggleCapsLock()
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
        AiCommandSuggestionStore.record(this, prompt)
        if (KeyboardPreferences.offlineMode(this)) {
            updateAiUi("AI bị tắt trong chế độ offline")
            return
        }
        val keys = SecureApiKeyStore.readAll(this)
        val provider = AiPreferences.provider(this)
        val models = AiPreferences.models(this)
        if (keys.isEmpty()) {
            updateAiUi("Hãy thiết lập API key trong Cài đặt")
            return
        }
        val generation = ++aiGeneration
        aiCancellation?.cancel()
        val cancellation = AiRequestCancellation().also { aiCancellation = it }
        updateAiUi("AI đang xử lý…")
        Thread({
            val result = runCatching {
                val candidates = keys.mapNotNull { key ->
                    cancellation.check()
                    val saved = AiKeyStatsStore.get(this, key)
                    val keyProvider = saved?.provider?.ifBlank { null } ?: AiProviderClient.detectProvider(key)
                        ?: return@mapNotNull null
                    if (keyProvider !in AiProviderClient.providerChoices) return@mapNotNull null
                    val keyModels = when {
                        saved?.models?.isNotEmpty() == true -> saved.models
                        keyProvider == provider && models.isNotEmpty() -> models
                        else -> runCatching { AiProviderClient.discover(key) }
                            .onSuccess { AiKeyStatsStore.save(this, key, it) }
                            .onFailure { AiKeyStatsStore.markFailure(this, key, it.message ?: "Không thể quét model") }
                            .getOrNull()?.models.orEmpty()
                    }
                    if (keyModels.isEmpty()) null else AiProviderCandidate(key, keyProvider, keyModels)
                }
                AiProviderClient.generateWithFallback(
                    candidates,
                    AiPreferences.tone(this),
                    prompt,
                    cancellation,
                ) { failure ->
                    AiKeyStatsStore.markFailure(this, failure.apiKey, failure.message)
                }.also { AiKeyStatsStore.clearFailure(this, it.apiKey) }
            }
            aiHandler.post {
                if (aiCancellation === cancellation) aiCancellation = null
                if (generation != aiGeneration || !aiMode) return@post
                result.onSuccess { generated ->
                    currentInputConnection?.commitText(generated.output, 1)
                    updateAiUi("Đã xử lý • ${generated.provider} • ${generated.model}")
                }.onFailure { updateAiUi(it.message ?: "AI không thể xử lý") }
            }
        }, "kai-ai-request").start()
    }

    private fun updateAiUi(status: String = "") {
        val suggestions = if (aiMode) buildAiSuggestions() else emptyList()
        keyboardView?.setAiState(aiMode, aiPrompt, aiCursor, status, AiPreferences.tone(this).label, suggestions)
    }

    private fun buildAiSuggestions(): List<String> {
        if (aiPrompt.isBlank()) return emptyList()
        val cursor = aiCursor.coerceIn(0, aiPrompt.length)
        val before = aiPrompt.substring(0, cursor)
        val words = aiSuggestionTokenRegex.findAll(before).map { it.value }.toList()
        val hasPartial = before.lastOrNull()?.isWhitespace() != true && words.isNotEmpty()
        val partial = if (hasPartial) words.last() else ""
        val history = if (hasPartial) words.dropLast(1) else words
        val ai = AiCommandSuggestionEngine.suggest(
            aiPrompt, cursor, AiCommandSuggestionStore.read(this), limit = 3,
        )
        val personal: List<String>
        val offline: List<String>
        if (partial.isNotEmpty()) {
            personal = VietnameseSuggestionEngine.suggestLearnedOnly(partial, UserLexiconStore.read(this), 3)
            offline = VietnameseSuggestionEngine.suggest(partial, learned = emptyMap(), previousWord = history.lastOrNull())
                .filterNot { it.equals(partial, ignoreCase = true) }
        } else {
            personal = PhraseLearningStore.suggestPersonal(this, history, 3)
            offline = PhraseLearningStore.suggestOffline(this, history, 3)
        }
        return SuggestionPriority.merge(ai, personal, offline)
    }

    private fun stopAi(commit: Boolean) {
        aiHandler.removeCallbacks(aiRunnable)
        aiCancellation?.cancel()
        aiCancellation = null
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
            KeyAction.VoiceTranslation -> startVoiceRecognitionActivity(
                VoiceTarget.TRANSLATION,
                VoiceLanguageResolver.resolve(TranslationPreferences.source(this)),
                getString(R.string.voice_prompt_translation),
            )
            KeyAction.CancelVoice -> cancelInlineVoice()
            KeyAction.ToggleVoicePause -> toggleInlineVoicePause()
            is KeyAction.Character -> {
                val value = if (shifted) action.value.uppercaseChar() else action.value
                translationSource = appendTranslatedInput(translationSource, value).takeLast(500)
                if (shifted && !capsLocked) { shifted = false; keyboardView?.setShifted(false) }
                updateTranslationUi(); scheduleTranslation()
            }
            KeyAction.Space -> {
                if (translationSource.isNotEmpty() && !translationSource.endsWith(' ')) translationSource += " "
                updateTranslationUi(); scheduleTranslation()
            }
            KeyAction.Backspace -> {
                translationSource = UnicodeDeletionPolicy.removeLastCluster(translationSource)
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
            KeyAction.Shift -> handleShiftTap()
            KeyAction.CapsLock -> toggleCapsLock()
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
        if (KeyboardPreferences.offlineMode(this)) {
            run()
        } else {
            client.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnSuccessListener { run() }
                .addOnFailureListener { updateTranslationUi("Lỗi tải model"); client.close() }
        }
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

    private fun startVoiceRecognitionActivity(target: VoiceTarget, language: String, prompt: String) {
        if (KeyboardPreferences.offlineMode(this)) {
            Toast.makeText(this, "Giọng nói bị tắt trong chế độ offline", Toast.LENGTH_SHORT).show()
            return
        }
        if (target != VoiceTarget.NORMAL) {
            startInlineVoiceWithPermission(target, language, prompt)
            return
        }
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == VoiceInputActivity.RESULT_ERROR) {
                    val message = resultData?.getString(VoiceInputActivity.EXTRA_ERROR).orEmpty()
                    when (runCatching { VoiceTarget.valueOf(resultData?.getString(VoiceInputActivity.EXTRA_TARGET).orEmpty()) }.getOrNull()) {
                        VoiceTarget.AI_COMMAND -> updateAiUi(message)
                        VoiceTarget.TRANSLATION -> updateTranslationUi(message)
                        else -> Unit
                    }
                    return
                }
                if (resultCode != VoiceInputActivity.RESULT_SPEECH) return
                val result = VoiceResultRouter.prepare(
                    resultData?.getString(VoiceInputActivity.EXTRA_TARGET),
                    resultData?.getString(VoiceInputActivity.EXTRA_TEXT),
                ) ?: return
                when (result.target) {
                    VoiceTarget.NORMAL -> currentInputConnection?.commitText(result.text, 1)
                    VoiceTarget.TRANSLATION -> {
                        stopAi(commit = false)
                        translationMode = true
                        translationSource = result.text
                        translationResult = ""
                        updateTranslationUi("Đã nhận giọng nói • đang dịch…")
                        scheduleTranslation()
                    }
                    VoiceTarget.AI_COMMAND -> {
                        stopTranslation(commit = false)
                        aiMode = true
                        aiHandler.removeCallbacks(aiRunnable)
                        aiPrompt = result.text
                        aiCursor = aiPrompt.length
                        updateAiUi("Đã nhận giọng nói • nhấn gửi AI")
                    }
                }
            }
        }
        startActivity(
            Intent(this, VoiceInputActivity::class.java)
                .putExtra(VoiceInputActivity.EXTRA_RECEIVER, receiver)
                .putExtra(VoiceInputActivity.EXTRA_LANGUAGE, language)
                .putExtra(VoiceInputActivity.EXTRA_PROMPT, prompt)
                .putExtra(VoiceInputActivity.EXTRA_TARGET, target.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun startInlineVoiceWithPermission(target: VoiceTarget, language: String, prompt: String) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startInlineVoice(target, language, prompt)
            return
        }
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == VoiceInputActivity.RESULT_PERMISSION_GRANTED) {
                    startInlineVoice(target, language, prompt)
                } else {
                    val message = resultData?.getString(VoiceInputActivity.EXTRA_ERROR).orEmpty()
                        .ifEmpty { "Chưa cấp quyền micro" }
                    if (target == VoiceTarget.AI_COMMAND) updateAiUi(message) else updateTranslationUi(message)
                }
            }
        }
        startActivity(Intent(this, VoiceInputActivity::class.java)
            .putExtra(VoiceInputActivity.EXTRA_RECEIVER, receiver)
            .putExtra(VoiceInputActivity.EXTRA_TARGET, target.name)
            .putExtra(VoiceInputActivity.EXTRA_PERMISSION_ONLY, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun startInlineVoice(target: VoiceTarget, language: String, prompt: String) {
        val resuming = voicePaused && activeVoiceTarget == target
        val resumedPrefix = if (resuming) activeVoicePartial.trim() else ""
        if (!resuming) cancelInlineVoice()
        if (!android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            val message = getString(R.string.voice_unavailable)
            if (target == VoiceTarget.AI_COMMAND) updateAiUi(message) else updateTranslationUi(message)
            return
        }
        activeVoiceTarget = target
        activeVoiceLanguage = language
        activeVoicePrompt = prompt
        if (!resuming) activeVoicePartial = ""
        voicePaused = false
        val generation = ++voiceGeneration
        var partial = activeVoicePartial
        keyboardView?.setVoicePanel(true, "Đang chuẩn bị micro…")
        inlineVoiceRecognizer = InlineVoiceRecognizer(this, object : InlineVoiceRecognizer.Callback {
            override fun onReady() {
                if (generation == voiceGeneration) keyboardView?.setVoicePanel(true, "Đang nghe…", partial)
            }

            override fun onLevel(level: Float) {
                if (generation == voiceGeneration) keyboardView?.setVoicePanel(true, "Đang nghe…", partial, level)
            }

            override fun onPartial(text: String) {
                if (generation != voiceGeneration) return
                partial = joinVoiceSegments(resumedPrefix, text).take(target.maxCharacters)
                activeVoicePartial = partial
                keyboardView?.setVoicePanel(true, "Đang nghe…", partial)
            }

            override fun onResult(text: String) {
                if (generation != voiceGeneration) return
                val liveText = joinVoiceSegments(resumedPrefix, text)
                val result = VoiceResultRouter.prepare(target.name, liveText) ?: return onError("Không nhận được giọng nói")
                releaseInlineVoice(generation)
                applyVoiceResult(result.target, result.text)
            }

            override fun onError(message: String) {
                if (generation != voiceGeneration) return
                inlineVoiceRecognizer?.destroy()
                inlineVoiceRecognizer = null
                keyboardView?.setVoicePanel(true, message, partial)
                aiHandler.postDelayed({
                    if (generation == voiceGeneration) cancelInlineVoice()
                }, 1_500L)
            }
        })
        runCatching { inlineVoiceRecognizer?.start(language, prompt) }
            .onFailure { inlineVoiceRecognizer?.let { it.destroy() }; inlineVoiceRecognizer = null; keyboardView?.setVoicePanel(false) }
    }

    private fun joinVoiceSegments(prefix: String, current: String): String = when {
        prefix.isBlank() -> current.trim()
        current.isBlank() -> prefix.trim()
        else -> "${prefix.trim()} ${current.trim()}"
    }

    private fun applyVoiceResult(target: VoiceTarget, text: String) {
        when (target) {
            VoiceTarget.NORMAL -> currentInputConnection?.commitText(text, 1)
            VoiceTarget.TRANSLATION -> {
                stopAi(commit = false)
                translationMode = true
                translationSource = text
                translationResult = ""
                updateTranslationUi("Đã nhận giọng nói • đang dịch…")
                scheduleTranslation()
            }
            VoiceTarget.AI_COMMAND -> {
                stopTranslation(commit = false)
                aiMode = true
                aiHandler.removeCallbacks(aiRunnable)
                aiPrompt = text
                aiCursor = aiPrompt.length
                updateAiUi("Đã nhận giọng nói • nhấn gửi AI")
            }
        }
    }

    private fun releaseInlineVoice(generation: Int) {
        if (generation != voiceGeneration) return
        inlineVoiceRecognizer?.destroy()
        inlineVoiceRecognizer = null
        voicePaused = false
        activeVoiceTarget = null
        activeVoicePartial = ""
        keyboardView?.setVoicePanel(false)
    }

    private fun cancelInlineVoice() {
        voiceGeneration++
        inlineVoiceRecognizer?.cancel()
        inlineVoiceRecognizer?.destroy()
        inlineVoiceRecognizer = null
        voicePaused = false
        activeVoiceTarget = null
        activeVoiceLanguage = ""
        activeVoicePrompt = ""
        activeVoicePartial = ""
        keyboardView?.setVoicePanel(false)
    }

    private fun toggleInlineVoicePause() {
        if (voicePaused) {
            val target = activeVoiceTarget ?: return cancelInlineVoice()
            startInlineVoice(target, activeVoiceLanguage, activeVoicePrompt)
            return
        }
        if (inlineVoiceRecognizer == null) return
        voiceGeneration++
        inlineVoiceRecognizer?.cancel()
        inlineVoiceRecognizer?.destroy()
        inlineVoiceRecognizer = null
        voicePaused = true
        syncPausedVoiceToFeatureInput()
        keyboardView?.setVoicePanel(true, "Đã tạm dừng", activeVoicePartial, paused = true)
    }

    private fun syncPausedVoiceToFeatureInput() {
        val text = activeVoicePartial.trim()
        if (text.isEmpty()) return
        syncVoiceTextToFeatureInput(text)
    }

    private fun syncVoiceTextToFeatureInput(text: String) {
        when (activeVoiceTarget) {
            VoiceTarget.AI_COMMAND -> {
                aiMode = true
                aiHandler.removeCallbacks(aiRunnable)
                aiPrompt = text.take(VoiceTarget.AI_COMMAND.maxCharacters)
                aiCursor = aiPrompt.length
                updateAiUi("Đã tạm dừng • có thể sửa hoặc tiếp tục nói")
            }
            VoiceTarget.TRANSLATION -> {
                translationHandler.removeCallbacks(translationRunnable)
                translationGeneration++
                translationMode = true
                translationSource = text.take(VoiceTarget.TRANSLATION.maxCharacters)
                translationResult = ""
                updateTranslationUi("Đã tạm dừng • đang dịch…")
                scheduleTranslation()
            }
            VoiceTarget.NORMAL, null -> Unit
        }
    }

    private fun resolveSpeechLanguage(info: EditorInfo?): String {
        val locale = (if (Build.VERSION.SDK_INT >= 24) info?.hintLocales?.get(0) else null) ?: Locale("vi", "VN")
        return if (locale.language == "en") locale.toLanguageTag() else "vi-VN"
    }

    private fun transformWordAtCursor(key: Char): Boolean {
        val connection = currentInputConnection ?: return false
        val before = connection.getTextBeforeCursor(80, 0)
        before ?: return false
        val after = connection.getTextAfterCursor(80, 0) ?: ""
        val context = WordCursorContext.read(before, after) ?: return false
        val transformed = TelexEngine.apply(context.word, key) ?: return false
        connection.deleteSurroundingText(context.before.length, context.after.length)
        composing = transformed
        rawComposing = "${context.word}$key"
        if (directCommitTelex) connection.commitText(composing, 1)
        else connection.setComposingText(composing, 1)
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
