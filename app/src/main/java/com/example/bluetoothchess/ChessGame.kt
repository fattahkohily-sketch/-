package com.example.bluetoothchess

class ChessGame {
    val board: Array<Array<ChessPiece?>> = Array(8) { arrayOfNulls<ChessPiece>(8) }

    var isWhiteTurn: Boolean = true
        private set

    init {
        setupInitialBoard()
    }

    private fun setupInitialBoard() {
        val backRank = arrayOf(
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
            board[0][col] = ChessPiece(backRank[col], false)
            board[1][col] = ChessPiece(PieceType.PAWN, false)
            board[6][col] = ChessPiece(PieceType.PAWN, true)
            board[7][col] = ChessPiece(backRank[col], true)
        }
    }

    fun applyMove(move: Move): Boolean {
        val from = move.from
        val to = move.to

        if (!from.isInsideBoard() || !to.isInsideBoard()) return false
        if (from == to) return false

        val piece = board[from.row][from.column] ?: return false
        if (piece.isWhite != isWhiteTurn) return false

        val target = board[to.row][to.column]
        if (target != null && target.isWhite == piece.isWhite) return false
        if (!isLegalMove(piece, from, to)) return false

        board[to.row][to.column] = piece
        board[from.row][from.column] = null

        isWhiteTurn = !isWhiteTurn
        return true
    }

    private fun isLegalMove(piece: ChessPiece, from: Position, to: Position): Boolean {
        val rowDelta = to.row - from.row
        val colDelta = to.column - from.column

        return when (piece.type) {
            PieceType.PAWN -> isPawnMove(piece, from, to, rowDelta, colDelta)
            PieceType.ROOK -> isStraightMove(from, to)
            PieceType.BISHOP -> isDiagonalMove(from, to)
            PieceType.KNIGHT -> isKnightMove(rowDelta, colDelta)
            PieceType.QUEEN -> isStraightMove(from, to) || isDiagonalMove(from, to)
            PieceType.KING -> isKingMove(rowDelta, colDelta)
        }
    }

    private fun isPawnMove(
        piece: ChessPiece,
        from: Position,
        to: Position,
        rowDelta: Int,
        colDelta: Int
    ): Boolean {
        val direction = if (piece.isWhite) -1 else 1
        val startRow = if (piece.isWhite) 6 else 1

        if (colDelta == 0 && rowDelta == direction && board[to.row][to.column] == null) return true
        if (colDelta == 0 && rowDelta == 2 * direction && from.row == startRow &&
            board[from.row + direction][from.column] == null &&
            board[to.row][to.column] == null
        ) return true

        if (kotlin.math.abs(colDelta) == 1 && rowDelta == direction &&
            board[to.row][to.column] != null &&
            board[to.row][to.column]!!.isWhite != piece.isWhite
        ) return true

        return false
    }

    private fun isStraightMove(from: Position, to: Position): Boolean {
        val rowDelta = to.row - from.row
        val colDelta = to.column - from.column

        if (rowDelta == 0 || colDelta == 0) {
            val stepRow = if (rowDelta == 0) 0 else rowDelta / kotlin.math.abs(rowDelta)
            val stepCol = if (colDelta == 0) 0 else colDelta / kotlin.math.abs(colDelta)

            var r = from.row + stepRow
            var c = from.column + stepCol
            while (r != to.row || c != to.column) {
                if (board[r][c] != null) return false
                r += stepRow
                c += stepCol
            }
            return true
        }
        return false
    }

    private fun isDiagonalMove(from: Position, to: Position): Boolean {
        val rowDelta = to.row - from.row
        val colDelta = to.column - from.column

        if (kotlin.math.abs(rowDelta) == kotlin.math.abs(colDelta)) {
            val stepRow = if (rowDelta == 0) 0 else rowDelta / kotlin.math.abs(rowDelta)
            val stepCol = if (colDelta == 0) 0 else colDelta / kotlin.math.abs(colDelta)

            var r = from.row + stepRow
            var c = from.column + stepCol
            while (r != to.row || c != to.column) {
                if (board[r][c] != null) return false
                r += stepRow
                c += stepCol
            }
            return true
        }
        return false
    }

    private fun isKnightMove(rowDelta: Int, colDelta: Int): Boolean =
        (kotlin.math.abs(rowDelta) == 2 && kotlin.math.abs(colDelta) == 1) ||
            (kotlin.math.abs(rowDelta) == 1 && kotlin.math.abs(colDelta) == 2)

    private fun isKingMove(rowDelta: Int, colDelta: Int): Boolean =
        kotlin.math.abs(rowDelta) <= 1 && kotlin.math.abs(colDelta) <= 1
}
