package com.example.irblaster.data.api

import android.util.Log

/**
 * Unified IR Code API Manager
 * Supports multiple data sources for IR codes
 */
object IRCodeApiManager {
    private const val TAG = "IRCodeApiManager"

    enum class DataSource(val displayName: String, val emoji: String, val description: String) {
        IRDB("IRDB", "📡", "probonopd/irdb - 500+ brands, CSV format"),
        FLIPPER("Flipper", "🐬", "Flipper Zero community database"),
        LIRC("LIRC", "🐧", "Linux Infrared Remote Control - oldest IR database")
    }

    /**
     * Unified brand representation across different sources
     */
    data class UnifiedBrand(
        val name: String,
        val source: DataSource,
        val path: String,
        val category: String? = null // For Flipper which has categories
    )

    /**
     * Unified device type
     */
    data class UnifiedDeviceType(
        val name: String,
        val path: String,
        val source: DataSource
    )

    /**
     * Unified IR code file
     */
    data class UnifiedCodeFile(
        val name: String,
        val path: String,
        val source: DataSource
    )

    /**
     * Unified IR signal/function
     */
    data class UnifiedIRSignal(
        val name: String,
        val frequency: Int,
        val pattern: IntArray,
        val protocol: String?,
        val source: DataSource
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as UnifiedIRSignal
            return name == other.name && pattern.contentEquals(other.pattern)
        }
        override fun hashCode(): Int = 31 * name.hashCode() + pattern.contentHashCode()
    }

    /**
     * Get all available data sources
     */
    fun getAvailableSources(): List<DataSource> = DataSource.entries

    /**
     * Search brands across a specific source
     */
    suspend fun searchBrands(query: String, source: DataSource): Result<List<UnifiedBrand>> {
        return when (source) {
            DataSource.IRDB -> {
                IRDBApiClient.searchBrands(query).map { brands ->
                    brands.map { brand ->
                        UnifiedBrand(
                            name = brand.name,
                            source = DataSource.IRDB,
                            path = brand.path
                        )
                    }
                }
            }
            DataSource.FLIPPER -> {
                FlipperIRDBClient.searchBrands(query).map { results ->
                    results.map { (category, brand) ->
                        UnifiedBrand(
                            name = brand.name,
                            source = DataSource.FLIPPER,
                            path = brand.path,
                            category = category.name
                        )
                    }
                }
            }
            DataSource.LIRC -> {
                LIRCApiClient.searchBrands(query).map { brands ->
                    brands.map { brand ->
                        UnifiedBrand(
                            name = brand.name,
                            source = DataSource.LIRC,
                            path = brand.path
                        )
                    }
                }
            }
        }
    }

    /**
     * Fetch brands with pagination for a source
     */
    suspend fun fetchBrands(source: DataSource, page: Int = 0, pageSize: Int = 30): Result<List<UnifiedBrand>> {
        return when (source) {
            DataSource.IRDB -> {
                IRDBApiClient.fetchBrands(page, pageSize).map { brands ->
                    brands.map { brand ->
                        UnifiedBrand(
                            name = brand.name,
                            source = DataSource.IRDB,
                            path = brand.path
                        )
                    }
                }
            }
            DataSource.FLIPPER -> {
                // Flipper has categories first, then brands
                // For simplicity, fetch all categories as "brands"
                FlipperIRDBClient.fetchCategories().map { categories ->
                    categories.map { category ->
                        UnifiedBrand(
                            name = category.name,
                            source = DataSource.FLIPPER,
                            path = category.path,
                            category = "Category"
                        )
                    }
                }
            }
            DataSource.LIRC -> {
                LIRCApiClient.fetchBrands(page, pageSize).map { brands ->
                    brands.map { brand ->
                        UnifiedBrand(
                            name = brand.name,
                            source = DataSource.LIRC,
                            path = brand.path
                        )
                    }
                }
            }
        }
    }

    /**
     * Get total brand count for a source
     */
    suspend fun getTotalBrandCount(source: DataSource): Int {
        return when (source) {
            DataSource.IRDB -> IRDBApiClient.getTotalBrandCount()
            DataSource.FLIPPER -> FlipperIRDBClient.fetchCategories().getOrNull()?.size ?: 0
            DataSource.LIRC -> LIRCApiClient.getTotalBrandCount()
        }
    }

    /**
     * Fetch device types / sub-categories for a brand
     */
    suspend fun fetchDeviceTypes(brand: UnifiedBrand): Result<List<UnifiedDeviceType>> {
        return when (brand.source) {
            DataSource.IRDB -> {
                val irdbBrand = IRDBApiClient.Brand(brand.name, brand.path)
                IRDBApiClient.fetchDeviceTypes(irdbBrand).map { types ->
                    types.map { type ->
                        UnifiedDeviceType(
                            name = type.name,
                            path = type.path,
                            source = DataSource.IRDB
                        )
                    }
                }
            }
            DataSource.FLIPPER -> {
                // For Flipper, if it's a category, fetch brands within it
                val category = FlipperIRDBClient.Category(brand.name, brand.path)
                FlipperIRDBClient.fetchBrands(category).map { brands ->
                    brands.map { b ->
                        UnifiedDeviceType(
                            name = b.name,
                            path = b.path,
                            source = DataSource.FLIPPER
                        )
                    }
                }
            }
            DataSource.LIRC -> {
                // For LIRC, fetch remote files directly as "device types"
                val lircBrand = LIRCApiClient.Brand(brand.name, brand.path)
                LIRCApiClient.fetchRemoteFiles(lircBrand).map { files ->
                    files.map { file ->
                        UnifiedDeviceType(
                            name = file.name,
                            path = file.downloadUrl,
                            source = DataSource.LIRC
                        )
                    }
                }
            }
        }
    }

    /**
     * Fetch code files for a device type
     */
    suspend fun fetchCodeFiles(brand: UnifiedBrand, deviceType: UnifiedDeviceType): Result<List<UnifiedCodeFile>> {
        return when (brand.source) {
            DataSource.IRDB -> {
                val irdbBrand = IRDBApiClient.Brand(brand.name, brand.path)
                val irdbType = IRDBApiClient.DeviceType(deviceType.name, deviceType.path)
                IRDBApiClient.fetchCodeFiles(irdbBrand, irdbType).map { files ->
                    files.map { file ->
                        UnifiedCodeFile(
                            name = file.name,
                            path = file.path,
                            source = DataSource.IRDB
                        )
                    }
                }
            }
            DataSource.FLIPPER -> {
                // For Flipper, deviceType is actually a brand, fetch IR files
                val flipperBrand = FlipperIRDBClient.Brand(deviceType.name, deviceType.path)
                FlipperIRDBClient.fetchIRFiles(flipperBrand).map { files ->
                    files.map { file ->
                        UnifiedCodeFile(
                            name = file.name,
                            path = file.downloadUrl,
                            source = DataSource.FLIPPER
                        )
                    }
                }
            }
            DataSource.LIRC -> {
                // For LIRC, deviceType already contains the remote file info
                // Return it as a single code file
                Result.success(listOf(
                    UnifiedCodeFile(
                        name = deviceType.name,
                        path = deviceType.path,
                        source = DataSource.LIRC
                    )
                ))
            }
        }
    }

    /**
     * Download IR codes from a file
     */
    suspend fun downloadCodes(codeFile: UnifiedCodeFile): Result<List<UnifiedIRSignal>> {
        return when (codeFile.source) {
            DataSource.IRDB -> {
                val irdbFile = IRDBApiClient.IRCodeFile(codeFile.name, codeFile.path)
                IRDBApiClient.downloadCodes(irdbFile).map { functions ->
                    functions.mapNotNull { func ->
                        func.pattern?.let { pattern ->
                            UnifiedIRSignal(
                                name = func.name,
                                frequency = func.frequency,
                                pattern = pattern,
                                protocol = func.protocol,
                                source = DataSource.IRDB
                            )
                        }
                    }
                }
            }
            DataSource.FLIPPER -> {
                val flipperFile = FlipperIRDBClient.IRFile(codeFile.name, codeFile.path)
                FlipperIRDBClient.downloadIRFile(flipperFile).map { signals ->
                    signals.mapNotNull { signal ->
                        signal.rawData?.let { pattern ->
                            UnifiedIRSignal(
                                name = signal.name,
                                frequency = signal.frequency,
                                pattern = pattern,
                                protocol = signal.protocol,
                                source = DataSource.FLIPPER
                            )
                        }
                    }
                }
            }
            DataSource.LIRC -> {
                val lircFile = LIRCApiClient.RemoteFile(codeFile.name, codeFile.path)
                LIRCApiClient.downloadRemote(lircFile).map { buttons ->
                    buttons.mapNotNull { button ->
                        button.rawPattern?.let { pattern ->
                            UnifiedIRSignal(
                                name = button.name,
                                frequency = 38000, // LIRC default
                                pattern = pattern,
                                protocol = "LIRC",
                                source = DataSource.LIRC
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear all cached data
     */
    fun clearAllCaches() {
        IRDBApiClient.clearCache()
        FlipperIRDBClient.clearCache()
        LIRCApiClient.clearCache()
    }
}

