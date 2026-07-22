package vn.kai.board.touch

import android.content.Context
import java.util.Locale
import vn.kai.board.input.KeyAction

/**
 * Offline per-key touch bias so hit-testing follows where the user actually taps.
 *
 * Stores mean offsets as fractions of key width/height (layout-size independent).
 * Does not move drawn keys — only the score center used by [TouchTargetPolicy].
 * No typed content is stored; only geometric offsets keyed by letter/space/backspace.
 */
object TouchAdaptationStore {
    private const val FILE = "touch_adaptation"
    private const val KEY = "biases_v1"
    private const val ROW = "\u001F"
    private const val FIELD = "\u001E"
    private const val LIMIT = 64
    private const val MAX_SAMPLES = 48
    /** Cap so the adaptive center stays inside the key. */
    private const val MAX_FRAC = 0.32f
    private const val MIN_SAMPLES_FOR_FULL = 12

    data class Bias(
        val dxFrac: Float,
        val dyFrac: Float,
        val samples: Int,
    ) {
        val strength: Float get() = (samples.toFloat() / MIN_SAMPLES_FOR_FULL).coerceIn(0f, 1f)
    }

    @Volatile private var cache: Map<String, Bias>? = null

    fun read(context: Context): Map<String, Bias> {
        cache?.let { return it }
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        if (raw.isEmpty()) return emptyMap<String, Bias>().also { cache = it }
        return raw.split(ROW).mapNotNull { row ->
            val p = row.split(FIELD)
            if (p.size != 4) return@mapNotNull null
            val id = p[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val dx = p[1].toFloatOrNull() ?: return@mapNotNull null
            val dy = p[2].toFloatOrNull() ?: return@mapNotNull null
            val n = p[3].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
            if (n <= 0) return@mapNotNull null
            id to Bias(dx.coerceIn(-MAX_FRAC, MAX_FRAC), dy.coerceIn(-MAX_FRAC, MAX_FRAC), n.coerceAtMost(MAX_SAMPLES))
        }.toMap().also { cache = it }
    }

    /**
     * Absolute pixel center bias for the current layout: (dxPx, dyPx) per geometry id,
     * mapped through [stableId] so letter keys share one profile across rows/shift.
     */
    fun absoluteBiasPx(context: Context, keys: List<KeyGeometry>): Map<String, Pair<Float, Float>> {
        val learned = read(context)
        if (learned.isEmpty()) return emptyMap()
        val out = HashMap<String, Pair<Float, Float>>(keys.size)
        for (key in keys) {
            val stable = stableId(key) ?: continue
            val bias = learned[stable] ?: continue
            if (bias.samples <= 0 || bias.strength <= 0f) continue
            val w = (key.right - key.left).coerceAtLeast(1f)
            val h = (key.bottom - key.top).coerceAtLeast(1f)
            val dx = bias.dxFrac * w * bias.strength
            val dy = bias.dyFrac * h * bias.strength
            out[key.id] = dx to dy
        }
        return out
    }

    fun record(
        context: Context,
        key: KeyGeometry,
        touchX: Float,
        touchY: Float,
    ) {
        val stable = stableId(key) ?: return
        val w = (key.right - key.left).coerceAtLeast(1f)
        val h = (key.bottom - key.top).coerceAtLeast(1f)
        val dxFrac = ((touchX - key.centerX) / w).coerceIn(-MAX_FRAC, MAX_FRAC)
        val dyFrac = ((touchY - key.centerY) / h).coerceIn(-MAX_FRAC, MAX_FRAC)
        // Ignore near-center taps — they do not teach a useful shift.
        if (kotlin.math.abs(dxFrac) < 0.04f && kotlin.math.abs(dyFrac) < 0.04f) return

        val current = read(context).toMutableMap()
        val old = current[stable]
        val next = if (old == null || old.samples <= 0) {
            Bias(dxFrac, dyFrac, 1)
        } else {
            val n = old.samples.coerceAtMost(MAX_SAMPLES)
            // Running mean with soft cap so recent taps still matter.
            val weight = 1f / (n + 1f)
            Bias(
                dxFrac = (old.dxFrac * (1f - weight) + dxFrac * weight).coerceIn(-MAX_FRAC, MAX_FRAC),
                dyFrac = (old.dyFrac * (1f - weight) + dyFrac * weight).coerceIn(-MAX_FRAC, MAX_FRAC),
                samples = (n + 1).coerceAtMost(MAX_SAMPLES),
            )
        }
        current[stable] = next
        // Evict least-sampled when over limit.
        val limited = if (current.size <= LIMIT) current else {
            current.entries.sortedByDescending { it.value.samples }.take(LIMIT).associate { it.toPair() }
        }
        write(context, limited)
    }

    fun clear(context: Context) {
        cache = emptyMap()
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun count(context: Context): Int = read(context).size

    /** Stable id independent of row index / shift layout. */
    fun stableId(key: KeyGeometry): String? = when (val action = key.action) {
        is KeyAction.Character -> {
            val c = action.value
            if (c.isLetterOrDigit() || c in ".,'") {
                "c:${c.lowercaseChar()}"
            } else null
        }
        KeyAction.Space -> "space"
        KeyAction.Backspace -> "backspace"
        KeyAction.Shift -> "shift"
        KeyAction.Enter -> "enter"
        else -> null
    }

    private fun write(context: Context, values: Map<String, Bias>) {
        cache = values
        val encoded = values.entries.joinToString(ROW) { (id, b) ->
            listOf(
                id,
                String.format(Locale.US, "%.5f", b.dxFrac),
                String.format(Locale.US, "%.5f", b.dyFrac),
                b.samples.toString(),
            ).joinToString(FIELD)
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, encoded).apply()
    }
}
