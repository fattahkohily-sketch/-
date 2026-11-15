package com.example.bluetoothchess

import kotlin.math.abs

enum class PieceType {
    PAWN,
    ROOK,
    KNIGHT,
    BISHOP,
    QUEEN,
    KING
}

data class Position(val row: Int, val col: Int)

data class Piece(val type: PieceType, val isWhite: Boolean) {
    val symbol: String
        get() = when (type) {
            PieceType.PAWN -> if (isWhite) "♙" else "♟"
            PieceType.ROOK -> if (isWhite) "♖" else "♜"
            PieceType.KNIGHT -> if (isWhite) "♘" else "♞"
            PieceType.BISHOP -> if (isWhite) "♗" else "♝"
            PieceType.QUEEN -> if (isWhite) "♕" else "♛"
            PieceType.KING -> if (isWhite) "♔" else "♚"
        }
}

data class Move(val from: Position, val to: Position) {
    fun toWireString(): String = "${from.row},${from.col},${to.row},${to.col}"

    companion object {
        fun fromWireString(value: String): Move? {
            val parts = value.split(",")
            if (parts.size != 4) return null
            return try {
                val fromRow = parts[0].toInt()
                val fromCol = parts[1].toInt()
                val toRow = parts[2].toInt()
                val toCol = parts[3].toInt()
                Move(Position(fromRow, fromCol), Position(toRow, toCol))
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}

class ChessGame {
    val board = Array(8) { Array<Piece?>(8) { null } }
    var isWhiteTurn: Boolean = true
        private set

    init {
        setupInitialBoard()
    }

    private fun setupInitialBoard() {
        val backRank = listOf(
            PieceType.ROOK,
            PieceType.KNIGHT,
            PieceType.BISHOP,
            PieceType.QUEEN,
            PieceType.KING,
            PieceType.BISHOP,
            PieceType.KNIGHT,
            PieceType.ROOK
        )

        for (col in 0..7) {
            board[0][col] = Piece(backRank[col], false)
            board[1][col] = Piece(PieceType.PAWN, false)
            board[6][col] = Piece(PieceType.PAWN, true)
            board[7][col] = Piece(backRank[col], true)
        }
    }

    private fun inBounds(position: Position): Boolean =
        position.row in 0..7 && position.col in 0..7

    private fun isEnemy(piece: Piece?, targetColor: Boolean): Boolean =
        piece != null && piece.isWhite != targetColor

    private fun addSlidingMoves(
        position: Position,
        piece: Piece,
        directions: List<Pair<Int, Int>>,
        targetList: MutableList<Move>
    ) {
        directions.forEach { (rowDir, colDir) ->
            var row = position.row + rowDir
            var col = position.col + colDir
            while (inBounds(Position(row, col))) {
                val targetSquare = board[row][col]
                if (targetSquare == null) {
                    targetList.add(Move(position, Position(row, col)))
                } else {
                    if (isEnemy(targetSquare, piece.isWhite)) {
                        targetList.add(Move(position, Position(row, col)))
                    }
                    break
                }
                row += rowDir
                col += colDir
            }
        }
    }

    fun legalMovesFor(position: Position): List<Move> {
        val piece = board[position.row][position.col] ?: return emptyList()
        val moves = mutableListOf<Move>()
        val direction = if (piece.isWhite) -1 else 1

        when (piece.type) {
            PieceType.PAWN -> {
                val oneStep = Position(position.row + direction, position.col)
                if (inBounds(oneStep) && board[oneStep.row][oneStep.col] == null) {
                    moves.add(Move(position, oneStep))
                    val startRow = if (piece.isWhite) 6 else 1
                    val twoStep = Position(position.row + (2 * direction), position.col)
                    if (position.row == startRow && inBounds(twoStep) && board[twoStep.row][twoStep.col] == null) {
                        moves.add(Move(position, twoStep))
                    }
                }

                listOf(-1, 1).forEach { offset ->
                    val target = Position(position.row + direction, position.col + offset)
                    if (inBounds(target) && isEnemy(board[target.row][target.col], piece.isWhite)) {
                        moves.add(Move(position, target))
                    }
                }
            }
            PieceType.ROOK -> addSlidingMoves(position, piece, listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1), moves)
            PieceType.BISHOP -> addSlidingMoves(position, piece, listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1), moves)
            PieceType.QUEEN -> addSlidingMoves(
                position,
                piece,
                listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1),
                moves
            )
            PieceType.KNIGHT -> {
                val offsets = listOf(
                    1 to 2,
                    2 to 1,
                    2 to -1,
                    1 to -2,
                    -1 to -2,
                    -2 to -1,
                    -2 to 1,
                    -1 to 2
                )
                for ((rowOffset, colOffset) in offsets) {
                    val target = Position(position.row + rowOffset, position.col + colOffset)
                    if (inBounds(target) && (board[target.row][target.col] == null || isEnemy(board[target.row][target.col], piece.isWhite))) {
                        moves.add(Move(position, target))
                    }
                }
            }
            PieceType.KING -> {
                for (rowOffset in -1..1) {
                    for (colOffset in -1..1) {
                        if (rowOffset == 0 && colOffset == 0) continue
                        val target = Position(position.row + rowOffset, position.col + colOffset)
                        if (inBounds(target) && (board[target.row][target.col] == null || isEnemy(board[target.row][target.col], piece.isWhite))) {
                            moves.add(Move(position, target))
                        }
                    }
                }
            }
        }

        return moves
    }

    fun applyMove(move: Move): Boolean {
        val selectedPiece = board[move.from.row][move.from.col] ?: return false
        val legalMoves = legalMovesFor(move.from)
        if (!legalMoves.any { it.to == move.to }) return false
        if (selectedPiece.isWhite != isWhiteTurn) return false

        board[move.to.row][move.to.col] = selectedPiece
        board[move.from.row][move.from.col] = null
        isWhiteTurn = !isWhiteTurn
        return true
    }
}
