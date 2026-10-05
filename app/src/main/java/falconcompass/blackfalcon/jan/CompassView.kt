package falconcompass.blackfalcon.jan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.hardware.SensorManager
import android.location.Location
import android.view.View
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Full-screen flat compass with coordinates. Works in portrait and landscape. */
class CompassView(context: Context) : View(context) {

    var sensorAvailable = true
    var permissionGranted = true
    var accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    private var smooth = 0f
    private var first = true
    private var location: Location? = null

    private val d = resources.displayMetrics.density
    private val names = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    private val dark = Color.parseColor("#212121")
    private val red = Color.parseColor("#D32F2F")
    private val navy = Color.parseColor("#1A237E")

    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(150, 255, 255, 255)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * d
        color = dark
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = dark
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = dark
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = red
    }
    private val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }
    private val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        color = Color.parseColor("#616161")
        textSize = 14f * d
        typeface = Typeface.DEFAULT_BOLD
    }
    private val marker = Path()

    fun hasLocation(): Boolean = location != null

    fun setLocation(loc: Location) {
        location = loc
        invalidate()
    }

    fun setHeading(az: Float) {
        if (first) {
            smooth = az
            first = false
        } else {
            var delta = az - smooth
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            smooth = (smooth + delta * 0.2f + 360f) % 360f
        }
        invalidate()
    }

    private class Line(val text: String, val color: Int, val bold: Boolean)

    private fun dms(v: Double, pos: String, neg: String): String {
        val a = abs(v)
        val deg = a.toInt()
        val minF = (a - deg) * 60.0
        val min = minF.toInt()
        val sec = (minF - min) * 60.0
        return String.format(Locale.US, "%d\u00B0 %02d' %04.1f\" %s", deg, min, sec, if (v >= 0) pos else neg)
    }

    private fun buildLines(): List<Line> {
        val orange = Color.parseColor("#E65100")
        val lines = ArrayList<Line>()
        val loc = location
        if (loc != null) {
            val lat = loc.latitude
            val lon = loc.longitude
            lines.add(Line("Coordinates", navy, true))
            lines.add(Line(String.format(Locale.US, "Lat: %.6f\u00B0 %s", abs(lat), if (lat >= 0) "N" else "S"), dark, false))
            lines.add(Line(String.format(Locale.US, "Lon: %.6f\u00B0 %s", abs(lon), if (lon >= 0) "E" else "W"), dark, false))
            lines.add(Line("Lat: " + dms(lat, "N", "S"), dark, false))
            lines.add(Line("Lon: " + dms(lon, "E", "W"), dark, false))
            val alt = if (loc.hasAltitude()) String.format(Locale.US, "Alt: %d m", loc.altitude.roundToInt()) else "Alt: --"
            val acc = if (loc.hasAccuracy()) String.format(Locale.US, "   \u00B1%d m", loc.accuracy.roundToInt()) else ""
            lines.add(Line(alt + acc, dark, false))
        } else if (!permissionGranted) {
            lines.add(Line("Location permission needed", red, true))
        } else {
            lines.add(Line("Coordinates", navy, true))
            lines.add(Line("Waiting for GPS\u2026", dark, false))
        }
        if (!sensorAvailable) {
            lines.add(Line("Compass sensor not available", red, true))
        } else if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            lines.add(Line("Calibrate: move phone in a figure 8", orange, true))
        }
        return lines
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = 34f * d
        val land = w > h

        val r: Float
        val cx: Float
        val cy: Float
        if (land) {
            r = h / 2f - m
            cx = m + r
            cy = h / 2f
        } else {
            r = w / 2f - m
            cx = w / 2f
            cy = maxOf(m + r, h * 0.36f)
        }

        // Dial background + ring
        canvas.drawCircle(cx, cy, r, discPaint)
        canvas.drawCircle(cx, cy, r, ringPaint)

        // Rotating dial
        canvas.save()
        canvas.rotate(-smooth, cx, cy)
        val degSize = r * 0.075f
        val cardSize = r * 0.16f
        for (i in 0 until 72) {
            val a = i * 5
            val len: Float
            when {
                a % 90 == 0 -> {
                    len = 24f * d
                    tickPaint.strokeWidth = 3.5f * d
                }
                a % 30 == 0 -> {
                    len = 18f * d
                    tickPaint.strokeWidth = 2.5f * d
                }
                a % 10 == 0 -> {
                    len = 12f * d
                    tickPaint.strokeWidth = 1.8f * d
                }
                else -> {
                    len = 7f * d
                    tickPaint.strokeWidth = 1.2f * d
                }
            }
            tickPaint.color = if (a == 0) red else dark
            canvas.save()
            canvas.rotate(a.toFloat(), cx, cy)
            canvas.drawLine(cx, cy - r, cx, cy - r + len, tickPaint)
            if (a % 90 == 0) {
                textPaint.typeface = Typeface.DEFAULT_BOLD
                textPaint.textSize = cardSize
                textPaint.color = if (a == 0) red else dark
                canvas.drawText(names[a / 45], cx, cy - r + 28f * d + cardSize, textPaint)
            } else if (a % 30 == 0) {
                textPaint.typeface = Typeface.DEFAULT
                textPaint.textSize = degSize
                textPaint.color = Color.parseColor("#424242")
                canvas.drawText(a.toString(), cx, cy - r + 22f * d + degSize, textPaint)
            }
            canvas.restore()
        }
        canvas.restore()

        // Fixed heading marker at the top of the dial
        marker.reset()
        marker.moveTo(cx, cy - r - 2f * d)
        marker.lineTo(cx - 14f * d, cy - r - 26f * d)
        marker.lineTo(cx + 14f * d, cy - r - 26f * d)
        marker.close()
        canvas.drawPath(marker, markerPaint)

        // Centre: heading degrees + direction name
        val heading = smooth.roundToInt() % 360
        val bigSize = r * 0.30f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.color = dark
        textPaint.textSize = bigSize
        canvas.drawText("$heading\u00B0", cx, cy + bigSize * 0.1f, textPaint)
        val dirSize = r * 0.16f
        textPaint.textSize = dirSize
        textPaint.color = red
        val dirName = names[((smooth + 22.5f) / 45f).toInt() % 8]
        canvas.drawText(dirName, cx, cy + bigSize * 0.1f + dirSize * 1.4f, textPaint)

        // Coordinates
        val lines = buildLines()
        val areaX: Float
        val areaW: Float
        if (land) {
            areaX = cx + r + 2f * m
            areaW = w - areaX - m
        } else {
            areaX = m
            areaW = w - 2f * m
        }
        val size = minOf(areaW / 14f, 26f * d)
        val lineH = size * 1.5f
        var y = if (land) {
            h / 2f - lines.size * lineH / 2f + size
        } else {
            cy + r + m + size
        }
        for (line in lines) {
            infoPaint.color = line.color
            infoPaint.typeface = if (line.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            infoPaint.textSize = if (line.bold) size * 1.1f else size
            canvas.drawText(line.text, areaX, y, infoPaint)
            y += lineH
        }

        // Credit in a corner
        canvas.drawText("By: Black Falcon", w - 16f * d, h - 16f * d, creditPaint)
    }
}
