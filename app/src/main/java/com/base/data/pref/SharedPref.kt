package com.base.data.pref

import com.base.data.db.Test
import com.chibatching.kotpref.KotprefModel
import com.chibatching.kotpref.gsonpref.gsonNullablePref

/**
 * Created by Nikul on 14-08-2020.
 */
object SharedPref : KotprefModel() {
    var obj by gsonNullablePref<Test>()
    var authToken by stringPref()
    var stringList by gsonNullablePref<ArrayList<String>>()

    fun clearPref() {
        clear()
    }
}