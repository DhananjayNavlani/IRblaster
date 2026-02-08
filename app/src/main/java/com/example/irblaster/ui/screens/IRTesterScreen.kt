package com.example.irblaster.ui.screens

import android.hardware.ConsumerIrManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Samsung TV IR codes
object SamsungTVCodes {
    const val FREQUENCY = 38000

    val POWER_VARIATIONS = listOf(
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000),
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000),
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 42000),
        intArrayOf(4600, 4500, 550, 1700, 550, 1700, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 1700, 550, 1700, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 1700, 550, 600, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550, 45000),
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 42000),
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 42000),
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 42000),
        intArrayOf(4480, 4480, 560, 1680, 560, 1680, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680, 560, 1680, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1680, 560, 560, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 47040),
        intArrayOf(4500, 4500, 560, 1700, 560, 1700, 560, 1700, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1700, 560, 1700, 560, 1700, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1700, 560, 1700, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1700, 560, 1700, 560, 1700, 560, 1700, 560, 1700, 560, 1700, 560, 42000),
        intArrayOf(4500, 4450, 600, 1650, 600, 1650, 600, 1650, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 1650, 600, 1650, 600, 1650, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 1650, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 1650, 600, 1650, 600, 1650, 600, 1650, 600, 1650, 600, 1650, 600, 1650, 600, 40000),
        intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000),
        intArrayOf(4400, 4400, 550, 1650, 550, 1650, 550, 1650, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 1650, 550, 1650, 550, 1650, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 1650, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550, 1650, 550, 550, 550, 1650, 550, 1650, 550, 1650, 550, 1650, 550, 1650, 550, 1650, 550, 43000)
    )

    val POWER = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
    val VOLUME_UP = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
    val VOLUME_DOWN = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
    val CHANNEL_UP = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
    val CHANNEL_DOWN = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
    val MUTE = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
    val SOURCE = intArrayOf(4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 42000)
}

// Konftel 50 IR codes (RC5 Protocol)
object KonftelCodes {
    const val FREQUENCY = 36000 // RC5 uses 36kHz

    // RC5 pattern structure (based on generic RC5 / Philips 36kHz)
    // Pulse Width Modulated
    val POWER = intArrayOf(2666, 889, 444, 444, 444, 444, 889, 889, 444, 444, 444, 444, 444, 444, 444, 444, 444, 444, 889, 444, 444, 889, 444, 444, 444, 90000)
    val VOLUME_UP = intArrayOf(2666, 889, 444, 444, 444, 444, 889, 889, 444, 444, 444, 444, 444, 444, 889, 444, 444, 444, 444, 889, 444, 444, 444, 444, 444, 90000)
    val VOLUME_DOWN = intArrayOf(2666, 889, 444, 444, 444, 444, 889, 889, 444, 444, 444, 444, 444, 444, 889, 444, 444, 889, 444, 444, 444, 444, 444, 444, 444, 90000)
    val MUTE = intArrayOf(2666, 889, 444, 444, 444, 444, 889, 889, 444, 444, 444, 444, 444, 444, 444, 889, 444, 444, 889, 444, 444, 889, 444, 444, 444, 90000)
}

