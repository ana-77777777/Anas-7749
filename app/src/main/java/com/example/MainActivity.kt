package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EntriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LedgerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainLayout()
            }
        }
    }
}

@Composable
fun AppHeader(currentTab: Int, businessName: String, onLock: () -> Unit) {
    val title = "المحاسب anas برو"
    val subtext = when (currentTab) {
        0 -> "Pro Ledger • لوحة التحكم"
        1 -> "Pro Ledger • إدارة الحسابات"
        2 -> "Pro Ledger • العمليات والقيود"
        3 -> "Pro Ledger • تهيئة الإعدادات"
        4 -> "Pro Ledger • المستشار المالي الذكي"
        5 -> "Pro Ledger • سلة المحذوفات"
        else -> "Pro Ledger"
    }

    val avatarChar = if (businessName.isNotBlank()) businessName.trim().first().toString() else "👤"
    val borderColor = MaterialTheme.colorScheme.outline

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = borderColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = subtext,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Manual safety lock button
                    IconButton(
                        onClick = onLock,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل التطبيق وتأكيد الأمان",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarChar,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainLayout() {
    val viewModel: LedgerViewModel = viewModel()
    val isSecurityEnabled by viewModel.isSecurityEnabled.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    
    // Lock state on startup
    var isUnlocked by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) } // Tabs index: 0=Dashboard, 1=Accounts, 2=Entries, 3=Settings, 4=AI Advisor, 5=Trash
    val businessName by viewModel.businessName.collectAsState()
    
    val navigateToTrash: () -> Unit = { currentTab = 5 }

    // Sync state if security configuration or pin changes dynamically
    LaunchedEffect(isSecurityEnabled, securityPin) {
        if (!isSecurityEnabled) {
            isUnlocked = true
        }
    }

    // Helper function to transition seamlessly from dashboard/accounts list to specific statements detail
    val navigateToAccountStatement: (Int) -> Unit = { accountId ->
        viewModel.selectAccount(accountId)
        currentTab = 2 // Switch to Entries tab
    }

    if (!isUnlocked && isSecurityEnabled && securityPin.isNotBlank()) {
        PinLockScreen(
            correctPin = securityPin,
            onUnlockSuccess = { isUnlocked = true }
        )
    } else if (!isUnlocked && (!isSecurityEnabled || securityPin.isBlank())) {
        // Beautiful PIN Onboarding Setup Screen for first time users or when protection is unconfigured
        PinSetupScreen(
            onPinSet = { newPin ->
                viewModel.updateSecuritySettings(true, newPin)
                isUnlocked = true
            },
            onSkip = {
                isUnlocked = true
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppHeader(
                    currentTab = currentTab, 
                    businessName = businessName,
                    onLock = {
                        isUnlocked = false
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    // Settings Tab
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                        label = { Text("الإعدادات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )

                    // AI Advisor Tab
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = { Icon(Icons.Default.Lightbulb, contentDescription = "المستشار") },
                        label = { Text("المستشار", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )

                    // Operations Tab
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "العمليات") },
                        label = { Text("العمليات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )

                    // Accounts Tab
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Group, contentDescription = "الحسابات") },
                        label = { Text("الحسابات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )

                    // Dashboard Tab
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        ) { innerPadding ->
            val isCloudFrozen by viewModel.isCloudFrozen.collectAsState()
            val cloudClientId by viewModel.cloudClientId.collectAsState()

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                if (isCloudFrozen && currentTab != 3) {
                    FrozenBlockedScreen(
                        clientId = cloudClientId,
                        onGoToSettings = { currentTab = 3 }
                    )
                } else {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToAccount = navigateToAccountStatement
                        )
                        1 -> AccountsScreen(
                            viewModel = viewModel,
                            onNavigateToAccount = navigateToAccountStatement
                        )
                        2 -> EntriesScreen(
                            viewModel = viewModel
                        )
                        3 -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToTrash = navigateToTrash
                        )
                        4 -> AiAdvisorScreen(
                            viewModel = viewModel
                        )
                        5 -> TrashScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    correctPin: String,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val actualCorrectPin = correctPin.ifBlank { "1234" } // Fallback default PIN if enabled but empty

    // Helper when typing digits
    val onDigitClick: (String) -> Unit = { digit ->
        if (enteredPin.length < 4) {
            showError = false
            enteredPin += digit
            if (enteredPin.length == 4) {
                if (enteredPin == actualCorrectPin) {
                    onUnlockSuccess()
                } else {
                    showError = true
                    enteredPin = ""
                }
            }
        }
    }

    val onBackspace: () -> Unit = {
        if (enteredPin.isNotEmpty()) {
            showError = false
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scaffoldBackgroundColorKeyboardHelper() ?: MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "المحاسب anas برو • Pro Ledger",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "أدخل رمز PIN المكون من 4 أرقام لفتح التطبيق",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..3).forEach { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            // Error Message
            if (showError) {
                Text(
                    text = "❌ الرمز السري غير صحيح، يرجى المحاولة مرة أخرى",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dial Pad Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { button ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (button.isNotBlank()) {
                                    if (button == "back") {
                                        IconButton(
                                            onClick = onBackspace,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "مسح الخلف",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .clickable { onDigitClick(button) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = button,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupScreen(
    onPinSet: (String) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = Enter PIN, 2 = Confirm PIN
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val onDigitClick: (String) -> Unit = { digit ->
        showError = false
        if (step == 1) {
            if (enteredPin.length < 4) {
                enteredPin += digit
                if (enteredPin.length == 4) {
                    step = 2
                }
            }
        } else {
            if (confirmedPin.length < 4) {
                confirmedPin += digit
                if (confirmedPin.length == 4) {
                    if (confirmedPin == enteredPin) {
                        onPinSet(confirmedPin)
                    } else {
                        showError = true
                        confirmedPin = ""
                    }
                }
            }
        }
    }

    val onBackspace: () -> Unit = {
        showError = false
        if (step == 1) {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
            }
        } else {
            if (confirmedPin.isNotEmpty()) {
                confirmedPin = confirmedPin.dropLast(1)
            } else {
                // Return to step 1
                step = 1
                enteredPin = enteredPin.dropLast(1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scaffoldBackgroundColorKeyboardHelper() ?: MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "إنشاء قفل الأمان السري 🔒",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (step == 1) 
                        "أدخل رمز PIN مكوّن من 4 أرقام لتأمين سجلاتك" 
                    else 
                        "أعد إدخال الرمز السري لتأكيده وتنشيط الحماية فوراً",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentPinLength = if (step == 1) enteredPin.length else confirmedPin.length
                (0..3).forEach { index ->
                    val isFilled = index < currentPinLength
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            // Error or Help message
            if (showError) {
                Text(
                    text = "❌ الرموز غير متطابقة! يرجى إعادة التأكيد مجدداً",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Keyboard Dial Pad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { button ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (button.isNotBlank()) {
                                    if (button == "back") {
                                        IconButton(
                                            onClick = onBackspace,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "تراجع",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .clickable { onDigitClick(button) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = button,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Option to skip / dismiss setup
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("تخطي للرئيسية بدون قفل حالياً 🔓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Extension to safely load default helper without crashing on null custom values
private fun ColorScheme.scaffoldBackgroundColorKeyboardHelper(): Color? {
    return try {
        this.background
    } catch(e: Exception) {
        null
    }
}

@Composable
fun FrozenBlockedScreen(clientId: String, onGoToSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Text(
                    text = "عذراً، هذا الحساب مجمد! 🛑",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "لقد تم تجميد أو تعليق ترخيص هذا الجهاز (المعرّف: $clientId) بواسطة المالك العام للنظام في لوحة التحكم الإدارية للويندوز.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Text(
                    text = "الرجاء التواصل مع المطور/المشرف لإعادة تفعيل حسابك ومتابعة عملياتك المالية اليومية:\nanas774928318@gmail.com",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onGoToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتقل للإعدادات لإعادة مزامنة التفعيل 💳", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

