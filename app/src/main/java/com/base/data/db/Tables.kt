package com.base.data.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

/**
 * Created by Nikul on 12/5/2020.
 */

@Entity
@Parcelize
data class Test(
    @PrimaryKey
    var idd: String,
    val yo: String?,
    var man: String?
) : Parcelable

