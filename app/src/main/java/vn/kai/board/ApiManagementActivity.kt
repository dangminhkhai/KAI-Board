package vn.kai.board

import android.app.Activity
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import vn.kai.board.ai.AiPreferences
import vn.kai.board.ai.AiProviderClient
import vn.kai.board.ai.AiKeyStats
import vn.kai.board.ai.AiKeyStatsStore
import vn.kai.board.ai.ApiKeyPool
import vn.kai.board.ai.SecureApiKeyStore
import vn.kai.board.settings.KeyboardPreferences
import vn.kai.board.settings.KeyboardThemePalette
import vn.kai.board.settings.ThemeMode

class ApiManagementActivity : Activity() {
    private val density get() = resources.displayMetrics.density
    private var statusAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        when (KeyboardPreferences.theme(this)) {
            ThemeMode.LIGHT -> setTheme(R.style.Theme_KAIBoard_Light)
            ThemeMode.DARK -> setTheme(R.style.Theme_KAIBoard_Dark)
            ThemeMode.SYSTEM -> setTheme(R.style.Theme_KAIBoard)
        }
        super.onCreate(savedInstanceState)

        val dark = when (KeyboardPreferences.theme(this)) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        val palette = KeyboardThemePalette.resolve(this, dark)
        window.statusBarColor = palette.gradientColors?.first() ?: palette.background
        window.navigationBarColor = palette.gradientColors?.last() ?: palette.background

