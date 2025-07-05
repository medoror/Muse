# Large Text Support Implementation Plan

## Overview
Progressive implementation plan to safely increase text input limits in the Muse TTS app, starting with 25,000 characters and including efficiency measures, chunking, and background processing.

## Current State
- **Text Input**: Uses `BasicTextField` that fills entire screen but isn't scrollable
- **Hard Limit**: `MAX_TEXT_LENGTH = 10_000` in `MuseRepo.kt:65`
- **Original Issue**: Comment indicates "large text loads slowly, can cause ANR"
- **Processing**: All text operations happen on main thread

## Progressive Implementation Strategy

### Phase 1: Efficient 25k Implementation (Week 1)
**Goal**: Increase to 25k characters with proper background processing and chunking

#### 1.1 Update Character Limit with Background Processing
- [ ] **File**: `composeApp/src/androidMain/kotlin/io/github/kkoshin/muse/repo/MuseRepo.kt`
  - Change line 65: `const val MAX_TEXT_LENGTH = 25_000`
  - Add background text processing methods

```kotlin
const val MAX_TEXT_LENGTH = 25_000
private const val CHUNK_SIZE = 10_000 // Process in 10k chunks

suspend fun processTextAsync(text: String): List<String> = withContext(Dispatchers.Default) {
    if (text.length <= CHUNK_SIZE) {
        // Small text - process directly
        text.split(Regex("\\s+")).filter { it.isNotBlank() }
    } else {
        // Large text - process in chunks
        text.chunked(CHUNK_SIZE).flatMap { chunk ->
            yield() // Prevent blocking
            chunk.split(Regex("\\s+")).filter { it.isNotBlank() }
        }
    }
}

suspend fun getTextStats(text: String): TextStats = withContext(Dispatchers.Default) {
    val lines = text.lines()
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    
    TextStats(
        characters = text.length,
        words = words.size,
        lines = lines.size,
        estimatedProcessingTime = estimateProcessingTime(words.size)
    )
}

data class TextStats(
    val characters: Int,
    val words: Int,
    val lines: Int,
    val estimatedProcessingTime: Int // in seconds
)
```

#### 1.2 Add Chunked Scrollable Text Input
- [ ] **File**: `composeApp/src/androidMain/kotlin/io/github/kkoshin/muse/dashboard/ScriptCreatorScreen.kt`
  - Implement chunked text display for large texts
  - Add background text statistics calculation
  - Smart text processing on text changes

```kotlin
@Composable
fun ScriptCreatorScreen(
    modifier: Modifier = Modifier,
    script: Script? = null,
    onResult: (scriptId: UUID?) -> Unit,
) {
    var content by remember { mutableStateOf(script?.text ?: "") }
    var textStats by remember { mutableStateOf<TextStats?>(null) }
    val repo = rememberKoinInject<MuseRepo>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Background text processing
    LaunchedEffect(content) {
        if (content.isNotEmpty()) {
            scope.launch {
                textStats = repo.getTextStats(content)
            }
        }
    }
    
    // ... existing code ...
    
    content = { paddingValues ->
        if (content.length > 15_000) {
            // Use chunked editor for large texts
            ChunkedTextEditor(
                text = content,
                onTextChange = { newText ->
                    if (newText.length <= MAX_TEXT_LENGTH) {
                        content = newText
                    } else {
                        context.toast("Text limit: 25,000 characters")
                    }
                },
                textStats = textStats,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Use regular editor for smaller texts
            RegularTextEditor(
                text = content,
                onTextChange = { newText ->
                    if (newText.length <= MAX_TEXT_LENGTH) {
                        content = newText
                    } else {
                        context.toast("Text limit: 25,000 characters")
                    }
                },
                textStats = textStats,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
```

#### 1.3 Create Chunked Text Editor Component
- [ ] **File**: `composeApp/src/androidMain/kotlin/io/github/kkoshin/muse/components/ChunkedTextEditor.kt` (new file)
  - Efficient text rendering for large texts
  - Smart scrolling with lazy loading
  - Background text operations

