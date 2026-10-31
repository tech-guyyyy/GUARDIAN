package com.guardian.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.guardian.app.core.model.BlockedDomain

@Database(entities = [BlockedDomain::class], version = 1, exportSchema = false)
abstract class GuardianDatabase : RoomDatabase() {

    abstract fun blockedDomainDao(): BlockedDomainDao

    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        fun getDatabase(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_protection.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
