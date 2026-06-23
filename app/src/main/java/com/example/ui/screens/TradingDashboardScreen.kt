package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import com.example.data.model.MarketCandle
import com.example.data.model.PortfolioAsset
import com.example.data.model.TransactionRecord
import com.example.ui.viewmodel.TradingViewModel
import java.text.SimpleDateFormat
import java.util.*

// Cosmic Dark Terminal Colors
val DeepBg = Color(0xFF030303)
val CardBg = Color(0xFF0B0E14)
val BorderColor = Color(0xFF1E2633)
val NeonGreen = Color(0xFF00FF88)
val NeonRed = Color(0xFFFF3366)
val GrayText = Color(0xFF888E99)
val AccentBlue = Color(0xFF2962FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingDashboardScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
    val rawPrice by viewModel.currentPrice.collectAsStateWithLifecycle()
    val change24h by viewModel.priceChange24h.collectAsStateWithLifecycle()
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val candlesList by viewModel.currentCandlesList.collectAsStateWithLifecycle()

    val bookBids by viewModel.orderBookBids.collectAsStateWithLifecycle()
    val bookAsks by viewModel.orderBookAsks.collectAsStateWithLifecycle()

    val aiConsoleLog by viewModel.aiConsoleLog.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val policyWeights by viewModel.policyWeights.collectAsStateWithLifecycle()
    val isContinuousLearning by viewModel.isContinuousLearningEnabled.collectAsStateWithLifecycle()
    val policyLogs by viewModel.policyAdaptationLogs.collectAsStateWithLifecycle()
    val isAutoTradingEnabled by viewModel.isAutoTradingEnabled.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Terminal, 1: Ajan Matrisi, 2: Cüzdan & Kayıtlar
    val focusManager = LocalFocusManager.current

    val symbolsList = listOf("BTC", "ETH", "AAPL", "TSLA")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AKATRADE AI",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepBg,
                    titleContentColor = Color.White
                ),
                actions = {
                    val tryCashAsset = assets.find { it.symbol == "TRY" }
                    val tryCashValue = tryCashAsset?.quantity ?: 1000.0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = NeonGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, NeonGreen.copy(alpha = 0.35f), shape = RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Bakiye: ${"%,.2f".format(tryCashValue)} ₺",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("wallet_balance")
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.resetPortfolioSimulation() },
                            modifier = Modifier.testTag("reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sıfırla",
                                tint = NeonRed
                            )
                        }
                    }
                }
            )
        },
        containerColor = DeepBg,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepBg)
        ) {
            // Live Global Market Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(CardBg, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, shape = RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Symbol Ticker Selector Row
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    symbolsList.forEach { sym ->
                        val isSelected = (sym == selectedSymbol)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) AccentBlue.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentBlue else BorderColor,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    viewModel.selectSymbol(sym)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("symbol_tab_$sym"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sym,
                                color = if (isSelected) Color.White else GrayText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Selected Info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TRY ${"%,.2f".format(rawPrice)}",
                        color = if (change24h >= 0) NeonGreen else NeonRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (change24h >= 0) "▲" else "▼",
                            color = if (change24h >= 0) NeonGreen else NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${if (change24h >= 0) "+" else ""}${"%.2f".format(change24h)}%",
                            color = if (change24h >= 0) NeonGreen else NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Material 3 Tabs in Turkish
            val tabTitles = listOf("⚡ Terminal", "🤖 Ajan Matrisi", "📁 Portföy & Kayıtlar")
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = DeepBg,
                contentColor = AccentBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = {
                            activeTab = index
                            focusManager.clearFocus()
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = if (activeTab == index) Color.White else GrayText
                            )
                        },
                        modifier = Modifier.testTag("main_tab_$index")
                    )
                }
            }

            // Tabs Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> TerminalTabScreen(
                        candles = candlesList,
                        currentPrice = rawPrice,
                        symbol = selectedSymbol,
                        bids = bookBids,
                        asks = bookAsks,
                        onBuy = { qty -> viewModel.placeOrder("AL", qty) },
                        onSell = { qty -> viewModel.placeOrder("SAT", qty) },
                        aiConsoleLog = aiConsoleLog,
                        isAiLoading = isAiLoading,
                        onTriggerAi = { viewModel.triggerAiDeliberation() },
                        isAutoTradingEnabled = isAutoTradingEnabled,
                        onToggleAutoTrading = { viewModel.toggleAutoTrading() }
                    )
                    1 -> PolicyMatrixTabScreen(
                        weights = policyWeights,
                        isLearning = isContinuousLearning,
                        logs = policyLogs,
                        onToggleLearning = { viewModel.toggleContinuousLearning(it) },
                        onManualAdjust = { t, f, s -> viewModel.manuallyAdjustWeights(t, f, s) }
                    )
                    2 -> PortfolioTabScreen(
                        assets = assets,
                        transactions = transactions,
                        currentPrice = rawPrice,
                        selectedSymbol = selectedSymbol
                    )
                }
            }
        }
    }
}

