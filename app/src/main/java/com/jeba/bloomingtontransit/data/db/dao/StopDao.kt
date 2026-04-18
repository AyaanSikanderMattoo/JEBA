package com.jeba.bloomingtontransit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jeba.bloomingtontransit.data.db.entity.StopEntity

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<StopEntity>)

    @Query("SELECT * FROM stops")
    suspend fun getAllStops(): List<StopEntity>

    @Query("SELECT * FROM stops WHERE stopId = :stopId LIMIT 1")
    suspend fun getStopById(stopId: String): StopEntity?

    @Query("SELECT COUNT(*) FROM stops")
    suspend fun count(): Int

    @Query("SELECT stopName FROM stops WHERE stopId = :stopId LIMIT 1")
    suspend fun getStopName(stopId: String): String?

}