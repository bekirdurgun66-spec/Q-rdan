package com.example.data.database

import androidx.room.*
import com.example.data.model.MarketCandle
import com.example.data.model.PortfolioAsset
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {

    // --- PORTFOLIO ---
    @Query("SELECT * FROM portfolio_assets")
    fun getAllPortfolioAssetsFlow(): Flow<List<PortfolioAsset>>

    @Query("SELECT * FROM portfolio_assets")
    suspend fun getAllPortfolioAssets(): List<PortfolioAsset>

    @Query("SELECT * FROM portfolio_assets WHERE symbol = :symbol LIMIT 1")
    suspend fun getPortfolioAssetBySymbol(symbol: String): PortfolioAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioAsset(asset: PortfolioAsset)

    @Update
    suspend fun updatePortfolioAsset(asset: PortfolioAsset)

    @Delete
    suspend fun deletePortfolioAsset(asset: PortfolioAsset)

    @Query("DELETE FROM portfolio_assets")
    suspend fun clearPortfolio()

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(record: TransactionRecord)

    @Query("DELETE FROM transaction_records")
    suspend fun clearTransactions()

    // --- TIME-SERIES CANDLES ---
    @Query("SELECT * FROM market_candles WHERE symbol = :symbol ORDER BY timestamp ASC")
    fun getCandlesForSymbolFlow(symbol: String): Flow<List<MarketCandle>>

    @Query("SELECT * FROM market_candles WHERE symbol = :symbol ORDER BY timestamp ASC")
    suspend fun getCandlesForSymbol(symbol: String): List<MarketCandle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<MarketCandle>)

    @Query("DELETE FROM market_candles WHERE symbol = :symbol")
    suspend fun clearCandlesForSymbol(symbol: String)
}
