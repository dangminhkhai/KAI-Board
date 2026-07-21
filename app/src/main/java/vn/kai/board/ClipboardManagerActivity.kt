package vn.kai.board

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.kai.board.input.ClipboardHistoryStore
import vn.kai.board.input.NoteStore
import vn.kai.board.settings.KeyboardColorStyle
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.ThemeMode
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.settings.ClipboardExpiry
import android.widget.Toast

class ClipboardManagerActivity : Activity() {
    private val density get() = resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()
    private var primaryText = Color.rgb(17, 24, 39)
    private var secondaryText = Color.rgb(75, 85, 99)
    private var selectedColor = Color.rgb(37, 99, 235)
    private var cardColor = Color.WHITE
    private var outlineColor = Color.rgb(226, 232, 240)
    private var screenBackground: android.graphics.drawable.Drawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)
        configureColors()
        showContent()
    }

    private fun configureColors() {
        val mode = KeyboardPreferences.theme(this)
        val dark = mode == ThemeMode.DARK || mode == ThemeMode.SYSTEM &&
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val palette = KeyboardThemePalette.resolve(this, dark)
        primaryText = palette.text
        secondaryText = palette.hint
        selectedColor = palette.accent
        cardColor = palette.key
        outlineColor = palette.specialKey
        screenBackground = palette.gradientColors?.let {
            GradientDrawable(GradientDrawable.Orientation.TL_BR, it)
        } ?: GradientDrawable().apply { setColor(palette.background) }
    }

    private fun showContent() {
        val notes = NoteStore.read(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(52), dp(20), dp(24))
            background = screenBackground
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.notes_manager_title); textSize = 28f; setTextColor(primaryText)
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.notes_manager_status, notes.size); textSize = 14f; setTextColor(secondaryText)
            setPadding(0, dp(6), 0, dp(14))
        })
        root.addView(card(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addView(TextView(this@ClipboardManagerActivity).apply {
                text = getString(R.string.clipboard_expiry_title); textSize = 16f; setTextColor(primaryText)
            })
            addView(LinearLayout(this@ClipboardManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                val current = KeyboardPreferences.clipboardExpiry(this@ClipboardManagerActivity)
                listOf(
                    ClipboardExpiry.ONE_HOUR to R.string.clipboard_expiry_hour,
                    ClipboardExpiry.ONE_DAY to R.string.clipboard_expiry_day,
                    ClipboardExpiry.NEVER to R.string.clipboard_expiry_never,
                ).forEach { (value, label) ->
                    addView(MaterialButton(this@ClipboardManagerActivity).apply {
                        text = getString(label); textSize = 12f
                        setTextColor(if (value == current) Color.WHITE else selectedColor)
                        backgroundTintList = ColorStateList.valueOf(if (value == current) selectedColor else cardColor)
                        setOnClickListener { KeyboardPreferences.setClipboardExpiry(this@ClipboardManagerActivity, value); showContent() }
                    }, LinearLayout.LayoutParams(0, dp(40), 1f))
                }
            })
        }), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        root.addView(MaterialButton(this).apply {
            text = getString(R.string.clipboard_clear_unpinned)
            setTextColor(selectedColor)
            backgroundTintList = ColorStateList.valueOf(cardColor)
            strokeColor = ColorStateList.valueOf(selectedColor)
            strokeWidth = dp(1)
            setOnClickListener {
                val unpinned = ClipboardHistoryStore.readEntries(this@ClipboardManagerActivity).count { !it.pinned }
                if (unpinned <= 0) {
                    Toast.makeText(this@ClipboardManagerActivity, R.string.clipboard_clear_unpinned_empty, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                MaterialAlertDialogBuilder(this@ClipboardManagerActivity)
                    .setTitle(R.string.clipboard_clear_unpinned_title)
                    .setMessage(getString(R.string.clipboard_clear_unpinned_message, unpinned))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.clipboard_clear_unpinned_confirm) { _, _ ->
                        val removed = ClipboardHistoryStore.clearUnpinned(this@ClipboardManagerActivity)
                        Toast.makeText(
                            this@ClipboardManagerActivity,
                            getString(R.string.clipboard_clear_unpinned_done, removed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    .show()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        root.addView(MaterialButton(this).apply {
            text = getString(R.string.add_note)
            setTextColor(Color.WHITE); backgroundTintList = ColorStateList.valueOf(selectedColor)
            setOnClickListener { editNote(null) }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        if (notes.isEmpty()) {
            root.addView(card(TextView(this).apply {
                text = getString(R.string.notes_empty); textSize = 15f; setTextColor(secondaryText)
                gravity = Gravity.CENTER; setPadding(dp(18), dp(28), dp(18), dp(28))
            }))
        } else notes.forEach { note ->
            root.addView(noteCard(note), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun noteCard(note: String): MaterialCardView {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(12), dp(10))
            addView(TextView(this@ClipboardManagerActivity).apply {
                text = note; textSize = 16f; setTextColor(primaryText); maxLines = 5
            })
            addView(LinearLayout(this@ClipboardManagerActivity).apply {
                gravity = Gravity.END
                addView(MaterialButton(this@ClipboardManagerActivity).apply {
                    text = getString(R.string.edit); setTextColor(selectedColor)
                    backgroundTintList = ColorStateList.valueOf(cardColor)
                    setOnClickListener { editNote(note) }
                })
                addView(MaterialButton(this@ClipboardManagerActivity).apply {
                    text = getString(R.string.clear); setTextColor(selectedColor)
                    backgroundTintList = ColorStateList.valueOf(cardColor)
                    setOnClickListener { confirmDelete(note) }
                })
            })
        }
        return card(body)
    }

    private fun card(child: android.view.View) = MaterialCardView(this).apply {
        radius = dp(18).toFloat(); cardElevation = 0f; setCardBackgroundColor(cardColor)
        strokeColor = outlineColor; strokeWidth = dp(1); addView(child)
    }

    private fun confirmDelete(note: String) {
        MaterialAlertDialogBuilder(this).setTitle(R.string.delete_note_title)
            .setMessage(R.string.delete_note_message).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear) { _, _ ->
                NoteStore.save(this, NoteStore.read(this).filterNot { it == note }); showContent()
            }.show()
    }

    private fun editNote(old: String?) {
        val input = EditText(this).apply { setText(old.orEmpty()); minLines = 3; setTextColor(primaryText) }
        MaterialAlertDialogBuilder(this).setTitle(if (old == null) R.string.add_note else R.string.edit_note)
            .setView(input).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val value = input.text.toString().trim()
                val notes = NoteStore.read(this).toMutableList()
                if (old != null) notes.remove(old)
                if (value.isNotEmpty()) notes.add(0, value)
                NoteStore.save(this, notes.distinct()); showContent()
            }.show()
    }
}
