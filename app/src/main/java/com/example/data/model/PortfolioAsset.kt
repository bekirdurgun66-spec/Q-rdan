package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_assets")
data class PortfolioAsset(
    @PrimaryKey val symbol: String, // e.g. "BTC", "USD" (for cash represent)
    val name: String,
    val quantity: Double,
    val averageCost: Double
)