data class IRButton(
    val name: String,
    val code: IntArray,
    val emoji: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IRButton
        return name == other.name && code.contentEquals(other.code)
    }
    override fun hashCode(): Int = 31 * name.hashCode() + code.contentHashCode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IRTesterScreen(
    irManager: ConsumerIrManager?,
    onMenuClick: () -> Unit,
    onAddToRemote: (name: String, emoji: String, code: IntArray, frequency: Int) -> Unit
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("Ready") }
    var hasIrBlaster by remember { mutableStateOf(false) }
    var powerVariationIndex by remember { mutableIntStateOf(0) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var autoPlayDelay by remember { mutableIntStateOf(500) }

    // Brand Selection
    var selectedBrand by remember { mutableStateOf("Samsung") }
    val brands = listOf("Samsung", "Konftel (RC5)")

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingButtonName by remember { mutableStateOf("") }
    var pendingButtonEmoji by remember { mutableStateOf("") }
    var pendingButtonCode by remember { mutableStateOf<IntArray?>(null) }

    // Button type selection
    var selectedButtonType by remember { mutableStateOf("Power") }
    val buttonTypes = listOf("Power", "Vol+", "Vol-", "Ch+", "Ch-", "Mute", "Source")

    LaunchedEffect(irManager) {
        hasIrBlaster = irManager?.hasIrEmitter() == true
        statusMessage = if (hasIrBlaster) "IR Blaster Available ✓" else "No IR Blaster Found ✗"
    }

    // Reset variation index when button type changes
    LaunchedEffect(selectedButtonType, selectedBrand) {
        powerVariationIndex = 0
        isAutoPlaying = false
    }

    // Get current code based on selection
    val currentCode: IntArray = remember(selectedButtonType, powerVariationIndex, selectedBrand) {
        if (selectedBrand == "Samsung") {
            when (selectedButtonType) {
                "Power" -> SamsungTVCodes.POWER_VARIATIONS.getOrElse(powerVariationIndex) { SamsungTVCodes.POWER }
                "Vol+" -> SamsungTVCodes.VOLUME_UP
                "Vol-" -> SamsungTVCodes.VOLUME_DOWN
                "Ch+" -> SamsungTVCodes.CHANNEL_UP
                "Ch-" -> SamsungTVCodes.CHANNEL_DOWN
                "Mute" -> SamsungTVCodes.MUTE
                "Source" -> SamsungTVCodes.SOURCE
                else -> SamsungTVCodes.POWER
            }
        } else {
            when (selectedButtonType) {
                "Power" -> KonftelCodes.POWER
                "Vol+" -> KonftelCodes.VOLUME_UP
                "Vol-" -> KonftelCodes.VOLUME_DOWN
                "Mute" -> KonftelCodes.MUTE
                else -> KonftelCodes.POWER
            }
        }
    }

    val variationCount = if (selectedBrand == "Samsung" && selectedButtonType == "Power") SamsungTVCodes.POWER_VARIATIONS.size else 1

    LaunchedEffect(isAutoPlaying, powerVariationIndex) {
        if (isAutoPlaying && hasIrBlaster && selectedButtonType == "Power" && selectedBrand == "Samsung") {
            try {
                val code = SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex]
                irManager?.transmit(SamsungTVCodes.FREQUENCY, code)
                statusMessage = "🔄 Auto: Power V${powerVariationIndex + 1} - Press STOP if TV responds!"
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
            }
            delay(autoPlayDelay.toLong())
            if (isAutoPlaying) {
                powerVariationIndex = (powerVariationIndex + 1) % SamsungTVCodes.POWER_VARIATIONS.size
            }
        }
    }

    fun sendIRSignal(code: IntArray, buttonName: String) {
        if (irManager?.hasIrEmitter() == true) {
            try {
                val frequency = if (selectedBrand == "Samsung") SamsungTVCodes.FREQUENCY else KonftelCodes.FREQUENCY
                Log.d("IRBlaster", "========== IR TRANSMISSION ==========")
                Log.d("IRBlaster", "Button: $buttonName")
                Log.d("IRBlaster", "Carrier Frequency: $frequency Hz (${frequency / 1000} kHz)")
                Log.d("IRBlaster", "Pattern Length: ${code.size} values")
                Log.d("IRBlaster", "======================================")
                irManager.transmit(frequency, code)
                statusMessage = "Sent: $buttonName @ ${frequency / 1000}kHz"
            } catch (e: Exception) {
                Log.e("IRBlaster", "Error transmitting: ${e.message}", e)
                statusMessage = "Error: ${e.message}"
            }
        } else {
            statusMessage = "No IR Blaster!"
            Toast.makeText(context, "Device doesn't have IR blaster", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔬 Samsung IR Tester") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasIrBlaster) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            // Button Type Selector
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "$selectedBrand - Select Button",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Brand Selector
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        brands.forEach { brand ->
                            FilterChip(
                                selected = selectedBrand == brand,
                                onClick = { selectedBrand = brand },
                                label = { Text(brand) }
                            )
                        }
                    }

                    // Button type chips - row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        buttonTypes.take(4).forEach { buttonType ->
                            FilterChip(
                                selected = selectedButtonType == buttonType,
                                onClick = { selectedButtonType = buttonType },
                                label = { Text(buttonType, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button type chips - row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        buttonTypes.drop(4).forEach { buttonType ->
                            FilterChip(
                                selected = selectedButtonType == buttonType,
                                onClick = { selectedButtonType = buttonType },
                                label = { Text(buttonType, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space
                        repeat(4 - buttonTypes.drop(4).size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // Show variation count for Power
                    if (selectedButtonType == "Power") {
                        Text(
                            text = "${SamsungTVCodes.POWER_VARIATIONS.size} power code variations available",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Test Section
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Test $selectedButtonType", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    if (variationCount > 1) {
                        Text(
                            "Variation: ${powerVariationIndex + 1}/$variationCount",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Auto-Play Section for Power
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAutoPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isAutoPlaying) "🔴 AUTO-SENDING..." else "▶ Auto-Cycle Power Codes",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Text(
                                    text = if (isAutoPlaying) "Testing V${powerVariationIndex + 1} - Press STOP when TV responds!"
                                    else "Will cycle through all $variationCount variations",
                                    fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (isAutoPlaying) {
                                            isAutoPlaying = false
                                            statusMessage = "Stopped at Power V${powerVariationIndex + 1}"
                                        } else {
                                            isAutoPlaying = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAutoPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = if (isAutoPlaying) "⏹ STOP - Signal Works!" else "▶ START Auto-Cycle",
                                        fontSize = 16.sp, fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!isAutoPlaying) {
                                    Text("Delay: ${autoPlayDelay / 1000.0}s", fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                                    Slider(
                                        value = autoPlayDelay.toFloat(),
                                        onValueChange = { autoPlayDelay = it.toInt() },
                                        valueRange = 500f..5000f, steps = 7,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }

                        // Variation navigation
                        Text("Try Different Variations:", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    powerVariationIndex = if (powerVariationIndex > 0) powerVariationIndex - 1 else variationCount - 1
                                    sendIRSignal(SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex], "Power V${powerVariationIndex + 1}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                enabled = !isAutoPlaying
                            ) { Text("← Prev") }
                            Button(
                                onClick = {
                                    powerVariationIndex = (powerVariationIndex + 1) % variationCount
                                    sendIRSignal(SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex], "Power V${powerVariationIndex + 1}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                enabled = !isAutoPlaying
                            ) { Text("Next →") }
                        }
                    }

                    // Manual send button
                    val currentButtonName = if (selectedButtonType == "Power" && variationCount > 1) {
                        "Power V${powerVariationIndex + 1}"
                    } else {
                        selectedButtonType
                    }

                    Button(
                        onClick = { sendIRSignal(currentCode, currentButtonName) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        enabled = !isAutoPlaying
                    ) {
                        Text("Send $currentButtonName")
                    }

                    // Add to Remote Button
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val emoji = when (selectedButtonType) {
                                "Power" -> "🔴"
                                "Vol+" -> "🔊"
                                "Vol-" -> "🔉"
                                "Ch+" -> "⬆️"
                                "Ch-" -> "⬇️"
                                "Mute" -> "🔇"
                                "Source" -> "📺"
                                else -> "📡"
                            }
                            pendingButtonName = currentButtonName
                            pendingButtonEmoji = emoji
                            pendingButtonCode = currentCode
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isAutoPlaying
                    ) {
                        Text("✓ Add to My Remote", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Tip", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Use '🌐 Browse All Brands' from the menu for other TV brands and devices.",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add to Remote Dialog
    if (showAddDialog && pendingButtonCode != null) {
        var customName by remember { mutableStateOf(pendingButtonName) }
        var customEmoji by remember { mutableStateOf(pendingButtonEmoji) }

        val emojiOptions = listOf("🔴", "🔊", "🔉", "🔇", "⬆️", "⬇️", "◀️", "▶️", "✅", "❌", "↩️", "📋", "📺", "🔼", "🔽", "⏸️")

        AlertDialog(
            onDismissRequest = { showAddDialog = false; pendingButtonCode = null },
            title = { Text("Add Button to Remote") },
            text = {
                Column {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Button Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Emoji:", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        emojiOptions.take(8).forEach { e ->
                            Card(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { customEmoji = e },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (e == customEmoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(e, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        emojiOptions.drop(8).forEach { e ->
                            Card(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { customEmoji = e },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (e == customEmoji) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(e, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val frequency = if (selectedBrand == "Samsung") SamsungTVCodes.FREQUENCY else KonftelCodes.FREQUENCY
                        onAddToRemote(customName, customEmoji, pendingButtonCode!!, frequency)
                        showAddDialog = false
                        pendingButtonCode = null
                    },
                    enabled = customName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; pendingButtonCode = null }) { Text("Cancel") }
            }
        )
    }
}

