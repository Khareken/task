package era.tor.task

import android.graphics.Bitmap
import android.widget.ImageView

interface MView {
    fun showProgress()

    fun hideProgress()

    fun showResult(bitmap: Bitmap, view: ImageView)

    fun showResults(bitmaps: Array<Bitmap>)

}