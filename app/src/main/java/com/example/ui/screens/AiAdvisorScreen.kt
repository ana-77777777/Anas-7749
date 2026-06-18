package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GeminiRepository
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.ui.platform.LocalContext


// Simple local message representation
data class Message(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(viewModel: LedgerViewModel) {
    // Collect financial states for AI context
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    // Gemini Repository
    val geminiRepository = remember { GeminiRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                textInput = "تحليل الصورة الملتقطة..."
                Toast.makeText(context, "تم التقاط الصورة، جاري التحليل...", Toast.LENGTH_SHORT).show()
                // TODO: Handle bitmap (send to Gemini/process)
            }
        }
    )

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                textInput = "تحليل الملف: $uri"
                Toast.makeText(context, "جاري استيراد الملف...", Toast.LENGTH_SHORT).show()
                // TODO: Handle uri (read Excel/PDF)
            }
        }
    )

    // Offline-by-default state matching the user's intent to keep AI local
    var isOfflineMode by remember { mutableStateOf(true) }

    // Chat history state
    var messages by remember(isOfflineMode) {
        mutableStateOf(
            listOf(
                Message(
                    id = "welcome",
                    text = if (isOfflineMode) {
                        "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **الذكاء المالي المحلي (بدون إنترنت 📴)** لتحليل كامل السجلات والحسابات والديون المحفوظة محلياً على هاتفك فوراً ومجاناً وبمنتهى الخصوصية!\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                    } else {
                        "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **طاقة التفكير الفائق السحابي (gemini-3.1-pro-preview)** لمساندتك في اتخاذ أصعب القرارات وصياغة تقارير بالغة التعقيد.\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                    },
                    isUser = false
                )
            )
        )
    }

    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Suggested queries dynamically loaded from viewmodel/shared preferences
    val suggestionChips by viewModel.aiSuggestions.collectAsState()

    // Dialog state for editing AI topics
    var showManageTopicsDialog by remember { mutableStateOf(false) }
    var topicToEditIndex by remember { mutableStateOf<Int?>(null) }
    var topicEditText by remember { mutableStateOf("") }
    var newTopicText by remember { mutableStateOf("") }

    // Scroll to bottom when new messages show up
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val onSendMessage: (String) -> Unit = { prompt ->
        if (prompt.isNotBlank()) {
            errorMessage = null
            val userMsg = Message(id = java.util.UUID.randomUUID().toString(), text = prompt, isUser = true)
            messages = messages + userMsg
            textInput = ""
            isLoading = true

            scope.launch {
                val result = geminiRepository.generateDeepThinkingResponse(
                    prompt = prompt,
                    businessName = businessName,
                    businessPhone = businessPhone,
                    businessAddress = businessAddress,
                    accounts = accounts,
                    transactions = transactions,
                    defaultCurrency = defaultCurrency,
                    isOfflineMode = isOfflineMode
                )

                result.onSuccess { reply ->
                    val aiMsg = Message(id = java.util.UUID.randomUUID().toString(), text = reply, isUser = false)
                    messages = messages + aiMsg
                }.onFailure { error ->
                    errorMessage = error.localizedMessage
                    val errorMsg = Message(
                        id = java.util.UUID.randomUUID().toString(),
                        text = "⚠️ عذرًا، حدث خطأ أثناء تشغيل وضع التفكير المتقدم:\n${error.localizedMessage}",
                        isUser = false
                    )
                    messages = messages + errorMsg
                }
                isLoading = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Info Bar displaying current configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOfflineMode) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isOfflineMode) "وضع التحليل الذاتي الآمن نشط" else "تمكين التفكير الفائق نشط",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOfflineMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isOfflineMode) "يعمل بالكامل بدون اتصال بالإنترنت 100%" else "مستند لنموذج: gemini-3.1-pro-preview",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reset conversation button
                    IconButton(
                        onClick = {
                            messages = listOf(
                                Message(
                                    id = "welcome",
                                    text = if (isOfflineMode) {
                                        "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **الذكاء المالي المحلي (بدون إنترنت 📴)** لتحليل كامل السجلات والحسابات والديون المحفوظة محلياً على هاتفك فوراً ومجاناً وبمنتهى الخصوصية!\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                                    } else {
                                        "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **طاقة التفكير الفائق السحابي (gemini-3.1-pro-preview)** لمساندتك في اتخاذ أصعب القرارات وصياءة تقارير بالغة التعقيد.\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                                    },
                                    isUser = false
                                )
                            )
                            errorMessage = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "مسح المحادثة",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Chat Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }

                if (isLoading) {
                    item {
                        ThinkingIndicator(isOffline = isOfflineMode)
                    }
                }
            }

            // Customize AI Suggested topics row and button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { showManageTopicsDialog = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تعديل المواضيع",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تعديل وتخصيص الأسئلة والمواضيع الذكية 🔧",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "أسئلة مقترحة سريعة:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }

            // Suggestion Chips list
            ScrollableSuggestionRow(
                chips = suggestionChips,
                onChipClick = { onSendMessage(it) },
                enabled = !isLoading
            )

            if (showManageTopicsDialog) {
                AlertDialog(
                    onDismissRequest = { showManageTopicsDialog = false },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.resetAiSuggestions() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("إعادة ضبط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("تعديل وتخصيص أسئلة الذكاء", fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "يمكنك إضافة، تعديل، أو حذف الأسئلة والمواضيع المفتاحية السريعة التي تظهر في شريط الاقتراحات للذكاء الاصطناعي لتسهيل العمل اليومي.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Add new topic form
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (newTopicText.isNotBlank()) {
                                            viewModel.addAiSuggestion(newTopicText.trim())
                                            newTopicText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "إضافة")
                                }
                                OutlinedTextField(
                                    value = newTopicText,
                                    onValueChange = { newTopicText = it },
                                    label = { Text("إضافة سؤال/موضوع مقترح جديد...") },
                                    placeholder = { Text("مثال: كم رصيد صندوق المحل اليوم؟") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = TextAlign.Right)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // List of current suggestion chips
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(suggestionChips.size) { index ->
                                    val chipText = suggestionChips.getOrNull(index) ?: ""
                                    if (topicToEditIndex == index) {
                                        // Edit Mode row for this chip
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (topicEditText.isNotBlank()) {
                                                        viewModel.updateAiSuggestion(index, topicEditText.trim())
                                                        topicToEditIndex = null
                                                        topicEditText = ""
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "حفظ",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            OutlinedTextField(
                                                value = topicEditText,
                                                onValueChange = { topicEditText = it },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Right)
                                            )
                                            IconButton(
                                                onClick = {
                                                    topicToEditIndex = null
                                                    topicEditText = ""
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "إلغاء",
                                                    tint = Color.Gray
                                                )
                                            }
                                        }
                                    } else {
                                        // Regular Mode row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        topicToEditIndex = index
                                                        topicEditText = chipText
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteAiSuggestion(index) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = chipText,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showManageTopicsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("إغلاق وتطبيق", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Input Row at the bottom
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Buttons (Camera/File)
                    IconButton(onClick = { cameraLauncher.launch(null) }) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "كاميرا", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { fileLauncher.launch("application/*") }) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = "ملف", tint = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(if (isOfflineMode) "اطرح سؤالك أو ارفق صورة/فاتورة..." else "اطرح موضوعك...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    FloatingActionButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onSendMessage(textInput)
                            }
                        },
                        containerColor = if (textInput.isNotBlank()) {
                            if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (textInput.isNotBlank()) {
                            if (isOfflineMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
         MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Surface(
                color = containerColor,
                shape = bubbleShape,
                border = if (!message.isUser) androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
                tonalElevation = if (message.isUser) 0.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = formatMarkdown(message.text),
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(isOffline: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isOffline) "جاري استعلام السجلات وتوليد التحليل بأمان..." else "جاري استحضار نموذج التفكير العميق لـ Gemini...",
                    fontSize = 12.sp,
                    color = if (isOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScrollableSuggestionRow(
    chips: List<String>,
    onChipClick: (String) -> Unit,
    enabled: Boolean
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(chips) { text ->
            SuggestionChip(
                onClick = { if (enabled) onChipClick(text) },
                label = { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    labelColor = MaterialTheme.colorScheme.primary
                ),
                border = null,
                enabled = enabled
            )
        }
    }
}

// Keep asterisks for list bullets and structure styling, but clean up double asterisks in bold
private fun formatMarkdown(input: String): String {
    return input.replace("**", "")
}
