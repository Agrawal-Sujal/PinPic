package com.sunflower.pinpic.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sunflower.pinpic.data.local.entity.Status
import com.sunflower.pinpic.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: SyncOperationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<SyncOperationEntity>)

    /**
     * Fetch pending operations in FIFO order (oldest to newest) for sync execution.
     */
    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingOperationsBatch(limit: Int): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun getOldestPendingOperation(): SyncOperationEntity?

    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingOperationsFlow(): Flow<List<SyncOperationEntity>>

    /**
     * Fetch all operations sorted with newest first for display/monitoring.
     */
    @Query("SELECT * FROM sync_operations ORDER BY createdAt DESC")
    suspend fun getAllOperations(): List<SyncOperationEntity>

    @Query("SELECT * FROM sync_operations ORDER BY createdAt DESC")
    fun getAllOperationsFlow(): Flow<List<SyncOperationEntity>>

    @Query("UPDATE sync_operations SET status = :status WHERE operationId = :operationId")
    suspend fun updateStatus(operationId: String, status: Status)

    @Update
    suspend fun updateOperation(operation: SyncOperationEntity)

    @Delete
    suspend fun deleteOperation(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_operations WHERE status = 'COMPLETED'")
    suspend fun clearCompletedOperations()
}
