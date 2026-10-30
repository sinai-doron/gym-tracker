package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.doron.gymtracker.data.db.entities.RouteEntity
import com.doron.gymtracker.data.db.entities.RoutePointEntity

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY createdAt DESC")
    suspend fun getAll(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun get(id: Long): RouteEntity?

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY seq")
    suspend fun getPoints(routeId: Long): List<RoutePointEntity>

    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Insert
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Transaction
    suspend fun insertWithPoints(route: RouteEntity, points: (Long) -> List<RoutePointEntity>): Long {
        val id = insert(route)
        insertPoints(points(id))
        return id
    }

    @Query("UPDATE routes SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM route_points WHERE routeId = :id")
    suspend fun deletePoints(id: Long)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun deleteRoute(id: Long)

    @Transaction
    suspend fun delete(id: Long) {
        deletePoints(id)
        deleteRoute(id)
    }
}
