package ani.dantotsu.others

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import ani.dantotsu.R
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Animated Kujaku splash logo. Pure Canvas code, works on API 21+.
 *
 * Timeline (TOTAL_MS): logo pops in, sparkles twinkle, crest beads glow,
 * a light shimmer sweeps over the bird, then [onFinished] is called.
 * With "Remove animations" enabled the final frame is shown without motion.
 */
class SplashLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Called once when the animation is done (on the main thread). */
    var onFinished: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val bgColor = Color.parseColor("#03001C")

    private val logo: Bitmap? = decode(R.drawable.splash_logo)
    private val shineMask: Bitmap? = decode(R.drawable.splash_shine_mask)

    private val reduceMotion: Boolean = try {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    } catch (e: Throwable) {
        false
    }

    private var startTime = -1L
    private var finished = false

    // layout (computed in onSizeChanged)
    private val tileRect = RectF()
    private var tileSize = 0f
    private var cornerRadius = 0f
    private val tilePath = Path()
    private var glowShader: RadialGradient? = null
    private var bandWidth = 0f
    private var bandShader: LinearGradient? = null

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.argb(46, 255, 255, 255)
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F1EBFF")
    }
    private val starGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 3f,
            intArrayOf(Color.argb(110, 185, 168, 255), Color.argb(0, 185, 168, 255)),
            null, Shader.TileMode.CLAMP
        )
    }
    private val beadGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(Color.argb(190, 90, 235, 255), Color.argb(0, 90, 235, 255)),
            null, Shader.TileMode.CLAMP
        )
    }
    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    // 4-point star with radius 1 centred on (0,0)
    private val starPath = Path().apply {
        moveTo(0f, -1f)
        quadTo(0.12f, -0.12f, 1f, 0f)
        quadTo(0.12f, 0.12f, 0f, 1f)
        quadTo(-0.12f, 0.12f, -1f, 0f)
        quadTo(-0.12f, -0.12f, 0f, -1f)
        close()
    }

    private val overshoot = OvershootInterpolator(1.1f)
    private val decelerate = DecelerateInterpolator()

    private class Sparkle(
        val x: Float, val y: Float, val r: Float, val phase: Float, val period: Float,
        val star: Boolean = true
    )

    // positions are fractions of the logo tile
    private val sparkles = listOf(
        Sparkle(0.416f, 0.162f, 0.060f, 0f, 900f),
        Sparkle(0.174f, 0.322f, 0.055f, 300f, 1000f),
        Sparkle(0.256f, 0.423f, 0.022f, 600f, 800f),
        Sparkle(0.245f, 0.628f, 0.040f, 150f, 950f),
        Sparkle(0.289f, 0.210f, 0.010f, 0f, 700f, false),
        Sparkle(0.361f, 0.294f, 0.011f, 350f, 760f, false),
        Sparkle(0.155f, 0.522f, 0.010f, 500f, 820f, false),
        Sparkle(0.322f, 0.722f, 0.011f, 200f, 900f, false)
    )

    // crest bead centres, as fractions of the logo tile
    private val beads = listOf(
        floatArrayOf(0.664f, 0.165f),
        floatArrayOf(0.759f, 0.191f),
        floatArrayOf(0.818f, 0.258f)
    )

    // small ambient sparkles over the whole screen: x, y (fractions), size (dp), phase (ms)
    private val ambient = listOf(
        floatArrayOf(0.12f, 0.16f, 3.5f, 100f),
        floatArrayOf(0.84f, 0.12f, 2.5f, 450f),
        floatArrayOf(0.20f, 0.78f, 3f, 700f),
        floatArrayOf(0.90f, 0.66f, 3.5f, 250f),
        floatArrayOf(0.07f, 0.48f, 2f, 550f),
        floatArrayOf(0.70f, 0.90f, 2.5f, 350f),
        floatArrayOf(0.48f, 0.07f, 2f, 800f),
        floatArrayOf(0.93f, 0.36f, 2f, 150f)
    )

    private fun decode(id: Int): Bitmap? = try {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(resources, id, opts)
    } catch (e: Throwable) {
        null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tileSize = min(min(w, h) * 0.62f, 320f * density)
        val cx = w / 2f
        val cy = h * 0.46f
        tileRect.set(cx - tileSize / 2, cy - tileSize / 2, cx + tileSize / 2, cy + tileSize / 2)
        cornerRadius = tileSize * 0.22f
        tilePath.reset()
        tilePath.addRoundRect(tileRect, cornerRadius, cornerRadius, Path.Direction.CW)

        glowShader = RadialGradient(
            cx, cy, tileSize * 0.95f,
            intArrayOf(Color.argb(150, 124, 77, 255), Color.argb(0, 124, 77, 255)),
            null, Shader.TileMode.CLAMP
        )
        bandWidth = tileSize * 0.30f
        bandShader = LinearGradient(
            -bandWidth / 2f, 0f, bandWidth / 2f, 0f,
            intArrayOf(Color.argb(0, 255, 255, 255), Color.argb(150, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        bandPaint.shader = bandShader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val now = SystemClock.uptimeMillis()
        if (startTime < 0) startTime = now
        val t = if (reduceMotion) TOTAL_MS.toFloat() else min((now - startTime).toFloat(), TOTAL_MS.toFloat())

        canvas.drawColor(bgColor)

        val cx = tileRect.centerX()
        val cy = tileRect.centerY()
        val tileAlpha = decelerate.getInterpolation(clamp01(t / 450f))
        val popScale = 0.84f + 0.16f * overshoot.getInterpolation(clamp01(t / 650f))

        // soft violet glow behind the tile, gently breathing
        glowShader?.let {
            val breathe = 0.85f + 0.15f * sin(2.0 * PI * t / 1400.0).toFloat()
            glowPaint.shader = it
            glowPaint.alpha = (255 * smooth(0f, 500f, t) * breathe).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, tileSize * 0.95f, glowPaint)
        }

        drawAmbient(canvas, t)

        if (logo != null) {
            canvas.save()
            canvas.scale(popScale, popScale, cx, cy)
            val pad = 4f * density
            val layer = canvas.saveLayerAlpha(
                tileRect.left - pad, tileRect.top - pad, tileRect.right + pad, tileRect.bottom + pad,
                (tileAlpha * 255).toInt().coerceIn(0, 255)
            )

            canvas.save()
            canvas.clipPath(tilePath)
            canvas.drawBitmap(logo, null, tileRect, bitmapPaint)
            drawBeadGlows(canvas, t)
            drawSparkles(canvas, t)
            drawShine(canvas, t)
            canvas.restore()

            canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, borderPaint)
            canvas.restoreToCount(layer)
            canvas.restore()
        }

        if (t < TOTAL_MS) {
            postInvalidateOnAnimation()
        } else if (!finished) {
            finished = true
            if (reduceMotion) postDelayed({ onFinished?.invoke() }, 400L) else onFinished?.invoke()
        }
    }

    private fun drawBeadGlows(canvas: Canvas, t: Float) {
        for ((i, b) in beads.withIndex()) {
            val start = 500f + i * 120f
            val env = smooth(start, start + 250f, t)
            if (env <= 0f) continue
            val pulse = 0.5f + 0.5f * sin(2.0 * PI * (t - start) / 700.0 - PI / 2).toFloat()
            val radius = tileSize * 0.085f * (0.9f + 0.25f * pulse)
            beadGlowPaint.alpha = (255 * env * (0.30f + 0.55f * pulse)).toInt().coerceIn(0, 255)
            canvas.save()
            canvas.translate(tileRect.left + b[0] * tileSize, tileRect.top + b[1] * tileSize)
            canvas.scale(radius, radius)
            canvas.drawCircle(0f, 0f, 1f, beadGlowPaint)
            canvas.restore()
        }
    }

    private fun drawSparkles(canvas: Canvas, t: Float) {
        for (s in sparkles) {
            val env = smooth(250f + s.phase * 0.3f, 550f + s.phase * 0.3f, t)
            if (env <= 0f) continue
            val wave = 0.5f + 0.5f * sin(2.0 * PI * (t - s.phase) / s.period).toFloat()
            val k = 0.25f + 0.75f * wave.pow(1.4f)
            val radius = s.r * tileSize * (0.6f + 0.4f * k)
            canvas.save()
            canvas.translate(tileRect.left + s.x * tileSize, tileRect.top + s.y * tileSize)
            canvas.scale(radius, radius)
            starGlowPaint.alpha = (255 * env * k).toInt().coerceIn(0, 255)
            canvas.drawCircle(0f, 0f, 3f, starGlowPaint)
            starPaint.alpha = (255 * env * k).toInt().coerceIn(0, 255)
            if (s.star) canvas.drawPath(starPath, starPaint) else canvas.drawCircle(0f, 0f, 0.8f, starPaint)
            canvas.restore()
        }
    }

    private fun drawShine(canvas: Canvas, t: Float) {
        val mask = shineMask ?: return
        val p = (t - SHINE_START) / (SHINE_END - SHINE_START)
        if (p <= 0f || p >= 1f) return
        val layer = canvas.saveLayer(tileRect.left, tileRect.top, tileRect.right, tileRect.bottom, null)
        canvas.save()
        canvas.translate(
            tileRect.left + tileSize * (-0.3f + 1.6f * p),
            tileRect.centerY()
        )
        canvas.rotate(-20f)
        canvas.drawRect(-bandWidth / 2f, -tileSize, bandWidth / 2f, tileSize, bandPaint)
        canvas.restore()
        canvas.drawBitmap(mask, null, tileRect, maskPaint)
        canvas.restoreToCount(layer)
    }

    private fun drawAmbient(canvas: Canvas, t: Float) {
        for (a in ambient) {
            val env = smooth(300f + a[3] * 0.4f, 700f + a[3] * 0.4f, t)
            if (env <= 0f) continue
            val wave = 0.5f + 0.5f * sin(2.0 * PI * (t - a[3]) / 1100.0).toFloat()
            val k = 0.2f + 0.8f * wave
            val drift = 10f * density * (t / 1000f)
            val radius = a[2] * density * (0.7f + 0.3f * k)
            canvas.save()
            canvas.translate(a[0] * width, a[1] * height - drift)
            canvas.scale(radius, radius)
            starPaint.alpha = (200 * env * k).toInt().coerceIn(0, 255)
            canvas.drawPath(starPath, starPaint)
            canvas.restore()
        }
    }

    private fun clamp01(v: Float) = max(0f, min(1f, v))

    private fun smooth(from: Float, to: Float, v: Float): Float {
        val x = clamp01((v - from) / (to - from))
        return x * x * (3f - 2f * x)
    }

    companion object {
        /** Total length of the splash animation. */
        const val TOTAL_MS = 1500L
        private const val SHINE_START = 650f
        private const val SHINE_END = 1250f
    }
}
