package com.funTrip.fun2go.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SavedSpotDao {

    @Query("SELECT * FROM saved_spots ORDER BY rowid ASC")
    fun getAllSavedSpots(): LiveData<List<SavedSpotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spot: SavedSpotEntity)

    @Query("DELETE FROM saved_spots WHERE id = :id")
    suspend fun deleteById(id: Int)
}
