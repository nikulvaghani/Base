//package com.base.data.db
//
//import androidx.room.TypeConverter
//import app.wakeupp.data.db.RepliedItem
//import com.google.gson.Gson
//
///**
// * Created by Nikul on 14-08-2020.
// */
//class Converters {
//
//    @TypeConverter
//    fun fromJsonToRepliedItem(jsonString: String?): RepliedItem? =
//        jsonString?.let { Gson().fromJson(jsonString, RepliedItem::class.java) }
//
//    @TypeConverter
//    fun repliedItemToJson(repliedItem: RepliedItem?): String? =
//        repliedItem?.let { Gson().toJson(repliedItem) }
//}