package vn.kai.board.voice

import android.app.Activity
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
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
        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(EXTRA_PERMISSION_ONLY, false)) requestMicrophonePermission()
            else launchRecognizer()
        }
    }

    private fun requestMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            receiver?.send(RESULT_PERMISSION_GRANTED, Bundle())
            finish()
        } else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_MICROPHONE) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            receiver?.send(RESULT_PERMISSION_GRANTED, Bundle())
        } else sendError("Chưa cấp quyền micro")
        finish()
    }

    private fun launchRecognizer() {
        val language = intent.getStringExtra(EXTRA_LANGUAGE) ?: "vi-VN"
        val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: getString(R.string.voice_prompt)
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(speechIntent, REQUEST_SPEECH)
        } catch (_: ActivityNotFoundException) {
            sendError(getString(R.string.voice_unavailable))
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
                receiver?.send(RESULT_SPEECH, Bundle().apply {
                    putString(EXTRA_TEXT, text)
                    putString(EXTRA_TARGET, intent.getStringExtra(EXTRA_TARGET))
                })
            } else sendError("Không nhận được giọng nói")
        } else if (requestCode == REQUEST_SPEECH) {
            sendError("Đã hủy nhận giọng nói")
        }
        finish()
    }

    private fun sendError(message: String) = receiver?.send(RESULT_ERROR, Bundle().apply {
        putString(EXTRA_ERROR, message)
        putString(EXTRA_TARGET, intent.getStringExtra(EXTRA_TARGET))
    })

    @Suppress("DEPRECATION")
    private fun readReceiver(source: Intent): ResultReceiver? = if (Build.VERSION.SDK_INT >= 33) {
        source.getParcelableExtra(EXTRA_RECEIVER, ResultReceiver::class.java)
    } else {
        source.getParcelableExtra(EXTRA_RECEIVER)
    }

    companion object {
        const val EXTRA_RECEIVER = "vn.kai.board.voice.RECEIVER"
        const val EXTRA_LANGUAGE = "vn.kai.board.voice.LANGUAGE"
        const val EXTRA_PROMPT = "vn.kai.board.voice.PROMPT"
        const val EXTRA_TARGET = "vn.kai.board.voice.TARGET"
        const val EXTRA_TEXT = "vn.kai.board.voice.TEXT"
        const val EXTRA_ERROR = "vn.kai.board.voice.ERROR"
        const val RESULT_SPEECH = 1
        const val RESULT_ERROR = 2
        const val RESULT_PERMISSION_GRANTED = 3
        const val EXTRA_PERMISSION_ONLY = "vn.kai.board.voice.PERMISSION_ONLY"
        private const val REQUEST_SPEECH = 10
        private const val REQUEST_MICROPHONE = 11
    }
}
