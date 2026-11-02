package com.perceptionparadox.oneura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import kotlinx.coroutines.launch
import com.perceptionparadox.oneura.data.ai.GeminiCloudProvider
import com.perceptionparadox.oneura.data.ai.MediaPipeLocalProvider
import com.perceptionparadox.oneura.data.ai.OllamaProvider
import com.perceptionparadox.oneura.data.local.OneuraDatabase
import com.perceptionparadox.oneura.data.local.entity.ChatMessageEntity
import com.perceptionparadox.oneura.data.local.entity.DreamEntity
import com.perceptionparadox.oneura.data.pref.SettingsRepository
import com.perceptionparadox.oneura.data.remote.InternalLogger
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

                val cognitiveRepository = remember { 
                    CognitiveRepository(db.cognitiveDao()) {
                        val currentProvider = settingsViewModel.aiProvider.value
                        when (currentProvider) {
                            "GEMINI" -> GeminiCloudProvider(settingsViewModel.geminiApiKey.value)
                            "MEDIAPIPE" -> MediaPipeLocalProvider(context, "/data/user/0/com.perceptionparadox.oneura/files/model.bin")
                            else -> OllamaProvider(settingsViewModel.modelName.value)
                        }
                    } 
                }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    chatViewModel: ChatViewModel, 
    brainViewModel: BrainViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "Reflections", "Dreams", "Settings", "Logs")
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "ONEURA", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ) 
                },
                actions = {
                    if (currentTab == 0) {
                        IconButton(onClick = { chatViewModel.clearChat() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
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
                                    2 -> if (currentTab == 2) Icons.Default.NightsStay else Icons.Outlined.NightsStay
                                    3 -> if (currentTab == 3) Icons.Default.Settings else Icons.Outlined.Settings
                                    else -> if (currentTab == 4) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List
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
                    2 -> DreamScreen(brainViewModel)
                    3 -> SettingsScreen(settingsViewModel)
                    4 -> LogScreen()
                }
            }
        }
    }
}

// ---- Dreams tab: ported from the Antigravity copy (Jul 24) ----

@Composable
fun DreamScreen(viewModel: BrainViewModel) {
    val dreams by viewModel.dreams.collectAsState()
    val isDreaming by viewModel.isDreaming.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "dream_brain")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF8A2BE2).copy(alpha = 0.6f),
                                Color(0xFF00FFFF).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Button(
                onClick = { viewModel.dream() },
                enabled = !isDreaming,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NightsStay, contentDescription = null)
                    Text(if (isDreaming) "Dreaming..." else "Consolidate Dream", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (dreams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Oneura is awake. No dreams consolidated yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(dreams) { dream ->
                    DreamCard(dream)
                }
            }
        }
    }
}

