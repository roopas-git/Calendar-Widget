package `in`.versionx.calendarwidget.widget

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import `in`.versionx.calendarwidget.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val HUE_COLORS = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

/** Quick-pick colors shown under the wheel. */
private val SWATCHES = listOf(
    0xFF000000, 0xFF1B1B1B, 0xFF616161, 0xFFFFFFFF,
    0xFFF44336, 0xFFFF5F6D, 0xFFE91E63, 0xFF9C27B0, 0xFF3A1C71,
    0xFF3F51B5, 0xFF2196F3, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
    0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFF795548,
).map { it.toInt() }

/**
 * HSV color picker modeled on the old app's dialog (screenshot 4 in
 * docs/reference-screenshots): a hue ring, a center disc showing the old color
 * (left half) next to the new one (right half), a saturation slider and a
 * brightness slider, plus a row of quick swatches. Built with Compose Canvas, no
 * third-party dependency.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Int,
    title: String,
    onDismiss: () -> Unit,
    onColorPicked: (Int) -> Unit,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))

    fun setFrom(color: Int) {
        val hsv = FloatArray(3).also { AndroidColor.colorToHSV(color, it) }
        // Keep the hue for grays so the ring handle doesn't jump to red.
        if (hsv[1] > 0f) hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF262626), contentColor = Color.White) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFFC371))))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = "🎨  ${title.uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HueRing(
                        hue = hue,
                        oldColor = Color(initialColor),
                        newColor = Color(currentColor),
                        onHueChange = { hue = it },
                    )

                    GradientSlider(
                        value = saturation,
                        colors = listOf(
                            Color(AndroidColor.HSVToColor(floatArrayOf(hue, 0f, brightness))),
                            Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, brightness))),
                        ),
                        onValueChange = { saturation = it },
                    )
                    GradientSlider(
                        value = brightness,
                        colors = listOf(Color.Black, Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f)))),
                        onValueChange = { brightness = it },
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SWATCHES.forEach { swatch ->
                            val selected = (swatch and 0xFFFFFF) == (currentColor and 0xFFFFFF)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) Color(0xFFFF7A59) else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape,
                                    )
                                    .clickable { setFrom(swatch) },
                            )
                        }
                    }

                    Text(
                        text = "#%06X".format(currentColor and 0xFFFFFF),
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        ) {
                            Text(text = stringResource(R.string.color_picker_close).uppercase())
                        }
                        Button(
                            onClick = { onColorPicked(currentColor) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A59), contentColor = Color.White),
                        ) {
                            Text(text = stringResource(R.string.color_picker_pick).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HueRing(
    hue: Float,
    oldColor: Color,
    newColor: Color,
    onHueChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    fun update(p: Offset) {
                        val angle = Math.toDegrees(
                            atan2((p.y - size.height / 2f).toDouble(), (p.x - size.width / 2f).toDouble()),
                        ).toFloat()
                        onHueChange((angle + 360f) % 360f)
                    }
                    detectTapGestures { update(it) }
                }
                .pointerInput(Unit) {
                    fun update(p: Offset) {
                        val angle = Math.toDegrees(
                            atan2((p.y - size.height / 2f).toDouble(), (p.x - size.width / 2f).toDouble()),
                        ).toFloat()
                        onHueChange((angle + 360f) % 360f)
                    }
                    detectDragGestures(onDragStart = { update(it) }) { change, _ ->
                        change.consume()
                        update(change.position)
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringWidth = size.minDimension * 0.09f
                val radius = size.minDimension / 2f - ringWidth
                val c = center
                drawCircle(
                    brush = Brush.sweepGradient(HUE_COLORS, center = c),
                    radius = radius,
                    center = c,
                    style = Stroke(width = ringWidth),
                )

                // Center disc: old color on the left, new color on the right.
                val discR = radius * 0.5f
                val discTopLeft = Offset(c.x - discR, c.y - discR)
                val discSize = Size(discR * 2, discR * 2)
                drawArc(oldColor, 90f, 180f, useCenter = true, topLeft = discTopLeft, size = discSize)
                drawArc(newColor, -90f, 180f, useCenter = true, topLeft = discTopLeft, size = discSize)

                // Handle on the ring.
                val rad = Math.toRadians(hue.toDouble())
                val handle = Offset(c.x + radius * cos(rad).toFloat(), c.y + radius * sin(rad).toFloat())
                val handleR = min(ringWidth * 0.95f, size.minDimension * 0.07f)
                drawCircle(Color.Black.copy(alpha = 0.35f), handleR + 3f, handle)
                drawCircle(Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))), handleR, handle)
                drawCircle(Color.White, handleR, handle, style = Stroke(width = 4f))
            }
        }
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    colors: List<Color>,
    onValueChange: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { onValueChange((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragStart = { onValueChange((it.x / size.width).coerceIn(0f, 1f)) }) { change, _ ->
                    change.consume()
                    onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackH = size.height * 0.35f
            val thumbR = size.height / 2f - 2f
            val top = (size.height - trackH) / 2f
            drawRoundRect(
                brush = Brush.horizontalGradient(colors, startX = thumbR, endX = size.width - thumbR),
                topLeft = Offset(thumbR, top),
                size = Size(size.width - thumbR * 2, trackH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f),
            )
            val x = thumbR + value * (size.width - thumbR * 2)
            val thumbColor = lerpColor(colors.first(), colors.last(), value)
            drawCircle(Color.Black.copy(alpha = 0.35f), thumbR, Offset(x, center.y + 1.5f))
            drawCircle(thumbColor, thumbR - 2f, Offset(x, center.y))
            drawCircle(Color.White, thumbR - 2f, Offset(x, center.y), style = Stroke(width = 4f))
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)
