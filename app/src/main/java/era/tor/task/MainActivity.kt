package era.tor.task

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding2.view.*
import com.jakewharton.rxbinding2.widget.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import era.tor.task.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), MView {

    private lateinit var activityViewActivityMainBinding : ActivityMainBinding
    private val presenter = Presenter()

    private val sizes = Point(64, 64)
    private var floodFillSpeed = 100
    private var firstFloodFillMethod = 0
    private var secondFloodFillMethod = 0


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityViewActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(activityViewActivityMainBinding.root)

        val firstBitmapFloodFiller = createFirstBitmapMotionEventObservable()
        val secondBitmapFloodFiller = createSecondBitmapMotionEventObservable()

        val bitmapGenerator = createBitmapGeneratorClickObservable()
        val bitmapSizePicker = createBitmapSizeClickObservable()

        bitmapGenerator
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { showProgress() }
                .observeOn(Schedulers.computation())
                .map { presenter.generateRandomBitmaps(it.x, it.y) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideProgress()
                    showResults(it)
                }

        firstBitmapFloodFiller
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it.action == MotionEvent.ACTION_DOWN }
                .observeOn(Schedulers.computation())
                .map { presenter.executeFloodFilling(firstFloodFillMethod, activityViewActivityMainBinding.firstAlgorithmImage, it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ showResult(it, activityViewActivityMainBinding.firstAlgorithmImage) }, { it.printStackTrace() })

        secondBitmapFloodFiller
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it.action == MotionEvent.ACTION_DOWN }
                .observeOn(Schedulers.computation())
                .map { presenter.executeFloodFilling(secondFloodFillMethod, activityViewActivityMainBinding.secondAlgorithmImage, it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ showResult(it, activityViewActivityMainBinding.secondAlgorithmImage) }, { it.printStackTrace() })

        bitmapSizePicker
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

        createSpinnerItemsListeners()
        createSeekBarValueListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFirstBitmapMotionEventObservable(): Observable<MotionEvent> = Observable.create { emitter ->
        activityViewActivityMainBinding.firstAlgorithmImage.setOnTouchListener { view, event ->
            if ((view as ImageView).drawable != null) emitter.onNext(event)
            true
        }

        emitter.setCancellable { activityViewActivityMainBinding.firstAlgorithmImage.setOnTouchListener(null) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createSecondBitmapMotionEventObservable(): Observable<MotionEvent> = Observable.create { emitter ->
        activityViewActivityMainBinding.secondAlgorithmImage.setOnTouchListener { view, event ->
            if ((view as ImageView).drawable != null) emitter.onNext(event)
            true
        }

        emitter.setCancellable { activityViewActivityMainBinding.secondAlgorithmImage.setOnTouchListener(null) }
    }

    @SuppressLint("CheckResult")
    private fun createBitmapGeneratorClickObservable(): Observable<Point> = Observable.create { emitter ->
        RxView.clicks(activityViewActivityMainBinding.generateNoiseButton)
                .subscribe {
                    activityViewActivityMainBinding.firstAlgorithmImage.setImageDrawable(null)
                    activityViewActivityMainBinding.secondAlgorithmImage.setImageDrawable(null)

                    emitter.onNext(sizes)
                }
    }

    @SuppressLint("CheckResult")
    private fun createBitmapSizeClickObservable(): Completable = Completable.create { emitter ->
        val dialogView = layoutInflater.inflate(R.layout.dialog_size, null)

        val dialogBuilder = AlertDialog.Builder(this)
        val dialog = dialogBuilder.create()
        dialog.setView(dialogView)

        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
        val okButton = dialogView.findViewById<Button>(R.id.ok_button)

        val width = dialogView.findViewById<EditText>(R.id.edit_width)
        val height = dialogView.findViewById<EditText>(R.id.edit_height)

        okButton.isEnabled = false

        val widthWatcherObservable = RxTextView.textChanges(width)
        val heightWatcherObservable = RxTextView.textChanges(width)

        Observable.merge(widthWatcherObservable, heightWatcherObservable)
                .subscribe {
                    okButton.isEnabled = (width.text.toString() != "") && (height.text.toString() != "")
                }

        RxView.clicks(activityViewActivityMainBinding.sizeButton).subscribe { dialog.show() }
        activityViewActivityMainBinding.sizeButton.setOnClickListener { dialog.show() }

        RxView.clicks(okButton).subscribe {
            sizes.x = width.text.toString().toInt()
            sizes.y = height.text.toString().toInt()

            width.hint = width.text
            height.hint = height.text

            dialog.dismiss()
            emitter.onComplete()
        }

        RxView.clicks(cancelButton).subscribe { dialog.dismiss() }
    }

    @SuppressLint("CheckResult")
    private fun createSpinnerItemsListeners() {
        val adapter = ArrayAdapter.createFromResource(this,
                R.array.algorithms_array, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        activityViewActivityMainBinding.firstAlgorithmSpinner.adapter = adapter
        activityViewActivityMainBinding.secondAlgorithmSpinner.adapter = adapter

        RxAdapterView.itemSelections(activityViewActivityMainBinding.firstAlgorithmSpinner)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { firstFloodFillMethod = it }

        RxAdapterView.itemSelections( activityViewActivityMainBinding.secondAlgorithmSpinner)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { firstFloodFillMethod = it }
    }

    @SuppressLint("CheckResult")
    private fun createSeekBarValueListener() {
        RxSeekBar.userChanges(  activityViewActivityMainBinding.speedSeekbar).subscribe { floodFillSpeed = it }
    }
    override fun showProgress() {
        activityViewActivityMainBinding.loadingView.visibility = View.VISIBLE

        activityViewActivityMainBinding.loadingView.playAnimation()
        activityViewActivityMainBinding.loadingView.loop(true)
    }

    override fun hideProgress() {
        activityViewActivityMainBinding.loadingView.cancelAnimation()
        activityViewActivityMainBinding.loadingView.visibility = View.GONE
    }

    override fun showResult(bitmap: Bitmap, view: ImageView) {
        val emptyBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        if (bitmap.sameAs(emptyBitmap)) return

        view.setImageBitmap(bitmap)
    }

    override fun showResults(bitmaps: Array<Bitmap>) {
        activityViewActivityMainBinding.firstAlgorithmImage.setImageBitmap(bitmaps[0])
        activityViewActivityMainBinding.secondAlgorithmImage.setImageBitmap(bitmaps[1])
    }
}