```kotlin
@Composable
fun ChunkedTextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    textStats: TextStats?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Text statistics bar
        TextStatsBar(
            stats = textStats,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // For very large texts, use optimized rendering
        if (text.length > 20_000) {
            LazyTextEditor(
                text = text,
                onTextChange = onTextChange,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Regular scrollable text field
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                textStyle = MaterialTheme.typography.h5.copy(
                    color = MaterialTheme.colors.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Enter your text here...",
                                style = MaterialTheme.typography.h5,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun LazyTextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // For very large texts, implement lazy loading
    // This is a simplified version - full implementation would be more complex
    val chunks = remember(text) { 
        text.chunked(5000) // 5k character chunks for display
    }
    
    var editingChunk by remember { mutableStateOf(-1) }
    
    LazyColumn(modifier = modifier) {
        itemsIndexed(chunks) { index, chunk ->
            if (editingChunk == index) {
                // Editable chunk
                var chunkText by remember { mutableStateOf(chunk) }
                BasicTextField(
                    value = chunkText,
                    onValueChange = { newChunk ->
                        chunkText = newChunk
                        // Reconstruct full text
                        val newText = chunks.toMutableList().apply {
                            this[index] = newChunk
                        }.joinToString("")
                        onTextChange(newText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.body1
                )
            } else {
                // Display-only chunk
                Text(
                    text = chunk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingChunk = index }
                        .padding(16.dp),
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Composable
fun TextStatsBar(
    stats: TextStats?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (stats != null) {
                    "${stats.characters} chars"
                } else {
                    "Calculating..."
                },
                style = MaterialTheme.typography.caption,
                color = when {
                    stats?.characters ?: 0 > 20_000 -> Color.Red
                    stats?.characters ?: 0 > 15_000 -> Color.Orange
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                }
            )
            
            if (stats != null) {
                Text(
                    text = "${stats.words} words",
                    style = MaterialTheme.typography.caption
                )
                Text(
                    text = "${stats.lines} lines",
                    style = MaterialTheme.typography.caption
                )
                Text(
                    text = "~${stats.estimatedProcessingTime}s TTS",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
```

#### 1.4 Update Export Processing
- [ ] **File**: `composeApp/src/androidMain/kotlin/io/github/kkoshin/muse/export/ExportViewModel.kt`
  - Use background text processing for phrase extraction
  - Process text in chunks to prevent ANR

```kotlin
// Update queryPhrases to use background processing
suspend fun queryPhrases(scriptId: String): List<String>? {
    val script = repo.getScript(UUID.fromString(scriptId))
    return script?.let { 
        repo.processTextAsync(it.text) // Use background processing
    }
}

// Add chunked TTS processing
fun startTTSChunked(
    voiceId: String,
    phrases: List<String>,
    onProgress: (Int, Int) -> Unit,
    onSuccess: (pcmList: List<Uri>) -> Unit,
) {
    _progress.value = TTSProcessing()
    viewModelScope.launch {
        val uniquePhrases = phrases.toSet()
        val totalPhrases = uniquePhrases.size
        var processedPhrases = 0
        
        // Process in smaller batches for large texts
        val batchSize = if (totalPhrases > 1000) 50 else maxNumberOfConcurrentRequests.value
        
        for (batch in uniquePhrases.chunked(batchSize)) {
            val requests = batch.map { phrase ->
                async {
                    ttsManager.getOrGenerate(voiceId, phrase)
                        .mapCatching { saveAsPcm(phrase, it, repo.getPcmCache(voiceId, phrase)) }
                        .onFailure { _progress.value = TTSFailed(throwable = it) }
                }
            }
            
            requests.awaitAll()
            processedPhrases += batch.size
            onProgress(processedPhrases, totalPhrases)
            
            // Yield between batches to prevent ANR
            yield()
        }
        
        // Return results in original order
        onSuccess(phrases.map { repo.getPcmCache(voiceId, it).toUri() })
    }
}
```

### Phase 2: Kill Switch and Monitoring (Week 1)
**Goal**: Add safety mechanisms with performance monitoring

#### 2.1 Performance Monitoring
- [ ] **File**: `composeApp/src/androidMain/kotlin/io/github/kkoshin/muse/monitoring/PerformanceMonitor.kt` (new file)
  - Monitor text processing performance
  - Track memory usage and UI responsiveness
  - Automatic performance degradation detection

```kotlin
object PerformanceMonitor {
    private val textProcessingTimes = mutableListOf<Long>()
    private val memoryUsage = mutableListOf<Long>()
    
    fun trackTextProcessing(textLength: Int, processingTime: Long) {
        textProcessingTimes.add(processingTime)
        logcat("PerformanceMonitor") { 
            "Text processing: ${textLength} chars in ${processingTime}ms" 
        }
        
        // Keep only recent measurements
        if (textProcessingTimes.size > 10) {
            textProcessingTimes.removeAt(0)
        }
        
        // Check for performance degradation
        if (textProcessingTimes.size >= 3) {
            val avgTime = textProcessingTimes.average()
            if (avgTime > 1000) { // 1 second threshold
                logcat("PerformanceMonitor") { 
                    "Performance degradation detected: ${avgTime}ms avg" 
                }
            }
        }
    }
    
    fun trackMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        memoryUsage.add(usedMemory)
        
        if (memoryUsage.size > 5) {
            memoryUsage.removeAt(0)
        }
        
        // Check for memory leaks
        if (memoryUsage.size >= 3) {
            val memoryGrowth = memoryUsage.last() - memoryUsage.first()
            val growthMB = memoryGrowth / (1024 * 1024)
            if (growthMB > 50) { // 50MB growth threshold
                logcat("PerformanceMonitor") { 
                    "Memory usage growing: ${growthMB}MB increase" 
                }
            }
        }
    }
    
    fun getPerformanceReport(): String {
        val avgProcessingTime = textProcessingTimes.average()
        val currentMemoryMB = memoryUsage.lastOrNull()?.let { it / (1024 * 1024) } ?: 0
        return "Avg processing: ${avgProcessingTime}ms, Memory: ${currentMemoryMB}MB"
    }
}
```

