package vn.kai.board.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class InlineVoiceRecognizer(
    context: Context,
    private val callback: Callback,
) : RecognitionListener {
    interface Callback {
        fun onReady()
        fun onLevel(level: Float)
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(message: String)
    }

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(this@InlineVoiceRecognizer)
    }

    fun start(language: String, prompt: String) {
        recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1))
    }

    fun cancel() = recognizer.cancel()
    fun destroy() = recognizer.destroy()

    override fun onReadyForSpeech(params: Bundle?) = callback.onReady()
    override fun onRmsChanged(rmsdB: Float) = callback.onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            ?.takeIf(String::isNotBlank)?.let(callback::onPartial)
    }
    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
        if (text.isEmpty()) callback.onError("Không nhận được giọng nói") else callback.onResult(text)
    }
    override fun onError(error: Int) = callback.onError(when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Không thu được âm thanh"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chưa cấp quyền micro"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi kết nối nhận giọng nói"
        SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận ra nội dung"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Micro đang bận"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy giọng nói"
        else -> "Không thể nhận giọng nói"
    })
    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
