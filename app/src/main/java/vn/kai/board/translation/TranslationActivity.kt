package vn.kai.board.translation

import android.app.Activity
import android.os.Bundle
import android.os.ResultReceiver
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val languages = TranslationLanguages.all
        val source = Spinner(this)
        val target = Spinner(this)
        val labels = languages.map { it.name }
        source.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        target.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        source.setSelection(languages.indexOfFirst { it.tag == TranslationPreferences.source(this) }.coerceAtLeast(0))
        target.setSelection(languages.indexOfFirst { it.tag == TranslationPreferences.target(this) }.coerceAtLeast(0))
        val input = EditText(this).apply {
            hint = getString(vn.kai.board.R.string.translation_input_hint)
            setText(intent.getStringExtra(EXTRA_TEXT).orEmpty())
            minLines = 3
        }
        val output = TextView(this).apply { textSize = 18f; setPadding(0, 20, 0, 20) }
        val translate = Button(this).apply { text = getString(vn.kai.board.R.string.translate) }
        val insert = Button(this).apply { text = getString(vn.kai.board.R.string.insert_translation); isEnabled = false }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36, 50, 36, 30)
            addView(TextView(this@TranslationActivity).apply { text = getString(vn.kai.board.R.string.translation_title); textSize = 25f })
            addView(source); addView(target); addView(input); addView(translate); addView(output); addView(insert)
            addView(TextView(this@TranslationActivity).apply { text = "Powered by Google"; textSize = 12f })
        })
        translate.setOnClickListener {
            val sourceTag = languages[source.selectedItemPosition].tag
            val targetTag = languages[target.selectedItemPosition].tag
            if (sourceTag == targetTag || input.text.isBlank()) return@setOnClickListener
            TranslationPreferences.save(this, sourceTag, targetTag, false)
            val sourceCode = TranslateLanguage.fromLanguageTag(sourceTag) ?: return@setOnClickListener
            val targetCode = TranslateLanguage.fromLanguageTag(targetTag) ?: return@setOnClickListener
            val client = Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(sourceCode).setTargetLanguage(targetCode).build())
            translate.isEnabled = false
            output.text = getString(vn.kai.board.R.string.translating)
            val runTranslation = {
                client.translate(input.text.toString()).addOnSuccessListener { value ->
                    output.text = value; insert.isEnabled = true; translate.isEnabled = true; client.close()
                }.addOnFailureListener { error ->
                    output.text = error.localizedMessage; translate.isEnabled = true; client.close()
                }
            }
            client.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnSuccessListener { runTranslation() }
                .addOnFailureListener { output.text = it.localizedMessage; translate.isEnabled = true; client.close() }
        }
        insert.setOnClickListener {
            receiver()?.send(RESULT_TRANSLATION, Bundle().apply { putString(EXTRA_RESULT, output.text.toString()) })
            finish()
        }
    }

    @Suppress("DEPRECATION") private fun receiver(): ResultReceiver? = intent.getParcelableExtra(EXTRA_RECEIVER)

    companion object {
        const val EXTRA_TEXT = "text"; const val EXTRA_RESULT = "result"; const val EXTRA_RECEIVER = "receiver"
        const val RESULT_TRANSLATION = 710
    }
}
