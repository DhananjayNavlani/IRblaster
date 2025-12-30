package com.example.irblaster.ui.screens

import android.hardware.ConsumerIrManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblaster.data.RemoteButtonEntity
import com.example.irblaster.data.RemoteWithButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRemoteScreen(
    remoteWithButtons: RemoteWithButtons?,
    irManager: ConsumerIrManager?,
    onBackClick: () -> Unit,
    onEditRemote: () -> Unit,
    onDeleteRemote: () -> Unit,
    onEditButton: (RemoteButtonEntity) -> Unit,
    onDeleteButton: (RemoteButtonEntity) -> Unit,
    onAddButtonClick: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteRemoteDialog by remember { mutableStateOf(false) }
    var buttonToDelete by remember { mutableStateOf<RemoteButtonEntity?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    fun sendIRSignal(button: RemoteButtonEntity) {
        if (irManager?.hasIrEmitter() == true) {
            try {
                Log.d("IRBlaster", "========== IR TRANSMISSION ==========")
                Log.d("IRBlaster", "Button: ${button.name}")
                Log.d("IRBlaster", "Carrier Frequency: ${button.frequency} Hz (${button.frequency / 1000} kHz)")
                Log.d("IRBlaster", "Pattern Length: ${button.irCode.size} values")
                Log.d("IRBlaster", "======================================")

                irManager.transmit(button.frequency, button.irCode)
                Toast.makeText(context, "Sent: ${button.name} @ ${button.frequency / 1000}kHz", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("IRBlaster", "Error transmitting: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No IR Blaster available", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (remoteWithButtons != null) {
                            Text(
                                text = "${remoteWithButtons.remote.emoji} ${remoteWithButtons.remote.name}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text("Loading...")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { isEditMode = !isEditMode }) {
                        Text(if (isEditMode) "Done" else "Edit")
                    }
                    IconButton(onClick = onEditRemote) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Remote")
                    }
                    IconButton(onClick = { showDeleteRemoteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Remote")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddButtonClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Button")
            }
        }
    ) { paddingValues ->
        if (remoteWithButtons == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (remoteWithButtons.buttons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No buttons yet",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add buttons from IR Tester",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(remoteWithButtons.buttons.sortedBy { it.orderIndex }) { button ->
                    RemoteButtonCard(
                        button = button,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) {
                                onEditButton(button)
                            } else {
                                sendIRSignal(button)
                            }
                        },
                        onDelete = { buttonToDelete = button }
                    )
                }
            }
        }
    }

    // Delete Remote Confirmation Dialog
    if (showDeleteRemoteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRemoteDialog = false },
            title = { Text("Delete Remote?") },
            text = { Text("Are you sure you want to delete this remote and all its buttons?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteRemoteDialog = false
                        onDeleteRemote()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRemoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Button Confirmation Dialog
    buttonToDelete?.let { button ->
        AlertDialog(
            onDismissRequest = { buttonToDelete = null },
            title = { Text("Delete Button?") },
            text = { Text("Are you sure you want to delete '${button.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteButton(button)
                        buttonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { buttonToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RemoteButtonCard(
    button: RemoteButtonEntity,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditMode)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (button.emoji.isNotEmpty()) {
                    Text(
                        text = button.emoji,
                        fontSize = 24.sp
                    )
                }
                Text(
                    text = button.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

