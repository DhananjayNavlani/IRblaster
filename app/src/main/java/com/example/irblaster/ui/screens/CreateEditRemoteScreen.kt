package com.example.irblaster.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.irblaster.data.RemoteEntity

// Available emojis for remotes
val remoteEmojis = listOf(
    "📺", "🎬", "🔊", "🎵", "💡", "❄️", "🌡️", "🎮",
    "📻", "🔌", "🏠", "🛋️", "🎤", "📀", "📹", "🖥️"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditRemoteScreen(
    existingRemote: RemoteEntity? = null,
    onSave: (name: String, emoji: String) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(existingRemote?.name ?: "") }
    var selectedEmoji by remember { mutableStateOf(existingRemote?.emoji ?: "📺") }

    val isEditMode = existingRemote != null
    val title = if (isEditMode) "Edit Remote" else "Create Remote"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), selectedEmoji)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
        ) {
            // Remote Name
            Text(
                text = "Remote Name",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g., Living Room TV") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Emoji Selection
            Text(
                text = "Select Icon",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = selectedEmoji,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name.ifBlank { "Remote Name" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Emoji Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(remoteEmojis) { emoji ->
                    EmojiItem(
                        emoji = emoji,
                        isSelected = emoji == selectedEmoji,
                        onClick = { selectedEmoji = emoji }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedEmoji)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Create Remote",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmojiItem(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )
        }
    }
}

