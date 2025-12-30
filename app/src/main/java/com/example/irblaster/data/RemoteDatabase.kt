package com.example.irblaster.data

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

// Type converter for IntArray (IR codes)
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromIntArray(value: IntArray): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntArray(value: String): IntArray {
        val type = object : TypeToken<IntArray>() {}.type
        return gson.fromJson(value, type)
    }
}

// Entity for a saved remote
@Entity(tableName = "remotes")
data class RemoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Entity for a button in a remote
@Entity(
    tableName = "remote_buttons",
    foreignKeys = [
        ForeignKey(
            entity = RemoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["remoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("remoteId")]
)
data class RemoteButtonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: Long,
    val name: String,
    val emoji: String,
    val irCode: IntArray,
    val frequency: Int = 38000,
    val orderIndex: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RemoteButtonEntity
        if (id != other.id) return false
        if (remoteId != other.remoteId) return false
        if (name != other.name) return false
        if (emoji != other.emoji) return false
        if (!irCode.contentEquals(other.irCode)) return false
        if (frequency != other.frequency) return false
        if (orderIndex != other.orderIndex) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + remoteId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + emoji.hashCode()
        result = 31 * result + irCode.contentHashCode()
        result = 31 * result + frequency
        result = 31 * result + orderIndex
        return result
    }
}

// Data class for Remote with its buttons
data class RemoteWithButtons(
    @Embedded val remote: RemoteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "remoteId"
    )
    val buttons: List<RemoteButtonEntity>
)

// DAO for remotes
@Dao
interface RemoteDao {
    @Query("SELECT * FROM remotes ORDER BY createdAt DESC")
    fun getAllRemotes(): Flow<List<RemoteEntity>>

    @Query("SELECT * FROM remotes WHERE id = :remoteId")
    suspend fun getRemoteById(remoteId: Long): RemoteEntity?

    @Transaction
    @Query("SELECT * FROM remotes WHERE id = :remoteId")
    fun getRemoteWithButtons(remoteId: Long): Flow<RemoteWithButtons?>

    @Transaction
    @Query("SELECT * FROM remotes ORDER BY createdAt DESC")
    fun getAllRemotesWithButtons(): Flow<List<RemoteWithButtons>>

    @Insert
    suspend fun insertRemote(remote: RemoteEntity): Long

    @Update
    suspend fun updateRemote(remote: RemoteEntity)

    @Delete
    suspend fun deleteRemote(remote: RemoteEntity)

    @Query("DELETE FROM remotes WHERE id = :remoteId")
    suspend fun deleteRemoteById(remoteId: Long)
}

// DAO for buttons
@Dao
interface RemoteButtonDao {
    @Query("SELECT * FROM remote_buttons WHERE remoteId = :remoteId ORDER BY orderIndex")
    fun getButtonsForRemote(remoteId: Long): Flow<List<RemoteButtonEntity>>

    @Insert
    suspend fun insertButton(button: RemoteButtonEntity): Long

    @Update
    suspend fun updateButton(button: RemoteButtonEntity)

    @Delete
    suspend fun deleteButton(button: RemoteButtonEntity)

    @Query("DELETE FROM remote_buttons WHERE id = :buttonId")
    suspend fun deleteButtonById(buttonId: Long)

    @Query("SELECT MAX(orderIndex) FROM remote_buttons WHERE remoteId = :remoteId")
    suspend fun getMaxOrderIndex(remoteId: Long): Int?
}

// Room Database
@Database(
    entities = [RemoteEntity::class, RemoteButtonEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RemoteDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
    abstract fun remoteButtonDao(): RemoteButtonDao

    companion object {
        @Volatile
        private var INSTANCE: RemoteDatabase? = null

        fun getDatabase(context: Context): RemoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RemoteDatabase::class.java,
                    "ir_remote_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

