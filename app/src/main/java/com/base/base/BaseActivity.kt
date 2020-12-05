package com.base.base

import android.os.Bundle
import android.os.Handler
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import app.wakeupp.utils.isNetworkConnected
import app.wakeupp.utils.plusAssign
import com.base.MyApp
import com.base.R
import com.base.data.network.RequestInterface
import com.base.data.network.ResponseCode.*
import com.base.data.pref.SharedPref
import com.base.widget.AppLoader
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton
import org.jetbrains.anko.toast
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Created by Nikul on 14-08-2020.
 */
abstract class BaseActivity(@LayoutRes contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {

    protected val TAG: String = this.javaClass.simpleName

    val mDao by lazy { MyApp.getInstance().getDao() }
    val mHandler by lazy { Handler() }
    val requestInterface by lazy { RequestInterface.getInstance() }
    val mGson by lazy { Gson() }

    protected var mCompositeDisposable = CompositeDisposable()
    private var noOfApiCall = 0

    lateinit var loader: AppLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loader = AppLoader(this)
    }

    override fun onDestroy() {
        mCompositeDisposable.clear()
        super.onDestroy()
    }

    protected fun <T> callApi(
        observable: Observable<T>,
        showLoader: Boolean = true,
        hideOnSuccess: Boolean = showLoader,
        hideOnFail: Boolean = showLoader,
        responseBlock: (T) -> Unit
    ) {
        if (!isNetworkConnected()) {
            if (mCompositeDisposable.size() == 0)
                toast(getString(R.string.msg_no_internet))
            return
        }

        if (showLoader) {
            noOfApiCall++
            Timber.d("noOfApiCall $noOfApiCall")
            if (noOfApiCall == 1) showLoader()
        }

        mCompositeDisposable += observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                onResponse(hideOnSuccess)
                responseBlock(it)
            }, { onResponseFailure(it, hideOnFail) })
    }

    private fun onResponseFailure(throwable: Throwable, hideOnFail: Boolean) {
        onResponse(hideOnFail)

        Timber.e("onResponseFailure ${throwable.message}")
        when (throwable) {
            is HttpException -> {
                Timber.e("response code ${throwable.code()}")
                handleResponseError(throwable)
            }
            is ConnectException -> {
                toast(getString(R.string.msg_no_internet))
            }
            is SocketTimeoutException -> {
                toast(getString(R.string.time_out))
            }
        }
    }

    private fun handleResponseError(throwable: HttpException) {
        when (throwable.code()) {
            InValidateData.code -> {
                val errorRawData = throwable.response()?.errorBody()?.string()?.trim()
                if (!errorRawData.isNullOrEmpty()) {
                    val jsonObject = JSONObject(errorRawData)
                    val jObject = jsonObject.optJSONObject("errors")
                    if (jObject != null) {
                        val keys: Iterator<String> = jObject.keys()
                        if (keys.hasNext()) {
                            val msg = StringBuilder()
                            while (keys.hasNext()) {
                                val key: String = keys.next()
                                if (jObject.get(key) is String) {
                                    msg.append("- ${jObject.get(key)}\n")
                                }
                            }
                            errorDialog(msg.toString(), "Alert")
                        } else {
                            errorDialog(jsonObject.optString("message", ""))
                        }
                    } else {
                        errorDialog(JSONObject(errorRawData).optString("message"), "Alert")
                    }
                }
            }
            Unauthenticated.code -> {
                val errorRawData = throwable.response()?.errorBody()?.string()?.trim()
                if (!errorRawData.isNullOrEmpty()) {
                    alert(
                        errorRawData,
                        getString(R.string.alert)
                    ) { okButton { onAuthFail() } }.show()
                } else {
                    onAuthFail()
                }
            }
            ForceUpdate.code -> {

            }
            ServerError.code -> errorDialog("Internal Server error")
            BadRequest.code,
            Unauthorized.code,
            NotFound.code,
            RequestTimeOut.code,
            Conflict.code,
            Locked.code -> {
                val errorRawData = throwable.response()?.errorBody()?.string()?.trim()
                if (!errorRawData.isNullOrEmpty()) {
                    errorDialog(JSONObject(errorRawData).optString("message", ""))
                }
            }
        }
    }

    private fun onResponse(hideLoader: Boolean) {
        noOfApiCall--
        if (noOfApiCall <= 0) {
            noOfApiCall = 0
            if (hideLoader)
                hideLoader()
        }
    }

    private fun showLoader() {
        loader.run()
    }

    private fun hideLoader() {
        loader.dismiss()
    }

    private fun errorDialog(optString: String?, title: String = getString(R.string.app_name)) {
        optString?.let {
            alert(it, title) { okButton { } }.show()
        }
    }

    private fun onAuthFail() {
        SharedPref.clearPref()
//        startActivity(intentFor<SplashActivity>().clearTask().newTask())
    }

    fun gotoHomeScreen() {  //method created here becase may be we need to other scenario for goto home screen
        moveTaskToBack(true)
    }
}