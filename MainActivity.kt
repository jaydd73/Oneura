package com.perceptionparadox.oneura

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.perceptionparadox.oneura.data.local.OneuraDatabase
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.pref.SettingsRepository
import com.perceptionparadox.oneura.data.remote.InternalLogger
import com.perceptionparadox.oneura.data.remote.NetworkModule
import com.perceptionparadox.oneura.data.repository.CognitiveRepository
import com.perceptionparadox.oneura.ui.theme.OneuraTheme
import com.perceptionparadox.oneura.viewmodel.BrainViewModel
import com.perceptionparadox.oneura.viewmodel.ChatViewModel
import com.perceptionparadox.oneura.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InternalLogger.log("SYS", "App Started")
        enableEdgeToEdge()
        setContent {
            OneuraTheme {
                val context = LocalContext.current
                
                val db = remember {
                    Room.databaseBuilder(context, OneuraDatabase::class.java, "oneura-db")
                        .fallbackToDestructiveMigration()
                        .build()
                }
                
                val settingsRepository = remember { SettingsRepository(context) }
                
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return SettingsViewModel(settingsRepository) as T
                        }
                    }
                )

                val api = NetworkModule.apiService
                val cognitiveRepository = remember { CognitiveRepository(db.cognitiveDao(), api) }

                val chatViewModel: ChatViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ChatViewModel(cognitiveRepository) as T
                        }
                    }
                )

                val brainViewModel: BrainViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return BrainViewModel(cognitiveRepository) as T
                        }
                    }
                )

                MainNavigation(chatViewModel, brainViewModel, settingsViewModel)
            }
        }
    }
}

@Composable
fun MainNavigation(
    chatViewModel: ChatViewModel, 
    brainViewModel: BrainViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "Reflections", "Settings", "Logs")
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> if (currentTab == 0) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline
                                    1 -> if (currentTab == 1) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb
                                    2 -> if (currentTab == 2) Icons.Default.Settings else Icons.Outlined.Settings
                                    else -> if (currentTab == 3) Icons.Default.List else Icons.Outlined.List
                                },
                                contentDescription = label
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> ChatScreen(chatViewModel)
                    1 -> ReflectionScreen(brainViewModel)
                    2 -> SettingsScreen(settingsViewModel)
                    3 -> LogScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val timeout by viewModel.timeoutSeconds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Network Configuration", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { viewModel.setBaseUrl(it) },
            label = { Text("Ollama / API Base URL") },
            placeholder = { Text("http://10.63.132.14:8000/") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = modelName,
            onValueChange = { viewModel.setModelName(it) },
            label = { Text("Model Name") },
            placeholder = { Text("phi3.5") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Timeout: ${timeout}s", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = timeout.toFloat(),
            onValueChange = { viewModel.setTimeout(it.toLong()) },
            valueRange = 10f..300f,
            steps = 29
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        
        Text("Quick Presets", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.setBaseUrl("http://127.0.0.1:8000/") }) { Text("Termux") }
            Button(onClick = { viewModel.setBaseUrl("http://10.0.2.2:11434/") }) { Text("Emulator") }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("Internal Status", color = Color.Gray)
        Text("Connected: ${if (baseUrl.isNotBlank()) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LogScreen() {
    val logs = InternalLogger.logs
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Internal Diagnostics", 
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            items(logs) { log ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row {
                        Text(
                            text = "[${sdf.format(Date(log.timestamp))}] ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = log.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = when(log.tag) {
                                "NET" -> Color.Cyan
                                "SYS" -> Color.Green
                                else -> Color.Yellow
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            ChatInput(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
    }
}

@Composable
fun ReflectionScreen(viewModel: BrainViewModel) {
    val reflections by viewModel.reflections.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            GlowingOrb()
            Button(
                onClick = { viewModel.reflect() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            ) {
                Text("Trigger Reflection")
            }
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(reflections) { reflection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Text(
                        text = reflection.content,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(color = MaterialTheme.colorScheme.primary, blurRadius = 2f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GlowingOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val size by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "size"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(100.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00E5FF).copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = size * 2
            ),
            radius = size * 2
        )
    }
}

@Composable
fun MessageBubble(entry: ChatMessageEntity) {
    val isUser = entry.sender == "USER"
    val isInner = !entry.isOuterVoice
    
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isInner -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isInner -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .widthIn(max = 300.dp)
                .background(
                    color = bgColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            if (isInner) {
                Text(
                    text = "Thought",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            Text(text = entry.content, color = textColor)
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Think about something...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(onClick = onSend, enabled = text.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
