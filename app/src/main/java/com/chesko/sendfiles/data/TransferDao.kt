package com.chesko.sendfiles.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(record: TransferRecord): Long

    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<TransferRecord>>

    @Query("UPDATE transfer_history SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransferStatus)

    @Query("DELETE FROM transfer_history")
    suspend fun clearHistory()
}
