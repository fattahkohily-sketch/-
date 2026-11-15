package com.example.bluetoothchess

data class Position(val row: Int, val column: Int) {
    fun isInsideBoard(): Boolean = row in 0..7 && column in 0..7
}

data class Move(val from: Position, val to: Position) {
    fun toWireString(): String =
        "${from.row},${from.column},${to.row},${to.column}"

    companion object {
        fun fromWireString(raw: String): Move? {
            val parts = raw.trim().split(",")
            if (parts.size != 4) return null

            val coords = parts.map { it.toIntOrNull() ?: return null }
            val from = Position(coords[0], coords[1])
            val to = Position(coords[2], coords[3])

            if (!from.isInsideBoard() || !to.isInsideBoard()) return null
            return Move(from, to)
        }
    }
}

enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

data class ChessPiece(
    val type: PieceType,
    val isWhite: Boolean
) {
    val symbol: String
        get() = when (type) {
            PieceType.KING -> if (isWhite) "♔" else "♚"
            PieceType.QUEEN -> if (isWhite) "♕" else "♛"
            PieceType.ROOK -> if (isWhite) "♖" else "♜"
            PieceType.BISHOP -> if (isWhite) "♗" else "♝"
            PieceType.KNIGHT -> if (isWhite) "♘" else "♞"
            PieceType.PAWN -> if (isWhite) "♙" else "♟"
        }
}
