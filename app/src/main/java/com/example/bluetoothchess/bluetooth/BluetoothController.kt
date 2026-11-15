package com.example.bluetoothchess.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothController(private val context: Context) {
    var onMoveReceived: ((String) -> Unit)? = null

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun isSupported(): Boolean = adapter != null

    fun hasPermissions(): Boolean {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return required.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPairedDevices(): List<BluetoothDevice> =
        adapter?.bondedDevices?.toList() ?: emptyList()

    fun startServer(): Boolean {
        if (adapter == null || !hasPermissions()) return false

        return try {
            val serverSocket: BluetoothServerSocket =
                adapter.listenUsingRfcommWithServiceRecord("BluetoothChess", uuid)

            Thread {
                try {
                    val accepted = serverSocket.accept()
                    socket = accepted
                    outputStream = accepted.outputStream
                    inputStream = accepted.inputStream
                    readIncomingMessages()
                } catch (_: Exception) {
                    runCatching { serverSocket.close() }
                }
            }.start()

            true
        } catch (_: Exception) {
            false
        }
    }

    fun connectToDevice(device: BluetoothDevice): Boolean {
        if (adapter == null || !hasPermissions()) return false

        return try {
            val clientSocket = device.createRfcommSocketToServiceRecord(uuid)
            clientSocket.connect()
            socket = clientSocket
            outputStream = clientSocket.outputStream
            inputStream = clientSocket.inputStream
            readIncomingMessages()
            true
        } catch (_: Exception) {
            closeConnection()
            false
        }
    }

    fun sendMove(message: String) {
        val stream = outputStream ?: return
        runCatching {
            stream.write(message.toByteArray())
            stream.flush()
        }
    }

    private fun readIncomingMessages() {
        val stream = inputStream ?: return
        val buffer = ByteArray(1024)

        Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val count = stream.read(buffer)
                    if (count <= 0) break
                    val message = String(buffer, 0, count).trim()
                    if (message.isNotEmpty()) {
                        onMoveReceived?.invoke(message)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }.start()
    }

    fun closeConnection() {
        runCatching { inputStream?.close() }
        runCatching { outputStream?.close() }
        runCatching { socket?.close() }

        inputStream = null
        outputStream = null
        socket = null
    }
}
