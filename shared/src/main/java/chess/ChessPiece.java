package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */

    public List<ChessMove> getBishopMoves(ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        int startRow = start.getRow();
        int startCol = start.getColumn();

        // Four diagonal directions
        int[][] directions = {
                {1,  1},  // up-right
                {1, -1},  // up-left
                {-1, 1},  // down-right
                {-1,-1}   // down-left
        };

        for (int[] dir : directions) {
            int row = startRow + dir[0];
            int col = startCol + dir[1];

            // Keep going in this direction until we leave the board
            while (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                moves.add(new ChessMove(start, new ChessPosition(row, col), null));

                row += dir[0]; // move further in same direction
                col += dir[1];
            }
        }

        return moves;
    }



    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ChessPiece piece = board.getPiece(myPosition);
        if(piece.getPieceType() == PieceType.BISHOP){
            return getBishopMoves(myPosition);
        }
        return List.of();
    }
}
