package vn.kai.board

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.settings.ThemeExtension
import vn.kai.board.settings.ThemeExtensionStore
import vn.kai.board.settings.ThemeMode
import vn.kai.board.ui.ThemePreviewView

class CustomThemesActivity : Activity() {
    private val density get() = resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()
    private var pendingExportId: String? = null
    private var dark = false
    private lateinit var palette: KeyboardThemePalette

    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        dark = KeyboardPreferences.theme(this) == ThemeMode.DARK ||
            KeyboardPreferences.theme(this) == ThemeMode.SYSTEM &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        palette = KeyboardThemePalette.resolve(this, dark)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(48), dp(18), dp(28))
            background = palette.gradientColors?.let { GradientDrawable(GradientDrawable.Orientation.TL_BR, it) }
                ?: GradientDrawable().apply { setColor(palette.background) }
        }
        root.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(MaterialButton(this@CustomThemesActivity).apply {
                text = "‹"; textSize = 28f; minWidth = 0; minimumWidth = 0
                setTextColor(palette.text); backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                contentDescription = getString(R.string.back)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(LinearLayout(this@CustomThemesActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(getString(R.string.custom_themes_title), 24f, palette.text, bold = true))
                addView(label(getString(R.string.custom_themes_subtitle), 13f, palette.hint))
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(6) })
        })

        root.addView(card(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(label(getString(R.string.custom_themes_add_title), 17f, palette.text, bold = true))
            addView(label(getString(R.string.theme_extensions_hint), 13f, palette.hint).apply { setPadding(0, dp(3), 0, dp(8)) })
            addView(LinearLayout(this@CustomThemesActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(actionButton(getString(R.string.theme_extension_download), primary = true) { showUrlDialog() }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) })
                addView(actionButton(getString(R.string.theme_extension_import)) {
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                    }, REQUEST_IMPORT)
                }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) })
            })
        }), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })

        val installed = ThemeExtensionStore.installed(this)
        root.addView(label(getString(R.string.custom_themes_installed, installed.size), 17f, palette.text, bold = true).apply {
            setPadding(dp(2), dp(22), 0, dp(8))
        })
        if (installed.isEmpty()) {
            root.addView(card(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(20), dp(28), dp(20), dp(28))
                addView(label("◇", 30f, palette.accent).apply { gravity = Gravity.CENTER })
                addView(label(getString(R.string.theme_extension_empty), 15f, palette.text, bold = true).apply { gravity = Gravity.CENTER })
                addView(label(getString(R.string.custom_themes_empty_hint), 13f, palette.hint).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
            }))
        } else installed.forEach { extension ->
            root.addView(themeCard(extension), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }

    private fun themeCard(extension: ThemeExtension): MaterialCardView {
        val active = KeyboardPreferences.themeExtensionId(this) == extension.id
        val preview = ThemeExtensionStore.palette(this, extension.id, dark)
        return card(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(13), dp(15), dp(10))
            addView(LinearLayout(this@CustomThemesActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(LinearLayout(this@CustomThemesActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(extension.name, 17f, palette.text, bold = true))
                    addView(label(getString(R.string.custom_theme_by, extension.author), 12f, palette.hint))
                }, LinearLayout.LayoutParams(0, -2, 1f))
                if (active) addView(label(getString(R.string.theme_active), 12f, palette.accent, bold = true))
            })
            if (preview != null) addView(LinearLayout(this@CustomThemesActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                listOf(preview.background, preview.key, preview.specialKey, preview.accent).forEach { color ->
                    addView(android.view.View(this@CustomThemesActivity).apply {
                        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(8).toFloat() }
                    }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(4) })
                }
            }, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(10) })
            addView(LinearLayout(this@CustomThemesActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(actionButton(if (active) getString(R.string.theme_extension_preview) else getString(R.string.preview_and_apply), primary = !active) {
                    showPreview(extension)
                }, LinearLayout.LayoutParams(0, dp(42), 1.25f).apply { marginEnd = dp(3) })
                addView(actionButton(getString(R.string.theme_extension_export_share)) { exportMenu(extension) }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(3); marginEnd = dp(3) })
                addView(actionButton(getString(R.string.theme_extension_delete)) { confirmDelete(extension) }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(3) })
            }, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(8) })
        }).apply { strokeColor = if (active) palette.accent else palette.specialKey; strokeWidth = dp(if (active) 2 else 1) }
    }

    private fun showPreview(extension: ThemeExtension) {
        val value = ThemeExtensionStore.palette(this, extension.id, dark) ?: return
        MaterialAlertDialogBuilder(this).setTitle(extension.name).setView(ThemePreviewView(this, value))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.theme_extension_apply) { _, _ ->
                KeyboardPreferences.setThemeExtension(this, extension.id); palette = value; render()
            }.show()
    }

    private fun showUrlDialog() {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.theme_extension_url_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        val field = TextInputLayout(this).apply { boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; setPadding(dp(20), dp(8), dp(20), 0); addView(input) }
        MaterialAlertDialogBuilder(this).setTitle(R.string.theme_extension_download).setView(field)
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.theme_extension_download) { _, _ ->
                Thread({
                    val result = runCatching { ThemeExtensionStore.installFromUrl(this, input.text.toString()) }
                    runOnUiThread { result.onSuccess { Toast.makeText(this, getString(R.string.theme_extension_installed, it.name), Toast.LENGTH_SHORT).show(); render() }
                        .onFailure { Toast.makeText(this, it.message ?: getString(R.string.theme_download_failed), Toast.LENGTH_LONG).show() } }
                }, "kai-theme-download").start()
            }.show()
    }

    private fun exportMenu(extension: ThemeExtension) {
        MaterialAlertDialogBuilder(this).setTitle(extension.name)
            .setItems(arrayOf(getString(R.string.save_file), getString(R.string.share))) { _, choice ->
                if (choice == 0) {
                    pendingExportId = extension.id
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, "${extension.id}.json")
                    }, REQUEST_EXPORT)
                } else runCatching {
                    val uri = FileProvider.getUriForFile(this, "$packageName.files", ThemeExtensionStore.fileForSharing(this, extension.id))
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, getString(R.string.share_theme)))
                }.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
            }.show()
    }

    private fun confirmDelete(extension: ThemeExtension) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.delete_custom_theme)
            .setMessage(getString(R.string.delete_custom_theme_message, extension.name)).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ -> ThemeExtensionStore.delete(this, extension.id); palette = KeyboardThemePalette.resolve(this, dark); render() }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        runCatching {
            if (requestCode == REQUEST_IMPORT) {
                val extension = contentResolver.openInputStream(uri)?.let { ThemeExtensionStore.install(this, it) } ?: error("Không thể đọc file theme")
                Toast.makeText(this, getString(R.string.theme_extension_imported, extension.name), Toast.LENGTH_SHORT).show(); render()
            } else if (requestCode == REQUEST_EXPORT) {
                val id = pendingExportId ?: error("Không tìm thấy theme cần xuất")
                contentResolver.openOutputStream(uri)?.use { it.write(ThemeExtensionStore.exportBytes(this, id)) } ?: error("Không thể mở file")
                pendingExportId = null; Toast.makeText(this, R.string.theme_extension_exported, Toast.LENGTH_SHORT).show()
            }
        }.onFailure { Toast.makeText(this, it.message ?: getString(R.string.theme_action_failed), Toast.LENGTH_LONG).show() }
    }

    private fun actionButton(title: String, primary: Boolean = false, action: () -> Unit) = MaterialButton(this).apply {
        text = title; textSize = 12f; minWidth = 0; minimumWidth = 0
        setTextColor(if (primary) Color.WHITE else palette.accent)
        backgroundTintList = ColorStateList.valueOf(if (primary) palette.accent else Color.TRANSPARENT)
        if (!primary) { strokeColor = ColorStateList.valueOf(palette.accent); strokeWidth = dp(1) }
        setOnClickListener { action() }
    }

    private fun card(child: android.view.View) = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; setCardBackgroundColor(palette.key)
        strokeColor = palette.specialKey; strokeWidth = dp(1); addView(child)
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    companion object { private const val REQUEST_IMPORT = 601; private const val REQUEST_EXPORT = 602 }
}
