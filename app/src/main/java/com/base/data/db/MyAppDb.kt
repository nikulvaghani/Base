package com.base.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.base.utils.DATABASE_NAME

/**
 * Created by Nikul on 14-08-2020.
 */

@Database(
    entities = [Test::class],
    version = 1,
    exportSchema = false
)
//@TypeConverters(Converters::class)
abstract class MyAppDb : RoomDatabase() {

    abstract fun getDao(): MyAppDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: MyAppDb? = null

        fun getDatabase(context: Context): MyAppDb {
            val tempInstance = INSTANCE

            if (tempInstance != null) return tempInstance

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, MyAppDb::class.java,
                    DATABASE_NAME
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
//                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                return instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE CryptKey ADD COLUMN serverStanzaId TEXT")
            }
        }
    }

}