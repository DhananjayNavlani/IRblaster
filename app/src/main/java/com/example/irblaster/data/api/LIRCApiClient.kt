package com.example.irblaster.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * API client for fetching IR codes from LIRC (Linux Infrared Remote Control)
 *
 * Primary source attempts (in order):
 * 1. GitHub mirror of LIRC remotes: probonopd/lirc-remotes
 * 2. Alternative: lirc-remotes/lirc-remotes
 *
 * LIRC is one of the oldest and most comprehensive IR code databases
 */
object LIRCApiClient {
    private const val TAG = "LIRCApiClient"
    // Try multiple possible GitHub mirrors
    private val API_URLS = listOf(
        "https://api.github.com/repos/probonopd/lirc-remotes/contents/remotes",
        "https://api.github.com/repos/lirc-remotes/lirc-remotes/contents/remotes"
    )
    private var workingApiUrl: String? = null

    // Cached data
    private var cachedBrands: List<Brand>? = null

    data class Brand(
        val name: String,
        val path: String
    )

    data class RemoteFile(
        val name: String,
        val downloadUrl: String
    )

    data class LIRCRemote(
        val name: String,
        val buttons: List<LIRCButton>,
        val frequency: Int,
        val flags: List<String>,
        val header: Pair<Int, Int>?,
        val one: Pair<Int, Int>?,
        val zero: Pair<Int, Int>?,
        val ptrail: Int?,
        val gap: Int?
    )

