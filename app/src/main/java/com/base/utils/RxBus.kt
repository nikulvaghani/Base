package com.base.utils

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created by Nikul on 14-08-2020.
 */
object RxBus {

    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}
