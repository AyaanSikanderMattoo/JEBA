package com.jeba.bloomingtontransit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeba.bloomingtontransit.data.db.dao.RouteDao
import com.jeba.bloomingtontransit.data.db.dao.ShapePointDao
import com.jeba.bloomingtontransit.data.db.dao.StopDao
import com.jeba.bloomingtontransit.data.db.dao.TripDao
import com.jeba.bloomingtontransit.data.db.entity.RouteEntity
import com.jeba.bloomingtontransit.data.db.entity.ShapePointEntity
import com.jeba.bloomingtontransit.data.db.entity.StopEntity
import com.jeba.bloomingtontransit.data.db.entity.TripEntity

@Database(
    entities = [StopEntity::class, RouteEntity::class, ShapePointEntity::class, TripEntity::class],
    version = 2,
    exportSchema = false
)
abstract class TransitDatabase : RoomDatabase() {

    abstract fun stopDao(): StopDao
    abstract fun routeDao(): RouteDao
    abstract fun shapePointDao(): ShapePointDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: TransitDatabase? = null

        fun getInstance(context: Context): TransitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransitDatabase::class.java,
                    "transit_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}