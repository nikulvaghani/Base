package com.base.widget

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.base.R

/**
 * Created by Nikul on 14-08-2020.
 */
class AppLoader(private val activity: Activity?) : Thread() {

    private var dialog: Dialog? = Dialog(activity!!, android.R.style.Theme_Light_NoTitleBar)
    var view: View? = null

    init {
        dialog?.run {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.run {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(MATCH_PARENT, MATCH_PARENT)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = ContextCompat.getColor(activity!!, android.R.color.transparent)

                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }

        view = activity?.layoutInflater?.inflate(R.layout.dialog_app_loader, null)

        view?.let { dialog?.setContentView(it) }

        setCanceledOnTouchOutside(false)
        setCancelable(true)
    }

    override fun run() {
        super.run()
        try {
            activity?.run {
                runOnUiThread {
                    if (!isFinishing) {
                        dialog?.show()
                    }
                }
            }
        } catch (e: WindowManager.BadTokenException) {
        }
    }

    fun dismiss() {
        dialog?.dismiss()
        this.interrupt()
    }

    fun setCancelable(flag: Boolean) {
        dialog?.setCancelable(flag)
    }

    fun setCanceledOnTouchOutside(flag: Boolean) {
        dialog?.setCanceledOnTouchOutside(flag)
    }
}