    data class LIRCButton(
        val name: String,
        val code: Long,
        val rawPattern: IntArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as LIRCButton
            return name == other.name && code == other.code
        }
        override fun hashCode(): Int = 31 * name.hashCode() + code.hashCode()
    }

    /**
     * Fetch list of all brands from LIRC
     */
    suspend fun fetchBrands(page: Int = 0, pageSize: Int = 30): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
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
     * Get total brand count
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
     * Search brands by query
     */
    suspend fun searchBrands(query: String): Result<List<Brand>> = withContext(Dispatchers.IO) {
        try {
            val allBrands = cachedBrands ?: run {
                val brands = fetchBrandsFromApi()
                cachedBrands = brands
                brands
            }

            if (query.isBlank()) {
                Result.success(allBrands.take(30))
            } else {
                val filtered = allBrands.filter { it.name.contains(query, ignoreCase = true) }
                Result.success(filtered)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching brands: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchBrandsFromApi(): List<Brand> {
        // Try each API URL until one works
        for (apiUrl in API_URLS) {
            try {
                val url = URL(apiUrl)
                Log.d(TAG, "Trying LIRC API: $apiUrl")

                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "IRBlaster-Android-App")
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    connection.disconnect()

                    val jsonArray = JSONArray(response)
                    val brands = mutableListOf<Brand>()

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

                    if (brands.isNotEmpty()) {
                        // Remember the working URL
                        workingApiUrl = apiUrl
                        Log.d(TAG, "LIRC API working: $apiUrl, found ${brands.size} brands")
                        return brands.sortedBy { it.name.lowercase() }
                    }
                } else {
                    Log.w(TAG, "HTTP ${connection.responseCode} from $apiUrl")
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch from $apiUrl: ${e.message}")
            }
        }

        Log.e(TAG, "All LIRC API URLs failed")
        return emptyList()
    }

    /**
     * Fetch remote files for a brand
     */
    suspend fun fetchRemoteFiles(brand: Brand): Result<List<RemoteFile>> = withContext(Dispatchers.IO) {
        try {
            val encodedBrand = java.net.URLEncoder.encode(brand.name, "UTF-8").replace("+", "%20")

            // Try to use the working URL first, otherwise try all URLs
            val urlsToTry = if (workingApiUrl != null) {
                listOf(workingApiUrl!!)
            } else {
                API_URLS
            }

            for (baseUrl in urlsToTry) {
                try {
                    // Extract the base repo path from the API URL
                    val repoPath = baseUrl.substringAfter("repos/").substringBefore("/contents")
                    val url = URL("https://api.github.com/repos/$repoPath/contents/remotes/$encodedBrand")
                    Log.d(TAG, "Fetching remote files from: $url")

                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                        setRequestProperty("User-Agent", "IRBlaster-Android-App")
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val files = mutableListOf<RemoteFile>()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonArray = JSONArray(response)

                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val fileName = item.getString("name")
                            // LIRC files typically have .lircd.conf or .conf extension
                            if (item.getString("type") == "file" &&
                                (fileName.endsWith(".conf") || fileName.endsWith(".lircd.conf"))) {
                                files.add(
                                    RemoteFile(
                                        name = fileName.removeSuffix(".lircd.conf").removeSuffix(".conf"),
                                        downloadUrl = item.getString("download_url")
                                    )
                                )
                            }
                        }
                        connection.disconnect()
                        return@withContext Result.success(files.sortedBy { it.name })
                    } else {
                        Log.e(TAG, "HTTP Error ${connection.responseCode} fetching remote files for ${brand.name}")
                        val errorStream = connection.errorStream?.bufferedReader()?.readText()
                        Log.e(TAG, "Error response: $errorStream")
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch remote files: ${e.message}")
                }
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote files: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Download and parse LIRC configuration file
     */
    suspend fun downloadRemote(remoteFile: RemoteFile): Result<List<LIRCButton>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(remoteFile.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "IRBlaster-Android-App")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val buttons = mutableListOf<LIRCButton>()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().readText()
                val remote = parseLIRCConfig(content)

                // Convert buttons to raw patterns
                remote?.let { r ->
                    r.buttons.forEach { button ->
                        val rawPattern = convertToRawPattern(r, button.code)
                        buttons.add(
                            LIRCButton(
                                name = button.name,
                                code = button.code,
                                rawPattern = rawPattern
                            )
                        )
                    }
                }
            }

            connection.disconnect()
            Result.success(buttons)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading remote: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse LIRC configuration file format
     * Example:
     * begin remote
     *   name  Samsung_BN59-00516A
     *   bits           32
     *   flags SPACE_ENC|CONST_LENGTH
     *   eps            30
     *   aeps          100
     *   header       4500  4500
     *   one           560  1690
     *   zero          560   560
     *   ptrail        560
     *   gap          108000
     *   frequency    38000
     *   begin codes
     *       KEY_POWER                0xE0E040BF
     *       KEY_VOLUMEUP             0xE0E0E01F
     *   end codes
     * end remote
     */
    private fun parseLIRCConfig(content: String): LIRCRemote? {
        val lines = content.lines()

        var inRemote = false
        var inCodes = false
        var name = ""
        var frequency = 38000
        var flags = mutableListOf<String>()
        var header: Pair<Int, Int>? = null
        var one: Pair<Int, Int>? = null
        var zero: Pair<Int, Int>? = null
        var ptrail: Int? = null
        var gap: Int? = null
        val buttons = mutableListOf<LIRCButton>()

        for (line in lines) {
            val trimmed = line.trim()

            when {
                trimmed.startsWith("begin remote") -> inRemote = true
                trimmed.startsWith("end remote") -> {
                    if (inRemote && buttons.isNotEmpty()) {
                        return LIRCRemote(
                            name = name,
                            buttons = buttons,
                            frequency = frequency,
                            flags = flags,
                            header = header,
                            one = one,
                            zero = zero,
                            ptrail = ptrail,
                            gap = gap
                        )
                    }
                    inRemote = false
                }
                trimmed.startsWith("begin codes") -> inCodes = true
                trimmed.startsWith("end codes") -> inCodes = false
                inRemote && !inCodes -> {
                    // Parse remote parameters
                    val parts = trimmed.split(Regex("\\s+"))
                    when {
                        parts.size >= 2 && parts[0] == "name" -> {
                            name = parts.drop(1).joinToString(" ")
                        }
                        parts.size >= 2 && parts[0] == "frequency" -> {
                            frequency = parts[1].toIntOrNull() ?: 38000
                        }
                        parts.size >= 2 && parts[0] == "flags" -> {
                            flags = parts[1].split("|").toMutableList()
                        }
                        parts.size >= 3 && parts[0] == "header" -> {
                            val h1 = parts[1].toIntOrNull() ?: 0
                            val h2 = parts[2].toIntOrNull() ?: 0
                            header = h1 to h2
                        }
                        parts.size >= 3 && parts[0] == "one" -> {
                            val o1 = parts[1].toIntOrNull() ?: 0
                            val o2 = parts[2].toIntOrNull() ?: 0
                            one = o1 to o2
                        }
                        parts.size >= 3 && parts[0] == "zero" -> {
                            val z1 = parts[1].toIntOrNull() ?: 0
                            val z2 = parts[2].toIntOrNull() ?: 0
                            zero = z1 to z2
                        }
                        parts.size >= 2 && parts[0] == "ptrail" -> {
                            ptrail = parts[1].toIntOrNull()
                        }
                        parts.size >= 2 && parts[0] == "gap" -> {
                            gap = parts[1].toIntOrNull()
                        }
                    }
                }
                inCodes -> {
                    // Parse button codes
                    val parts = trimmed.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        val buttonName = parts[0]
                        val codeStr = parts[1]
                        val code = try {
                            if (codeStr.startsWith("0x") || codeStr.startsWith("0X")) {
                                codeStr.substring(2).toLong(16)
                            } else {
                                codeStr.toLongOrNull() ?: 0L
                            }
                        } catch (e: Exception) {
                            0L
                        }

                        if (buttonName.isNotEmpty() && !buttonName.startsWith("#")) {
                            buttons.add(LIRCButton(name = buttonName, code = code))
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Convert LIRC code to raw timing pattern
     */
    private fun convertToRawPattern(remote: LIRCRemote, code: Long): IntArray {
        val pattern = mutableListOf<Int>()

        // Add header if present
        remote.header?.let { (mark, space) ->
            pattern.add(mark)
            pattern.add(space)
        }

        // Get bit timing
        val oneMark = remote.one?.first ?: 560
        val oneSpace = remote.one?.second ?: 1690
        val zeroMark = remote.zero?.first ?: 560
        val zeroSpace = remote.zero?.second ?: 560

        // Determine number of bits (usually 32 for NEC-like)
        val bits = when {
            code > 0xFFFFFF -> 32
            code > 0xFFFF -> 24
            code > 0xFF -> 16
            else -> 8
        }

        // Check if we should send MSB first or LSB first
        val isSpaceEnc = remote.flags.any { it.contains("SPACE_ENC", ignoreCase = true) }

        // Add data bits
        for (i in (bits - 1) downTo 0) {
            val bit = (code shr i) and 1L
            if (bit == 1L) {
                pattern.add(oneMark)
                pattern.add(oneSpace)
            } else {
                pattern.add(zeroMark)
                pattern.add(zeroSpace)
            }
        }

        // Add trail pulse if present
        remote.ptrail?.let { pattern.add(it) }

        // Add gap
        pattern.add(remote.gap ?: 40000)

        return pattern.toIntArray()
    }

    /**
     * Get frequency for a remote (default 38kHz)
     */
    fun getFrequency(remote: LIRCRemote): Int = remote.frequency

    /**
     * Clear cached data
     */
    fun clearCache() {
        cachedBrands = null
    }
}

