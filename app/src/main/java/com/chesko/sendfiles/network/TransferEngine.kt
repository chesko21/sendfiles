package com.chesko.sendfiles.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.chesko.sendfiles.data.AppDatabase
import com.chesko.sendfiles.data.TransferDirection
import com.chesko.sendfiles.data.TransferRecord
import com.chesko.sendfiles.data.TransferStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class TransferEngine(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val transferDao = database.transferDao()

    suspend fun startServer(port: Int, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val serverSocket = ServerSocket(port)
            Log.d("TransferEngine", "Server started on port $port")
            while (true) {
                val socket = serverSocket.accept()
                handleIncomingConnection(socket, onProgress)
            }
        } catch (e: Exception) {
            Log.e("TransferEngine", "Server error", e)
        }
    }

    private suspend fun handleIncomingConnection(socket: Socket, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        socket.use { s ->
            val input = DataInputStream(s.getInputStream())
            val fileName = input.readUTF()
            val fileSize = input.readLong()
            val senderName = input.readUTF()

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

            val downloadsDir = context.getExternalFilesDir(null)
            val file = File(downloadsDir, fileName)
            val output = FileOutputStream(file)

            try {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead: Long = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    onProgress(totalRead.toFloat() / fileSize)
                }
                output.flush()
                transferDao.updateStatus(recordId, TransferStatus.COMPLETED)
                Log.d("TransferEngine", "File received: $fileName")
            } catch (e: Exception) {
                Log.e("TransferEngine", "Error receiving file", e)
                transferDao.updateStatus(recordId, TransferStatus.FAILED)
            } finally {
                output.close()
            }
        }
    }

    suspend fun sendFile(
        device: Device,
        uri: Uri,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val fileName = getFileName(uri) ?: "unknown_file"
        val fileSize = getFileSize(uri)

        val recordId = transferDao.insertTransfer(
            TransferRecord(
                fileName = fileName,
                fileSize = fileSize,
                senderName = "Me",
                receiverName = device.name,
                status = TransferStatus.IN_PROGRESS,
                direction = TransferDirection.SEND
            )
        )

        try {
            val socket = Socket(device.host, device.port)
            socket.use { s ->
                val output = DataOutputStream(s.getOutputStream())
                output.writeUTF(fileName)
                output.writeLong(fileSize)
                output.writeUTF("Me")

                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalSent: Long = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalSent += bytesRead
                        onProgress(totalSent.toFloat() / fileSize)
                    }
                    output.flush()
                }
            }
            transferDao.updateStatus(recordId, TransferStatus.COMPLETED)
            Log.d("TransferEngine", "File sent: $fileName")
        } catch (e: Exception) {
            Log.e("TransferEngine", "Error sending file", e)
            transferDao.updateStatus(recordId, TransferStatus.FAILED)
        }
    }

    private fun getFileName(uri: Uri): String? {
        return uri.path?.substringAfterLast('/')
    }

    private fun getFileSize(uri: Uri): Long {
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0
    }
}
