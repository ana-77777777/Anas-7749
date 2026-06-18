package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LedgerViewModel
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTrash: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Retrieve state flows from view model
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    val isSecurityEnabled by viewModel.isSecurityEnabled.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    val cloudClientId by viewModel.cloudClientId.collectAsState()
    val cloudServerUrl by viewModel.cloudServerUrl.collectAsState()
    val isCloudFrozen by viewModel.isCloudFrozen.collectAsState()
    val lastCloudSync by viewModel.lastCloudSync.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()

    // PDF States
    val pdfLogo by viewModel.pdfLogo.collectAsState()
    val pdfHeaderShowLogo by viewModel.pdfHeaderShowLogo.collectAsState()
    val pdfHeaderCustomTitle by viewModel.pdfHeaderCustomTitle.collectAsState()
    val pdfHeaderCustomSubtitle by viewModel.pdfHeaderCustomSubtitle.collectAsState()
    val pdfFooterCustomText by viewModel.pdfFooterCustomText.collectAsState()
    val pdfShowSignature by viewModel.pdfShowSignature.collectAsState()
    val pdfFontStyle by viewModel.pdfFontStyle.collectAsState()
    val pdfFontSize by viewModel.pdfFontSize.collectAsState()
    val pdfThemeColor by viewModel.pdfThemeColor.collectAsState()
    val pdfColDetailsLabel by viewModel.pdfColDetailsLabel.collectAsState()
    val pdfColQtyVisible by viewModel.pdfColQtyVisible.collectAsState()
    val pdfColQtyLabel by viewModel.pdfColQtyLabel.collectAsState()
    val pdfColPriceVisible by viewModel.pdfColPriceVisible.collectAsState()
    val pdfColPriceLabel by viewModel.pdfColPriceLabel.collectAsState()
    val pdfColAdditionVisible by viewModel.pdfColAdditionVisible.collectAsState()
    val pdfColAdditionLabel by viewModel.pdfColAdditionLabel.collectAsState()
    val pdfColTotalVisible by viewModel.pdfColTotalVisible.collectAsState()
    val pdfColTotalLabel by viewModel.pdfColTotalLabel.collectAsState()

    // Local inputs initialized from viewmodel values
    var tempBizName by remember { mutableStateOf(businessName) }
    var tempBizPhone by remember { mutableStateOf(businessPhone) }
    var tempBizAddress by remember { mutableStateOf(businessAddress) }
    var tempCurrency by remember { mutableStateOf(defaultCurrency) }

    // Security PIN Inputs
    var tempPin by remember { mutableStateOf(securityPin) }
    var tempPinEnabled by remember { mutableStateOf(isSecurityEnabled) }

    // Cloud configuration inputs
    var tempClientId by remember { mutableStateOf(cloudClientId) }
    var tempServerUrl by remember { mutableStateOf(cloudServerUrl) }

    // PDF Configuration States
    var tempPdfLogo by remember { mutableStateOf(pdfLogo) }
    var tempShowLogo by remember { mutableStateOf(pdfHeaderShowLogo) }
    var tempCustomTitle by remember { mutableStateOf(pdfHeaderCustomTitle) }
    var tempCustomSubtitle by remember { mutableStateOf(pdfHeaderCustomSubtitle) }
    var tempCustomFooter by remember { mutableStateOf(pdfFooterCustomText) }
    var tempShowSignature by remember { mutableStateOf(pdfShowSignature) }
    var tempFontStyle by remember { mutableStateOf(pdfFontStyle) }
    var tempFontSize by remember { mutableStateOf(pdfFontSize) }
    var tempPdfColor by remember { mutableStateOf(pdfThemeColor) }
    var tempColDetails by remember { mutableStateOf(pdfColDetailsLabel) }
    var tempColQtyVisible by remember { mutableStateOf(pdfColQtyVisible) }
    var tempColQtyLabel by remember { mutableStateOf(pdfColQtyLabel) }
    var tempColPriceVisible by remember { mutableStateOf(pdfColPriceVisible) }
    var tempColPriceLabel by remember { mutableStateOf(pdfColPriceLabel) }
    var tempColAddVisible by remember { mutableStateOf(pdfColAdditionVisible) }
    var tempColAddLabel by remember { mutableStateOf(pdfColAdditionLabel) }
    var tempColTotalVisible by remember { mutableStateOf(pdfColTotalVisible) }
    var tempColTotalLabel by remember { mutableStateOf(pdfColTotalLabel) }

    // Synchronize inputs when Flow emits updates
    LaunchedEffect(businessName, businessPhone, businessAddress, defaultCurrency, securityPin, isSecurityEnabled) {
        tempBizName = businessName
        tempBizPhone = businessPhone
        tempBizAddress = businessAddress
        tempCurrency = defaultCurrency
        tempPin = securityPin
        tempPinEnabled = isSecurityEnabled
    }

    LaunchedEffect(cloudClientId, cloudServerUrl) {
        tempClientId = cloudClientId
        tempServerUrl = cloudServerUrl
    }

    // Modal & Toast Simulation Signals
    var saveStatusMsg by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.End
    ) {
        // App settings title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إعدادات النظام والمنشأة ⚙️",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- SECTION 1: BUSINESS PROFILE SETTINGS ---
        SettingsCard(
            title = "هوية المنشأة والمسؤول 🏢",
            icon = Icons.Default.Business,
            description = "حدد اسم المحل التجاري أو المنشأة وبيانات التواصل التي ستظهر في أعلى التقارير وكشوفات الحساب الرسمية الصادرة."
        ) {
            OutlinedTextField(
                value = tempBizName,
                onValueChange = { tempBizName = it },
                label = { Text("اسم المحل أو المنشأة التجارية") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = tempBizPhone,
                onValueChange = { tempBizPhone = it },
                label = { Text("هاتف التواصل أو رقم الواتساب") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = tempBizAddress,
                onValueChange = { tempBizAddress = it },
                label = { Text("العنوان الرئيسي للمكتب/المحل") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            Button(
                onClick = {
                    viewModel.updateBusinessProfile(tempBizName, tempBizPhone, tempBizAddress)
                    saveStatusMsg = "تم حفظ بيانات المنشأة التجارية بنجاح! سيتم تطبيق التغييرات على ترويسة التقارير وصياغات البث الفورية."
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ بيانات المنشأة 💾", fontWeight = FontWeight.Bold)
            }
        }

        // --- SECTION 2.5: TRASH Management ---
        SettingsCard(
            title = "سلة المحذوفات المسترجعة 🗑️",
            icon = Icons.Default.Delete,
            description = "يمكنك إدارة العناصر المحذوفة، استعادتها أو حذفها نهائياً من هنا."
        ) {
            Button(
                onClick = onNavigateToTrash,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("فتح سلة المحذوفات 🧺", fontWeight = FontWeight.Bold)
            }
        }
        
        // --- SECTION 2: MULTI-CURRENCY SETTINGS ---
        SettingsCard(
            title = "العملة والتحويل المالي 💵",
            icon = Icons.Default.AttachMoney,
            description = "اختر العملة الافتراضية المناسبة لدفتر العملات، وحدد سعر صرف العملات الأجنبية مقابل الريال اليمني للتعامل بمرونة وسهولة."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("العملة الافتراضية لدفتر الحسابات:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        Text(curr, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal)
                    }
                }
            }

            var usdRate by remember { mutableStateOf(viewModel.getDefaultExchangeRate("USD", "YER")) }
            var sarRate by remember { mutableStateOf(viewModel.getDefaultExchangeRate("SAR", "YER")) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sarRate.toString(),
                    onValueChange = { doubleVal -> sarRate = doubleVal.toDoubleOrNull() ?: 0.0 },
                    label = { Text("سعر الرياضي (SAR مقابل YER)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = usdRate.toString(),
                    onValueChange = { doubleVal -> usdRate = doubleVal.toDoubleOrNull() ?: 0.0 },
                    label = { Text("سعر الدولار (USD مقابل YER)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.updateStandardRates(usdRate, sarRate)
                    saveStatusMsg = "تم تحديث أسعار صرف العملات القياسية بنجاح! سيتم تعديل واجهة الآلة وقيم الفواتير أوتوماتيكياً."
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("تعديل أسعار الصرف 💰", fontWeight = FontWeight.Bold)
            }
        }

        // --- SECTION 3: APP SECURITY & FINGERPRINT ---
        SettingsCard(
            title = "أمان التطبيق وقفل الشاشة 🔒",
            icon = Icons.Default.Lock,
            description = "قم بتنشيط ميزة الحماية برمز السري لمنع التسلل والعبث بحسابات المبيعات وسجلات الديون عند قفل الهاتف."
        ) {
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
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            Button(
                onClick = {
                    if (tempPinEnabled && tempPin.isBlank()) {
                        saveStatusMsg = "عذراً، يجب عليك تحديد رمز السري PIN لتتمكن من تفعيل الحظر الأمني المتقدم للتطبيق."
                        showDialog = true
                    } else {
                        viewModel.updateSecuritySettings(tempPinEnabled, tempPin)
                        saveStatusMsg = if (tempPinEnabled) "تم ضبط الرمز السري بنجاح وتفعيل القفل الأمني التلقائي!" else "تم إلغاء الحظر الأمني والتأمين بنجاح!"
                        showDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ إعدادات الأمان والتشفير 🔑", fontWeight = FontWeight.Bold)
            }

            // Biometrics / Fingerprint switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.updateBiometricSettings(isChecked)
                    }
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isBiometricEnabled) "حماية البصمة: مفعّلة 🔐" else "حماية البصمة: معطّلة 🔓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBiometricEnabled) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يطلب التطبيق بصمتك للتحقق الفوري",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- SECTION 4: CLOUD ADMIN & SYNC ---
        SettingsCard(
            title = "المزامنة السحابية وإعدادات الويندوز المدير ☁️",
            icon = Icons.Default.CloudSync,
            description = "أدخل الكود الموحد لمزامنة كامل بياناتك وحساباتك للعمل أونلاين وأوفلاين مع سطح المكتب وإشعار الأجهزة التابعة."
        ) {
            OutlinedTextField(
                value = tempClientId,
                onValueChange = { tempClientId = it },
                label = { Text("معرف العميل (Client Identifier)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = tempServerUrl,
                onValueChange = { tempServerUrl = it },
                label = { Text("عنوان خادم المزامنة (Cloud URL)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isCloudFrozen,
                    onCheckedChange = { isChecked ->
                        viewModel.setCloudFrozen(isChecked)
                    }
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("تجميد حساب المزامنة وتأمين الحذف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("يمنع الكتابة والتدمير الخلفي للبيانات", fontSize = 9.sp, color = Color.Gray)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateCloudSettings(tempClientId, tempServerUrl)
                        saveStatusMsg = "تم تحديث إعدادات الخادم والعميل بنجاح! يمكنك الآن تجربة الاتصال المباشر بجهاز الكمبيوتر وأعمال السحابة."
                        showDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("حفظ الضبط ⚙️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.syncWithCloud { success, msg ->
                            saveStatusMsg = if (success) "مزامنة ناجحة! 👉 $msg" else "خطأ بالربط: $msg"
                            showDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCloudSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isCloudSyncing) "جاري الإرسال..." else "بدء المزامنة فورا ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "آخر مزامنة سحابية مادية: $lastCloudSync",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- SECTION 5: PDF CUSTOMIZER SETTINGS ---
        SettingsCard(
            title = "مصمم قوالب وتنسيق كشوفات PDF 📄",
            icon = Icons.Default.PictureAsPdf,
            description = "تحكم بشكل وألوان وعواميد كشوفات الحساب المصدرة للزبائن ومندوبي الديون بما يناسب طبيعة المتجر التجاري الخاص بك."
        ) {
            OutlinedTextField(
                value = tempPdfLogo,
                onValueChange = { tempPdfLogo = it },
                label = { Text("أيقونة/لوغو التقارير (مثال: 🏢)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = tempShowLogo, onCheckedChange = { tempShowLogo = it })
                Text("إظهار أيقونة المنشأة الحسابية", fontSize = 12.sp)
            }

            OutlinedTextField(
                value = tempCustomTitle,
                onValueChange = { tempCustomTitle = it },
                label = { Text("عنوان الترويسة المخصص") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = tempCustomSubtitle,
                onValueChange = { tempCustomSubtitle = it },
                label = { Text("العنوان الفرعي المخصص") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = tempCustomFooter,
                onValueChange = { tempCustomFooter = it },
                label = { Text("تذييل وملحوظة أسفل الفاتورة") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = tempShowSignature, onCheckedChange = { tempShowSignature = it })
                Text("إظهار توقيع المسؤول في الكشف المطبوع", fontSize = 12.sp)
            }

            OutlinedTextField(
                value = tempColDetails,
                onValueChange = { tempColDetails = it },
                label = { Text("اسم كولوم الملاحظات والبيان") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Button(
                onClick = {
                    viewModel.updatePdfTemplateSettings(
                        logo = tempPdfLogo,
                        showLogo = tempShowLogo,
                        customTitle = tempCustomTitle,
                        customSubtitle = tempCustomSubtitle,
                        customFooter = tempCustomFooter,
                        showSignature = tempShowSignature,
                        fontStyle = tempFontStyle,
                        fontSize = tempFontSize,
                        themeColor = tempPdfColor,
                        colDetailsLabel = tempColDetails,
                        colQtyVisible = tempColQtyVisible,
                        colQtyLabel = tempColQtyLabel,
                        colPriceVisible = tempColPriceVisible,
                        colPriceLabel = tempColPriceLabel,
                        colAdditionVisible = tempColAddVisible,
                        colAdditionLabel = tempColAddLabel,
                        colTotalVisible = tempColTotalVisible,
                        colTotalLabel = tempColTotalLabel
                    )
                    saveStatusMsg = "تم حفظ وتعديل كافة قوالب وكشوفات الـ PDF بنجاح! سيتم تطبيق الأسلوب المختار مع المشاركة الفورية."
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("تطبيق وحفظ قالب التقارير 📄", fontWeight = FontWeight.Bold)
            }
        }

        // --- SECTION 6: ARABIC/YEMENI MESSAGES GENERATOR ---
        SettingsCard(
            title = "نماذج صياغة الرسائل وكشوفات المطالبة ✉️",
            icon = Icons.Default.Feedback,
            description = "تحقق من النماذج المحاسبية المتناسقة للاستخدام أثناء حركات التذكير بقيمة المديونات ومشاركتها بضغطة زر على منصات المحادثة."
        ) {
            val list = viewModel.getYemeniMessageTemplates("أحمد صالح", 45000.0, defaultCurrency)
            list.forEachIndexed { i, template ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                        .padding(10.dp)
                ) {
                    Text("الصياغة النموذجية الرائجة #${i + 1}:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(template, fontSize = 11.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // --- SECTION 7: FISCAL YEAR CLOSING ---
        var showClosingSuccessDialog by remember { mutableStateOf(false) }
        var closingResultMsg by remember { mutableStateOf("") }
        var selectedClosingDate by remember { mutableStateOf("2026-12-31") }

        SettingsCard(
            title = "الإقفال المباشر للدفاتر السنوية 📅",
            icon = Icons.Default.EventBusy,
            description = "يتيح تصفية حركات وصناديق الحساب بنهاية العام المالي لشركة أو مستثمر، والبدء بقاعدة بيانات جديدة مع ترحيل أرصدة إفتتاحية للمقبوضين بنقرة زر."
        ) {
            OutlinedTextField(
                value = selectedClosingDate,
                onValueChange = { selectedClosingDate = it },
                label = { Text("تاريخ الإقفال وترحيل الصناديق (سنة-شهر-يوم)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    viewModel.performYearlyClosing(selectedClosingDate) { resultMsg ->
                        closingResultMsg = resultMsg
                        showClosingSuccessDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("تنفيذ ترحيل الأرصدة وإقفال العام مصفراً ⚡", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (showClosingSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showClosingSuccessDialog = false },
                title = { Text("تحديث الإقفال السنوي والترصيد 📢", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = { Text(closingResultMsg, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    Button(onClick = { showClosingSuccessDialog = false }) {
                        Text("حسناً")
                    }
                }
            )
        }

        // Status Feedback Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("حالة التحديث والضبط 📢", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = { Text(saveStatusMsg, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                confirmButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("موافق")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Description
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Content fields/buttons
            content()
        }
    }
}
