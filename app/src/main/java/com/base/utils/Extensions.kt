package app.wakeupp.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.base.BuildConfig
import com.base.R
import com.base.base.BaseAdapter
import com.base.utils.FORMAT_UTC_DATE_TIME
import com.base.utils.GlideApp
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Created by Nikul on 14-08-2020.
 */

fun <T> Context.isServiceRunning(service: Class<T>): Boolean {
    return (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == service.name }
}

fun Fragment.isNetworkConnected(): Boolean {
    return context?.isNetworkConnected() ?: false
}

fun Context.isNetworkConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}

fun Context.getDeviceHeight(): Int = resources.displayMetrics.heightPixels

fun Context.getDeviceWidth(): Int = resources.displayMetrics.widthPixels

fun Context.copy(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip: ClipData = ClipData.newPlainText("Copy", text)
    clipboard?.setPrimaryClip(clip)
}

fun Context.share(text: String, shareFile: File) {
    try {
        if (!shareFile.exists()) {
            toast("File not exists...")
            return
        }

        val intent = Intent(Intent.ACTION_SEND)
        shareFile.let {
            intent.type = "*/*"  /*video/mp4  WA*/
            val uri = FileProvider.getUriForFile(
                this, applicationContext.packageName + ".provider",
                it
            )
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_TEXT, text)
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.storeImageFromBitmap(
    bitmap: Bitmap,
    directory: File?,
    storeFileName: String
): File? {
    var foStream: FileOutputStream
    val wrapper = ContextWrapper(this)

//        File file = getStoryDirectoryName(context);
    val file = File(directory, storeFileName)
    try {
        var stream: OutputStream? = null
        stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return file
//        Uri savedImageURI = Uri.parse(file.getAbsolutePath());
//        return savedImageURI;
}

fun Context.copyDataFromOneToAnother(
    fromPath: String,
    toPath: String,
    onCopyCompleted: (fullDBPath: String) -> Unit
) {
    val inStream = File(fromPath).inputStream()
    val outStream = FileOutputStream(toPath)

    doAsync {
        inStream.use { input ->
            outStream.use { output ->
                input.copyTo(output)
            }
        }
        onComplete {
            onCopyCompleted(toPath)
        }
    }
}

fun Context.zipFiles(
    files: Array<String>,
    destZipPath: String,
    onZipCompleted: (zipPath: String) -> Unit
) {
    doAsync {
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(destZipPath)))
        for (file in files) {
            val fileInputStream = FileInputStream(file)
            val origin = BufferedInputStream(fileInputStream)
            val entry = ZipEntry(file.substring(file.lastIndexOf("/")))
            out.putNextEntry(entry)
            origin.copyTo(out/*,1024*/)
            origin.close()
        }
        out.close()

        onComplete {
            onZipCompleted(destZipPath)
        }
    }
}

fun Context.unZipFiles(
    zipFilePath: String,
    destZipPath: String,
    onUnZipCompleted: (unZipPath: String) -> Unit
) {
    doAsync {
        var filename: String
        val zipInput = ZipInputStream(BufferedInputStream(FileInputStream(zipFilePath)))
//        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipPath)))
        var zipEntry: ZipEntry
        while (zipInput.nextEntry.also { zipEntry = it } != null) {
            filename = zipEntry.name

            // Need to create directories if not exists, or
            // it will generate an Exception...
            if (zipEntry.isDirectory) {
                val fmd = File(destZipPath + filename)
                fmd.mkdirs()
                continue
            }
            val fileOutputStream = FileOutputStream(destZipPath + filename)
            zipInput.use { input ->
                fileOutputStream.use { output ->
                    input.copyTo(output)
                }
            }
//            fileOutputStream.close()
        }
        zipInput.close()

        onComplete {
            onUnZipCompleted(destZipPath)
        }
    }
}

data class ZipIO(val entry: ZipEntry, val output: File)

fun Context.unzip(
    zipFilePath: String,
    unzipLocationRoot: File,
    onUnZipCompleted: (unZipPath: String) -> Unit
) {
    doAsync {
        if (!unzipLocationRoot.exists()) {
            unzipLocationRoot.mkdirs()
        }

        ZipFile(zipFilePath).use { zip ->
            zip.entries()
                .asSequence()
                .map {
                    val outputFile = File(unzipLocationRoot.absolutePath + File.separator + it.name)
                    ZipIO(it, outputFile)
                }
                .map {
                    it.output.parentFile?.run {
                        if (!exists()) mkdirs()
                    }
                    it
                }
                .filter { !it.entry.isDirectory }
                .forEach { (entry, output) ->
                    zip.getInputStream(entry).use { input ->
                        output.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
        }
        onComplete {
            onUnZipCompleted(unzipLocationRoot.absolutePath)
        }
    }
}

fun String?.decodeUtfString(): String {
    return URLDecoder.decode(this, "UTF-8").orEmpty()
}

fun String?.encodeUtfString(): String {
    return URLEncoder.encode(this, "UTF-8").orEmpty()
}







fun ViewGroup.addViewObserver(function: () -> Unit) {
    val view = this
    view.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            function.invoke()
        }
    })
}

operator fun CompositeDisposable.plusAssign(disposable: Disposable?) {
    if (disposable != null) this.add(disposable)
}

