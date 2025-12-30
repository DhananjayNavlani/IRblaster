package com.example.irblaster

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblaster.ui.theme.IRblasterTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var irManager: ConsumerIrManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize IR Manager
        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

        enableEdgeToEdge()
        setContent {
            IRblasterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IRBlasterScreen(
                        irManager = irManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Samsung TV IR codes (NEC protocol - 38kHz carrier frequency)
object SamsungTVCodes {
    const val FREQUENCY = 38000 // 38 kHz carrier frequency for Samsung

    // Samsung TV uses NEC protocol with specific patterns
    // Format: Header + Address + Command + Inverse Command

    // Power - Multiple variations for different Samsung TV models
    val POWER_VARIATIONS = listOf(
        // Variation 1 - Samsung Power On/Off (0x07 command, address 0x07)
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
            590, 1690, 590, 42000
        ),
        // Variation 2 - Samsung Power (0x02 command) - Common for many models
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
            590, 1690, 590, 42000
        ),
        // Variation 3 - Samsung Power Toggle (0x98 command)
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590,
            590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690,
            590, 590, 590, 42000
        ),
        // Variation 4 - Samsung BN59 Power (longer header)
        intArrayOf(
            4600, 4500, 550, 1700, 550, 1700, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600,
            550, 600, 550, 1700, 550, 1700, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600,
            550, 600, 550, 600, 550, 1700, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600,
            550, 600, 550, 1700, 550, 600, 550, 1700, 550, 1700, 550, 1700, 550, 1700, 550, 1700,
            550, 1700, 550, 45000
        ),
        // Variation 5 - Samsung TV Power (E0E040BF - most common)
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590,
            590, 1690, 590, 42000
        ),
        // Variation 6 - Samsung Discrete Power ON
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590,
            590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690,
            590, 590, 590, 42000
        ),
        // Variation 7 - Samsung Discrete Power OFF
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590,
            590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690,
            590, 590, 590, 42000
        ),
        // Variation 8 - Samsung AA59-00666A Remote Power
        intArrayOf(
            4480, 4480, 560, 1680, 560, 1680, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560,
            560, 560, 560, 1680, 560, 1680, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560,
            560, 560, 560, 560, 560, 1680, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560,
            560, 560, 560, 1680, 560, 560, 560, 1680, 560, 1680, 560, 1680, 560, 1680, 560, 1680,
            560, 1680, 560, 47040
        ),
        // Variation 9 - Samsung 2014+ Smart TV Power
        intArrayOf(
            4500, 4500, 560, 1700, 560, 1700, 560, 1700, 560, 560, 560, 560, 560, 560, 560, 560,
            560, 560, 560, 1700, 560, 1700, 560, 1700, 560, 560, 560, 560, 560, 560, 560, 560,
            560, 560, 560, 1700, 560, 1700, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560,
            560, 560, 560, 560, 560, 560, 560, 1700, 560, 1700, 560, 1700, 560, 1700, 560, 1700,
            560, 1700, 560, 42000
        ),
        // Variation 10 - Samsung QLED Power
        intArrayOf(
            4500, 4450, 600, 1650, 600, 1650, 600, 1650, 600, 550, 600, 550, 600, 550, 600, 550,
            600, 550, 600, 1650, 600, 1650, 600, 1650, 600, 550, 600, 550, 600, 550, 600, 550,
            600, 550, 600, 1650, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550, 600, 550,
            600, 550, 600, 550, 600, 1650, 600, 1650, 600, 1650, 600, 1650, 600, 1650, 600, 1650,
            600, 1650, 600, 40000
        ),
        // Variation 11 - Samsung Crystal UHD Power
        intArrayOf(
            4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
            590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
            590, 1690, 590, 42000
        ),
        // Variation 12 - Alternative Samsung Power (shifted timing)
        intArrayOf(
            4400, 4400, 550, 1650, 550, 1650, 550, 1650, 550, 550, 550, 550, 550, 550, 550, 550,
            550, 550, 550, 1650, 550, 1650, 550, 1650, 550, 550, 550, 550, 550, 550, 550, 550,
            550, 550, 550, 550, 550, 1650, 550, 550, 550, 550, 550, 550, 550, 550, 550, 550,
            550, 550, 550, 1650, 550, 550, 550, 1650, 550, 1650, 550, 1650, 550, 1650, 550, 1650,
            550, 1650, 550, 43000
        )
    )

    // Power
    val POWER = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Volume Up
    val VOLUME_UP = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Volume Down
    val VOLUME_DOWN = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Channel Up
    val CHANNEL_UP = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Channel Down
    val CHANNEL_DOWN = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Mute
    val MUTE = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Source/Input - This is what old V5 was sending (E0E0807F)
    val SOURCE = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Menu
    val MENU = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Navigation - Up
    val NAV_UP = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Navigation - Down
    val NAV_DOWN = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Navigation - Left
    val NAV_LEFT = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Navigation - Right
    val NAV_RIGHT = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Enter/OK
    val ENTER = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Return/Back
    val RETURN = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Exit
    val EXIT = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    // Number keys 0-9
    val NUM_0 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_1 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_2 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_3 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_4 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_5 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_6 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_7 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_8 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )

    val NUM_9 = intArrayOf(
        4500, 4500, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 1690, 590, 1690, 590, 1690, 590, 590, 590, 590, 590, 590, 590, 590,
        590, 590, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590,
        590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 1690, 590, 590, 590, 1690,
        590, 1690, 590, 42000
    )
}

