package vn.kai.board.translation

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import vn.kai.board.R
import vn.kai.board.settings.KeyboardColorStyle
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode

class TranslationModelsActivity : Activity() {
    private val manager = RemoteModelManager.getInstance()
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var models: LinearLayout
    private var selectedColor = Color.rgb(37, 99, 235)
    private var cardColor = Color.WHITE
    private var primaryText = Color.rgb(17, 24, 39)
    private var secondaryText = Color.rgb(75, 85, 99)
    private var outlineColor = Color.rgb(226, 232, 240)
    private var density = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        density = resources.displayMetrics.density
        val themeMode = KeyboardPreferences.theme(this)
        val dark = themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val gradient = KeyboardPreferences.colorStyle(this) == KeyboardColorStyle.AI_GRADIENT_2026
        primaryText = if (dark) Color.rgb(248, 250, 252) else Color.rgb(17, 24, 39)
        secondaryText = if (dark) Color.rgb(203, 213, 225) else Color.rgb(75, 85, 99)
        selectedColor = if (gradient) {
            if (dark) Color.rgb(139, 92, 246) else Color.rgb(79, 70, 229)
        } else Color.rgb(37, 99, 235)
        cardColor = if (gradient) {
            if (dark) Color.argb(224, 15, 23, 42) else Color.argb(232, 255, 255, 255)
        } else if (dark) Color.rgb(30, 41, 59) else Color.rgb(248, 250, 252)
        outlineColor = if (gradient) {
            if (dark) Color.rgb(124, 58, 237) else Color.rgb(165, 180, 252)
        } else if (dark) Color.rgb(51, 65, 85) else Color.rgb(226, 232, 240)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(52), dp(20), dp(24))
            if (gradient) background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (dark) intArrayOf(Color.rgb(7, 17, 31), Color.rgb(23, 37, 84), Color.rgb(59, 7, 100))
                else intArrayOf(Color.rgb(207, 250, 254), Color.rgb(221, 214, 254), Color.rgb(252, 231, 243)),
            ) else setBackgroundColor(if (dark) Color.rgb(15, 23, 42) else Color.WHITE)
        }
        content.addView(TextView(this).apply {
            text = getString(R.string.translation_models_title); textSize = 28f; setTextColor(primaryText)
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.translation_model_note); textSize = 14f; setTextColor(secondaryText)
            setPadding(0, dp(6), 0, dp(14))
        })
        status = TextView(this).apply { textSize = 16f; setTextColor(primaryText); setPadding(dp(16), dp(16), dp(16), dp(16)) }
        content.addView(card(status), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        models = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(models)
        content.addView(TextView(this).apply {
            text = "Powered by Google"; textSize = 12f; setTextColor(secondaryText); gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(content) })
        refresh()
    }

    private fun refresh() {
        status.text = getString(R.string.translation_models_checking)
        manager.getDownloadedModels(TranslateRemoteModel::class.java).addOnSuccessListener { downloaded ->
            val tags = downloaded.map { it.language }.toSet()
            status.text = getString(R.string.translation_models_status, tags.size, TranslationLanguages.all.size, tags.size * 30)
            models.removeAllViews()
            TranslationLanguages.all.sortedByDescending { it.tag in tags }.forEach { language ->
                models.addView(modelCard(language, language.tag in tags), LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(9)
                })
            }
        }.addOnFailureListener {
            status.text = it.localizedMessage ?: getString(R.string.translation_models_error)
        }
    }

    private fun modelCard(language: TranslationLanguage, installed: Boolean): MaterialCardView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(10), dp(10))
            addView(LinearLayout(this@TranslationModelsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@TranslationModelsActivity).apply {
                    text = language.name; textSize = 17f; setTextColor(primaryText)
                })
                addView(TextView(this@TranslationModelsActivity).apply {
                    text = getString(if (installed) R.string.translation_model_downloaded else R.string.translation_model_not_downloaded)
                    textSize = 12f; setTextColor(if (installed) selectedColor else secondaryText)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(MaterialButton(this@TranslationModelsActivity).apply {
                text = getString(if (installed) R.string.delete_model else R.string.download_model)
                if (installed) {
                    setTextColor(selectedColor); backgroundTintList = ColorStateList.valueOf(cardColor)
                    strokeColor = ColorStateList.valueOf(selectedColor); strokeWidth = dp(1)
                } else {
                    setTextColor(Color.WHITE); backgroundTintList = ColorStateList.valueOf(selectedColor)
                }
                setOnClickListener { button ->
                    button.isEnabled = false
                    status.text = getString(if (installed) R.string.translation_model_deleting else R.string.translation_model_downloading, language.name)
                    changeModel(language.tag, installed)
                }
            })
        }
        return card(row)
    }

    private fun card(child: android.view.View) = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; setCardBackgroundColor(cardColor)
        strokeColor = outlineColor; strokeWidth = dp(1); addView(child)
    }

    private fun changeModel(tag: String, installed: Boolean) {
        val code = TranslateLanguage.fromLanguageTag(tag) ?: return
        val model = TranslateRemoteModel.Builder(code).build()
        val task = if (installed) manager.deleteDownloadedModel(model)
        else manager.download(model, DownloadConditions.Builder().requireWifi().build())
        task.addOnSuccessListener { refresh() }.addOnFailureListener {
            Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show(); refresh()
        }
    }

    private fun dp(value: Int) = (value * density).toInt()
}
