package com.sunflower.pinpic.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey
    val operationId: String = UUID.randomUUID().toString(),
    val entityType: EntityType,
    val entityId: String,
    val operation: Operations,
    val status: Status = Status.PENDING,
    val createdAt: String
)
