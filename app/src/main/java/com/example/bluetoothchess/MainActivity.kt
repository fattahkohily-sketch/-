package com.example.bluetoothchess

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bluetoothchess.bluetooth.BluetoothController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var chessGame: ChessGame
    private lateinit var bluetoothController: BluetoothController
    private var selectedPosition: Position? = null
    private val boardButtons = Array(8) { arrayOfNulls<Button>(8) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chessGame = ChessGame()
        bluetoothController = BluetoothController(this)
        bluetoothController.onMoveReceived = { moveText ->
            runOnUiThread {
                val move = Move.fromWireString(moveText)
                if (move != null) {
                    val accepted = chessGame.applyMove(move)
                    if (accepted) {
                        updateStatus(if (chessGame.isWhiteTurn) "White to move" else "Black to move")
                        selectedPosition = null
                        renderBoard()
                    }
                }
            }
        }

        setupBoard()
        renderBoard()
        bindActions()
        updateStatus("White to move")
    }

    private fun bindActions() {
        val hostButton = findViewById<Button>(R.id.hostButton)
        val joinButton = findViewById<Button>(R.id.joinButton)

        hostButton.setOnClickListener {
            val result = bluetoothController.startServer()
            if (result) {
                updateStatus("Hosting a Bluetooth game")
            } else {
                updateStatus("Bluetooth not available or permissions missing")
            }
        }

        joinButton.setOnClickListener {
            val devices = bluetoothController.getPairedDevices()
            if (devices.isEmpty()) {
                updateStatus("No paired devices")
                return@setOnClickListener
            }

            val names = devices.map { it.name ?: it.address }.toTypedArray()
            MaterialAlertDialogBuilder(this)
                .setTitle("Choose device")
                .setItems(names) { _, index ->
                    val chosen = devices[index]
                    val result = bluetoothController.connectToDevice(chosen)
                    if (result) {
                        updateStatus("Connected to ${chosen.name ?: chosen.address}")
                    } else {
                        updateStatus("Unable to connect")
                    }
                }
                .show()
        }
    }

    private fun setupBoard() {
        val boardContainer = findViewById<GridLayout>(R.id.boardContainer)
        boardContainer.removeAllViews()

        for (row in 0..7) {
            for (column in 0..7) {
                val button = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        rowSpec = GridLayout.spec(row)
                        columnSpec = GridLayout.spec(column)
                        width = 0
                        height = 0
                        setMargins(2, 2, 2, 2)
                        columnWeight = 1f
                        rowWeight = 1f
                    }
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
        return if (isDark) {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        } else {
            ContextCompat.getColor(this, android.R.color.white)
        }
    }

    private fun handleSquareClick(row: Int, column: Int) {
        val clickedPosition = Position(row, column)
        val piece = chessGame.board[row][column]

        if (selectedPosition == null) {
            if (piece != null && piece.isWhite == chessGame.isWhiteTurn) {
                selectedPosition = clickedPosition
                renderBoard()
            }
            return
        }

        val move = Move(selectedPosition!!, clickedPosition)
        if (chessGame.applyMove(move)) {
            selectedPosition = null
            bluetoothController.sendMove(move.toWireString())
            updateStatus(if (chessGame.isWhiteTurn) "White to move" else "Black to move")
            renderBoard()
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
                val isSelected = selectedPosition == Position(row, column)

                button.text = piece?.symbol ?: ""
                button.setBackgroundColor(getSquareColor(row, column))
                if (isSelected) {
                    button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                }
            }
        }
    }

    private fun updateStatus(message: String) {
        val statusView = findViewById<TextView>(R.id.statusText)
        statusView.text = message
    }
}
