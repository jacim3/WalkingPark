package com.example.walkingpark.database.room

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ParkDao {
    @Query("SELECT * FROM ParkDB")
    suspend fun getAll(): List<ParkDB>

    @Query("SELECT * FROM ParkDB WHERE pk = 1")
    suspend fun checkQuery(): List<ParkDB>

}

@Dao
interface GridDao {
    @Query("SELECT * FROM GridDB")
    suspend fun getAll(): List<GridDB>
}
