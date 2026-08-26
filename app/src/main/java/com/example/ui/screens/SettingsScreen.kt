package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppSpacing
import com.example.ui.viewmodel.LedgerViewModel
import java.util.*

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    CATEGORIES("جميع الإعدادات", Icons.Default.Category),
    PROFILE("هوية المنشأة", Icons.Default.Business),
    SECURITY("الأمان والحماية", Icons.Default.Lock),
    CLOUD("المزامنة السحابية", Icons.Default.CloudSync),
    CURRENCY("العملات والصرف", Icons.Default.AttachMoney),
    PDF("قوالب الفواتير والـ PDF", Icons.Default.PictureAsPdf),
    TRASH("سلة المحذوفات", Icons.Default.Delete),
    ABOUT("حول التطبيق", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTrash: () -> Unit
) {
    var activeCategory by remember { mutableStateOf(SettingsCategory.CATEGORIES) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppSpacing.normal)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.normal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeCategory != SettingsCategory.CATEGORIES) {
                IconButton(onClick = { activeCategory = SettingsCategory.CATEGORIES }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع للقائمة")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = if (activeCategory == SettingsCategory.CATEGORIES) "إعدادات النظام والمنشأة ⚙️" else activeCategory.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right
            )
        }

        if (activeCategory == SettingsCategory.CATEGORIES) {
            // Categorized Overview Menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                SettingsMenuCard(
                    title = "هوية المنشأة والمسؤول 🏢",
                    subtitle = "اسم المحل والتواصل والعنوان الخارجي",
                    icon = Icons.Default.Business,
                    onClick = { activeCategory = SettingsCategory.PROFILE }
                )
                SettingsMenuCard(
                    title = "أمان التطبيق وقفل الـ PIN 🔒",
                    subtitle = "تفعيل القفل السري والبصمة لمنع العبث",
                    icon = Icons.Default.Lock,
                    onClick = { activeCategory = SettingsCategory.SECURITY }
                )
                SettingsMenuCard(
                    title = "المزامنة السحابية والويندوز ☁️",
                    subtitle = "ربط وتأمين خادم المزامنة والسحابة",
                    icon = Icons.Default.CloudSync,
                    onClick = { activeCategory = SettingsCategory.CLOUD }
                )
                SettingsMenuCard(
                    title = "العملات والتحويل المالي 💵",
                    subtitle = "ضبط العملة القياسية وأسعار الصرف",
                    icon = Icons.Default.AttachMoney,
                    onClick = { activeCategory = SettingsCategory.CURRENCY }
                )
                SettingsMenuCard(
                    title = "تصميم الفواتير والـ PDF 📄",
                    subtitle = "تأطير الهيدر والتوقيع والألوان واللوجو",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = { activeCategory = SettingsCategory.PDF }
                )
                SettingsMenuCard(
                    title = "سلة المحذوفات المسترجعة 🗑️",
                    subtitle = "استرجاع الحسابات والعمليات المحذوفة",
                    icon = Icons.Default.Delete,
                    onClick = onNavigateToTrash
                )
                SettingsMenuCard(
                    title = "حول التطبيق والنسخة ℹ️",
                    subtitle = "معلومات الإصدار والحقوق والترخيص",
                    icon = Icons.Default.Info,
                    onClick = { activeCategory = SettingsCategory.ABOUT }
                )
            }
        } else {
            // Category Detail Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (activeCategory) {
                    SettingsCategory.PROFILE -> ProfileSettingsSection(viewModel)
                    SettingsCategory.SECURITY -> SecuritySettingsSection(viewModel)
                    SettingsCategory.CLOUD -> CloudSettingsSection(viewModel)
                    SettingsCategory.CURRENCY -> CurrencySettingsSection(viewModel)
                    SettingsCategory.PDF -> PdfSettingsSection(viewModel)
                    SettingsCategory.ABOUT -> AboutSettingsSection()
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun SettingsMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsSection(viewModel: LedgerViewModel) {
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()

    var tempBizName by remember { mutableStateOf(businessName) }
    var tempBizPhone by remember { mutableStateOf(businessPhone) }
    var tempBizAddress by remember { mutableStateOf(businessAddress) }
    var showSavedMessage by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        OutlinedTextField(
            value = tempBizName,
            onValueChange = { tempBizName = it },
            label = { Text("اسم المحل أو المنشأة التجارية") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        OutlinedTextField(
            value = tempBizPhone,
            onValueChange = { tempBizPhone = it },
            label = { Text("هاتف التواصل أو رقم الواتساب") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        OutlinedTextField(
            value = tempBizAddress,
            onValueChange = { tempBizAddress = it },
            label = { Text("العنوان الرئيسي للمكتب/المحل") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        Button(
            onClick = {
                viewModel.updateBusinessProfile(tempBizName, tempBizPhone, tempBizAddress)
                showSavedMessage = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("حفظ بيانات المنشأة 💾", fontWeight = FontWeight.Bold)
        }
        if (showSavedMessage) {
            Text(
                text = "✅ تم حفظ بيانات المنشأة التجارية بنجاح!",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SecuritySettingsSection(viewModel: LedgerViewModel) {
    val isSecurityEnabled by viewModel.isSecurityEnabled.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var tempPinEnabled by remember { mutableStateOf(isSecurityEnabled) }
    var tempPin by remember { mutableStateOf(securityPin) }
    var msg by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = tempPinEnabled,
                onCheckedChange = { isChecked ->
                    tempPinEnabled = isChecked
                    if (!isChecked) {
                        viewModel.updateSecuritySettings(false, "")
                    }
                }
            )
            Text("تأمين التطبيق برمز PIN الخاص بك", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (tempPinEnabled) {
            OutlinedTextField(
                value = tempPin,
                onValueChange = { tempPin = it },
                label = { Text("رمز الـ PIN المكوّن من 4 أرقام") },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                singleLine = true
            )
        }

        Button(
            onClick = {
                if (tempPinEnabled && tempPin.isBlank()) {
                    msg = "يرجى تحديد رمز PIN المكون من 4 أرقام"
                } else {
                    viewModel.updateSecuritySettings(tempPinEnabled, tempPin)
                    msg = if (tempPinEnabled) "تم تفعيل القفل بنجاح" else "تم إيقاف القفل"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("حفظ إعدادات الأمان 🔑", fontWeight = FontWeight.Bold)
        }

        if (msg.isNotEmpty()) {
            Text(msg, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(AppSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = { viewModel.updateBiometricSettings(it) }
            )
            Text("تفعيل البصمة للفتح الفوري 🔐", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CloudSettingsSection(viewModel: LedgerViewModel) {
    val cloudClientId by viewModel.cloudClientId.collectAsState()
    val cloudServerUrl by viewModel.cloudServerUrl.collectAsState()
    val lastCloudSync by viewModel.lastCloudSync.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()

    var tempClientId by remember { mutableStateOf(cloudClientId) }
    var tempServerUrl by remember { mutableStateOf(cloudServerUrl) }
    var syncStatus by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        OutlinedTextField(
            value = tempClientId,
            onValueChange = { tempClientId = it },
            label = { Text("معرف العميل (Client ID)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        OutlinedTextField(
            value = tempServerUrl,
            onValueChange = { tempServerUrl = it },
            label = { Text("عنوان خادم المزامنة (Cloud URL)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )

        Button(
            onClick = {
                viewModel.updateCloudSettings(tempClientId, tempServerUrl)
                viewModel.syncWithCloud { success, result ->
                    syncStatus = result
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCloudSyncing,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isCloudSyncing) "جاري الإرسال..." else "حفظ ومزامنة فورية ⚡", fontWeight = FontWeight.Bold)
        }

        if (syncStatus.isNotEmpty()) {
            Text(syncStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Right)
        }

        Text("آخر مزامنة: $lastCloudSync", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Right)
    }
}

@Composable
fun CurrencySettingsSection(viewModel: LedgerViewModel) {
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    var tempCurrency by remember { mutableStateOf(defaultCurrency) }
    var usdRate by remember { mutableStateOf(viewModel.getDefaultExchangeRate("USD", "YER")) }
    var sarRate by remember { mutableStateOf(viewModel.getDefaultExchangeRate("SAR", "YER")) }
    var status by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        Text("العملة الافتراضية:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            listOf("YER", "USD", "SAR").forEach { curr ->
                val isSelected = tempCurrency == curr
                ElevatedButton(
                    onClick = {
                        tempCurrency = curr
                        viewModel.updateDefaultCurrency(curr)
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(curr, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        OutlinedTextField(
            value = usdRate.toString(),
            onValueChange = { usdRate = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("سعر صرف USD مقابل YER") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )
        OutlinedTextField(
            value = sarRate.toString(),
            onValueChange = { sarRate = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("سعر صرف SAR مقابل YER") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )

        Button(
            onClick = {
                viewModel.updateStandardRates(usdRate, sarRate)
                status = "تم تحديث أسعار الصرف بنجاح"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تحديث أسعار الصرف 💰", fontWeight = FontWeight.Bold)
        }

        if (status.isNotEmpty()) {
            Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PdfSettingsSection(viewModel: LedgerViewModel) {
    val pdfLogo by viewModel.pdfLogo.collectAsState()
    val pdfCustomTitle by viewModel.pdfHeaderCustomTitle.collectAsState()
    val pdfCustomFooter by viewModel.pdfFooterCustomText.collectAsState()

    var tempLogo by remember { mutableStateOf(pdfLogo) }
    var tempTitle by remember { mutableStateOf(pdfCustomTitle) }
    var tempFooter by remember { mutableStateOf(pdfCustomFooter) }
    var saved by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        OutlinedTextField(
            value = tempLogo,
            onValueChange = { tempLogo = it },
            label = { Text("لوجو/رمز الفاتورة (مثال: 🏢)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )
        OutlinedTextField(
            value = tempTitle,
            onValueChange = { tempTitle = it },
            label = { Text("عنوان الفاتورة المخصص") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )
        OutlinedTextField(
            value = tempFooter,
            onValueChange = { tempFooter = it },
            label = { Text("تذييل الفاتورة المخصص") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )

        Button(
            onClick = {
                viewModel.updatePdfTemplateSettings(
                    logo = tempLogo,
                    showLogo = true,
                    customTitle = tempTitle,
                    customSubtitle = "",
                    customFooter = tempFooter,
                    showSignature = true,
                    fontStyle = "DEFAULT",
                    fontSize = "MEDIUM",
                    themeColor = "SLATE",
                    colDetailsLabel = "التفاصيل والبيان",
                    colQtyVisible = true,
                    colQtyLabel = "الكمية",
                    colPriceVisible = true,
                    colPriceLabel = "السعر",
                    colAdditionVisible = true,
                    colAdditionLabel = "الإضافي",
                    colTotalVisible = true,
                    colTotalLabel = "الإجمالي"
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ قالب الفاتورة 📄", fontWeight = FontWeight.Bold)
        }

        if (saved) {
            Text("✅ تم حفظ قالب الفاتورة بنجاح", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AboutSettingsSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text("المحاسب anas برو • Pro Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Text("الإصدار: 1.0.0 (إنتاجي)", fontSize = 12.sp, color = Color.Gray)
        Text("جميع الحقوق محفوظة © 2026", fontSize = 11.sp, color = Color.Gray)
    }
}
