package com.example.irblaster

import android.hardware.ConsumerIrManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.irblaster.data.*
import com.example.irblaster.navigation.Screen
import com.example.irblaster.ui.screens.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    irManager: ConsumerIrManager?,
    repository: RemoteRepository
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Collect all remotes
    val remotes by repository.getAllRemotes().collectAsState(initial = emptyList())

    // State for pending button to add to remote
    var pendingButton by remember { mutableStateOf<PendingButton?>(null) }
    var showSelectRemoteDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📱 My Remotes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()

                // IR Tester option
                NavigationDrawerItem(
                    label = { Text("🔬 IR Tester (Samsung)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Brand Browser option
                NavigationDrawerItem(
                    label = { Text("🌐 Browse All Brands (Online)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.BrandBrowser.route)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Saved Remotes",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // List of saved remotes
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(remotes) { remote ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = "${remote.emoji} ${remote.name}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(Screen.CustomRemote.createRoute(remote.id))
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Create new remote button
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Create New Remote") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.CreateRemote.route)
                    },
                    modifier = Modifier.padding(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            // Home - IR Tester Screen
            composable(Screen.Home.route) {
                IRTesterScreen(
                    irManager = irManager,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAddToRemote = { name, emoji, code, frequency ->
                        pendingButton = PendingButton(name, emoji, code, frequency)
                        showSelectRemoteDialog = true
                    }
                )
            }

            // Brand Browser Screen
            composable(Screen.BrandBrowser.route) {
                BrandBrowserScreen(
                    irManager = irManager,
                    onBackClick = { navController.popBackStack() },
                    onAddToRemote = { name, emoji, code, frequency ->
                        pendingButton = PendingButton(name, emoji, code, frequency)
                        showSelectRemoteDialog = true
                    }
                )
            }

            // Create Remote Screen
            composable(Screen.CreateRemote.route) {
                CreateEditRemoteScreen(
                    existingRemote = null,
                    onSave = { name, emoji ->
                        scope.launch {
                            repository.insertRemote(RemoteEntity(name = name, emoji = emoji))
                            navController.popBackStack()
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Edit Remote Screen
            composable(
                route = Screen.EditRemote.route,
                arguments = listOf(navArgument("remoteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getLong("remoteId") ?: return@composable
                var remote by remember { mutableStateOf<RemoteEntity?>(null) }

                LaunchedEffect(remoteId) {
                    remote = repository.getRemoteById(remoteId)
                }

                remote?.let { existingRemote ->
                    CreateEditRemoteScreen(
                        existingRemote = existingRemote,
                        onSave = { name, emoji ->
                            scope.launch {
                                repository.updateRemote(existingRemote.copy(name = name, emoji = emoji))
                                navController.popBackStack()
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Custom Remote Screen
            composable(
                route = Screen.CustomRemote.route,
                arguments = listOf(navArgument("remoteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val remoteId = backStackEntry.arguments?.getLong("remoteId") ?: return@composable
                val remoteWithButtons by repository.getRemoteWithButtons(remoteId).collectAsState(initial = null)

                // State for editing button
                var buttonToEdit by remember { mutableStateOf<RemoteButtonEntity?>(null) }
                var showEditButtonDialog by remember { mutableStateOf(false) }

                CustomRemoteScreen(
                    remoteWithButtons = remoteWithButtons,
                    irManager = irManager,
                    onBackClick = { navController.popBackStack() },
                    onEditRemote = { navController.navigate(Screen.EditRemote.createRoute(remoteId)) },
                    onDeleteRemote = {
                        scope.launch {
                            repository.deleteRemoteById(remoteId)
                            navController.popBackStack()
                        }
                    },
                    onEditButton = { button ->
                        buttonToEdit = button
                        showEditButtonDialog = true
                    },
                    onDeleteButton = { button ->
                        scope.launch {
                            repository.deleteButton(button)
                        }
                    },
                    onAddButtonClick = {
                        // Navigate back to IR Tester to add buttons
                        Toast.makeText(context, "Use IR Tester to find and add buttons", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.Home.route)
                    }
                )

                // Edit Button Dialog
                if (showEditButtonDialog && buttonToEdit != null) {
                    EditButtonDialog(
                        button = buttonToEdit!!,
                        onSave = { name, emoji ->
                            scope.launch {
                                repository.updateButton(buttonToEdit!!.copy(name = name, emoji = emoji))
                            }
                            showEditButtonDialog = false
                            buttonToEdit = null
                        },
                        onDismiss = {
                            showEditButtonDialog = false
                            buttonToEdit = null
                        }
                    )
                }
            }
        }
    }

    // Dialog to select which remote to add button to
    if (showSelectRemoteDialog && pendingButton != null) {
        SelectRemoteDialog(
            remotes = remotes,
            onSelectRemote = { remote ->
                // Capture the pending button before clearing state
                val buttonToAdd = pendingButton!!
                showSelectRemoteDialog = false
                pendingButton = null

                scope.launch {
                    val orderIndex = repository.getNextOrderIndex(remote.id)
                    repository.insertButton(
                        RemoteButtonEntity(
                            remoteId = remote.id,
                            name = buttonToAdd.name,
                            emoji = buttonToAdd.emoji,
                            irCode = buttonToAdd.code,
                            frequency = buttonToAdd.frequency,
                            orderIndex = orderIndex
                        )
                    )
                    Toast.makeText(context, "Added '${buttonToAdd.name}' to ${remote.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onCreateNewRemote = {
                showSelectRemoteDialog = false
                navController.navigate(Screen.CreateRemote.route)
            },
            onDismiss = {
                showSelectRemoteDialog = false
                pendingButton = null
            }
        )
    }
}

// Data class for pending button
data class PendingButton(
    val name: String,
    val emoji: String,
    val code: IntArray,
    val frequency: Int
)

@Composable
fun SelectRemoteDialog(
    remotes: List<RemoteEntity>,
    onSelectRemote: (RemoteEntity) -> Unit,
    onCreateNewRemote: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Remote") },
        text = {
            Column {
                if (remotes.isEmpty()) {
                    Text("No remotes yet. Create one first!")
                } else {
                    Text("Select a remote:", modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(remotes) { remote ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectRemote(remote) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(remote.emoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(remote.name, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNewRemote) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Remote")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditButtonDialog(
    button: RemoteButtonEntity,
    onSave: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(button.name) }
    var emoji by remember { mutableStateOf(button.emoji) }

    val buttonEmojis = listOf(
        "🔴", "🔊", "🔉", "🔇", "⬆️", "⬇️", "◀️", "▶️",
        "✅", "❌", "↩️", "📋", "📺", "🔼", "🔽", "⏸️"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Button") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Button Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Emoji:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                // Emoji grid (simplified)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    buttonEmojis.take(8).forEach { e ->
                        Card(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { emoji = e },
                            colors = CardDefaults.cardColors(
                                containerColor = if (e == emoji)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(e, fontSize = 20.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    buttonEmojis.drop(8).forEach { e ->
                        Card(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { emoji = e },
                            colors = CardDefaults.cardColors(
                                containerColor = if (e == emoji)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(e, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, emoji) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