#### 2.2 Kill Switch Implementation
- [ ] **File**: `composeApp/src/androidMain/kotlin/io/github/kkoshin/muse/repo/MuseRepo.kt`
  - Add runtime text limit configuration
  - Automatic limit reduction on performance issues

```kotlin
object TextLimits {
    private var currentLimit = 25_000
    private var emergencyMode = false
    
    fun getCurrentLimit(): Int {
        return if (emergencyMode) 10_000 else currentLimit
    }
    
    fun checkPerformanceAndAdjust(): Boolean {
        val report = PerformanceMonitor.getPerformanceReport()
        
        // Simple performance check
        if (report.contains("processing: ") && 
            report.substringAfter("processing: ").substringBefore("ms").toDoubleOrNull()?.let { it > 2000 } == true) {
            
            activateEmergencyMode()
            return true
        }
        
        return false
    }
    
    private fun activateEmergencyMode() {
        emergencyMode = true
        currentLimit = 10_000
        logcat("TextLimits") { "Emergency mode activated - reverting to 10k limit" }
    }
    
    fun resetToNormal() {
        emergencyMode = false
        currentLimit = 25_000
        logcat("TextLimits") { "Normal mode restored - 25k limit active" }
    }
}
```

### Phase 3: Testing and Validation (Week 2)
**Goal**: Comprehensive testing of efficient 25k implementation

#### 3.1 Performance Testing
- [ ] Test text processing performance with 10k, 15k, 20k, 25k texts
- [ ] Monitor memory usage during text operations
- [ ] Test background processing doesn't block UI
- [ ] Verify chunked text editor works smoothly

#### 3.2 Efficiency Validation
- [ ] Test phrase extraction performance with large texts
- [ ] Verify TTS export processing works with chunked approach
- [ ] Test kill switch activation scenarios
- [ ] Monitor performance metrics over time

#### 3.3 User Experience Testing
- [ ] Test scrolling performance with large texts
- [ ] Verify text statistics accuracy
- [ ] Test text editing in chunked mode
- [ ] Test clipboard paste with large content

### Phase 4: Progressive Scaling (Week 3+)
**Goal**: Increase limits based on performance validation

#### 4.1 Conditional Increases
- [ ] If 25k performs well with background processing: increase to 50k
- [ ] If performance monitoring shows stability: consider 100k
- [ ] Each increase must maintain efficiency measures

#### 4.2 Efficiency Scaling
- [ ] Adjust chunk sizes based on text length
- [ ] Optimize background processing for larger texts
- [ ] Scale monitoring and kill switch thresholds

## Technical Architecture

### Background Processing Flow
```
User Input → Background Text Processing → UI Update
    ↓              ↓                        ↓
Text Change → Dispatchers.Default → Stats Update
    ↓              ↓                        ↓
Large Text → Chunked Processing → Lazy Rendering
```

### Chunking Strategy
- **Display Chunks**: 5k characters for UI rendering
- **Processing Chunks**: 10k characters for background operations
- **TTS Chunks**: Sentence-based for optimal voice processing

### Memory Management
- Lazy loading for texts > 20k characters
- Background garbage collection hints
- Chunk-based text operations to reduce peak memory

## Success Metrics

### Performance Benchmarks
- Text processing: < 500ms for 25k characters
- Memory usage: < 100MB increase for large texts
- UI responsiveness: No ANR during text operations
- Scrolling: 60fps performance maintained

### Efficiency Measures
- Background processing: All text operations off main thread
- Chunked rendering: Large texts don't block UI
- Smart monitoring: Performance tracking without overhead

## Timeline
- **Week 1**: Efficient 25k implementation with chunking and background processing
- **Week 2**: Testing and performance validation
- **Week 3**: Evaluate results and plan next scaling phase

This plan now includes proper efficiency measures from the start, ensuring performance scales with text size increases.