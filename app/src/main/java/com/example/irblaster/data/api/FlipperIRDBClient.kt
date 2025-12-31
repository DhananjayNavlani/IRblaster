package com.example.irblaster.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * API client for fetching IR codes from Flipper IRDB (GitHub-based)
 * Source: https://github.com/Lucaslhm/Flipper-IRDB
 *
 * This is a comprehensive IR database maintained by the Flipper Zero community
 */
object FlipperIRDBClient {
    private const val TAG = "FlipperIRDBClient"

    // Primary Flipper IRDB repository
    private const val PRIMARY_API_URL = "https://api.github.com/repos/Lucaslhm/Flipper-IRDB/contents"
    // Alternative mirror
    private const val FALLBACK_API_URL = "https://api.github.com/repos/UberGuidoZ/Flipper-IRDB/contents"

    private var currentApiUrl = PRIMARY_API_URL

    // Cached data
    private var cachedCategories: List<Category>? = null

    data class Category(
        val name: String,
        val path: String,
        val emoji: String = "📁"
    )

    data class Brand(
        val name: String,
        val path: String
    )

    data class IRFile(
        val name: String,
        val downloadUrl: String
    )

    data class IRSignal(
        val name: String,
        val type: String, // "parsed" or "raw"
        val protocol: String?,
        val address: String?,
        val command: String?,
        val frequency: Int,
        val dutyCycle: Float,
        val rawData: IntArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as IRSignal
            return name == other.name && protocol == other.protocol
        }
        override fun hashCode(): Int = 31 * name.hashCode() + (protocol?.hashCode() ?: 0)
    }

    /**
     * Fetch top-level categories (TV, AC, Audio, etc.)
     */
    suspend fun fetchCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            cachedCategories?.let { return@withContext Result.success(it) }

            val apiUrls = listOf(PRIMARY_API_URL, FALLBACK_API_URL)

            for (apiUrl in apiUrls) {
                try {
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                        setRequestProperty("User-Agent", "IRBlaster-Android-App")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val categories = mutableListOf<Category>()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonArray = JSONArray(response)

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            if (item.getString("type") == "dir") {
                                val name = item.getString("name")
                                // Skip non-device folders
                                if (!name.startsWith(".") && !name.equals("_template", ignoreCase = true)) {
                                    categories.add(
                                        Category(
                                            name = name,
                                            path = item.getString("path"),
                                            emoji = getCategoryEmoji(name)
                                        )
                                    )
                                }
                            }
                        }

                        if (categories.isNotEmpty()) {
                            currentApiUrl = apiUrl
                            cachedCategories = categories.sortedBy { it.name }
                            connection.disconnect()
                            Log.d(TAG, "Successfully fetched ${categories.size} categories from $apiUrl")
                            return@withContext Result.success(cachedCategories!!)
                        }
                    } else {
                        Log.w(TAG, "HTTP ${connection.responseCode} from $apiUrl, trying fallback...")
                    }

                    connection.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch from $apiUrl: ${e.message}, trying fallback...")
                }
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch brands within a category
     */
    suspend fun fetchBrands(category: Category): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$currentApiUrl/${category.path}")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val brands = mutableListOf<Brand>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (item.getString("type") == "dir") {
                        val name = item.getString("name")
                        brands.add(
                            Brand(
                                name = name,
                                path = item.getString("path")
                            )
                        )
                    }
                }
            }

            connection.disconnect()
            Result.success(brands.sortedBy { it.name.lowercase() })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching brands: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Search brands across all categories
     */
    suspend fun searchBrands(query: String): Result<List<Pair<Category, Brand>>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<Pair<Category, Brand>>()
            val categories = cachedCategories ?: fetchCategories().getOrNull() ?: emptyList()

            for (category in categories) {
                val brands = fetchBrands(category).getOrNull() ?: continue
                brands.filter { it.name.contains(query, ignoreCase = true) }
                    .forEach { brand ->
                        results.add(category to brand)
                    }
            }

            Result.success(results.sortedBy { it.second.name.lowercase() })
        } catch (e: Exception) {
            Log.e(TAG, "Error searching brands: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch IR files for a brand
     */
    suspend fun fetchIRFiles(brand: Brand): Result<List<IRFile>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$currentApiUrl/${brand.path}")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val files = mutableListOf<IRFile>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (item.getString("type") == "file" && item.getString("name").endsWith(".ir")) {
                        files.add(
                            IRFile(
                                name = item.getString("name").removeSuffix(".ir"),
                                downloadUrl = item.getString("download_url")
                            )
                        )
                    }
                }
            }

            connection.disconnect()
            Result.success(files.sortedBy { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IR files: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Download and parse Flipper IR file format
     */
    suspend fun downloadIRFile(irFile: IRFile): Result<List<IRSignal>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(irFile.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val signals = mutableListOf<IRSignal>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().readText()
                signals.addAll(parseFlipperIRFile(content))
            }

            connection.disconnect()
            Result.success(signals)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading IR file: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse Flipper .ir file format
     * Example format:
     * Filetype: IR signals file
     * Version: 1
     * #
     * name: Power
     * type: parsed
     * protocol: NEC
     * address: 04 00 00 00
     * command: 08 00 00 00
     * #
     * name: Vol_up
     * type: raw
     * frequency: 38000
     * duty_cycle: 0.330000
     * data: 9024 4512 564 564 ...
     */
    private fun parseFlipperIRFile(content: String): List<IRSignal> {
        val signals = mutableListOf<IRSignal>()
        val lines = content.lines()

        var currentName: String? = null
        var currentType: String? = null
        var currentProtocol: String? = null
        var currentAddress: String? = null
        var currentCommand: String? = null
        var currentFrequency: Int = 38000
        var currentDutyCycle: Float = 0.33f
        var currentRawData: IntArray? = null

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("name:") -> {
                    // Save previous signal if exists
                    currentName?.let { name ->
                        signals.add(createSignal(
                            name, currentType, currentProtocol, currentAddress,
                            currentCommand, currentFrequency, currentDutyCycle, currentRawData
                        ))
                    }
                    // Start new signal
                    currentName = trimmed.substringAfter("name:").trim()
                    currentType = null
                    currentProtocol = null
                    currentAddress = null
                    currentCommand = null
                    currentFrequency = 38000
                    currentDutyCycle = 0.33f
                    currentRawData = null
                }
                trimmed.startsWith("type:") -> {
                    currentType = trimmed.substringAfter("type:").trim()
                }
                trimmed.startsWith("protocol:") -> {
                    currentProtocol = trimmed.substringAfter("protocol:").trim()
                }
                trimmed.startsWith("address:") -> {
                    currentAddress = trimmed.substringAfter("address:").trim()
                }
                trimmed.startsWith("command:") -> {
                    currentCommand = trimmed.substringAfter("command:").trim()
                }
                trimmed.startsWith("frequency:") -> {
                    currentFrequency = trimmed.substringAfter("frequency:").trim().toIntOrNull() ?: 38000
                }
                trimmed.startsWith("duty_cycle:") -> {
                    currentDutyCycle = trimmed.substringAfter("duty_cycle:").trim().toFloatOrNull() ?: 0.33f
                }
                trimmed.startsWith("data:") -> {
                    val dataStr = trimmed.substringAfter("data:").trim()
                    currentRawData = dataStr.split(" ")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .toIntArray()
                }
            }
        }

        // Add last signal
        currentName?.let { name ->
            signals.add(createSignal(
                name, currentType, currentProtocol, currentAddress,
                currentCommand, currentFrequency, currentDutyCycle, currentRawData
            ))
        }

        return signals
    }

    private fun createSignal(
        name: String,
        type: String?,
        protocol: String?,
        address: String?,
        command: String?,
        frequency: Int,
        dutyCycle: Float,
        rawData: IntArray?
    ): IRSignal {
        // If it's a parsed signal, convert to raw timing
        val finalRawData = if (type == "parsed" && protocol != null && address != null && command != null) {
            convertParsedToRaw(protocol, address, command)
        } else {
            rawData
        }

        return IRSignal(
            name = name,
            type = type ?: "unknown",
            protocol = protocol,
            address = address,
            command = command,
            frequency = frequency,
            dutyCycle = dutyCycle,
            rawData = finalRawData
        )
    }

    /**
     * Convert parsed Flipper format to raw timing
     */
    private fun convertParsedToRaw(protocol: String, address: String, command: String): IntArray {
        // Parse hex address and command (Flipper uses little-endian hex bytes)
        val addrBytes = address.split(" ").mapNotNull { it.trim().toIntOrNull(16) }
        val cmdBytes = command.split(" ").mapNotNull { it.trim().toIntOrNull(16) }

        val addr = if (addrBytes.isNotEmpty()) addrBytes[0] else 0
        val cmd = if (cmdBytes.isNotEmpty()) cmdBytes[0] else 0

        return when (protocol.uppercase()) {
            "NEC", "NEC42", "NECEXT" -> convertNECToRaw(addr, cmd)
            "SAMSUNG32" -> convertSamsungToRaw(addr, cmd)
            "RC5", "RC5X" -> convertRC5ToRaw(addr, cmd)
            "RC6" -> convertRC6ToRaw(addr, cmd)
            "SIRC", "SIRC15", "SIRC20" -> convertSonyToRaw(addr, cmd)
            else -> convertNECToRaw(addr, cmd) // Default to NEC
        }
    }

    private fun convertNECToRaw(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()

        // NEC Header
        pattern.add(9000)
        pattern.add(4500)

        // Address byte
        addNECByte(pattern, address)
        // Inverted address
        addNECByte(pattern, address.inv() and 0xFF)
        // Command byte
        addNECByte(pattern, command)
        // Inverted command
        addNECByte(pattern, command.inv() and 0xFF)

        // Stop bit
        pattern.add(560)
        pattern.add(40000)

        return pattern.toIntArray()
    }

    private fun addNECByte(pattern: MutableList<Int>, byte: Int) {
        for (i in 0 until 8) {
            pattern.add(560)
            pattern.add(if ((byte shr i) and 1 == 1) 1690 else 560)
        }
    }

    private fun convertSamsungToRaw(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()

        // Samsung Header
        pattern.add(4500)
        pattern.add(4500)

        // Address (sent twice)
        addNECByte(pattern, address)
        addNECByte(pattern, address)

        // Command and inverted
        addNECByte(pattern, command)
        addNECByte(pattern, command.inv() and 0xFF)

        // Stop bit
        pattern.add(590)
        pattern.add(42000)

        return pattern.toIntArray()
    }

    private fun convertRC5ToRaw(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()
        val bitTime = 889

        // Start bits
        repeat(4) { pattern.add(bitTime) }

        // Toggle + Address (5 bits) + Command (6 bits)
        repeat(12) { pattern.add(bitTime); pattern.add(bitTime) }

        pattern.add(90000)
        return pattern.toIntArray()
    }

    private fun convertRC6ToRaw(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()

        // Leader
        pattern.add(2666)
        pattern.add(889)

        // Data
        repeat(20) { pattern.add(444); pattern.add(444) }

        pattern.add(90000)
        return pattern.toIntArray()
    }

    private fun convertSonyToRaw(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()

        // Header
        pattern.add(2400)
        pattern.add(600)

        // Command (7 bits)
        for (i in 0 until 7) {
            pattern.add(if ((command shr i) and 1 == 1) 1200 else 600)
            pattern.add(600)
        }

        // Address (5 bits)
        for (i in 0 until 5) {
            pattern.add(if ((address shr i) and 1 == 1) 1200 else 600)
            pattern.add(600)
        }

        pattern.add(25000)
        return pattern.toIntArray()
    }

    private fun getCategoryEmoji(category: String): String {
        return when (category.lowercase()) {
            "tvs", "tv" -> "📺"
            "ac", "air_conditioners", "air conditioners" -> "❄️"
            "audio", "speakers" -> "🔊"
            "fans" -> "🌀"
            "projectors" -> "📽️"
            "dvd", "dvd_players" -> "📀"
            "vcr" -> "📼"
            "cable", "set_top_boxes" -> "📦"
            "lighting", "lights" -> "💡"
            "cameras" -> "📷"
            else -> "📱"
        }
    }

    fun clearCache() {
        cachedCategories = null
    }
}

