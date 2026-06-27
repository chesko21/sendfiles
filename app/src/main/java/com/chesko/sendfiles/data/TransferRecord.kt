package com.chesko.sendfiles.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileSize: Long,
    val senderName: String,
    val receiverName: String,
    val status: TransferStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: TransferDirection
)

enum class TransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class TransferDirection {
    SEND,
    RECEIVE
}
