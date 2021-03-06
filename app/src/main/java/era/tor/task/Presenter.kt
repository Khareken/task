package era.tor.task

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.MotionEvent
import android.widget.ImageView
import java.util.*


class Presenter {

    private var model = Model()

    fun executeFloodFilling(method: Int, view: ImageView, event: MotionEvent): Bitmap {

        val viewCoords = IntArray(2)
        view.getLocationOnScreen(viewCoords)

        val absX = event.rawX
        val absY = event.rawY

        val imgX = absX - viewCoords[0]
        val imgY = absY - viewCoords[1]

        val maxImgX = view.width
        val maxImgY = view.height

        var bitmap: Bitmap?
        try {
            bitmap = (view.drawable as BitmapDrawable).bitmap
        } catch (e: kotlin.TypeCastException) {
            e.printStackTrace()
            Log.i(TAG, "executeFloodFilling: exception occurred -> $e")
            bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.RGB_565)
            return bitmap
        }

        val maxX = bitmap.width
        val maxY = bitmap.height

        val x = (maxX * imgX / maxImgX.toFloat()).toInt()
        val y = (maxY * imgY / maxImgY.toFloat()).toInt()

        val color = bitmap.getPixel(x, y)
        val isBlack = color == Color.BLACK

        val replacementColor = if (isBlack) Color.WHITE else Color.BLACK

        model.useImage(bitmap)
        model.floodFill(method, x, y, color, replacementColor)

        return bitmap

    }

    fun generateRandomBitmaps(x: Int, y: Int): Array<Bitmap> {
        val bmp1 = generateRandomBitmap(x, y)
        val bmp2 = generateRandomBitmap(x, y)
        return arrayOf(bmp1, bmp2)
    }

    private fun generateRandomBitmap(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (i in 0 until width) {
            for (j in 0 until height) {
                val color = when ((0..1).random()) {
                    0 -> Color.BLACK
                    1 -> Color.WHITE
                    else -> Color.BLACK
                }
                bmp.setPixel(i, j, color)
            }
        }
        return bmp
    }

    private fun ClosedRange<Int>.random() = Random().nextInt((endInclusive + 1) - start) + start

    companion object {
        private const val TAG = "Presenter"
    }
}
