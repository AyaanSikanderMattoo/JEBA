package com.jeba.bloomingtontransit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jeba.bloomingtontransit.data.db.entity.RouteEntity

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Query("SELECT * FROM routes")
    suspend fun getAllRoutes(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE routeId = :routeId LIMIT 1")
    suspend fun getRouteById(routeId: String): RouteEntity?

    @Query("SELECT COUNT(*) FROM routes")
    suspend fun count(): Int
}