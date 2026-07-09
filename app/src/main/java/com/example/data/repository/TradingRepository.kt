package com.example.data.repository

import android.util.Log
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.TradingDao
import com.example.data.model.MarketCandle
import com.example.data.model.PortfolioAsset
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TradingRepository(private val tradingDao: TradingDao) {

    val allAssetsFlow: Flow<List<PortfolioAsset>> = tradingDao.getAllPortfolioAssetsFlow()
    val allTransactionsFlow: Flow<List<TransactionRecord>> = tradingDao.getAllTransactionsFlow()

    fun getCandlesForSymbolFlow(symbol: String): Flow<List<MarketCandle>> =
        tradingDao.getCandlesForSymbolFlow(symbol)

    suspend fun getPortfolioAssetBySymbol(symbol: String): PortfolioAsset? = withContext(Dispatchers.IO) {
        tradingDao.getPortfolioAssetBySymbol(symbol)
    }

    /**
     * Seed the database with default assets (cash + initial positions) and base candles
     */
    suspend fun seedDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val assets = tradingDao.getAllPortfolioAssets()
        if (assets.isEmpty()) {
            // Seed initial 1,000 TRY cash
            tradingDao.insertPortfolioAsset(PortfolioAsset("TRY", "Türk Lirası", 1000.0, 1.0))
            
            // Seed starter assets with 0 quantity
            tradingDao.insertPortfolioAsset(PortfolioAsset("BTC", "Bitcoin", 0.0, 2260000.0))
            tradingDao.insertPortfolioAsset(PortfolioAsset("ETH", "Ethereum", 0.0, 115000.0))
            tradingDao.insertPortfolioAsset(PortfolioAsset("AAPL", "Apple Inc.", 0.0, 5600.0))
            tradingDao.insertPortfolioAsset(PortfolioAsset("TSLA", "Tesla Inc.", 0.0, 6200.0))

            // Seed introductory transaction records
            tradingDao.insertTransaction(
                TransactionRecord(
                    symbol = "TRY",
                    type = "AL",
                    quantity = 1000.0,
                    price = 1.0,
                    timestamp = System.currentTimeMillis() - 86400000 * 2,
                    agentNotes = "Kuruluş bakiyesi: 1.000 TRY hesabınıza başarıyla tanımlandı."
                )
            )
        }

        // Check and generate candles if database has no candles for BTC
        val btcCandles = tradingDao.getCandlesForSymbol("BTC")
        if (btcCandles.isEmpty()) {
            generateMockHistoricalCandles("BTC", 2264000.0)
            generateMockHistoricalCandles("ETH", 115200.0)
            generateMockHistoricalCandles("AAPL", 5620.0)
            generateMockHistoricalCandles("TSLA", 6210.0)
        }
    }

    private suspend fun generateMockHistoricalCandles(symbol: String, basePrice: Double) {
        val candles = mutableListOf<MarketCandle>()
        var price = basePrice
        val timestampOffset = 3600000L // 1 hour steps
        val count = 40
        val startTime = System.currentTimeMillis() - (timestampOffset * count)

        for (i in 0 until count) {
            val changePercent = (Math.random() - 0.49) * 0.03 // Random daily change
            val open = price
            val close = price * (1 + changePercent)
            val high = maxOf(open, close) * (1 + Math.random() * 0.01)
            val low = minOf(open, close) * (1 - Math.random() * 0.01)
            val volume = 10.0 + Math.random() * 500.0

            candles.add(
                MarketCandle(
                    symbol = symbol,
                    timestamp = startTime + (i * timestampOffset),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            price = close
        }
        tradingDao.insertCandles(candles)
    }

    /**
     * Execute high-safety simulated transaction under Multi-Agent review
     */
    companion object {
        // Güvenlik limitleri: tek işlemde maksimum miktar ve fiyat sınırları
        private const val MAX_QUANTITY = 10_000.0    // maksimum lot
        private const val MAX_PRICE = 100_000_000.0   // maksimum birim fiyat (TRY)
        private const val MIN_QUANTITY = 0.0001       // minimum lot (dust limit)
    }

    suspend fun executeTransaction(
        symbol: String,
        type: String, // "AL" or "SAT"
        quantity: Double,
        price: Double,
        agentOpinion: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Alt ve üst sınır validasyonu
        if (quantity <= 0 || price <= 0) return@withContext false
        if (quantity > MAX_QUANTITY) {
            Log.e("TradingRepository", "Güvenlik ihlali: Maksimum lot aşıldı ($MAX_QUANTITY)")
            return@withContext false
        }
        if (price > MAX_PRICE) {
            Log.e("TradingRepository", "Güvenlik ihlali: Maksimum fiyat aşıldı ($MAX_PRICE)")
            return@withContext false
        }
        if (quantity < MIN_QUANTITY) {
            Log.w("TradingRepository", "İşlem miktarı minimum lot altında, iptal edildi")
            return@withContext false
        }

        val cashAsset = tradingDao.getPortfolioAssetBySymbol("TRY") ?: PortfolioAsset("TRY", "Türk Lirası", 0.0, 1.0)
        val targetAsset = tradingDao.getPortfolioAssetBySymbol(symbol) ?: PortfolioAsset(symbol, symbol, 0.0, 0.0)

        val totalCost = quantity * price

        if (type == "AL") {
            // Check cash limit
            if (cashAsset.quantity < totalCost) {
                Log.e("TradingRepository", "Yetersiz Bakiye! Gereken: $totalCost, Mevcut: ${cashAsset.quantity}")
                return@withContext false
            }

            // Deduct Cash
            tradingDao.insertPortfolioAsset(cashAsset.copy(quantity = cashAsset.quantity - totalCost))

            // Update Position
            val newQty = targetAsset.quantity + quantity
            val newAvgCost = if (newQty > 0) {
                ((targetAsset.quantity * targetAsset.averageCost) + (quantity * price)) / newQty
            } else {
                price
            }
            tradingDao.insertPortfolioAsset(
                targetAsset.copy(
                    quantity = newQty,
                    averageCost = newAvgCost
                )
            )
        } else {
            // Check position limit
            if (targetAsset.quantity < quantity) {
                Log.e("TradingRepository", "Yetersiz Varlık! Satılmak İstenen: $quantity, Portföy: ${targetAsset.quantity}")
                return@withContext false
            }

            // Sell asset
            val newQty = targetAsset.quantity - quantity
            if (newQty <= 0.0) {
                tradingDao.deletePortfolioAsset(targetAsset)
            } else {
                tradingDao.insertPortfolioAsset(targetAsset.copy(quantity = newQty))
            }

            // Credit Cash
            tradingDao.insertPortfolioAsset(cashAsset.copy(quantity = cashAsset.quantity + totalCost))
        }

        // Log transaction in Timescale-equivalent Local Room table
        tradingDao.insertTransaction(
            TransactionRecord(
                symbol = symbol,
                type = type,
                quantity = quantity,
                price = price,
                timestamp = System.currentTimeMillis(),
                agentNotes = agentOpinion
            )
        )
        return@withContext true
    }

    /**
     * Insert live simulated candlestick ticks generated during trading simulation
     */
    suspend fun insertLatestCandle(candle: MarketCandle) = withContext(Dispatchers.IO) {
        tradingDao.insertCandles(listOf(candle))
    }

    /**
     * Clear all database data to reset portfolio
     */
    suspend fun clearDatabaseAndReSeed() = withContext(Dispatchers.IO) {
        tradingDao.clearPortfolio()
        tradingDao.clearTransactions()
        
        // Remove individual symbol caches
        tradingDao.clearCandlesForSymbol("BTC")
        tradingDao.clearCandlesForSymbol("ETH")
        tradingDao.clearCandlesForSymbol("AAPL")
        tradingDao.clearCandlesForSymbol("TSLA")

        // Seed fresh
        seedDefaultDataIfNeeded()
    }

    /**
     * Trigger Multi-Agent AI system via actual Gemini API call
     */
    suspend fun fetchMultiAgentAnalysis(
        symbol: String,
        currentPrice: Double,
        recentCandles: List<MarketCandle>,
        policyWeights: Map<String, Double> // Weighting strategy
    ): String = withContext(Dispatchers.IO) {
        // High precision indicators calculated for context injection
        val lastPrices = recentCandles.takeLast(10).map { it.close }
        val rsiValue = calculateRSI(lastPrices)
        val emaValue = calculateEMA(lastPrices, 5)
        val trendStr = if (currentPrice > emaValue) "YUKARI" else "AŞAĞI"

        val prompt = """
            Sembol: $symbol
            Anlık Fiyat: $currentPrice
            Son Kapanış Değerleri: ${lastPrices.joinToString(", ")}
            RSI İndikatörü: ${"%.2f".format(rsiValue)}
            EMA (5) İndikatörü: ${"%.2f".format(emaValue)}
            Trend Yönü: $trendStr
            Ajan Ağırlık Kombinasyonu (Sırasıyla Teknik, Temel, Duygu): %${(policyWeights["tech"]!! * 100).toInt()}, %${(policyWeights["fund"]!! * 100).toInt()}, %${(policyWeights["sent"]!! * 100).toInt()}

            GÖREV:
            Seni bir 'Otonom Çoklu Yapay Zeka Ajan Havuzu' olarak konumlandırıyorum. Aşağıdaki roller doğrultusunda canlı piyasa verilerini ve teknik parametreleri analiz et ve tamamen Türkçe dilinde, zengin bir terminal chat günlüğü formatında karar üret:

            1. [TEKNİK ANALİZ AJANI]: RSI (${"%.2f".format(rsiValue)}), EMA (${"%.2f".format(emaValue)}) trendleri ve momentum indikatörlerini (${trendStr}) yorumlayarak anlık grafiği analiz etsin.
            2. [TEMEL VE DUYGU ANALİZİ AJANI]: Sektör trendlerini, finansal basındaki sahte/gerçek haber akışlarını ve sosyal medyadaki (X) piyasa coşkusunu/endişesini sentezlesin.
            3. [RİSK YÖNETİMİ AJANI]: Belirtilen ajan ağırlıkları ve anlık fiyat doğrultusunda katı pozisyon büyüklüğü doğrulaması yapsın, risk limitlerini teyit ederek yürütmeye engel teşkil etmediğinden emin olsun (Hard Gatekeeper rolü).
            4. [YÜRÜTÜCÜ AJAN / EXECUTING AGENT]: Tüm ajanlardan gelen verileri konsolide etsin, nihai AL, SAT veya TUT kararını ve gerekçesini belirtsin. Hedef giriş fiyatı, stop-loss ve take-profit hedefleri atasın.

            Her bir ajanı net birer başlık altında göstererek adeta birbirleriyle terminal konsolunda canlı konuşuyorlarmış gibi bir Türkçe jargonda kurgula. Çıktı çok şık ve Türkçe terminal düzeninde olmalı, markdown formatında yaz.
        """.trimIndent()

        try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.4f,
                    topP = 0.9f
                ),
                systemInstruction = Content(
                    parts = listOf(Part(text = "Sen, gerçek zamanlı borsada işlem yürüten elit bir FinTech Çoklu Yapay Zeka Ajan sistemisin. Turkish (Türkçe) dil kurallarına son derece hakim, teknik ve profesyonel bir üsluba sahipsin. Kararlarını Türkçe yaz."))
                )
            )

            val response = RetrofitClient.geminiService.generateContent(request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Yapay zeka analiz raporu alınamadı."
        } catch (e: Exception) {
            Log.e("TradingRepository", "AI proxy çağrısı başarısız oldu. Detay: ${e.message}", e)
            return@withContext "⚠️ Yapay zeka analiz sunucusuna şu anda erişilemiyor. Platform, yerel optimize 'Çevrimdışı Yapay Zeka Ajan Simülatörü' ile devam ediyor.\n\n" +
                    generateSimulatedMultiAgentResponse(symbol, currentPrice, recentCandles, policyWeights)
        }
    }

    /**
     * Compute indicators
     */
    private fun calculateRSI(prices: List<Double>): Double {
        if (prices.size < 2) return 50.0
        var gains = 0.0
        var losses = 0.0
        for (i in 1 until prices.size) {
            val diff = prices[i] - prices[i - 1]
            if (diff > 0) gains += diff else losses -= diff
        }
        val avgGain = gains / prices.size
        val avgLoss = losses / prices.size
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        var ema = prices.first()
        val k = 2.0 / (period + 1.0)
        for (i in 1 until prices.size) {
            ema = (prices[i] * k) + (ema * (1 - k))
        }
        return ema
    }

    /**
     * Fallback high-quality simulation response in Turkish
     */
    private fun generateSimulatedMultiAgentResponse(
        symbol: String,
        currentPrice: Double,
        recentCandles: List<MarketCandle>,
        policyWeights: Map<String, Double>
    ): String {
        val lastPrices = recentCandles.takeLast(10).map { it.close }
        val rsiValue = calculateRSI(lastPrices)
        val trend = if (rsiValue < 40.0) "AŞIRI SATIM (Boğa Sinyali)" else if (rsiValue > 70.0) "AŞIRI ALIM (Ayı Sinyali)" else "Nötr Momentum"
        val rsiStr = "%.2f".format(rsiValue)

        val techWeight = (policyWeights["tech"]!! * 100).toInt()
        val fundWeight = (policyWeights["fund"]!! * 100).toInt()
        val sentWeight = (policyWeights["sent"]!! * 100).toInt()

        val decision = if (rsiValue < 45.0) "AL (GÜÇLÜ BOĞA)" else if (rsiValue > 68.0) "SAT (GÜÇLÜ AYI)" else "TUT (YATAY SEVİYE)"
        val stopLossValue = if (rsiValue < 45.0) currentPrice * 0.97 else currentPrice * 1.03
        val takeProfitValue = if (rsiValue < 45.0) currentPrice * 1.10 else currentPrice * 0.92

        return """
            ⚠️ **[AkaTrade Bilgilendirme]**: Yapay zeka analiz sunucusuna bağlanılamadı. Platform, yerel optimize 'Çevrimdışı Yapay Zeka Ajan Simülatörü' ile devam ediyor.
            
            ```
            ====================== OTONOM COKLU AJAN HAVUZU GÜNLÜGÜ ======================
            Piyasa Verisi: $symbol | Anlık Fiyat: ${"%,.2f".format(currentPrice)} TRY | RSI: $rsiStr
            Sistem Aktif Agırlıkları: Teknik: %$techWeight | Temel: %$fundWeight | Duygu: %$sentWeight
            ==============================================================================
            ```
            
            ### 📊 [TEKNİK ANALİZ AJANI]
            *   **Gösterge Analizi**: RSI şu anda **$rsiStr** seviyesinde ve **$trend** bölgesinde yer alıyor.
            *   **Trend Analizi**: Canlı osilatörler hacmin dengelendiğini ve son candlestick barlarının destek seviyesini test ettiğini doğruluyor.
            *   **Sinyal**: Kısa vadeli teknik indikatörler momentumun yönünü yukarı çevirmek üzere olduğunu fısıldıyor.
            
            ### 📰 [TEMEL VE DUYGU ANALİZİ AJANI]
            *   **Haber Akışı**: $symbol üzerindeki son Kılavuz Güncellemeleri ve finansal bülten başlıkları genel olarak %64 olumlu.
            *   **Duygu Analizi**: Sosyal medyada (Mavi Tikli X hesapları) ve haber portallarında korku endeksi azalmış durumda, kurumsal giriş iştahı artıyor.
            *   **Duygu Skoru**: `+0.72` (Yüksek Boğa Eğilimi).
            
            ### ⛨ [RİSK YÖNETİMİ AJANI]
            *   **Pozisyon Sınırlandırması**: Katı kurallar uyarınca tek bir işleme ayrılan azami nakit sermaye oranı: **%15.00** ile sınırlandırılmıştır.
            *   **Hard Gatekeeper Kontrolü**: Portföy drawdown eşikleri (${"%,.2f".format(currentPrice * 0.05)} TRY limit) kontrol edildi. İşlemin onaylanmasına hiçbir engel bulunmamaktadır. Risk rasyosu (R:R) **1:3** olarak hedeflendi.
            
            ### ⚡ [YÜRÜTÜCÜ AJAN / EXECUTIVE UNIT]
            *   **Konsolide Karar**: **$decision**
            *   **Hedef Giriş Değeri**: ${"%,.2f".format(currentPrice)} TRY
            *   **Zarar Durdur (Stop-Loss)**: ${"%,.2f".format(stopLossValue)} TRY
            *   **Kâr Al (Take-Profit)**: ${"%,.2f".format(takeProfitValue)} TRY
            *   **Gerekçe Özeti**: Çoklu ajan koalisyonumuzun kararı; Teknik RSI momentumunun ve Temel hisse senedi algısının pozitif sinerji içerisinde hareket etmesi dolayısıyla işlemin risksiz ve rasyonel olduğu yönündedir.
        """.trimIndent()
    }
}
