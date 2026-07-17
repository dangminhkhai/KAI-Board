package vn.kai.board.voice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.speech.RecognizerIntent
import android.widget.Toast
import vn.kai.board.R

class VoiceInputActivity : Activity() {
    private var receiver: ResultReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = readReceiver(intent)
        if (savedInstanceState == null) launchRecognizer()
    }

    private fun launchRecognizer() {
        val language = intent.getStringExtra(EXTRA_LANGUAGE) ?: "vi-VN"
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt))
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(speechIntent, REQUEST_SPEECH)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.voice_unavailable, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Deprecated("Activity result callback for the platform speech intent")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                receiver?.send(RESULT_SPEECH, Bundle().apply { putString(EXTRA_TEXT, text) })
            }
        }
        finish()
    }

    @Suppress("DEPRECATION")
    private fun readReceiver(source: Intent): ResultReceiver? = if (Build.VERSION.SDK_INT >= 33) {
        source.getParcelableExtra(EXTRA_RECEIVER, ResultReceiver::class.java)
    } else {
        source.getParcelableExtra(EXTRA_RECEIVER)
    }

    companion object {
        const val EXTRA_RECEIVER = "vn.kai.board.voice.RECEIVER"
        const val EXTRA_LANGUAGE = "vn.kai.board.voice.LANGUAGE"
        const val EXTRA_TEXT = "vn.kai.board.voice.TEXT"
        const val RESULT_SPEECH = 1
        private const val REQUEST_SPEECH = 10
    }
}
