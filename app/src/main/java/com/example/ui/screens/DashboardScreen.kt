package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AccountWithBalance
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LedgerViewModel,
    onNavigateToAccount: (Int) -> Unit
) {
    var showVoiceSmartEntryDialog by remember { mutableStateOf(false) }
    val accountsWithBalance by viewModel.accountsWithBalance.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    // Compute Metrics
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdf.format(Date())

    // 1. Daily Earnings (Total invoices created today in base currency)
    val dailyEarnings = transactions.filter { 
        it.date == todayStr && !it.isPayment
    }.sumOf { it.total * it.exchangeRate }

    // 2. Weekly Earnings (Last 7 days in base currency)
    val calendar = Calendar.getInstance()
    val nowMs = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val sevenDaysAgoMs = calendar.timeInMillis
    val weeklyEarnings = transactions.filter {
        it.timestamp in sevenDaysAgoMs..nowMs && !it.isPayment
    }.sumOf { it.total * it.exchangeRate }

    // 3. Monthly Earnings (Last 30 days in base currency)
    calendar.timeInMillis = nowMs
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    val thirtyDaysAgoMs = calendar.timeInMillis
    val monthlyEarnings = transactions.filter {
        it.timestamp in thirtyDaysAgoMs..nowMs && !it.isPayment
    }.sumOf { it.total * it.exchangeRate }

    // 4. Summaries separation:
    // Debtors (المدينين - Customers who owe us money)
    val debtorsTotal = accountsWithBalance
        .filter { it.account.type == "مشتري" && it.balance > 0 }
        .sumOf { it.balance }

    // Creditors (الدائنين - Suppliers we owe money to)
    val creditorsTotal = accountsWithBalance
        .filter { it.account.type == "مورد" && it.balance > 0 }
        .sumOf { it.balance }

    val totalF = (debtorsTotal + creditorsTotal).toFloat()
    val debtorsFraction = if (totalF > 0) (debtorsTotal / totalF).toFloat() else 0.75f
    val creditorsFraction = if (totalF > 0) (creditorsTotal / totalF).toFloat() else 0.33f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App branding banner with customized logo and titles
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: app status subtheme
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "نظام لإدارة الحسابات",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Left
                    )
                    Text(
                        text = "الإصدار برو المتميز",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Left
                    )
                }

                // Right: Logo thumbnail + title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "المحاسب anas برو",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "إدارة العملاء والديون والموردين",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }

                    // Display newly generated beautiful logo image with modern border and roundness
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.anas_pro_logo_1781236904612),
                        contentDescription = "شعار المحاسب برو",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }

        // Welcome and Header (Now displaying High Density Net Balance Available)
        item {
            HeaderSection(netBalance = debtorsTotal - creditorsTotal, defaultCurrency = defaultCurrency)
        }

        // Voice Smart Entry Premium Assistant Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVoiceSmartEntryDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "مساعد القيد والديون الصوتي المبتكر ✨🎙️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "اضغط لنطق المعاملة بالصوت (أوفلاين) ليقوم المساعد الذكي باستخلاصها وقيدها فورا في حساب العميل أو المورد تلقائياً!",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Key Finance Cards Row (Debtors vs Creditors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "المدينون (عملاء)",
                    amount = debtorsTotal,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.TrendingUp,
                    progressFraction = debtorsFraction,
                    defaultCurrency = defaultCurrency,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "الدائنون (موردون)",
                    amount = creditorsTotal,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.TrendingDown,
                    progressFraction = creditorsFraction,
                    defaultCurrency = defaultCurrency,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Widgets for daily, weekly, monthly transactions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مؤشرات حركة المبيعات (الأنشطة)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    MetricRow(label = "مبيعات وإيرادات اليوم", amount = dailyEarnings, color = Color(0xFF22C55E), defaultCurrency = defaultCurrency)
                    Spacer(modifier = Modifier.height(12.dp))
                    MetricRow(label = "حركة الـ 7 أيام الأخيرة", amount = weeklyEarnings, color = Color(0xFFEAB308), defaultCurrency = defaultCurrency)
                    Spacer(modifier = Modifier.height(12.dp))
                    MetricRow(label = "حركة الـ 30 يوماً الأخيرة", amount = monthlyEarnings, color = Color(0xFF3B82F6), defaultCurrency = defaultCurrency)
                }
            }
        }

        // Recent Accounts Header
        item {
            Text(
                text = "الحسابات النشطة مؤخراً",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        // Recents List
        val activeAccounts = accountsWithBalance.filter { it.transactionCount > 0 }.take(5)
        if (activeAccounts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد حركات حساب حالية. ابدأ بإضافة الحسابات والعمليات اليومية.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeAccounts) { item ->
                ActiveAccountRow(item, defaultCurrency = defaultCurrency) {
                    onNavigateToAccount(item.account.id)
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showVoiceSmartEntryDialog) {
        VoiceSmartEntryDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceSmartEntryDialog = false },
            onEntrySaved = { }
        )
    }
}

@Composable
fun HeaderSection(netBalance: Double, defaultCurrency: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "تحديث الآن",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = "إجمالي الرصيد المتوفر",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                val sym = when (defaultCurrency) {
                    "USD" -> "$"
                    "SAR" -> "ر.س"
                    "YER" -> "ر.ي"
                    else -> defaultCurrency
                }
                Text(
                    text = " $sym",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val formattedBalance = String.format(Locale.US, "%,.2f", netBalance)
                Text(
                    text = formattedBalance,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    progressFraction: Float,
    defaultCurrency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            val sym = when (defaultCurrency) {
                "USD" -> "$"
                "SAR" -> "ر.س"
                "YER" -> "ر.ي"
                else -> defaultCurrency
            }
            Text(
                text = "${String.format(Locale.US, "%,.2f", amount)} $sym",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(100.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun MetricRow(
    label: String,
    amount: Double,
    color: Color,
    defaultCurrency: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sym = when (defaultCurrency) {
                "USD" -> "$"
                "SAR" -> "ر.س"
                "YER" -> "ر.ي"
                else -> defaultCurrency
            }
            Text(
                text = "${String.format(Locale.US, "%,.2f", amount)} $sym",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Simple visual bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            // Fraction simulation
            val fraction = if (amount > 0) 0.65f else 0f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(100.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun ActiveAccountRow(
    item: AccountWithBalance,
    defaultCurrency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: balance amount
            val balanceColor = if (item.balance >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
            val balanceSignText = if (item.balance >= 0) "له/عليه" else "زيادة"
            Column(horizontalAlignment = Alignment.Start) {
                val sym = when (defaultCurrency) {
                    "USD" -> "$"
                    "SAR" -> "ر.س"
                    "YER" -> "ر.ي"
                    else -> defaultCurrency
                }
                Text(
                    text = "${String.format(Locale.US, "%,.2f", Math.abs(item.balance))} $sym",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
                Text(
                    text = "$balanceSignText | ${item.transactionCount} حركة",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Right: profile name and type badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.account.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.account.phone,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (item.account.type == "مورد") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.account.type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.account.type == "مورد") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }
    }
}
