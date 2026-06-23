package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val type: String, // "AL" (Buy) or "SAT" (Sell)
    val quantity: Double,
    val price: Double,
    val timestamp: Long,
    val agentNotes: String // Turkish notes of risk validation/decisions
)
