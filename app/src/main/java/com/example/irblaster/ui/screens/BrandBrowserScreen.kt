package com.example.irblaster.ui.screens

import android.hardware.ConsumerIrManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblaster.data.api.IRDBApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen to browse brands from IRDB API with pagination and debounced search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandBrowserScreen(
    irManager: ConsumerIrManager?,
    onBackClick: () -> Unit,
    onAddToRemote: (name: String, emoji: String, code: IntArray, frequency: Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    // State
    var isLoading by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var brands by remember { mutableStateOf<List<IRDBApiClient.Brand>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<IRDBApiClient.Brand>?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var hasMorePages by remember { mutableStateOf(true) }
    var totalBrands by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Debounce job reference
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Navigation state
    var selectedBrand by remember { mutableStateOf<IRDBApiClient.Brand?>(null) }
    var selectedDeviceType by remember { mutableStateOf<IRDBApiClient.DeviceType?>(null) }
    var selectedCodeFile by remember { mutableStateOf<IRDBApiClient.IRCodeFile?>(null) }

    // Device types and codes
    var deviceTypes by remember { mutableStateOf<List<IRDBApiClient.DeviceType>>(emptyList()) }
    var codeFiles by remember { mutableStateOf<List<IRDBApiClient.IRCodeFile>>(emptyList()) }
    var irFunctions by remember { mutableStateOf<List<IRDBApiClient.IRFunction>>(emptyList()) }

    val pageSize = 30
    val listState = rememberLazyListState()

    // Load brands on first launch
    LaunchedEffect(Unit) {
        loadBrands(
            page = 0,
            onLoading = { isLoading = it },
            onResult = { result, total ->
                brands = result
                totalBrands = total
                hasMorePages = result.size >= pageSize
            },
            onError = { error = it }
        )
    }

    // Debounced search effect
    LaunchedEffect(searchQuery) {
        // Cancel previous search job
        searchJob?.cancel()

        if (searchQuery.isBlank()) {
            // Clear search results and show paginated list
            searchResults = null
            isSearching = false
        } else {
            // Debounce: wait 500ms before searching
            searchJob = scope.launch {
                delay(500)
                isSearching = true
                IRDBApiClient.searchBrands(searchQuery).fold(
                    onSuccess = { results ->
                        searchResults = results
                        isSearching = false
                    },
                    onFailure = { e ->
                        error = e.message
                        isSearching = false
                    }
                )
            }
        }
    }

    // Display list - either search results or paginated brands
    val displayBrands = searchResults ?: brands

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedCodeFile != null -> selectedCodeFile!!.name
                            selectedDeviceType != null -> "${selectedBrand?.name} - ${selectedDeviceType?.name}"
                            selectedBrand != null -> selectedBrand!!.name
                            else -> "Browse Brands (IRDB)"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedCodeFile != null -> {
                                selectedCodeFile = null
                                irFunctions = emptyList()
                            }
                            selectedDeviceType != null -> {
                                selectedDeviceType = null
                                codeFiles = emptyList()
                            }
                            selectedBrand != null -> {
                                selectedBrand = null
                                deviceTypes = emptyList()
                            }
                            else -> onBackClick()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedBrand == null) {
                        IconButton(onClick = {
                            scope.launch {
                                IRDBApiClient.clearCache()
                                currentPage = 0
                                loadBrands(
                                    page = 0,
                                    onLoading = { isLoading = it },
                                    onResult = { result, total ->
                                        brands = result
                                        totalBrands = total
                                        hasMorePages = result.size >= pageSize
                                    },
                                    onError = { error = it }
                                )
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Show IR Functions (codes)
                selectedCodeFile != null -> {
                    IRFunctionsView(
                        functions = irFunctions,
                        isLoading = isLoading,
                        error = error,
                        irManager = irManager,
                        onAddToRemote = onAddToRemote
                    )
                }

                // Show Code Files
                selectedDeviceType != null -> {
                    CodeFilesView(
                        codeFiles = codeFiles,
                        isLoading = isLoading,
                        error = error,
                        onFileClick = { file ->
                            selectedCodeFile = file
                            scope.launch {
                                isLoading = true
                                error = null
                                IRDBApiClient.downloadCodes(file).fold(
                                    onSuccess = { irFunctions = it },
                                    onFailure = { error = it.message }
                                )
                                isLoading = false
                            }
                        }
                    )
                }

                // Show Device Types
                selectedBrand != null -> {
                    DeviceTypesView(
                        deviceTypes = deviceTypes,
                        isLoading = isLoading,
                        error = error,
                        onDeviceTypeClick = { deviceType ->
                            selectedDeviceType = deviceType
                            scope.launch {
                                isLoading = true
                                error = null
                                IRDBApiClient.fetchCodeFiles(selectedBrand!!, deviceType).fold(
                                    onSuccess = { codeFiles = it },
                                    onFailure = { error = it.message }
                                )
                                isLoading = false
                            }
                        }
                    )
                }

                // Show Brands List
                else -> {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search brands...") },
                        leadingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )

                    // Stats
                    Text(
                        text = if (searchResults != null) {
                            "Found ${displayBrands.size} brands matching \"$searchQuery\""
                        } else {
                            "Showing ${displayBrands.size} of $totalBrands brands"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    if (isLoading && brands.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Loading brands from IRDB...")
                            }
                        }
                    } else if (error != null && brands.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    scope.launch {
                                        loadBrands(
                                            page = 0,
                                            onLoading = { isLoading = it },
                                            onResult = { result, total ->
                                                brands = result
                                                totalBrands = total
                                            },
                                            onError = { error = it }
                                        )
                                    }
                                }) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayBrands) { brand ->
                                BrandItem(
                                    brand = brand,
                                    onClick = {
                                        selectedBrand = brand
                                        scope.launch {
                                            isLoading = true
                                            error = null
                                            IRDBApiClient.fetchDeviceTypes(brand).fold(
                                                onSuccess = { deviceTypes = it },
                                                onFailure = { error = it.message }
                                            )
                                            isLoading = false
                                        }
                                    }
                                )
                            }

                            // Load more button - only show when not searching
                            if (hasMorePages && searchResults == null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        } else {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        val nextPage = currentPage + 1
                                                        loadBrands(
                                                            page = nextPage,
                                                            onLoading = { isLoading = it },
                                                            onResult = { result, _ ->
                                                                if (result.isNotEmpty()) {
                                                                    brands = brands + result
                                                                    currentPage = nextPage
                                                                    hasMorePages = result.size >= pageSize
                                                                } else {
                                                                    hasMorePages = false
                                                                }
                                                            },
                                                            onError = { error = it }
                                                        )
                                                    }
                                                }
                                            ) {
                                                Text("Load More")
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
}

private suspend fun loadBrands(
    page: Int,
    onLoading: (Boolean) -> Unit,
    onResult: (List<IRDBApiClient.Brand>, Int) -> Unit,
    onError: (String) -> Unit
) {
    onLoading(true)
    val total = IRDBApiClient.getTotalBrandCount()
    IRDBApiClient.fetchBrands(page, 30).fold(
        onSuccess = { onResult(it, total) },
        onFailure = { onError(it.message ?: "Unknown error") }
    )
    onLoading(false)
}

@Composable
private fun BrandItem(
    brand: IRDBApiClient.Brand,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand icon based on first letter
            Card(
                modifier = Modifier.size(48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brand.name.first().uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = brand.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = "Tap to browse devices",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "→",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceTypesView(
    deviceTypes: List<IRDBApiClient.DeviceType>,
    isLoading: Boolean,
    error: String?,
    onDeviceTypeClick: (IRDBApiClient.DeviceType) -> Unit
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading device types...")
                }
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        }
        deviceTypes.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No device types found")
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(deviceTypes) { deviceType ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onDeviceTypeClick(deviceType) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getDeviceEmoji(deviceType.name),
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = deviceType.name,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text("→", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeFilesView(
    codeFiles: List<IRDBApiClient.IRCodeFile>,
    isLoading: Boolean,
    error: String?,
    onFileClick: (IRDBApiClient.IRCodeFile) -> Unit
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading code files...")
                }
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        }
        codeFiles.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No code files found")
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "Select a remote model to download codes:",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
                items(codeFiles) { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onFileClick(file) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📄", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tap to download codes",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            Text("⬇", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IRFunctionsView(
    functions: List<IRDBApiClient.IRFunction>,
    isLoading: Boolean,
    error: String?,
    irManager: ConsumerIrManager?,
    onAddToRemote: (name: String, emoji: String, code: IntArray, frequency: Int) -> Unit
) {
    val context = LocalContext.current
    var testingFunction by remember { mutableStateOf<IRDBApiClient.IRFunction?>(null) }

    fun sendIR(function: IRDBApiClient.IRFunction) {
        if (irManager?.hasIrEmitter() == true && function.pattern != null) {
            try {
                Log.d("IRBlaster", "Sending: ${function.name} @ ${function.frequency}Hz")
                irManager.transmit(function.frequency, function.pattern)
                Toast.makeText(context, "Sent: ${function.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No IR Blaster or invalid pattern", Toast.LENGTH_SHORT).show()
        }
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Downloading IR codes...")
                }
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
        }
        functions.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No functions found in this file")
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Downloaded ${functions.size} IR codes",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap a button to test, long-press to add to your remote",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                items(functions) { function ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { sendIR(function) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getFunctionEmoji(function.name),
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = function.name,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${function.protocol} | ${function.frequency / 1000}kHz",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }

                            // Test button
                            FilledTonalButton(
                                onClick = { sendIR(function) },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Test", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Add button
                            Button(
                                onClick = {
                                    function.pattern?.let { pattern ->
                                        onAddToRemote(
                                            function.name,
                                            getFunctionEmoji(function.name),
                                            pattern,
                                            function.frequency
                                        )
                                    }
                                },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                enabled = function.pattern != null
                            ) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDeviceEmoji(deviceType: String): String {
    return when (deviceType.lowercase()) {
        "tv" -> "📺"
        "dvd" -> "📀"
        "vcr" -> "📼"
        "cd" -> "💿"
        "audio", "receiver", "amp" -> "🔊"
        "ac", "aircon", "air_conditioner" -> "❄️"
        "fan" -> "🌀"
        "light", "lamp" -> "💡"
        "projector" -> "📽️"
        "camera" -> "📷"
        "sat", "satellite" -> "📡"
        "cable", "stb" -> "📦"
        else -> "📱"
    }
}

private fun getFunctionEmoji(functionName: String): String {
    val name = functionName.lowercase()
    return when {
        name.contains("power") -> "🔴"
        name.contains("vol") && name.contains("up") -> "🔊"
        name.contains("vol") && name.contains("down") -> "🔉"
        name.contains("vol") -> "🔊"
        name.contains("mute") -> "🔇"
        name.contains("ch") && name.contains("up") -> "⬆️"
        name.contains("ch") && name.contains("down") -> "⬇️"
        name.contains("up") -> "🔼"
        name.contains("down") -> "🔽"
        name.contains("left") -> "◀️"
        name.contains("right") -> "▶️"
        name.contains("ok") || name.contains("enter") || name.contains("select") -> "✅"
        name.contains("menu") -> "📋"
        name.contains("back") || name.contains("return") -> "↩️"
        name.contains("exit") -> "❌"
        name.contains("home") -> "🏠"
        name.contains("input") || name.contains("source") -> "📺"
        name.contains("play") -> "▶️"
        name.contains("pause") -> "⏸️"
        name.contains("stop") -> "⏹️"
        name.contains("record") -> "⏺️"
        name.contains("forward") || name.contains("ff") -> "⏩"
        name.contains("rewind") || name.contains("rew") -> "⏪"
        name.contains("0") -> "0️⃣"
        name.contains("1") -> "1️⃣"
        name.contains("2") -> "2️⃣"
        name.contains("3") -> "3️⃣"
        name.contains("4") -> "4️⃣"
        name.contains("5") -> "5️⃣"
        name.contains("6") -> "6️⃣"
        name.contains("7") -> "7️⃣"
        name.contains("8") -> "8️⃣"
        name.contains("9") -> "9️⃣"
        else -> "📡"
    }
}

