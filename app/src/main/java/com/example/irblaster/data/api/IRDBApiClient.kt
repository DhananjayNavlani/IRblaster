package com.example.irblaster.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * API client for fetching IR codes from IRDB (GitHub-based open database)
 * Source: https://github.com/bengtmartensson/IrpTransmogrifier (actively maintained fork)
 * Fallback: Uses raw GitHub content from archived copies
 *
 * Repository structure:
 * codes/
 *   Brand_Name/
 *     Device_Type/
 *       model.csv
 *
 * This uses the GitHub API to fetch IR code files
 */
object IRDBApiClient {
    private const val TAG = "IRDBApiClient"

    // Primary: Simon Peter's IRDB (still active as of 2024)
    private const val PRIMARY_API_URL = "https://api.github.com/repos/simon-weber/irdb/contents/codes"
    private const val PRIMARY_BASE_URL = "https://raw.githubusercontent.com/simon-weber/irdb/master/codes"

    // Fallback: Alternative IRDB mirror
    private const val FALLBACK_API_URL = "https://api.github.com/repos/probonopd/irdb/contents/codes"
    private const val FALLBACK_BASE_URL = "https://raw.githubusercontent.com/probonopd/irdb/master/codes"

    // Currently active URLs (will switch on failure)
    private var currentApiUrl = PRIMARY_API_URL
    private var currentBaseUrl = PRIMARY_BASE_URL

    // Cached brand list
    private var cachedBrands: List<Brand>? = null

    data class Brand(
        val name: String,
        val path: String,
        val deviceTypes: List<String> = emptyList()
    )

    data class DeviceType(
        val name: String,
        val path: String,
        val files: List<String> = emptyList()
    )

    data class IRCodeFile(
        val name: String,
        val path: String,
        val functions: List<IRFunction> = emptyList()
    )