// --- TERMINAL TAB COMPOSABLE ---
@Composable
fun TerminalTabScreen(
    candles: List<MarketCandle>,
    currentPrice: Double,
    symbol: String,
    bids: List<Pair<Double, Double>>,
    asks: List<Pair<Double, Double>>,
    onBuy: (Double) -> Unit,
    onSell: (Double) -> Unit,
    aiConsoleLog: String,
    isAiLoading: Boolean,
    onTriggerAi: () -> Unit,
    isAutoTradingEnabled: Boolean,
    onToggleAutoTrading: () -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // High-Fidelity Custom Candlestick Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 Canlı Zaman Serisi Grafiği ($symbol / TRY)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EMA-5 AKTİF",
                                color = NeonGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Draw the stock candlestick chart
                    StockCandlestickChart(
                        candles = candles,
                        currentPrice = currentPrice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Executive Trade Execution Box and Book side-by-side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Order Book Left (Timescale/Redis speed representation)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(230.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "📁 Tahta (Order Book)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Fiyat (TRY)", color = GrayText, fontSize = 9.sp)
                            Text(text = "Miktar", color = GrayText, fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Asks (Red)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            asks.take(3).reversed().forEach { ask ->
                                OrderBookRow(price = ask.first, size = ask.second, color = NeonRed, maxVolume = 12.0)
                            }
                        }

                        // Divider Spread Info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(BorderColor.copy(alpha = 0.3f))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val spread = if (asks.isNotEmpty() && bids.isNotEmpty()) asks.first().first - bids.first().first else 0.0
                            Text(
                                text = "Makas: TRY ${"%.2f".format(spread)}",
                                color = GrayText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Bids (Green)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            bids.take(3).forEach { bid ->
                                OrderBookRow(price = bid.first, size = bid.second, color = NeonGreen, maxVolume = 12.0)
                            }
                        }
                    }
                }

                // Buy / Sell Input Console Right
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(230.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Emir Ekranı",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        // Lot Input Box
                        TextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            placeholder = { Text("0.0", color = GrayText, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, shape = RoundedCornerShape(4.dp))
                                .testTag("quantity_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DeepBg,
                                unfocusedContainerColor = DeepBg,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = AccentBlue,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            suffix = { Text("Lot", color = GrayText, fontSize = 11.sp) }
                        )

                        val qty = quantityText.toDoubleOrNull() ?: 0.0
                        val computedTotal = qty * currentPrice

                        // Estimated cost view
                        Column {
                            Text(text = "İşlem Tutarı:", color = GrayText, fontSize = 10.sp)
                            Text(
                                text = "TRY ${"%,.2f".format(computedTotal)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Emerald AL / Dark Crimson SAT actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    onBuy(qty)
                                    quantityText = ""
                                    focusManager.clearFocus()
                                },
                                enabled = qty > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("buy_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = DeepBg,
                                    disabledContainerColor = GrayText.copy(alpha = 0.2f)
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("AL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onSell(qty)
                                    quantityText = ""
                                    focusManager.clearFocus()
                                },
                                enabled = qty > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sell_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonRed,
                                    contentColor = Color.White,
                                    disabledContainerColor = GrayText.copy(alpha = 0.2f)
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("SAT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Multi-Agent Terminal Console Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🤖 Otonom Çoklu Ajan Karar Konsolu",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (!isAiLoading) {
                            Box(
                                modifier = Modifier
                                    .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, AccentBlue, RoundedCornerShape(4.dp))
                                    .clickable { onTriggerAi() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("trigger_ai_button")
                            ) {
                                Text(
                                    text = "Yapay Zeka Analizi",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Otonom Ajan Al-Sat Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBg, shape = RoundedCornerShape(4.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🤖 Ajan Otonom Al-Sat İşlemleri",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isAutoTradingEnabled) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAutoTradingEnabled) NeonGreen else NeonRed,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onToggleAutoTrading() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("toggle_auto_trading")
                        ) {
                            Text(
                                text = if (isAutoTradingEnabled) "DURDUR (AKTİF)" else "BAŞLAT (PASİF)",
                                color = if (isAutoTradingEnabled) NeonGreen else NeonRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated / Actual console response
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBg, shape = RoundedCornerShape(6.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        if (isAiLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = NeonGreen, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Yapay Zeka Ajanları İş Birliği Yapıyor...\n(Teknik, Temel, Risk ve Yürütücü Sentezleniyor)",
                                    color = GrayText,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Text(
                                text = aiConsoleLog,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- PORTFOLIO TAB COMPOSABLE ---
@Composable
fun PortfolioTabScreen(
    assets: List<PortfolioAsset>,
    transactions: List<TransactionRecord>,
    currentPrice: Double,
    selectedSymbol: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Portfolio Asset Summary Box (Turkish metrics)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "💳 Portföy Toplam Değeri", color = GrayText, fontSize = 12.sp)
                    
                    val tryCash = assets.find { it.symbol == "TRY" }?.quantity ?: 0.0
                    var cryptoValue = 0.0
                    assets.filter { it.symbol != "TRY" }.forEach { asset ->
                        // Calculate estimation using active simulation ticks
                        val livePrice = if (asset.symbol == selectedSymbol) currentPrice else asset.averageCost
                        cryptoValue += (asset.quantity * livePrice)
                    }
                    val netWorth = tryCash + cryptoValue

                    Text(
                        text = "TRY ${"%,.2f".format(netWorth)}",
                        color = NeonGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nakit: TRY ${"%,.2f".format(tryCash)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Hisse/Kripto: TRY ${"%,.2f".format(cryptoValue)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Holding Assets List
        item {
            Text(
                text = "💼 Varlık Bilgileri",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        val nonCashAssets = assets.filter { it.symbol != "TRY" }
        if (nonCashAssets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Portföyünüzde henüz hisse veya kripto varlık bulunmamaktadır. Terminal üzerinden alım satım yapabilirsiniz.",
                        color = GrayText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(nonCashAssets) { asset ->
                val livePrice = if (asset.symbol == selectedSymbol) currentPrice else asset.averageCost
                val currentVal = asset.quantity * livePrice
                val costVal = asset.quantity * asset.averageCost
                val pnlValue = currentVal - costVal
                val pnlPercent = if (costVal > 0) (pnlValue / costVal) * 100.0 else 0.0

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = asset.symbol,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(text = asset.name, color = GrayText, fontSize = 11.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${"%,.4f".format(asset.quantity)} Lot",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Maliyet: TRY ${"%,.2f".format(asset.averageCost)}",
                                color = GrayText,
                                fontSize = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TRY ${"%,.2f".format(currentVal)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${if (pnlValue >= 0) "+" else ""}${"%.2f".format(pnlPercent)}%",
                                color = if (pnlValue >= 0) NeonGreen else NeonRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Transactions History Logs
        item {
            Text(
                text = "📜 İşlem Geçmişi (TimescaleDB / Ledger Örneği)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Henüz onaylanmış bir borsa kaydı yok.", color = GrayText, fontSize = 11.sp)
                }
            }
        } else {
            items(transactions) { tx ->
                val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (tx.type == "AL") NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tx.type,
                                        color = if (tx.type == "AL") NeonGreen else NeonRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tx.symbol,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(tx.timestamp)),
                                color = GrayText,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Miktar: ${"%,.4f".format(tx.quantity)} Lot",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Fiyat: TRY ${"%,.2f".format(tx.price)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Toplam: TRY ${"%,.2f".format(tx.quantity * tx.price)}",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Display detailed AI risk report justification saved in SQLite Timeseries record
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(DeepBg, shape = RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = tx.agentNotes,
                                color = GrayText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- AJAN/RL MATRIX COMPOSABLE ---
@Composable
fun PolicyMatrixTabScreen(
    weights: Map<String, Double>,
    isLearning: Boolean,
    logs: List<String>,
    onToggleLearning: (Boolean) -> Unit,
    onManualAdjust: (Double, Double, Double) -> Unit
) {
    var techSlider by remember { mutableStateOf((weights["tech"] ?: 0.45).toFloat()) }
    var fundSlider by remember { mutableStateOf((weights["fund"] ?: 0.35).toFloat()) }
    var sentSlider by remember { mutableStateOf((weights["sent"] ?: 0.20).toFloat()) }

    // Synchronize sliders if database triggers changes externally
    LaunchedEffect(weights) {
        techSlider = (weights["tech"] ?: 0.45).toFloat()
        fundSlider = (weights["fund"] ?: 0.35).toFloat()
        sentSlider = (weights["sent"] ?: 0.20).toFloat()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RL Policy status configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🧠 Pekiştirmeli Öğrenme Modeli",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(text = "Otonom politika matrisi optimizasyonu", color = GrayText, fontSize = 10.sp)
                        }

                        Switch(
                            checked = isLearning,
                            onCheckedChange = { onToggleLearning(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = AccentBlue.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("learning_switch")
                        )
                    }
                }
            }
        }

        // Sliders weights controllers box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚙️ Manuel Karar Politikası Dengeleri",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Technical Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Teknik Analiz Ajanı Ağırlığı", color = Color.White, fontSize = 11.sp)
                            Text(
                                text = "%${(techSlider * 100).toInt()}",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = techSlider,
                            onValueChange = {
                                techSlider = it
                                onManualAdjust(techSlider.toDouble(), fundSlider.toDouble(), sentSlider.toDouble())
                            },
                            valueRange = 0.1f..0.8f,
                            colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen),
                            modifier = Modifier.testTag("tech_slider")
                        )
                    }

                    // Fundamental Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Temel Analiz Ajanı Ağırlığı", color = Color.White, fontSize = 11.sp)
                            Text(
                                text = "%${(fundSlider * 100).toInt()}",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = fundSlider,
                            onValueChange = {
                                fundSlider = it
                                onManualAdjust(techSlider.toDouble(), fundSlider.toDouble(), sentSlider.toDouble())
                            },
                            valueRange = 0.1f..0.8f,
                            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue),
                            modifier = Modifier.testTag("fund_slider")
                        )
                    }

                    // Sentiment Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Haber & X Duygu Ajanı Ağırlığı", color = Color.White, fontSize = 11.sp)
                            Text(
                                text = "%${(sentSlider * 100).toInt()}",
                                color = Color.Magenta,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = sentSlider,
                            onValueChange = {
                                sentSlider = it
                                onManualAdjust(techSlider.toDouble(), fundSlider.toDouble(), sentSlider.toDouble())
                            },
                            valueRange = 0.1f..0.8f,
                            colors = SliderDefaults.colors(thumbColor = Color.Magenta, activeTrackColor = Color.Magenta),
                            modifier = Modifier.testTag("sent_slider")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(DeepBg, shape = RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "*Kaydırıcılar ayarlandığında, ajanların karar ağırlıkları normalize edilip pekiştirilir ve otonom karar matrisine hemen uygulanır.",
                            color = GrayText,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Live RL adaptation model logs listing
        item {
            Text(
                text = "📊 Canlı Model Adaptasyon Günlükleri",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (logs.isEmpty()) {
            item {
                Text(text = "Adaptasyon kaydı henüz oluşturulmadı.", color = GrayText, fontSize = 11.sp)
            }
        } else {
            items(logs) { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = log,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// --- CORE UTILITY COMPOSABLES ---

@Composable
fun OrderBookRow(
    price: Double,
    size: Double,
    color: Color,
    maxVolume: Double
) {
    val fillWidth = (size / maxVolume).coerceIn(0.1, 1.0).toFloat()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Horizontal volume backdrop bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fillWidth)
                .background(color.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "%,.2f".format(price),
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "%,.4f".format(size),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Custom Candlestick Chart implementing Canvas and Grid
 */
@Composable
fun StockCandlestickChart(
    candles: List<MarketCandle>,
    currentPrice: Double,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(DeepBg)
            .border(1.dp, BorderColor, shape = RoundedCornerShape(4.dp))
    ) {
        val width = size.width
        val height = size.height

        if (candles.size < 2) {
            // Draw empty placeholder text
            return@Canvas
        }

        // Find extrema bounds to normalize graph
        val maxHigh = candles.maxOfOrNull { it.high } ?: 1.0
        val minLow = candles.minOfOrNull { it.low } ?: 0.0
        val priceRange = if (maxHigh == minLow) 1.0 else maxHigh - minLow

        // Helper price mapper Lambda
        fun mapPriceToY(price: Double): Float {
            val ratio = (price - minLow) / priceRange
            // Invert, as Canvas 0,0 is at top-left
            return (height - (ratio * height)).toFloat()
        }

        // 1. Draw grid lines inside canvas
        val gridLines = 4
        for (grid in 1 until gridLines) {
            val gridY = (height / gridLines) * grid
            drawLine(
                color = BorderColor.copy(alpha = 0.4f),
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1f
            )
        }

        // 2. Draw Candlesticks & lines
        val candleCount = candles.size
        // Give 1-candle width padding on sides
        val stepWidth = width / (candleCount + 1)

        candles.forEachIndexed { i, candle ->
            val x = (i + 1) * stepWidth
            
            val yOpen = mapPriceToY(candle.open)
            val yClose = mapPriceToY(candle.close)
            val yHigh = mapPriceToY(candle.high)
            val yLow = mapPriceToY(candle.low)

            val isBullish = candle.close >= candle.open
            val color = if (isBullish) NeonGreen else NeonRed

            // Wick
            drawLine(
                color = color,
                start = Offset(x, yHigh),
                end = Offset(x, yLow),
                strokeWidth = 2f
            )

            // Body Rect
            val bodyHeight = Math.abs(yClose - yOpen).coerceAtLeast(4f)
            val bodyTop = Math.min(yOpen, yClose)
            val rectWidth = stepWidth * 0.65f

            drawRect(
                color = color,
                topLeft = Offset(x - rectWidth / 2, bodyTop),
                size = Size(rectWidth, bodyHeight)
            )
        }

        // 3. Compute and draw EMA-5 Trendline overlay Path
        val emaPath = Path()
        var rsum = 0.0
        val kMultiplier = 2.0 / (5 + 1)
        var previousEma = candles.first().close

        candles.forEachIndexed { i, candle ->
            val x = (i + 1) * stepWidth
            // Calculate exponential moving average
            val ema = if (i == 0) candle.close else (candle.close * kMultiplier) + (previousEma * (1 - kMultiplier))
            previousEma = ema
            val emaY = mapPriceToY(ema)

            if (i == 0) {
                emaPath.moveTo(x, emaY)
            } else {
                emaPath.lineTo(x, emaY)
            }
        }

        drawPath(
            path = emaPath,
            color = AccentBlue,
            style = Stroke(width = 3f)
        )

        // 4. Draw a dotted horizontal line at Current Price
        val currPriceY = mapPriceToY(currentPrice)
        drawLine(
            color = NeonGreen.copy(alpha = 0.5f),
            start = Offset(0f, currPriceY),
            end = Offset(width, currPriceY),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
        )
    }
}
