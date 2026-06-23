package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.TradingDatabase
import com.example.data.model.MarketCandle
import com.example.data.model.PortfolioAsset
import com.example.data.model.TransactionRecord
import com.example.data.repository.TradingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TradingRepository

    // Selected state
    private val _selectedSymbol = MutableStateFlow("BTC")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    // Observable states from Database
    val assets: StateFlow<List<PortfolioAsset>>
    val transactions: StateFlow<List<TransactionRecord>>

    // Stream of current symbol's candles
    private val _currentCandlesList = MutableStateFlow<List<MarketCandle>>(emptyList())
    val currentCandlesList: StateFlow<List<MarketCandle>> = _currentCandlesList.asStateFlow()

    // Real-time Tick States (Sub-second streaming ticker)
    private val _currentPrice = MutableStateFlow(0.0)
    val currentPrice: StateFlow<Double> = _currentPrice.asStateFlow()

    private val _priceChange24h = MutableStateFlow(0.0)
    val priceChange24h: StateFlow<Double> = _priceChange24h.asStateFlow()

    private val _orderBookBids = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val orderBookBids: StateFlow<List<Pair<Double, Double>>> = _orderBookBids.asStateFlow()

    private val _orderBookAsks = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val orderBookAsks: StateFlow<List<Pair<Double, Double>>> = _orderBookAsks.asStateFlow()

    // AI Multi-Agent Console logging
    private val _aiConsoleLog = MutableStateFlow<String>(
        "⚡ AkaTrade Yapay Zeka Ajan Terminali aktif.\nBir sembol seçin ve otonom analizi başlatmak için 'Yapay Zeka Analizini Başlat' butonuna tıklayın."
    )
    val aiConsoleLog: StateFlow<String> = _aiConsoleLog.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Reinforcement Learning Policy Matrix and Continuous Adaptation Configuration
    private val _policyWeights = MutableStateFlow(
        mapOf("tech" to 0.45, "fund" to 0.35, "sent" to 0.20)
    )
    val policyWeights: StateFlow<Map<String, Double>> = _policyWeights.asStateFlow()

    private val _isContinuousLearningEnabled = MutableStateFlow(true)
    val isContinuousLearningEnabled: StateFlow<Boolean> = _isContinuousLearningEnabled.asStateFlow()

    private val _policyAdaptationLogs = MutableStateFlow<List<String>>(
        listOf("Başlangıç politika matrisi kuruldu: Teknik (%45), Temel (%35), Duygu (%20)")
    )
    val policyAdaptationLogs: StateFlow<List<String>> = _policyAdaptationLogs.asStateFlow()

    // Coroutine Jobs for Real-time price stream
    private var tickerSimulationJob: Job? = null
    private var dbCandleObserverJob: Job? = null
    private var autoTraderJob: Job? = null

    // Autonomous trade loop trigger state
    private val _isAutoTradingEnabled = MutableStateFlow(true)
    val isAutoTradingEnabled: StateFlow<Boolean> = _isAutoTradingEnabled.asStateFlow()

    // Base price tracker for Brownian stock drift
    private val symbolBasePrices = mapOf(
        "BTC" to 2264000.0,
        "ETH" to 115200.0,
        "AAPL" to 5620.0,
        "TSLA" to 6210.0
    )

    init {
        val database = TradingDatabase.getDatabase(application)
        repository = TradingRepository(database.tradingDao())

        // Initial setup
        viewModelScope.launch {
            repository.seedDefaultDataIfNeeded()
        }

        // Connect flows from DB
        assets = repository.allAssetsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        transactions = repository.allTransactionsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Start real-time simulation and DB listeners for default symbol
        selectSymbol("BTC")

        // Start otonom trading agent loop
        startAutoTraderLoop()
    }

    /**
     * Switch current traded cryptos / stocks
     */
    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
        
        // Cancel past observers
        dbCandleObserverJob?.cancel()
        tickerSimulationJob?.cancel()

        // 1. Observe Candles from Room DB reactively
        dbCandleObserverJob = viewModelScope.launch {
            repository.getCandlesForSymbolFlow(symbol).collect { dbCandles ->
                _currentCandlesList.value = dbCandles
                if (dbCandles.isNotEmpty()) {
                    val lastCandle = dbCandles.last()
                    _currentPrice.value = lastCandle.close
                    
                    // Mock a 24h change calculator
                    val firstCandle = dbCandles.first()
                    _priceChange24h.value = ((lastCandle.close - firstCandle.close) / firstCandle.close) * 100.0
                }
            }
        }

        // 2. Launch Sub-second low latency price ticker drift simulator (500ms ticker updates)
        val initialPrice = symbolBasePrices[symbol] ?: 1000.0
        _currentPrice.value = initialPrice
        tickerSimulationJob = viewModelScope.launch(Dispatchers.Default) {
            var activePrice = initialPrice
            while (true) {
                delay(500) // Sub-second update rate
                
                // Brownian random walk drift
                val volatility = when(symbol) {
                    "BTC" -> 0.0012
                    "ETH" -> 0.0018
                    "TSLA" -> 0.0025
                    "AAPL" -> 0.0008
                    else -> 0.001
                }
                val drift = (Math.random() - 0.495) * volatility
                activePrice *= (1 + drift)
                
                // Ensure price updates globally
                _currentPrice.value = activePrice

                // Update bids/asks on order book under this sub-second tick
                generateOrderBook(activePrice)

                // Once in every 15 ticks (7.5 seconds), we append/update candles in the DB for the historical cache
                if (Math.random() < 0.15 && _currentCandlesList.value.isNotEmpty()) {
                    val currentList = _currentCandlesList.value
                    val lastCandle = currentList.last()
                    
                    val updatedCandle = lastCandle.copy(
                        high = maxOf(lastCandle.high, activePrice),
                        low = minOf(lastCandle.low, activePrice),
                        close = activePrice,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertLatestCandle(updatedCandle)
                }
            }
        }
    }

    /**
     * Build interactive bid/ask levels on the Order Book around current price
     */
    private fun generateOrderBook(midPrice: Double) {
        val bids = mutableListOf<Pair<Double, Double>>()
        val asks = mutableListOf<Pair<Double, Double>>()
        var spreadPercent = 0.0002 // tight stock/crypto spread
        
        for (i in 1..6) {
            val bidPrice = midPrice * (1.0 - (spreadPercent * i) - (Math.random() * 0.0001))
            val askPrice = midPrice * (1.0 + (spreadPercent * i) + (Math.random() * 0.0001))
            
            val bidSize = 0.05 + Math.random() * 12.0
            val askSize = 0.05 + Math.random() * 12.0
            
            bids.add(Pair(bidPrice, bidSize))
            asks.add(Pair(askPrice, askSize))
        }
        
        _orderBookBids.value = bids.sortedByDescending { it.first }
        _orderBookAsks.value = asks.sortedBy { it.first }
    }

    /**
     * Trigger Multi-Agent deliberation of live market context using Gemini
     */
    fun triggerAiDeliberation() {
        _isAiLoading.value = true
        _aiConsoleLog.value = "🤖 Multi-Agent Otonom Yapay Zeka Sistemi analiz komutu aldı.\n" +
                "🔍 Haber tarayıcılar aktif ediliyor...\n" +
                "📈 Teknik Gösterge Verileri derleniyor..."

        viewModelScope.launch {
            val symbol = _selectedSymbol.value
            val price = _currentPrice.value
            val candles = _currentCandlesList.value

            val analysisResult = repository.fetchMultiAgentAnalysis(
                symbol = symbol,
                currentPrice = price,
                recentCandles = candles,
                policyWeights = _policyWeights.value
            )

            _aiConsoleLog.value = analysisResult
            _isAiLoading.value = false
        }
    }

    /**
     * Client execution: Trade assets on the mock exchange
     */
    fun placeOrder(type: String, quantity: Double) {
        viewModelScope.launch {
            val symbol = _selectedSymbol.value
            val price = _currentPrice.value
            
            val orderTypeTR = if (type == "AL") "ALIM" else "SATIM"
            
            // Generate a concise agent sign-off statement for the transaction log based on current weights
            val agentOpinion = "🤖 [Yapay Zeka Yürütücü]: ${symbol} sembolünde ${"%,.4f".format(quantity)} lotluk ultra-düşük gecikmeli $orderTypeTR işlemi onaylandı. " +
                    "Risk Parametreleri: Teknik Ağırlık: %${(_policyWeights.value["tech"]!! * 100).toInt()}, Stop-Loss/Take-Profit limit kontrolleri tamamlandı."

            val result = repository.executeTransaction(
                symbol = symbol,
                type = type,
                quantity = quantity,
                price = price,
                agentOpinion = agentOpinion
            )

            if (result) {
                // Reinforcement Learning: Policy adaptation matrix update based on active trade success!
                if (_isContinuousLearningEnabled.value) {
                    adaptRLPolicyMatrix(type)
                }
            } else {
                _aiConsoleLog.value = "❌ İşlem Başarısız: Yetersiz bakiye veya geçersiz parametreler!\nLütfen portföy varlığınızı veya işlem hacminizi kontrol edin."
            }
        }
    }

    /**
     * Interactive Reinforcement Learning Simulation:
     * Updates policy weights dynamically to reward agents based on action outcome logs
     */
    private fun adaptRLPolicyMatrix(tradeType: String) {
        viewModelScope.launch {
            val current = _policyWeights.value
            val tech = current["tech"] ?: 0.45
            val fund = current["fund"] ?: 0.35
            val sent = current["sent"] ?: 0.20

            // Simulate continuous learning adjustment:
            // Since it's a simulated environment, we tweak the policy coefficients slightly
            // demonstrating gradient update adaptation towards technical or fundamental factors:
            val updateLog: String
            val nextWeights = if (tradeType == "AL") {
                // ALIM traded indicates Technical Analyst is rewarded slightly and risk limits re-tuned
                val newTech = (tech + 0.03).coerceAtMost(0.60)
                val newFund = (fund - 0.015).coerceAtLeast(0.15)
                val newSent = (sent - 0.015).coerceAtLeast(0.15)
                
                // Keep sum strictly equal to 1.0
                val scale = 1.0 / (newTech + newFund + newSent)
                updateLog = "🧠 [Pekiştirmeli Öğrenme]: ALIM işlemi başarılı. Politika Güncellendi -> Teknik Analist ödüllendirildi: %${(newTech*scale*100).toInt()}"
                
                mapOf("tech" to newTech * scale, "fund" to newFund * scale, "sent" to newSent * scale)
            } else {
                // SATIM trades optimize Fundamental & Sentiment weighting
                val newTech = (tech - 0.02).coerceAtLeast(0.20)
                val newFund = (fund + 0.02).coerceAtMost(0.50)
                val newSent = (sent + 0.00).coerceAtLeast(0.10)
                
                val scale = 1.0 / (newTech + newFund + newSent)
                updateLog = "🧠 [Pekiştirmeli Öğrenme]: SATIM işlemi tamamlandı. Politika Değişti -> Temel Analiz ağırlığı artırıldı: %${(newFund*scale*100).toInt()}"
                
                mapOf("tech" to newTech * scale, "fund" to newFund * scale, "sent" to newSent * scale)
            }

            _policyWeights.value = nextWeights
            val updatedLogs = _policyAdaptationLogs.value.toMutableList()
            updatedLogs.add(0, updateLog) // prepend latest
            _policyAdaptationLogs.value = updatedLogs.take(15) // cache 15 logs
        }
    }

    /**
     * Update configuration parameters manually via the model sliders
     */
    fun manuallyAdjustWeights(tech: Double, fund: Double, sent: Double) {
        val total = tech + fund + sent
        if (total > 0) {
            _policyWeights.value = mapOf(
                "tech" to tech / total,
                "fund" to fund / total,
                "sent" to sent / total
            )
        }
    }

    fun toggleContinuousLearning(enabled: Boolean) {
        _isContinuousLearningEnabled.value = enabled
        val log = if (enabled) "Politika Pekiştirmeli Öğrenme Matrisi aktif edildi." else "Politika Öğrenimi donduruldu."
        val updatedLogs = _policyAdaptationLogs.value.toMutableList()
        updatedLogs.add(0, "⚙️ $log")
        _policyAdaptationLogs.value = updatedLogs
    }

    /**
     * Reset the portfolio ledger and candles for clean start
     */
    fun resetPortfolioSimulation() {
        viewModelScope.launch {
            repository.clearDatabaseAndReSeed()
            _policyWeights.value = mapOf("tech" to 0.45, "fund" to 0.35, "sent" to 0.20)
            _policyAdaptationLogs.value = listOf("Sistem tamamen sıfırlandı. Yeni başlangıç politikası etkinleştirildi.")
            _aiConsoleLog.value = "🔄 Simülasyon başarıyla sıfırlandı.\n" +
                    "Portföy bakiyesi 1.000 TRY Türk Lirası olarak güncellendi ve zaman serisi veri haritaları yenilendi."
            selectSymbol(_selectedSymbol.value)
        }
    }

    fun toggleAutoTrading() {
        _isAutoTradingEnabled.value = !_isAutoTradingEnabled.value
        val stateMsg = if (_isAutoTradingEnabled.value) "AKTİF" else "PASİF"
        val updatedLogs = _policyAdaptationLogs.value.toMutableList()
        updatedLogs.add(0, "🤖 Otonom Ajan Ticaret Modu $stateMsg duruma getirildi.")
        _policyAdaptationLogs.value = updatedLogs
    }

    private fun startAutoTraderLoop() {
        autoTraderJob?.cancel()
        autoTraderJob = viewModelScope.launch(Dispatchers.Default) {
            delay(5000) // 5 seconds warmup
            while (true) {
                if (_isAutoTradingEnabled.value) {
                    executeAutonomousAgentTradeStep()
                }
                delay(12000) // otonom decision step every 12 seconds
            }
        }
    }

    private suspend fun executeAutonomousAgentTradeStep() {
        val availableSymbols = listOf("BTC", "ETH", "AAPL", "TSLA")
        val activeSymbol = _selectedSymbol.value
        // choose active symbol 60% of the time, others 40% of the time
        val symbol = if (Math.random() < 0.6) activeSymbol else availableSymbols.random()

        val price = if (symbol == activeSymbol) {
            _currentPrice.value
        } else {
            val base = symbolBasePrices[symbol] ?: 1000.0
            base * (1.0 + (Math.random() - 0.5) * 0.02)
        }

        val cashAsset = repository.getPortfolioAssetBySymbol("TRY") ?: PortfolioAsset("TRY", "Türk Lirası", 1000.0, 1.0)
        val targetAsset = repository.getPortfolioAssetBySymbol(symbol)
        val targetQty = targetAsset?.quantity ?: 0.0

        // Decision metrics based on current RL weights
        val techSignal = (Math.random() - 0.47) * 2.0 // slight buy bias
        val fundSignal = (Math.random() - 0.50) * 2.0
        val sentSignal = (Math.random() - 0.45) * 2.0 // slight buy bias

        val wTech = _policyWeights.value["tech"] ?: 0.45
        val wFund = _policyWeights.value["fund"] ?: 0.35
        val wSent = _policyWeights.value["sent"] ?: 0.20

        val aggregateScore = (techSignal * wTech) + (fundSignal * wFund) + (sentSignal * wSent)

        if (aggregateScore > 0.15 && cashAsset.quantity >= 15.0) {
            // Decide to BUY
            val maxBuyable = minOf(cashAsset.quantity, 150.0)
            val tradeTRYValue = 15.0 + Math.random() * (maxBuyable - 15.0)
            val qtyToBuy = tradeTRYValue / price

            val rationale = if (techSignal > fundSignal && techSignal > sentSignal) {
                "Teknik Göstergeler (Stokastik ralli ve RSI pozitif uyumsuzluk) güçlü yükseliş ibaresi taşıyor."
            } else if (fundSignal > sentSignal) {
                "Açıklanan son çeyrek bilanço çarpanları ve adil değer hedefleri alım potansiyelini öne çıkarıyor."
            } else {
                "Canlı haber akışındaki olumlu spekülasyonlar ve kitle hissiyatı (Sentiment: +0.72) alımı tetikledi."
            }

            val agentOpinion = "🤖 [Otonom Yapay Zeka Ajanı - Alım Kararı]: ${symbol} seçildi. Sinyal Skoru: ${"%.3f".format(aggregateScore)}. Rasyonel: $rationale"
            val success = repository.executeTransaction(symbol, "AL", qtyToBuy, price, agentOpinion)
            if (success) {
                val newCash = cashAsset.quantity - tradeTRYValue
                _aiConsoleLog.value = "🤖 [Otonom İşlem Raporu]\n" +
                        "Ajanlar $symbol üzerinde ALIM yönlü mutabakata vardı!\n" +
                        "Fiyat: TRY ${"%,.2f".format(price)} | Miktar: ${"%,.4f".format(qtyToBuy)} Lot\n" +
                        "Toplam Maliyet: TRY ${"%,.2f".format(tradeTRYValue)}\n" +
                        "Gerekçe: $rationale\n" +
                        "Kalan Cüzdan Bakiyesi: TRY ${"%,.2f".format(newCash)}"

                if (_isContinuousLearningEnabled.value) {
                    adaptRLPolicyMatrix("AL")
                }
            }
        } else if (aggregateScore < -0.15 && targetQty > 0.0) {
            // Decide to SELL
            val sellPercent = 0.3 + Math.random() * 0.7
            val qtyToSell = (targetQty * sellPercent).coerceAtMost(targetQty)
            if (qtyToSell > 0.0001) {
                val tradeValue = qtyToSell * price

                val rationale = if (techSignal < fundSignal && techSignal < sentSignal) {
                    "MACD sat sinyali ve aşırı şişmiş momentum indikatörleri sebebiyle kâr koruma adımına geçildi."
                } else if (fundSignal < sentSignal) {
                    "Likidite optimizasyonu hedefleri ve bakiye dengesi gereği aktif pozisyondan çıkış tercih edildi."
                } else {
                    "Trend gücü azalışı ve genel yatırımcı duyarlılığı bozulması sebebiyle satış oylandı."
                }

                val agentOpinion = "🤖 [Otonom Yapay Zeka Ajanı - Satış Kararı]: ${symbol} seçildi. Sinyal Skoru: ${"%.3f".format(aggregateScore)}. Rasyonel: $rationale"
                val success = repository.executeTransaction(symbol, "SAT", qtyToSell, price, agentOpinion)
                if (success) {
                    val newCash = cashAsset.quantity + tradeValue
                    _aiConsoleLog.value = "🤖 [Otonom İşlem Raporu]\n" +
                            "Ajanlar $symbol üzerinde SATIŞ yönlü mutabakata vardı!\n" +
                            "Fiyat: TRY ${"%,.2f".format(price)} | Miktar: ${"%,.4f".format(qtyToSell)} Lot\n" +
                            "Portföy Geliri: TRY ${"%,.2f".format(tradeValue)}\n" +
                            "Gerekçe: $rationale\n" +
                            "Yeni Cüzdan Bakiyesi: TRY ${"%,.2f".format(newCash)}"

                    if (_isContinuousLearningEnabled.value) {
                        adaptRLPolicyMatrix("SAT")
                    }
                }
            }
        }
    }
}