data class IRButton(
    val name: String,
    val code: IntArray,
    val emoji: String = ""
)

// Data class for saved buttons in custom remote
data class SavedButton(
    val id: String,
    val name: String,
    val code: IntArray,
    val emoji: String = ""
)

@Composable
fun IRBlasterScreen(
    irManager: ConsumerIrManager?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("Ready") }
    var hasIrBlaster by remember { mutableStateOf(false) }
    var testIndex by remember { mutableIntStateOf(0) }
    var powerVariationIndex by remember { mutableIntStateOf(0) }

    // Auto-play state for cycling through variations
    var isAutoPlaying by remember { mutableStateOf(false) }
    var autoPlayDelay by remember { mutableIntStateOf(2500) } // 2.5 seconds between signals

    // Custom remote - saved working buttons
    var customRemoteButtons by remember { mutableStateOf(listOf<SavedButton>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingButtonToAdd by remember { mutableStateOf<SavedButton?>(null) }
    var customButtonName by remember { mutableStateOf("") }

    // Check if device has IR blaster
    LaunchedEffect(irManager) {
        hasIrBlaster = irManager?.hasIrEmitter() == true
        statusMessage = if (hasIrBlaster) {
            "IR Blaster Available ✓"
        } else {
            "No IR Blaster Found ✗"
        }
    }

    // Auto-play effect - cycles through power variations automatically
    LaunchedEffect(isAutoPlaying, powerVariationIndex) {
        if (isAutoPlaying && hasIrBlaster) {
            // Send current variation
            try {
                irManager?.transmit(
                    SamsungTVCodes.FREQUENCY,
                    SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex]
                )
                statusMessage = "🔄 Auto: Power V${powerVariationIndex + 1} - Press STOP if TV responds!"
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
            }

            // Wait before sending next
            delay(autoPlayDelay.toLong())

            // Move to next variation if still playing
            if (isAutoPlaying) {
                powerVariationIndex = (powerVariationIndex + 1) % SamsungTVCodes.POWER_VARIATIONS.size
            }
        }
    }

    // All test buttons in order for progressive testing
    val testButtons = listOf(
        IRButton("Power", SamsungTVCodes.POWER, "⏻"),
        IRButton("Vol +", SamsungTVCodes.VOLUME_UP, "🔊"),
        IRButton("Vol -", SamsungTVCodes.VOLUME_DOWN, "🔉"),
        IRButton("Ch +", SamsungTVCodes.CHANNEL_UP, "⬆"),
        IRButton("Ch -", SamsungTVCodes.CHANNEL_DOWN, "⬇"),
        IRButton("Mute", SamsungTVCodes.MUTE, "🔇"),
        IRButton("Source", SamsungTVCodes.SOURCE, "📺"),
        IRButton("Menu", SamsungTVCodes.MENU, "☰"),
        IRButton("▲", SamsungTVCodes.NAV_UP, ""),
        IRButton("▼", SamsungTVCodes.NAV_DOWN, ""),
        IRButton("◀", SamsungTVCodes.NAV_LEFT, ""),
        IRButton("▶", SamsungTVCodes.NAV_RIGHT, ""),
        IRButton("OK", SamsungTVCodes.ENTER, "✓"),
        IRButton("Return", SamsungTVCodes.RETURN, "↩"),
        IRButton("Exit", SamsungTVCodes.EXIT, "✕"),
        IRButton("0", SamsungTVCodes.NUM_0, ""),
        IRButton("1", SamsungTVCodes.NUM_1, ""),
        IRButton("2", SamsungTVCodes.NUM_2, ""),
        IRButton("3", SamsungTVCodes.NUM_3, ""),
        IRButton("4", SamsungTVCodes.NUM_4, ""),
        IRButton("5", SamsungTVCodes.NUM_5, ""),
        IRButton("6", SamsungTVCodes.NUM_6, ""),
        IRButton("7", SamsungTVCodes.NUM_7, ""),
        IRButton("8", SamsungTVCodes.NUM_8, ""),
        IRButton("9", SamsungTVCodes.NUM_9, ""),
    )

    fun sendIRSignal(code: IntArray, buttonName: String) {
        if (irManager?.hasIrEmitter() == true) {
            try {
                irManager.transmit(SamsungTVCodes.FREQUENCY, code)
                statusMessage = "Sent: $buttonName"
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
            }
        } else {
            statusMessage = "No IR Blaster!"
            Toast.makeText(context, "Device doesn't have IR blaster", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Samsung TV Remote",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasIrBlaster)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = statusMessage,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        // Progressive Test Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Progressive Test Mode",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Current: ${testButtons[testIndex].name} (${testIndex + 1}/${testButtons.size})",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Show variation info for Power button
                if (testButtons[testIndex].name == "Power") {
                    Text(
                        text = "Power Variation: ${powerVariationIndex + 1}/${SamsungTVCodes.POWER_VARIATIONS.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Auto-Play Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAutoPlaying)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isAutoPlaying) "🔴 AUTO-SENDING..." else "▶ Auto-Cycle Power Codes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            if (isAutoPlaying) {
                                Text(
                                    text = "Testing V${powerVariationIndex + 1} - Press STOP when TV responds!",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "Will cycle through all ${SamsungTVCodes.POWER_VARIATIONS.size} variations",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Play/Stop Button
                            Button(
                                onClick = {
                                    if (isAutoPlaying) {
                                        isAutoPlaying = false
                                        statusMessage = "Stopped at Power V${powerVariationIndex + 1}"
                                    } else {
                                        isAutoPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAutoPlaying)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (isAutoPlaying) "⏹ STOP - Signal Works!" else "▶ START Auto-Cycle",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Delay slider
                            if (!isAutoPlaying) {
                                Text(
                                    text = "Delay: ${autoPlayDelay / 1000.0}s between signals",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Slider(
                                    value = autoPlayDelay.toFloat(),
                                    onValueChange = { autoPlayDelay = it.toInt() },
                                    valueRange = 1000f..5000f,
                                    steps = 7,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // For Power button, use the current variation
                            if (testButtons[testIndex].name == "Power") {
                                sendIRSignal(
                                    SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex],
                                    "Power V${powerVariationIndex + 1}"
                                )
                            } else {
                                sendIRSignal(testButtons[testIndex].code, testButtons[testIndex].name)
                            }
                        },
                        enabled = !isAutoPlaying
                    ) {
                        Text("Send Test Signal")
                    }
                    Button(
                        onClick = {
                            testIndex = (testIndex + 1) % testButtons.size
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        enabled = !isAutoPlaying
                    ) {
                        Text("Next →")
                    }
                }

                // Try Alternate button - cycles through different IR code variations
                if (testButtons[testIndex].name == "Power") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                powerVariationIndex = (powerVariationIndex + 1) % SamsungTVCodes.POWER_VARIATIONS.size
                                sendIRSignal(
                                    SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex],
                                    "Power V${powerVariationIndex + 1}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            enabled = !isAutoPlaying
                        ) {
                            Text("Try Next Variation")
                        }
                    }
                }

                Button(
                    onClick = {
                        testIndex = 0
                        powerVariationIndex = 0
                        isAutoPlaying = false
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Reset to Start")
                }

                // Add to My Remote Button
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val currentButton = testButtons[testIndex]
                        val buttonCode = if (currentButton.name == "Power") {
                            SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex]
                        } else {
                            currentButton.code
                        }
                        val buttonName = if (currentButton.name == "Power") {
                            "Power V${powerVariationIndex + 1}"
                        } else {
                            currentButton.name
                        }
                        pendingButtonToAdd = SavedButton(
                            id = "${buttonName}_${System.currentTimeMillis()}",
                            name = buttonName,
                            code = buttonCode,
                            emoji = currentButton.emoji
                        )
                        customButtonName = buttonName
                        showAddDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isAutoPlaying
                ) {
                    Text("✓ Add to My Remote", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Add Button Dialog
        if (showAddDialog && pendingButtonToAdd != null) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    pendingButtonToAdd = null
                },
                title = { Text("Add to My Remote") },
                text = {
                    Column {
                        Text("Save this working button to your custom remote:")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customButtonName,
                            onValueChange = { customButtonName = it },
                            label = { Text("Button Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingButtonToAdd?.let { button ->
                                val savedButton = button.copy(
                                    id = "${customButtonName}_${System.currentTimeMillis()}",
                                    name = customButtonName
                                )
                                customRemoteButtons = customRemoteButtons + savedButton
                                Toast.makeText(context, "Added '$customButtonName' to My Remote!", Toast.LENGTH_SHORT).show()
                            }
                            showAddDialog = false
                            pendingButtonToAdd = null
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        pendingButtonToAdd = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // My Custom Remote Section
        if (customRemoteButtons.isNotEmpty()) {
            var showDeleteMode by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ My Custom Remote",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        TextButton(
                            onClick = { showDeleteMode = !showDeleteMode }
                        ) {
                            Text(
                                text = if (showDeleteMode) "Done" else "Edit",
                                color = if (showDeleteMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (showDeleteMode) {
                        Text(
                            text = "Tap ✕ to remove a button",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display saved buttons in a grid-like layout
                    val chunkedButtons = customRemoteButtons.chunked(3)
                    chunkedButtons.forEach { rowButtons ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowButtons.forEach { savedButton ->
                                Box(modifier = Modifier.weight(1f)) {
                                    Button(
                                        onClick = {
                                            if (!showDeleteMode) {
                                                sendIRSignal(savedButton.code, savedButton.name)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (showDeleteMode)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        if (showDeleteMode) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = savedButton.name,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        customRemoteButtons = customRemoteButtons.filter { it.id != savedButton.id }
                                                        Toast.makeText(context, "Removed '${savedButton.name}'", Toast.LENGTH_SHORT).show()
                                                        if (customRemoteButtons.isEmpty()) showDeleteMode = false
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = if (savedButton.emoji.isNotEmpty()) "${savedButton.emoji} ${savedButton.name}" else savedButton.name,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill remaining space if row is not complete
                            repeat(3 - rowButtons.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Clear all button
                    if (showDeleteMode) {
                        TextButton(
                            onClick = {
                                customRemoteButtons = emptyList()
                                showDeleteMode = false
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Power Button
        Button(
            onClick = { sendIRSignal(SamsungTVCodes.POWER_VARIATIONS[powerVariationIndex], "Power V${powerVariationIndex + 1}") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("⏻ POWER (V${powerVariationIndex + 1})", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Volume and Channel Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Volume Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Volume", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { sendIRSignal(SamsungTVCodes.VOLUME_UP, "Vol+") }) {
                    Text("🔊 +", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { sendIRSignal(SamsungTVCodes.MUTE, "Mute") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("🔇", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { sendIRSignal(SamsungTVCodes.VOLUME_DOWN, "Vol-") }) {
                    Text("🔉 -", fontSize = 16.sp)
                }
            }

            // Channel Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Channel", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { sendIRSignal(SamsungTVCodes.CHANNEL_UP, "Ch+") }) {
                    Text("⬆ +", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { sendIRSignal(SamsungTVCodes.SOURCE, "Source") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("📺", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { sendIRSignal(SamsungTVCodes.CHANNEL_DOWN, "Ch-") }) {
                    Text("⬇ -", fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation D-Pad
        Card(
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Navigation", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))

                // Menu button
                Button(
                    onClick = { sendIRSignal(SamsungTVCodes.MENU, "Menu") },
                    modifier = Modifier.width(80.dp)
                ) {
                    Text("Menu")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Up
                Button(
                    onClick = { sendIRSignal(SamsungTVCodes.NAV_UP, "Up") },
                    modifier = Modifier.width(60.dp)
                ) {
                    Text("▲")
                }

                // Left, OK, Right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { sendIRSignal(SamsungTVCodes.NAV_LEFT, "Left") },
                        modifier = Modifier.width(60.dp)
                    ) {
                        Text("◀")
                    }
                    Button(
                        onClick = { sendIRSignal(SamsungTVCodes.ENTER, "OK") },
                        modifier = Modifier.width(70.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { sendIRSignal(SamsungTVCodes.NAV_RIGHT, "Right") },
                        modifier = Modifier.width(60.dp)
                    ) {
                        Text("▶")
                    }
                }

                // Down
                Button(
                    onClick = { sendIRSignal(SamsungTVCodes.NAV_DOWN, "Down") },
                    modifier = Modifier.width(60.dp)
                ) {
                    Text("▼")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Return and Exit
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { sendIRSignal(SamsungTVCodes.RETURN, "Return") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("↩ Return")
                    }
                    Button(
                        onClick = { sendIRSignal(SamsungTVCodes.EXIT, "Exit") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("✕ Exit")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Number Pad
        Card(
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Number Pad", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))

                val numberCodes = listOf(
                    listOf("1" to SamsungTVCodes.NUM_1, "2" to SamsungTVCodes.NUM_2, "3" to SamsungTVCodes.NUM_3),
                    listOf("4" to SamsungTVCodes.NUM_4, "5" to SamsungTVCodes.NUM_5, "6" to SamsungTVCodes.NUM_6),
                    listOf("7" to SamsungTVCodes.NUM_7, "8" to SamsungTVCodes.NUM_8, "9" to SamsungTVCodes.NUM_9),
                    listOf("" to intArrayOf(), "0" to SamsungTVCodes.NUM_0, "" to intArrayOf())
                )

                numberCodes.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        row.forEach { (num, code) ->
                            if (num.isNotEmpty()) {
                                Button(
                                    onClick = { sendIRSignal(code, num) },
                                    modifier = Modifier.width(60.dp)
                                ) {
                                    Text(num, fontSize = 18.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(60.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun IRBlasterScreenPreview() {
    IRblasterTheme {
        IRBlasterScreen(irManager = null)
    }
}