@Composable
fun DreamCard(dream: DreamEntity) {
    val stageColor = when (dream.sleepStage.uppercase()) {
        "LIGHT" -> Color(0xFF00E5FF)
        "DEEP" -> Color(0xFF7C4DFF)
        "REM" -> Color(0xFFFF4081)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                DreamStageVisual(dream.sleepStage)
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
                
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(stageColor, shape = RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = dream.sleepStage,
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val sdf = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()) }
                Text(
                    text = sdf.format(Date(dream.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (dream.sleepStage.uppercase() == "DEEP") {
                    Text(
                        text = "Cognitive Vectors",
                        style = MaterialTheme.typography.labelLarge,
                        color = stageColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // TODO: these metric values are hard-coded placeholders from the Antigravity copy,
                        // not derived from the dream itself. Wire to real data or remove.
                        CognitiveMetricBar("Engagement Coefficient", 0.87f, stageColor)
                        CognitiveMetricBar("Motor-Facade Sync", 0.94f, stageColor)
                        CognitiveMetricBar("Arboreal Meanders", 0.41f, stageColor)
                        CognitiveMetricBar("Spatial Phenomena", 0.76f, stageColor)
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                }

                Text(
                    text = "Memory Consolidation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dream.memoryConsolidationSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Personality Insight Evolved",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF00FFCC),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dream.personalityInsightEvolved,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DreamStageVisual(stage: String) {
    // The Antigravity copy resolved these by name at runtime inside try/catch. try/catch around
    // composable calls doesn't compile, and the drawables now ship with the app, so use R directly.
    // TODO: no dedicated REM artwork exists yet; REM reuses dream_deep (same as the Antigravity copy).
    val resId = when (stage.uppercase()) {
        "LIGHT" -> R.drawable.dream_light
        "DEEP", "REM" -> R.drawable.dream_deep
        else -> null
    }
    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Dream State: $stage",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        DreamVisualFallback(stage)
    }
}

@Composable
fun DreamVisualFallback(stage: String) {
    val gradientColors = when (stage.uppercase()) {
        "LIGHT" -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
        "DEEP" -> listOf(Color(0xFF8A2BE2), Color(0xFF4B0082))
        else -> listOf(Color(0xFFFF007F), Color(0xFF7F00FF))
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Stage: $stage",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun CognitiveMetricBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color.Gray.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val timeout by viewModel.timeoutSeconds.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Brain Source", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = aiProvider == "GEMINI",
                onClick = { viewModel.setAiProvider("GEMINI") },
                label = { Text("Cloud") }
            )
            FilterChip(
                selected = aiProvider == "MEDIAPIPE",
                onClick = { viewModel.setAiProvider("MEDIAPIPE") },
                label = { Text("Device") }
            )
            FilterChip(
                selected = aiProvider == "OLLAMA",
                onClick = { viewModel.setAiProvider("OLLAMA") },
                label = { Text("Server") }
            )
        }

        if (aiProvider == "GEMINI") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cloud Setup", fontWeight = FontWeight.Bold)
                    Text("Paste your key below. Oneura will automatically trim any extra spaces.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { viewModel.setGeminiKey(it) },
                        label = { Text(if (geminiKey.isNotEmpty()) "Key Stored (${geminiKey.length} chars)" else "Paste Gemini API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        trailingIcon = {
                            if (geminiKey.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setGeminiKey("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Key")
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    var testResult by remember { mutableStateOf("") }
                    var testing by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    
                    Button(
                        onClick = {
                            scope.launch {
                                testing = true
                                testResult = "Testing Brain..."
                                val provider = GeminiCloudProvider(geminiKey)
                                val result = provider.generateResponse("Ping", "Say 'Online'")
                                testResult = result.fold(
                                    onSuccess = { "Cloud Brain: ONLINE ($it)" },
                                    onFailure = { it.message ?: "Failed" }
                                )
                                testing = false
                            }
                        },
                        enabled = geminiKey.isNotEmpty() && !testing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (testing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Test Cloud Brain")
                    }
                    if (testResult.isNotEmpty()) {
                        Text(
                            text = testResult,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (testResult.contains("ONLINE")) Color.Green else Color.Red,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        if (aiProvider == "OLLAMA") {
            Button(
                onClick = { viewModel.runSmartDiscovery() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning Network...")
                } else {
                    Icon(Icons.Default.Wifi, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-Detect Ollama")
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { viewModel.setBaseUrl(it.replace("\n", "").replace("\r", "")) },
                label = { Text("Base URL") },
                placeholder = { Text("http://127.0.0.1:11434") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { viewModel.setModelName(it.replace("\n", "").replace("\r", "")) },
                label = { Text("Model Name") },
                placeholder = { Text("tinyllama") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (aiProvider == "MEDIAPIPE") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("On-Device AI", fontWeight = FontWeight.Bold)
                    Text("Private and Offline. Requires a high-end device.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { /* TODO: Implement download */ }) {
                        Text("Download Brain (1.2GB)")
                    }
                }
            }
        }

        Text("Connection Timeout: ${timeout}s", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = timeout.toFloat(),
            onValueChange = { viewModel.setTimeout(it.toLong()) },
            valueRange = 10f..300f,
            steps = 29
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))
        
        Text("Quick Presets", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.setBaseUrl("http://127.0.0.1:8000/") }, modifier = Modifier.weight(1f)) { 
                Text("Termux") 
            }
            Button(onClick = { viewModel.setBaseUrl("http://10.0.2.2:11434/") }, modifier = Modifier.weight(1f)) { 
                Text("Emulator") 
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.setModelName("tinyllama") }, modifier = Modifier.weight(1f)) { 
                Text("TinyLlama") 
            }
            OutlinedButton(onClick = { viewModel.setModelName("llama3.2:1b") }, modifier = Modifier.weight(1f)) { 
                Text("Llama 3.2") 
            }
            OutlinedButton(onClick = { viewModel.setModelName("phi3:mini") }, modifier = Modifier.weight(1f)) { 
                Text("Phi-3")
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("System Info", fontWeight = FontWeight.Bold)
                if (aiProvider == "GEMINI") {
                    Text("Brain: Gemini 1.5 Flash (Cloud)", style = MaterialTheme.typography.bodySmall)
                    Text("API Key: ${if(geminiKey.length > 5) geminiKey.take(4) + "..." + geminiKey.takeLast(4) else "Missing"}", style = MaterialTheme.typography.bodySmall)
                } else if (aiProvider == "OLLAMA") {
                    Text("Brain: Local Server ($modelName)", style = MaterialTheme.typography.bodySmall)
                    Text("Address: $baseUrl", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Brain: On-Device (Private)", style = MaterialTheme.typography.bodySmall)
                }
                Text("Status: ${if (isScanning) "Scanning..." else "Standby"}", style = MaterialTheme.typography.bodySmall, color = if (isScanning) Color.Cyan else Color.Green)
            }
        }
    }
}

@Composable
fun LogScreen() {
    val logs = InternalLogger.logs
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Internal Diagnostics", 
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { InternalLogger.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color.Gray)
            }
        }
        
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
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.3f))
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