        val providers = AiProviderClient.providerChoices
        var providerHint = AiPreferences.providerHint(this).takeIf { it in providers } ?: "Tự động"
        val providerButtons = choiceButtons(providers, providers.indexOf(providerHint), palette.text, palette.key, palette.accent) { position ->
                providerHint = providers[position]
                AiPreferences.setProviderHint(this@ApiManagementActivity, providerHint)
        }
        val keyInput = TextInputEditText(this).apply {
            setTextColor(palette.text)
            minLines = 3
            maxLines = 8
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val status = TextView(this).apply {
            visibility = View.GONE
            textSize = 13f
            setTextColor(palette.hint)
            setPadding(dp(3), dp(10), dp(3), dp(6))
        }

        val apiList = object : RecyclerView(this) {
            override fun onMeasure(widthSpec: Int, heightSpec: Int) {
                // This list lives inside the settings ScrollView. Measure its complete
                // content instead of keeping only the first couple of key cards visible.
                val fullHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                super.onMeasure(widthSpec, fullHeight)
            }
        }.apply {
            layoutManager = LinearLayoutManager(this@ApiManagementActivity)
            isNestedScrollingEnabled = false
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                moveDuration = 180L
                changeDuration = 140L
            }
        }
        val apiEmpty = TextView(this).apply {
            text = getString(R.string.api_list_empty)
            textSize = 14f
            setTextColor(palette.hint)
            setPadding(dp(2), dp(8), dp(2), dp(8))
        }
        val apiCount = TextView(this).apply {
            textSize = 13f
            setTextColor(palette.hint)
            setPadding(0, dp(2), 0, dp(10))
        }
        lateinit var refreshApiList: () -> Unit
        val apiAdapter = ApiKeyAdapter(
            palette = palette,
            onDelete = { key ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.api_delete_title)
                    .setMessage(R.string.api_delete_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.api_delete_action) { _, _ ->
                        val remaining = SecureApiKeyStore.readAll(this).filter { it != key }
                        SecureApiKeyStore.saveAll(this, remaining)
                        AiKeyStatsStore.remove(this, key)
                        refreshApiList()
                    }
                    .show()
            },
            onOrderSaved = { orderedKeys -> SecureApiKeyStore.saveAll(this, orderedKeys) },
        )
        apiList.adapter = apiAdapter
        ItemTouchHelper(ApiKeyTouchCallback(apiAdapter, palette)).attachToRecyclerView(apiList)
        refreshApiList = {
            val savedKeys = SecureApiKeyStore.readAll(this@ApiManagementActivity)
            apiCount.text = resources.getQuantityString(R.plurals.api_saved_count, savedKeys.size, savedKeys.size)
            apiEmpty.visibility = if (savedKeys.isEmpty()) View.VISIBLE else View.GONE
            apiList.visibility = if (savedKeys.isEmpty()) View.GONE else View.VISIBLE
            apiAdapter.submit(savedKeys.map { key ->
                ApiKeyItem(key, AiKeyStatsStore.get(this@ApiManagementActivity, key) ?: fallbackStats(savedKeys.size))
            })
        }
        refreshApiList()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
            addView(MaterialButton(this@ApiManagementActivity).apply {
                text = "‹  ${getString(R.string.api_management_title)}"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextColor(palette.accent)
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                insetLeft = 0
                setOnClickListener { finish() }
            })
            addView(TextView(this@ApiManagementActivity).apply {
                text = getString(R.string.api_management_title)
                textSize = 28f
                setTextColor(palette.text)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(4), dp(8), dp(4), dp(4))
            })
            addView(TextView(this@ApiManagementActivity).apply {
                text = getString(R.string.api_management_note)
                textSize = 14f
                setTextColor(palette.hint)
                setPadding(dp(4), 0, dp(4), dp(16))
            })
            addView(MaterialCardView(this@ApiManagementActivity).apply {
                radius = dp(20).toFloat()
                cardElevation = 0f
                strokeWidth = dp(2)
                strokeColor = palette.specialKey
                setCardBackgroundColor(palette.key)
                setContentPadding(dp(16), dp(16), dp(16), dp(16))
                addView(LinearLayout(this@ApiManagementActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(this@ApiManagementActivity).apply {
                        text = getString(R.string.api_provider)
                        textSize = 15f
                        setTextColor(palette.text)
                        setPadding(dp(2), 0, 0, dp(4))
                    })
                    addView(providerButtons, LinearLayout.LayoutParams(-1, dp(42)))
                    addView(TextInputLayout(this@ApiManagementActivity).apply {
                        hint = getString(R.string.api_keys_hint)
                        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                        boxBackgroundColor = palette.key
                        boxStrokeColor = palette.accent
                        endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                        addView(keyInput, LinearLayout.LayoutParams(-1, -2))
                    }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
                    addView(MaterialButton(this@ApiManagementActivity).apply {
                        text = getString(R.string.api_scan_models)
                        setTextColor(Color.WHITE)
                        backgroundTintList = ColorStateList.valueOf(palette.accent)
                        setOnClickListener {
                            if (KeyboardPreferences.offlineMode(this@ApiManagementActivity)) {
                                Toast.makeText(this@ApiManagementActivity, "Tắt chế độ offline để kiểm tra API", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            val apiKeys = ApiKeyPool.parse(keyInput.text?.toString().orEmpty())
                            if (apiKeys.isEmpty()) {
                                Toast.makeText(this@ApiManagementActivity, "Nhập API key trước", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            isEnabled = false
                            status.visibility = View.VISIBLE
                            status.text = "●  ${getString(R.string.api_scanning)}"
                            status.setTextColor(palette.accent)
                            statusAnimator?.cancel()
                            statusAnimator = ObjectAnimator.ofFloat(status, View.ALPHA, 0.4f, 1f).apply {
                                duration = 650L
                                repeatMode = ValueAnimator.REVERSE
                                repeatCount = ValueAnimator.INFINITE
                                start()
                            }
                            Thread({
                                val results = apiKeys.map { key ->
                                    key to runCatching { AiProviderClient.discover(key, providerHint) }
                                }
                                runOnUiThread {
                                    if (isDestroyed) return@runOnUiThread
                                    isEnabled = true
                                    statusAnimator?.cancel()
                                    statusAnimator = null
                                    status.alpha = 1f
                                    val successful = results.mapNotNull { (key, result) ->
                                        result.getOrNull()?.let { key to it }
                                    }
                                    if (successful.isNotEmpty()) {
                                        val mergedKeys = ApiKeyPool.normalize(
                                            SecureApiKeyStore.readAll(this@ApiManagementActivity) + successful.map { it.first },
                                        )
                                        SecureApiKeyStore.saveAll(this@ApiManagementActivity, mergedKeys)
                                        successful.forEach { (key, found) ->
                                            AiKeyStatsStore.save(this@ApiManagementActivity, key, found)
                                        }
                                        val found = successful.first().second
                                        AiPreferences.saveDiscovery(this@ApiManagementActivity, found.provider, found.models)
                                        keyInput.text?.clear()
                                        status.text = "✓  ${getString(R.string.api_scan_success, successful.size)}"
                                        status.setTextColor(palette.accent)
                                    } else {
                                        status.text = "⚠  ${results.first().second.exceptionOrNull()?.message ?: getString(R.string.api_scan_failed)}"
                                        status.setTextColor(Color.rgb(220, 38, 38))
                                    }
                                    refreshApiList()
                                }
                            }, "kai-ai-discovery").start()
                        }
                    })
                })
            })
            addView(MaterialCardView(this@ApiManagementActivity).apply {
                radius = dp(20).toFloat()
                cardElevation = 0f
                strokeWidth = dp(2)
                strokeColor = palette.specialKey
                setCardBackgroundColor(palette.key)
                setContentPadding(dp(16), dp(16), dp(16), dp(12))
                addView(LinearLayout(this@ApiManagementActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(this@ApiManagementActivity).apply {
                        text = getString(R.string.api_list_title)
                        textSize = 19f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(palette.text)
                    })
                    addView(apiCount)
                    addView(TextView(this@ApiManagementActivity).apply {
                        text = getString(R.string.api_priority_note)
                        textSize = 12f
                        setTextColor(palette.hint)
                        setPadding(0, 0, 0, dp(8))
                    })
                    addView(status)
                    addView(apiEmpty)
                    addView(apiList)
                })
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        }
        val scroll = ScrollView(this).apply {
            palette.gradientColors?.let {
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, it)
            } ?: setBackgroundColor(palette.background)
            clipToPadding = false
            addView(content)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scroll)
        ViewCompat.requestApplyInsets(scroll)
    }

    override fun onDestroy() {
        statusAnimator?.cancel()
        statusAnimator = null
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * density).toInt()

    private data class ApiKeyItem(val key: String, val stats: AiKeyStats?)

    private inner class ApiKeyAdapter(
        private val palette: KeyboardThemePalette,
        private val onDelete: (String) -> Unit,
        private val onOrderSaved: (List<String>) -> Unit,
    ) : RecyclerView.Adapter<ApiKeyAdapter.Holder>() {
        private val items = mutableListOf<ApiKeyItem>()

        fun submit(values: List<ApiKeyItem>) {
            items.clear()
            items.addAll(values)
            notifyDataSetChanged()
        }

        fun move(from: Int, to: Int): Boolean {
            if (from !in items.indices || to !in items.indices || from == to) return false
            val item = items.removeAt(from)
            items.add(to, item)
            // Reorder only the in-memory list while dragging. Persist once in clearView.
            notifyItemMoved(from, to)
            return true
        }

        fun persistOrder() = onOrderSaved(items.map { it.key })

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder {
            val card = MaterialCardView(this@ApiManagementActivity).apply {
                radius = dp(14).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = palette.specialKey
                setCardBackgroundColor(palette.background)
                setContentPadding(dp(12), dp(10), dp(8), dp(10))
                layoutParams = RecyclerView.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            }
            val row = LinearLayout(this@ApiManagementActivity).apply { gravity = Gravity.CENTER_VERTICAL }
            val state = TextView(this@ApiManagementActivity).apply {
                text = "●"
                textSize = 17f
            }
            row.addView(state, LinearLayout.LayoutParams(dp(26), -2))
            val details = LinearLayout(this@ApiManagementActivity).apply { orientation = LinearLayout.VERTICAL }
            val title = TextView(this@ApiManagementActivity).apply {
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(palette.text)
            }
            val maskedKey = TextView(this@ApiManagementActivity).apply {
                textSize = 13f
                setTextColor(palette.hint)
            }
            val summary = TextView(this@ApiManagementActivity).apply { textSize = 12f }
            details.addView(title)
            details.addView(maskedKey)
            details.addView(summary)
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            val delete = MaterialButton(this@ApiManagementActivity).apply {
                text = getString(R.string.api_delete_action)
                setTextColor(palette.accent)
                backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            }
            row.addView(delete)
            card.addView(row)
            return Holder(card, state, title, maskedKey, summary, delete)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            val stats = item.stats
            holder.card.contentDescription = getString(R.string.api_priority_drag, position + 1)
            holder.state.contentDescription = getString(
                if (stats != null) R.string.api_status_ready else R.string.api_status_unknown,
            )
            holder.state.setTextColor(if (stats != null) palette.accent else Color.rgb(217, 119, 6))
            holder.title.text = getString(R.string.api_item_title, position + 1)
            holder.maskedKey.text = maskApiKey(item.key)
            holder.summary.text = stats?.let {
                val base = "${it.provider} • ${it.modelCount} model • ${it.accessSummary}"
                if (it.lastFailure.isBlank()) base else "$base\n⚠ ${it.lastFailure}"
            } ?: getString(R.string.api_key_not_scanned)
            holder.summary.setTextColor(if (stats?.lastFailure.isNullOrBlank()) palette.hint else Color.rgb(220, 38, 38))
            holder.delete.setOnClickListener { onDelete(item.key) }
        }

        inner class Holder(
            val card: MaterialCardView,
            val state: TextView,
            val title: TextView,
            val maskedKey: TextView,
            val summary: TextView,
            val delete: MaterialButton,
        ) : RecyclerView.ViewHolder(card)
    }

    private inner class ApiKeyTouchCallback(
        private val adapter: ApiKeyAdapter,
        private val palette: KeyboardThemePalette,
    ) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        private var orderChanged = false

        override fun isLongPressDragEnabled() = true

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val moved = adapter.move(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            orderChanged = orderChanged || moved
            return moved
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || viewHolder == null) return
            (viewHolder.itemView.parent as? RecyclerView)?.parent?.requestDisallowInterceptTouchEvent(true)
            viewHolder.itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            (viewHolder.itemView as? MaterialCardView)?.apply {
                cardElevation = dp(10).toFloat()
                strokeWidth = dp(2)
                strokeColor = palette.accent
                animate().scaleX(1.02f).scaleY(1.02f).setDuration(120L).start()
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            recyclerView.parent?.requestDisallowInterceptTouchEvent(false)
            (viewHolder.itemView as? MaterialCardView)?.apply {
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor = palette.specialKey
                animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
            }
            if (orderChanged) adapter.persistOrder()
            orderChanged = false
        }
    }

    private fun choiceButtons(
        labels: List<String>, initial: Int, textColor: Int, surfaceColor: Int, accentColor: Int,
        onSelected: (Int) -> Unit,
    ): HorizontalScrollView {
        var selected = initial.coerceIn(labels.indices)
        val buttons = mutableListOf<MaterialButton>()
        fun refresh() = buttons.forEachIndexed { index, button ->
            val active = index == selected
            button.setTextColor(if (active) Color.WHITE else textColor)
            button.backgroundTintList = ColorStateList.valueOf(if (active) accentColor else surfaceColor)
            button.strokeColor = ColorStateList.valueOf(accentColor)
            button.strokeWidth = dp(1)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            labels.forEachIndexed { index, label ->
                addView(MaterialButton(this@ApiManagementActivity).apply {
                    text = label; textSize = 13f
                    minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
                    insetTop = 0; insetBottom = 0
                    setPadding(dp(12), 0, dp(12), 0)
                    setOnClickListener { selected = index; refresh(); onSelected(index) }
                }.also(buttons::add), LinearLayout.LayoutParams(-2, dp(38)).apply {
                    if (index > 0) leftMargin = dp(6)
                })
            }
        }
        refresh()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            addView(row)
        }
    }

    private fun maskApiKey(key: String): String = when {
        key.length <= 4 -> "••••"
        key.length <= 10 -> "••••${key.takeLast(4)}"
        else -> "${key.take(4)}••••••${key.takeLast(4)}"
    }

    private fun fallbackStats(savedKeyCount: Int): AiKeyStats? {
        val models = AiPreferences.models(this)
        if (savedKeyCount != 1 || models.isEmpty()) return null
        return AiKeyStats(AiPreferences.provider(this), models.size, getString(R.string.api_key_recognized))
    }
}
