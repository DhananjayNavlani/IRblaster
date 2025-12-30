package com.example.irblaster

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.irblaster.data.RemoteDatabase
import com.example.irblaster.data.RemoteRepository
import com.example.irblaster.ui.theme.IRblasterTheme

class MainActivity : ComponentActivity() {
    private var irManager: ConsumerIrManager? = null
    private lateinit var database: RemoteDatabase
    private lateinit var repository: RemoteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize IR Manager
        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

        // Initialize Database
        database = RemoteDatabase.getDatabase(this)
        repository = RemoteRepository(database.remoteDao(), database.remoteButtonDao())

        enableEdgeToEdge()
        setContent {
            IRblasterTheme {
                MainApp(
                    irManager = irManager,
                    repository = repository
                )
            }
        }
    }
}