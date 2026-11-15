package com.example.bluetoothchess

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bluetoothchess.bluetooth.BluetoothController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var chessGame: ChessGame
    private lateinit var bluetoothController: BluetoothController
    private var selectedPosition: Position? = null
    private val boardButtons = Array(8) { arrayOfNulls<Button>(8) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.all { it.value }
            updateStatus(
                if (allGranted) "Bluetooth ready. Choose Host or Join."
                else "Bluetooth permissions are required to play."
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chessGame = ChessGame()
        bluetoothController = BluetoothController(this)
        bluetoothController.onMoveReceived = { moveText ->
            runOnUiThread {
                val move = Move.fromWireString(moveText)
                if (move != null && chessGame.applyMove(move)) {
                    selectedPosition = null
                    renderBoard()
                    updateStatus(
                        if (chessGame.isWhiteTurn) "White to move" else "Black to move"
                    )
                }
            }
        }

        requestBluetoothPermissionsIfNeeded()
        setupBoard()
        renderBoard()
        bindActions()
        updateStatus("White to move")
    }

    private fun bindActions() {
        val hostButton = findViewById<Button>(R.id.hostButton)
        val joinButton = findViewById<Button>(R.id.joinButton)

        hostButton.setOnClickListener {
            if (!bluetoothController.hasPermissions()) {
                requestBluetoothPermissionsIfNeeded()
                return@setOnClickListener
            }

            val result = bluetoothController.startServer()
            updateStatus(
                if (result) "Hosting a Bluetooth game"
                else "Bluetooth not available or permissions missing"
            )
        }

        joinButton.setOnClickListener {
            if (!bluetoothController.hasPermissions()) {
                requestBluetoothPermissionsIfNeeded()
                return@setOnClickListener
            }

            val devices = bluetoothController.getPairedDevices()
            if (devices.isEmpty()) {
                updateStatus("No paired devices found")
                return@setOnClickListener
            }

            val names = devices.map { it.name ?: it.address }.toTypedArray()

            MaterialAlertDialogBuilder(this)
                .setTitle("Choose device")
                .setItems(names) { _, index ->
                    val chosen = devices[index]
                    val result = bluetoothController.connectToDevice(chosen)
                    updateStatus(
                        if (result) "Connected to ${chosen.name ?: chosen.address}"
                        else "Unable to connect"
                    )
                }
                .show()
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        }
    }

    private fun setupBoard() {
        val boardContainer = findViewById<GridLayout>(R.id.boardContainer)
        boardContainer.removeAllViews()

        for (row in 0..7) {
            for (column in 0..7) {
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(column, 1f)
                ).apply {
                    width = 0
                    height = 0
                    setMargins(2, 2, 2, 2)
                }

                val button = Button(this).apply {
                    layoutParams = params
                    setTextSize(20f)
                    setPadding(0, 0, 0, 0)
                    setBackgroundColor(getSquareColor(row, column))
                    setOnClickListener { handleSquareClick(row, column) }
                }

                boardButtons[row][column] = button
                boardContainer.addView(button)
            }
        }
    }

    private fun getSquareColor(row: Int, column: Int): Int {
        val isDark = (row + column) % 2 == 1
        val colorRes = if (isDark) android.R.color.darker_gray else android.R.color.white
        return ContextCompat.getColor(this, colorRes)
    }

    private fun handleSquareClick(row: Int, column: Int) {
        val clickedPosition = Position(row, column)
        val piece = chessGame.board[row][column]
        val currentSelection = selectedPosition

        if (currentSelection == null) {
            if (piece != null && piece.isWhite == chessGame.isWhiteTurn) {
                selectedPosition = clickedPosition
                renderBoard()
            }
            return
        }

        val move = Move(currentSelection, clickedPosition)

        if (chessGame.applyMove(move)) {
            selectedPosition = null
            bluetoothController.sendMove(move.toWireString())
            renderBoard()
            updateStatus(
                if (chessGame.isWhiteTurn) "White to move" else "Black to move"
            )
        } else {
            selectedPosition = null
            renderBoard()
        }
    }

    private fun renderBoard() {
        for (row in 0..7) {
            for (column in 0..7) {
                val button = boardButtons[row][column] ?: continue
                val piece = chessGame.board[row][column]

                button.text = piece?.symbol ?: ""
                button.setBackgroundColor(getSquareColor(row, column))

                if (selectedPosition == Position(row, column)) {
                    button.setBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.holo_blue_light)
                    )
                }
            }
        }
    }

    private fun updateStatus(message: String) {
        findViewById<TextView>(R.id.statusText).text = message
    }
}
