package com.base.base

import android.content.Context
import app.wakeupp.utils.isNetworkConnected
import app.wakeupp.utils.plusAssign
import com.base.R
import com.base.data.network.RequestInterface
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.toast

/**
 * Created by Nikul on 5/5/2020.
 */

object BaseService {
    private var mCompositeDisposable = CompositeDisposable()
    val requestInterface by lazy { RequestInterface.getInstance() }

    fun <T> callApi(
        context: Context,
        observable: Observable<T>,
        responseBlock: (T) -> Unit
    ) {
        if (!context.isNetworkConnected()) {
            if (mCompositeDisposable.size() == 0)
                context.toast(context.getString(R.string.msg_no_internet))
            return
        }

        mCompositeDisposable += observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                responseBlock(it)
            }
    }
}