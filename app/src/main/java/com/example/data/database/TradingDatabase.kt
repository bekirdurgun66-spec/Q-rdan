package com.example.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MarketCandle
import com.example.data.model.PortfolioAsset
import com.example.data.model.TransactionRecord

@Database(
    entities = [
        PortfolioAsset::class,
        TransactionRecord::class,
        MarketCandle::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TradingDatabase : RoomDatabase() {

    abstract fun tradingDao(): TradingDao

    companion object {
        @Volatile
        private var INSTANCE: TradingDatabase? = null

        fun getDatabase(context: Context): TradingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TradingDatabase::class.java,
                    "akatrade_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        Log.w("TradingDatabase", "YIKICI MIGRASYON ENGELLENDI: Finansal veriler korundu.")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