    data class IRFunction(
        val name: String,
        val protocol: String,
        val device: String,
        val subdevice: String,
        val function: String,
        val frequency: Int = 38000,
        val pattern: IntArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as IRFunction
            return name == other.name && protocol == other.protocol
        }
        override fun hashCode(): Int = 31 * name.hashCode() + protocol.hashCode()
    }

    /**
     * Fetch list of all brands from IRDB
     */
    suspend fun fetchBrands(page: Int = 0, pageSize: Int = 20): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            // Use cached brands if available
            val allBrands = cachedBrands ?: run {
                val brands = fetchBrandsFromApi()
                cachedBrands = brands
                brands
            }

            // Apply pagination
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, allBrands.size)

            if (startIndex >= allBrands.size) {
                Result.success(emptyList())
            } else {
                Result.success(allBrands.subList(startIndex, endIndex))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching brands: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get total brand count for pagination
     */
    suspend fun getTotalBrandCount(): Int = withContext(Dispatchers.IO) {
        try {
            val allBrands = cachedBrands ?: run {
                val brands = fetchBrandsFromApi()
                cachedBrands = brands
                brands
            }
            allBrands.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Search brands by query - searches through all brands from API
     */
    suspend fun searchBrands(query: String): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            // Ensure brands are loaded
            val allBrands = cachedBrands ?: run {
                val brands = fetchBrandsFromApi()
                cachedBrands = brands
                brands
            }

            if (query.isBlank()) {
                Result.success(allBrands.take(30))
            } else {
                // Filter brands by query (case-insensitive)
                val filteredBrands = allBrands.filter { brand ->
                    brand.name.contains(query, ignoreCase = true)
                }
                Result.success(filteredBrands)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching brands: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchBrandsFromApi(): List<Brand> {
        // Try primary URL first, then fallback
        val urls = listOf(
            PRIMARY_API_URL to PRIMARY_BASE_URL,
            FALLBACK_API_URL to FALLBACK_BASE_URL
        )

        for ((apiUrl, baseUrl) in urls) {
            try {
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "IRBlaster-Android-App")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonArray = JSONArray(response)
                        val brands = mutableListOf<Brand>()

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            if (item.getString("type") == "dir") {
                                brands.add(
                                    Brand(
                                        name = item.getString("name"),
                                        path = item.getString("path")
                                    )
                                )
                            }
                        }

                        if (brands.isNotEmpty()) {
                            // Update current URLs to the working ones
                            currentApiUrl = apiUrl
                            currentBaseUrl = baseUrl
                            Log.d(TAG, "Successfully fetched ${brands.size} brands from $apiUrl")
                            return brands.sortedBy { it.name.lowercase() }
                        }
                    } else {
                        Log.w(TAG, "HTTP Error ${connection.responseCode} from $apiUrl, trying fallback...")
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch from $apiUrl: ${e.message}, trying fallback...")
            }
        }

        Log.e(TAG, "All API URLs failed")
        return emptyList()
    }

    /**
     * Fetch device types for a brand (e.g., TV, DVD, AC)
     */
    suspend fun fetchDeviceTypes(brand: Brand): Result<List<DeviceType>> = withContext(Dispatchers.IO) {
        try {
            // Use the path from the brand object which already contains the full path
            val encodedPath = java.net.URLEncoder.encode(brand.name, "UTF-8").replace("+", "%20")
            val url = URL("$currentApiUrl/$encodedPath")
            Log.d(TAG, "Fetching device types from: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val deviceTypes = mutableListOf<DeviceType>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (item.getString("type") == "dir") {
                        deviceTypes.add(
                            DeviceType(
                                name = item.getString("name"),
                                path = item.getString("path")
                            )
                        )
                    }
                }
            } else {
                Log.e(TAG, "HTTP Error ${connection.responseCode} fetching device types for ${brand.name}")
                val errorStream = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "Error response: $errorStream")
            }

            connection.disconnect()
            Result.success(deviceTypes.sortedBy { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device types: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch IR code files for a device type
     */
    suspend fun fetchCodeFiles(brand: Brand, deviceType: DeviceType): Result<List<IRCodeFile>> = withContext(Dispatchers.IO) {
        try {
            val encodedBrand = java.net.URLEncoder.encode(brand.name, "UTF-8").replace("+", "%20")
            val encodedType = java.net.URLEncoder.encode(deviceType.name, "UTF-8").replace("+", "%20")
            val url = URL("$currentApiUrl/$encodedBrand/$encodedType")
            Log.d(TAG, "Fetching code files from: $url")

            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val files = mutableListOf<IRCodeFile>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    if (item.getString("type") == "file" && item.getString("name").endsWith(".csv")) {
                        files.add(
                            IRCodeFile(
                                name = item.getString("name").removeSuffix(".csv"),
                                path = item.getString("download_url")
                            )
                        )
                    }
                }
            } else {
                Log.e(TAG, "HTTP Error ${connection.responseCode} fetching code files")
                val errorStream = connection.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "Error response: $errorStream")
            }

            connection.disconnect()
            Result.success(files)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching code files: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Download and parse IR codes from a CSV file
     */
    suspend fun downloadCodes(codeFile: IRCodeFile): Result<List<IRFunction>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(codeFile.path)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val functions = mutableListOf<IRFunction>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val lines = connection.inputStream.bufferedReader().readLines()

                // Parse CSV - first line is header
                if (lines.size > 1) {
                    val header = lines[0].split(",").map { it.trim() }
                    val functionIndex = header.indexOf("functionname")
                    val protocolIndex = header.indexOf("protocol")
                    val deviceIndex = header.indexOf("device")
                    val subdeviceIndex = header.indexOf("subdevice")
                    val funcIndex = header.indexOf("function")

                    for (i in 1 until lines.size) {
                        try {
                            val values = lines[i].split(",").map { it.trim() }
                            if (values.size >= 5) {
                                val funcName = if (functionIndex >= 0 && functionIndex < values.size)
                                    values[functionIndex] else "Function $i"
                                val protocol = if (protocolIndex >= 0 && protocolIndex < values.size)
                                    values[protocolIndex] else "NEC"
                                val device = if (deviceIndex >= 0 && deviceIndex < values.size)
                                    values[deviceIndex] else "0"
                                val subdevice = if (subdeviceIndex >= 0 && subdeviceIndex < values.size)
                                    values[subdeviceIndex] else "-1"
                                val func = if (funcIndex >= 0 && funcIndex < values.size)
                                    values[funcIndex] else "0"

                                // Convert protocol codes to raw timing pattern
                                val pattern = convertToRawPattern(protocol, device, subdevice, func)
                                val frequency = getFrequencyForProtocol(protocol)

                                functions.add(
                                    IRFunction(
                                        name = funcName,
                                        protocol = protocol,
                                        device = device,
                                        subdevice = subdevice,
                                        function = func,
                                        frequency = frequency,
                                        pattern = pattern
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing line $i: ${e.message}")
                        }
                    }
                }
            }

            connection.disconnect()
            Result.success(functions)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading codes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get carrier frequency for different protocols
     */
    private fun getFrequencyForProtocol(protocol: String): Int {
        return when (protocol.uppercase()) {
            "NEC", "NEC1", "NEC2", "NECX" -> 38000
            "RC5", "RC5X", "RC6" -> 36000
            "SONY", "SONY12", "SONY15", "SONY20" -> 40000
            "SAMSUNG", "SAMSUNG32" -> 38000
            "PANASONIC", "KASEIKYO" -> 37000
            "JVC" -> 38000
            "SHARP" -> 38000
            "DENON" -> 38000
            "PIONEER" -> 40000
            else -> 38000
        }
    }

    /**
     * Convert protocol-based IR codes to raw timing patterns
     * This is a simplified conversion - real implementation would need full protocol specs
     */
    private fun convertToRawPattern(protocol: String, device: String, subdevice: String, function: String): IntArray {
        return when (protocol.uppercase()) {
            "NEC", "NEC1", "NEC2", "NECX" -> convertNEC(device, subdevice, function)
            "SAMSUNG", "SAMSUNG32" -> convertSamsung(device, function)
            "SONY", "SONY12", "SONY15", "SONY20" -> convertSony(device, function)
            "RC5", "RC5X" -> convertRC5(device, function)
            "RC6" -> convertRC6(device, function)
            else -> convertNEC(device, subdevice, function) // Default to NEC
        }
    }

    /**
     * Convert NEC protocol to raw timing
     */
    private fun convertNEC(device: String, subdevice: String, function: String): IntArray {
        val deviceNum = device.toIntOrNull() ?: 0
        val subdeviceNum = if (subdevice == "-1") (deviceNum.inv() and 0xFF) else (subdevice.toIntOrNull() ?: 0)
        val funcNum = function.toIntOrNull() ?: 0
        val funcInv = (funcNum.inv() and 0xFF)

        val pattern = mutableListOf<Int>()

        // NEC Header: 9000µs mark, 4500µs space
        pattern.add(9000)
        pattern.add(4500)

        // Add device byte
        addNECByte(pattern, deviceNum)
        // Add subdevice byte
        addNECByte(pattern, subdeviceNum)
        // Add function byte
        addNECByte(pattern, funcNum)
        // Add inverted function byte
        addNECByte(pattern, funcInv)

        // Final burst
        pattern.add(560)
        pattern.add(40000)

        return pattern.toIntArray()
    }

    private fun addNECByte(pattern: MutableList<Int>, byte: Int) {
        for (i in 0 until 8) {
            pattern.add(560) // Mark
            if ((byte shr i) and 1 == 1) {
                pattern.add(1690) // Space for 1
            } else {
                pattern.add(560) // Space for 0
            }
        }
    }

    /**
     * Convert Samsung protocol to raw timing
     */
    private fun convertSamsung(device: String, function: String): IntArray {
        val deviceNum = device.toIntOrNull() ?: 7
        val funcNum = function.toIntOrNull() ?: 0

        val pattern = mutableListOf<Int>()

        // Samsung Header: 4500µs mark, 4500µs space
        pattern.add(4500)
        pattern.add(4500)

        // Device (sent twice)
        addNECByte(pattern, deviceNum)
        addNECByte(pattern, deviceNum)

        // Function and inverted function
        addNECByte(pattern, funcNum)
        addNECByte(pattern, funcNum.inv() and 0xFF)

        // Final burst
        pattern.add(590)
        pattern.add(42000)

        return pattern.toIntArray()
    }

    /**
     * Convert Sony protocol to raw timing
     */
    private fun convertSony(device: String, function: String): IntArray {
        val deviceNum = device.toIntOrNull() ?: 1
        val funcNum = function.toIntOrNull() ?: 0

        val pattern = mutableListOf<Int>()

        // Sony Header: 2400µs mark
        pattern.add(2400)
        pattern.add(600)

        // Command (7 bits, LSB first)
        for (i in 0 until 7) {
            if ((funcNum shr i) and 1 == 1) {
                pattern.add(1200)
            } else {
                pattern.add(600)
            }
            pattern.add(600)
        }

        // Device (5 bits for SONY12, LSB first)
        for (i in 0 until 5) {
            if ((deviceNum shr i) and 1 == 1) {
                pattern.add(1200)
            } else {
                pattern.add(600)
            }
            pattern.add(600)
        }

        // Gap
        pattern.add(25000)

        return pattern.toIntArray()
    }

    /**
     * Convert RC5 protocol to raw timing
     */
    private fun convertRC5(device: String, function: String): IntArray {
        val deviceNum = device.toIntOrNull() ?: 0
        val funcNum = function.toIntOrNull() ?: 0

        val pattern = mutableListOf<Int>()

        // RC5 uses Manchester encoding at 36kHz
        // Bit time is 889µs (1.778ms per bit)

        // Start bits (2x '1')
        pattern.add(889)
        pattern.add(889)
        pattern.add(889)
        pattern.add(889)

        // Toggle bit (we'll use 0)
        pattern.add(889)
        pattern.add(889)

        // Device address (5 bits, MSB first)
        for (i in 4 downTo 0) {
            val bit = (deviceNum shr i) and 1
            if (bit == 1) {
                pattern.add(889)
                pattern.add(889)
            } else {
                pattern.add(889)
                pattern.add(889)
            }
        }

        // Command (6 bits, MSB first)
        for (i in 5 downTo 0) {
            val bit = (funcNum shr i) and 1
            if (bit == 1) {
                pattern.add(889)
                pattern.add(889)
            } else {
                pattern.add(889)
                pattern.add(889)
            }
        }

        pattern.add(90000)

        return pattern.toIntArray()
    }

    /**
     * Convert RC6 protocol to raw timing
     */
    private fun convertRC6(device: String, function: String): IntArray {
        val pattern = mutableListOf<Int>()

        // RC6 Leader
        pattern.add(2666)
        pattern.add(889)

        // Start bit
        pattern.add(444)
        pattern.add(444)

        // Mode bits and data (simplified)
        for (i in 0 until 16) {
            pattern.add(444)
            pattern.add(444)
        }

        pattern.add(90000)

        return pattern.toIntArray()
    }

    /**
     * Clear cached data
     */
    fun clearCache() {
        cachedBrands = null
    }
}