fun TextView.fromHtml(@StringRes id: Int) {
    text = HtmlCompat.fromHtml(resources.getString(id), HtmlCompat.FROM_HTML_MODE_COMPACT)
}

fun TextView.fromHtml(str: String) {
    text = HtmlCompat.fromHtml(str, HtmlCompat.FROM_HTML_MODE_COMPACT)
}

fun EditText.setEditTextErrorWithMessage(msg: String, shakeAnimation: Animation) {
    startAnimation(shakeAnimation)
    error = msg
    requestFocus()
}

fun ImageView.load(url: String?, placeholder: Int, skipCaching: Boolean = false) {
    GlideApp.with(context)
        .load(url)
        .placeholder(placeholder)
        .skipMemoryCache(skipCaching)
        .error(placeholder)
        .into(this)
}





fun Activity.hideKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view = currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.showKeyboard() {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view = currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.showDeniedPermissionDialog(
    message: String,
    positiveClick: () -> Unit,
    negativeClick: () -> Unit
) {
    alert(message) {
        positiveButton("Allow") {
            startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            })
            positiveClick()
        }
        negativeButton("Deny") {
            negativeClick()
        }
    }.show()
}


fun Context.showAskingPermissionDialog(
    message: String,
    positiveClick: () -> Unit,
    negativeClick: () -> Unit
) {
    val alert = alert(message) {
        positiveButton("Allow") {
            positiveClick()
        }
        negativeButton("Deny") {
            negativeClick()
        }
    }
    alert.isCancelable = false
    alert.show()
}

fun Context.openFile(url: File) {
    try {
        if (!url.exists()) {
            toast("File not exists...")
            return
        }

        Timber.d(url.absolutePath)
        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", url)
        val intent = Intent(Intent.ACTION_VIEW)
        if (url.toString().contains(".pdf")) {
            intent.setDataAndType(uri, "application/pdf")
        } else if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
            intent.setDataAndType(uri, "application/msword")
        } else if (url.toString().contains(".xls")) {
            intent.setDataAndType(uri, "application/vnd.ms-excel")
        } else if (url.toString().contains(".xlsx")) {
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } else if (url.toString().contains(".odt")) {
            intent.setDataAndType(uri, "application/vnd.oasis.opendocument.text")
        } else if (url.toString().contains(".apk")) {
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
        } else if (url.toString().contains(".png") || url.toString().contains(".jpeg") || url.toString().contains(".jpg")) {
            intent.setDataAndType(uri, "image/*")
        } else if (url.toString().contains(".mp4")) {
            intent.setDataAndType(uri, "video/mp4")
        } else if (url.toString().contains(".mkv")) {
            intent.setDataAndType(uri, "video/x-matroska")
        } else if (url.toString().contains(".avi")) {
            intent.setDataAndType(uri, "video/avi")
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        toast("You may not have a proper app for viewing this content")
    }
}

fun Calendar.todayDate(): Long {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)

    return timeInMillis
}

fun Calendar.getDateTimeFormat(format: String): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(time)
}

fun Calendar.localToUTC(format: String = FORMAT_UTC_DATE_TIME): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(time)
}

fun Calendar.lastMonth(): Long {
    val cal = this.clone() as Calendar
    cal.add(Calendar.MONTH, -1)
    return cal.timeInMillis
}

fun Calendar.addMillis(milli: Long): Calendar {
    val cal = this.clone() as Calendar
    cal.timeInMillis = cal.timeInMillis + milli
    return cal
}

fun Calendar.nextMonth(): Long {
    val cal = this.clone() as Calendar
    cal.add(Calendar.MONTH, 1)
    return cal.timeInMillis
}


fun Int?.orDefault(value: Int = 0) = this ?: value
fun Long?.orDefault(value: Long = 0L) = this ?: value
fun Float?.orDefault(value: Float = 0f) = this ?: value
fun Double?.orDefault(value: Double = 0.0) = this ?: value

fun View.gone() {
    if (visibility != View.GONE)
        visibility = View.GONE
}

fun View.visible() {
    if (visibility != View.VISIBLE)
        visibility = View.VISIBLE
}

fun View.inVisible() {
    if (visibility != View.INVISIBLE)
        visibility = View.INVISIBLE
}

fun RecyclerView.setAdapter(adapter: RecyclerView.Adapter<*>?, emptyView: View?) {
    val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty(emptyView)
            super.onChanged()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty(emptyView)
            super.onItemRangeInserted(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty(emptyView)
            super.onItemRangeRemoved(positionStart, itemCount)
        }
    }

    this.adapter?.unregisterAdapterDataObserver(observer)
    this.adapter = adapter
    if (adapter is BaseAdapter<*>) {
        adapter.setEmptyView(emptyView)
    }
    adapter?.registerAdapterDataObserver(observer)
//    checkIfEmpty(emptyView)
}

fun RecyclerView.checkIfEmpty(emptyView: View?) {
    if (emptyView == null) {
        val emptyViewVisible = adapter?.itemCount == 0
        visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
    }
    if (emptyView != null && adapter != null) {
        val emptyViewVisible = adapter?.itemCount == 0
        emptyView.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
        visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
    }
}