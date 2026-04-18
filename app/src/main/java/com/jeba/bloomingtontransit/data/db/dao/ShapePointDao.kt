package com.jeba.bloomingtontransit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jeba.bloomingtontransit.data.db.entity.ShapePointEntity

@Dao
interface ShapePointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<ShapePointEntity>)

    @Query("SELECT * FROM shape_points WHERE shapeId = :shapeId ORDER BY sequence ASC")
    suspend fun getPointsForShape(shapeId: String): List<ShapePointEntity>

    @Query("SELECT COUNT(*) FROM shape_points")
    suspend fun count(): Int
}