package `in`.versionx.calendarwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tool, not a check: renders res/layout/widget_preview.xml to a transparent PNG for
 * the widget picker on Android 8–11 (which uses `previewImage`; Android 12+ draws the
 * layout itself). Re-run after changing that layout:
 *
 *   adb shell am instrument -w -e class in.versionx.calendarwidget.WidgetPreviewImageGenerator \
 *       in.versionx.calendarwidget.test/androidx.test.runner.AndroidJUnitRunner
 *   adb exec-out run-as in.versionx.calendarwidget cat files/widget_preview_image.png \
 *       > app/src/main/res/drawable-nodpi/widget_preview_image.png
 */
@RunWith(AndroidJUnit4::class)
class WidgetPreviewImageGenerator {

    @Test
    fun renderPreviewImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        val widthPx = (WIDTH_DP * density).toInt()
        val heightPx = (HEIGHT_DP * density).toInt()

        val view = LayoutInflater.from(context).inflate(R.layout.widget_preview, null)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, widthPx, heightPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val out = File(context.filesDir, "widget_preview_image.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 0)
    }

    private companion object {
        // Roughly a 3 x 2 cell widget.
        const val WIDTH_DP = 300
        const val HEIGHT_DP = 150
    }
}
