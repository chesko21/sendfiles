package com.chesko.sendfiles.network

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import com.chesko.sendfiles.data.AppDatabase
import com.chesko.sendfiles.data.TransferDirection
import com.chesko.sendfiles.data.TransferRecord
import com.chesko.sendfiles.data.TransferStatus
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.InetAddress

class TransferEngine(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val transferDao = database.transferDao()
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    suspend fun startServer(port: Int, onProgress: (Long, Float) -> Unit) = withContext(Dispatchers.IO) {
        if (serverJob?.isActive == true) return@withContext
        
        serverJob = launch {
            try {
                serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
                Log.d("TransferEngine", "Server started on ${serverSocket?.inetAddress?.hostAddress}:$port")
                while (isActive) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch {
                            try {
                                handleIncomingConnection(socket, onProgress)
                            } catch (e: Exception) {
                                Log.e("TransferEngine", "Error handling connection", e)
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) Log.e("TransferEngine", "Accept error", e)
                    }
                }
            } catch (e: Exception) {
                if (isActive) Log.e("TransferEngine", "Server error", e)
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("TransferEngine", "Error closing server socket", e)
        }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        Log.d("TransferEngine", "Server stopped")
    }

    private suspend fun handleIncomingConnection(socket: Socket, onProgress: (Long, Float) -> Unit) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                s.soTimeout = 30000
                val input = DataInputStream(BufferedInputStream(s.getInputStream()))

                val magic = input.readInt()
                if (magic != 0x5346444C) {
                    Log.e("TransferEngine", "Invalid magic number")
                    return@withContext
                }

                val fileName = input.readUTF()
                val fileSize = input.readLong()
                val senderName = input.readUTF()

                Log.d("TransferEngine", "Incoming file: $fileName ($fileSize bytes) from $senderName")

                val recordId = transferDao.insertTransfer(
                    TransferRecord(
                        fileName = fileName,
                        fileSize = fileSize,
                        senderName = senderName,
                        receiverName = "Me",
                        status = TransferStatus.IN_PROGRESS,
                        direction = TransferDirection.RECEIVE
                    )
                )

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    val created = downloadsDir.mkdirs()
                    Log.d("TransferEngine", "Creating downloads dir: $created")
                }
                
                var targetFile = File(downloadsDir, fileName)

                if (targetFile.exists()) {
                    val nameWithoutExt = fileName.substringBeforeLast(".")
                    val ext = fileName.substringAfterLast(".", "")
                    val timestamp = System.currentTimeMillis() % 10000
                    targetFile = File(downloadsDir, "${nameWithoutExt}_$timestamp.$ext")
                }
                
                try {
                    var totalRead: Long = 0
                    BufferedOutputStream(FileOutputStream(targetFile)).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        
                        while (totalRead < fileSize) {
                            ensureActive()
                            bytesRead = input.read(buffer, 0, minOf(buffer.size.toLong(), fileSize - totalRead).toInt())
                            if (bytesRead == -1) break
                            
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            
                            if (fileSize > 0) {
                                onProgress(recordId, totalRead.toFloat() / fileSize)
                            }
                        }
                        output.flush()
                    }
                    
                    if (totalRead == fileSize) {
                        transferDao.updateStatus(recordId, TransferStatus.COMPLETED)
                        Log.d("TransferEngine", "File received successfully: ${targetFile.absolutePath}")

                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(targetFile.absolutePath),
                            null
                        ) { path, uri ->
                            Log.d("TransferEngine", "Media scanner scanned: $path")
                        }
                    } else {
                        Log.e("TransferEngine", "Transfer incomplete: $totalRead/$fileSize")
                        transferDao.updateStatus(recordId, TransferStatus.FAILED)
                    }
                } catch (e: Exception) {
                    Log.e("TransferEngine", "Error writing file", e)
                    transferDao.updateStatus(recordId, TransferStatus.FAILED)
                }
            }
        } catch (e: Exception) {
            Log.e("TransferEngine", "Connection error", e)
        }
    }

    suspend fun sendFile(
        device: Device,
        uri: Uri,
        existingRecordId: Long? = null,
        onProgress: (Long, Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val fileName = getFileName(uri) ?: "unknown_file"
        val fileSize = getFileSize(uri)

        Log.d("TransferEngine", "Preparing to send: $fileName ($fileSize bytes) to ${device.host}")

        val recordId = existingRecordId ?: transferDao.insertTransfer(
            TransferRecord(
                fileName = fileName,
                fileSize = fileSize,
                senderName = "Me",
                receiverName = device.name,
                status = TransferStatus.IN_PROGRESS,
                direction = TransferDirection.SEND,
                fileUri = uri.toString(),
                receiverAddress = device.host.hostAddress,
                receiverPort = device.port
            )
        )
        
        if (existingRecordId != null) {
            transferDao.updateStatus(recordId, TransferStatus.IN_PROGRESS)
        }

        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(device.host, device.port), 10000) // 10s connect timeout
                socket.soTimeout = 30000
                
                val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

                output.writeInt(0x5346444C)
                output.writeUTF(fileName)
                output.writeLong(fileSize)
                output.writeUTF(android.os.Build.MODEL)
                output.flush()

                contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalSent: Long = 0
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        ensureActive()
                        output.write(buffer, 0, bytesRead)
                        totalSent += bytesRead
                        
                        if (fileSize > 0) {
                            onProgress(recordId, totalSent.toFloat() / fileSize)
                        }
                    }
                    output.flush()
                }
                
                transferDao.updateStatus(recordId, TransferStatus.COMPLETED)
                Log.d("TransferEngine", "File sent successfully")
            }
        } catch (e: Exception) {
            Log.e("TransferEngine", "Error sending file", e)
            transferDao.updateStatus(recordId, TransferStatus.FAILED)
        }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        }
        return uri.path?.substringAfterLast('/')
    }

    private fun getFileSize(uri: Uri): Long {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return cursor.getLong(sizeIndex)
                    }
                }
            }
        }
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0
    }
}
