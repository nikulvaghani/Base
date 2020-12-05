package com.base

import android.app.Application
import androidx.lifecycle.LifecycleObserver
import com.base.data.db.MyAppDao
import com.base.data.db.MyAppDb
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.gsonpref.gson
import com.google.gson.Gson
import timber.log.Timber


/**
 * Created by Nikul on 14-08-2020.
 */
class MyApp : Application(), LifecycleObserver {
    private lateinit var mDao: MyAppDao

    companion object {
        private lateinit var mInstance: MyApp

        //@Synchronized
        fun getInstance(): MyApp {
            return mInstance
        }
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        Kotpref.init(this)
        Kotpref.gson = Gson()
        mDao = MyAppDb.getDatabase(applicationContext).getDao()
        if (BuildConfig.DEBUG) Timber.plant(MyDebugTree())
    }

    fun getDao() = mDao

    class MyDebugTree : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String? {
            return "(${element.fileName}:${element.lineNumber}) #${element.methodName}"
        }
    }
}