package com.base.base

import android.content.Context
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import app.wakeupp.utils.isNetworkConnected
import app.wakeupp.utils.plusAssign
import com.base.MyApp
import com.base.R
import com.base.data.network.RequestInterface
import com.base.data.network.ResponseCode
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
 * Created by Nikul on 08-05-2020.
 */
abstract class BaseFragment(@LayoutRes contentLayoutId: Int) : Fragment(contentLayoutId) {

    protected val TAG: String = this.javaClass.simpleName

    val mDao by lazy { MyApp.getInstance().getDao() }
    val requestInterface by lazy { RequestInterface.getInstance() }
    val mGson by lazy { Gson() }

    protected var mCompositeDisposable = CompositeDisposable()
    private var noOfApiCall = 0
    private lateinit var loader: AppLoader

    private var mActivity: BaseActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loader = AppLoader(activity)

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
                requireContext().toast(getString(R.string.msg_no_internet))
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
                requireContext().toast(getString(R.string.msg_no_internet))
            }
            is SocketTimeoutException -> {
                requireContext().toast(getString(R.string.time_out))
            }
        }
    }

    private fun handleResponseError(throwable: HttpException) {
        when (throwable.code()) {
            ResponseCode.InValidateData.code -> {
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
            ResponseCode.Unauthenticated.code -> {
                val errorRawData = throwable.response()?.errorBody()?.string()?.trim()
                if (!errorRawData.isNullOrEmpty()) {
                    context?.alert(
                        errorRawData,
                        getString(R.string.alert)
                    ) { okButton { onAuthFail() } }?.show()
                } else {
                    onAuthFail()
                }
            }
            ResponseCode.ForceUpdate.code -> {

            }
            ResponseCode.ServerError.code -> errorDialog("Internal Server error")
            ResponseCode.BadRequest.code,
            ResponseCode.Unauthorized.code,
            ResponseCode.NotFound.code,
            ResponseCode.RequestTimeOut.code,
            ResponseCode.Conflict.code,
            ResponseCode.Locked.code -> {
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

    private fun showLoader() = loader.run()

    private fun hideLoader() = loader.dismiss()

    private fun errorDialog(optString: String?, title: String = getString(R.string.app_name)) {
        optString?.let {
            context?.alert(it, title) { okButton { } }?.show()
        }
    }

    private fun onAuthFail() {
        SharedPref.clearPref()
//        requireContext().startActivity(
//            requireContext().intentFor<SplashActivity>().clearTask().newTask()
//        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity) {
            val activity = context as BaseActivity?
            this.mActivity = activity
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (context is BaseActivity) {
            hideLoader()
            val activity = context as BaseActivity?
            this.mActivity = activity
        }
    }
}