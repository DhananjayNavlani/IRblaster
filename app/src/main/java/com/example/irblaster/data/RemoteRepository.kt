package com.example.irblaster.data

import kotlinx.coroutines.flow.Flow

class RemoteRepository(
    private val remoteDao: RemoteDao,
    private val buttonDao: RemoteButtonDao
) {
    // Remotes
    fun getAllRemotes(): Flow<List<RemoteEntity>> = remoteDao.getAllRemotes()

    fun getAllRemotesWithButtons(): Flow<List<RemoteWithButtons>> = remoteDao.getAllRemotesWithButtons()

    fun getRemoteWithButtons(remoteId: Long): Flow<RemoteWithButtons?> = remoteDao.getRemoteWithButtons(remoteId)

    suspend fun getRemoteById(remoteId: Long): RemoteEntity? = remoteDao.getRemoteById(remoteId)

    suspend fun insertRemote(remote: RemoteEntity): Long = remoteDao.insertRemote(remote)

    suspend fun updateRemote(remote: RemoteEntity) = remoteDao.updateRemote(remote)

    suspend fun deleteRemote(remote: RemoteEntity) = remoteDao.deleteRemote(remote)

    suspend fun deleteRemoteById(remoteId: Long) = remoteDao.deleteRemoteById(remoteId)

    // Buttons
    fun getButtonsForRemote(remoteId: Long): Flow<List<RemoteButtonEntity>> = buttonDao.getButtonsForRemote(remoteId)

    suspend fun insertButton(button: RemoteButtonEntity): Long = buttonDao.insertButton(button)

    suspend fun updateButton(button: RemoteButtonEntity) = buttonDao.updateButton(button)

    suspend fun deleteButton(button: RemoteButtonEntity) = buttonDao.deleteButton(button)

    suspend fun deleteButtonById(buttonId: Long) = buttonDao.deleteButtonById(buttonId)

    suspend fun getNextOrderIndex(remoteId: Long): Int {
        return (buttonDao.getMaxOrderIndex(remoteId) ?: -1) + 1
    }
}

