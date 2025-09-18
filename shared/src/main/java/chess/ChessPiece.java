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

    public List<ChessMove> getBishopMoves(ChessBoard board ,ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        int startRow = start.getRow();
        int startCol = start.getColumn();

        ChessPiece piece = board.getPiece(start);

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
                ChessPosition targetPos = new ChessPosition(row, col);
                ChessPiece targetPiece = board.getPiece(targetPos);

                if (targetPiece == null) {
                    // Empty square -> add move, keep going
                    moves.add(new ChessMove(start, targetPos, null));
                } else if (targetPiece.getTeamColor() != piece.getTeamColor()) {
                    // Enemy piece -> add capture and stop
                    moves.add(new ChessMove(start, targetPos, null));
                    break; // stop in this direction after capture
                } else {
                    // Friendly piece -> can't move here or past it
                    break;
                }

                row += dir[0];
                col += dir[1];

            }
        }

        return moves;
    }

    public List<ChessMove> getKingMoves(ChessBoard board, ChessPosition start){
        List<ChessMove> moves = new ArrayList<>();
        int startRow = start.getRow();
        int startCol = start.getColumn();

        ChessPiece piece = board.getPiece(start);

        int [][] directions = {
                {-1,1},
                {0,1},
                {1,1},
                {-1,0},
                {1,0},
                {-1,-1},
                {0,-1},
                {1,-1}
        };

        for (int[] dir : directions){
            int row = startRow + dir[0];
            int col = startCol + dir[1];


            // Make sure we are still on the board
            if (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                ChessPosition targetPos = new ChessPosition(row, col);
                ChessPiece targetPiece = board.getPiece(targetPos);

                if (targetPiece == null) {
                    // Empty square -> king can move there
                    moves.add(new ChessMove(start, targetPos, null));
                } else if (targetPiece.getTeamColor() != piece.getTeamColor()) {
                    // Enemy piece -> king can capture it
                    moves.add(new ChessMove(start, targetPos, null));
                }
                // If friendly piece is there, do nothing (king cannot move there)
            }

        }

        return moves;
    }

    public List<ChessMove> getRookMoves(ChessBoard board, ChessPosition start){
        List<ChessMove> moves = new ArrayList<>();
        int startRow = start.getRow();
        int startCol = start.getColumn();

        ChessPiece piece = board.getPiece(start);

        int [][] directions = {
            {1,0},
            {0,1},
            {-1,0},
            {0,-1}
        };


        for (int[] dir : directions) {
            int row = startRow + dir[0];
            int col = startCol + dir[1];


            // Keep going in this direction until we leave the board

            while (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                ChessPosition targetPos = new ChessPosition(row, col);
                ChessPiece targetPiece = board.getPiece(targetPos);

                if (targetPiece == null) {
                    // Empty square -> add move, keep going
                    moves.add(new ChessMove(start, targetPos, null));
                } else if (targetPiece.getTeamColor() != piece.getTeamColor()) {
                    // Enemy piece -> add capture and stop
                    moves.add(new ChessMove(start, targetPos, null));
                    break; // stop in this direction after capture
                } else {
                    // Friendly piece -> can't move here or past it
                    break;
                }

                row += dir[0];
                col += dir[1];
            }
        }

        return moves;

    }

    public List<ChessMove> getKnightMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        int startRow = start.getRow();
        int startCol = start.getColumn();

        ChessPiece piece = board.getPiece(start);

        int[][] directions = {
                { 2,  1}, { 2, -1},
                {-2,  1}, {-2, -1},
                { 1,  2}, { 1, -2},
                {-1,  2}, {-1, -2}
        };

        for (int[] dir : directions) {
            int row = startRow + dir[0];
            int col = startCol + dir[1];

            // Only consider moves that stay on the board
            if (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                ChessPosition targetPos = new ChessPosition(row, col);
                ChessPiece targetPiece = board.getPiece(targetPos);

                // Add move if square is empty or has enemy
                if (targetPiece == null || targetPiece.getTeamColor() != piece.getTeamColor()) {
                    moves.add(new ChessMove(start, targetPos, null));
                }
            }
        }

        return moves;
    }


    public List<ChessMove> getPawnMoves(ChessBoard board, ChessPosition start) {
        List<ChessMove> moves = new ArrayList<>();

        int startRow = start.getRow();
        int startCol = start.getColumn();

        ChessPiece piece = board.getPiece(start);

        // Decide which way the pawn moves
        int direction;
        int promotionRow;
        if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
            direction = 1;
            promotionRow = 8;
        } else {
            direction = -1;
            promotionRow = 1;
        }

        PieceType[] promotionPieces = {
                PieceType.QUEEN,
                PieceType.ROOK,
                PieceType.BISHOP,
                PieceType.KNIGHT
        };

        // ONE STEP FORWARD
        int oneStepRow = startRow + direction;
        if (oneStepRow >= 1 && oneStepRow <= 8) {
            ChessPosition oneStepPos = new ChessPosition(oneStepRow, startCol);
            if (board.getPiece(oneStepPos) == null) {
                if (oneStepRow == promotionRow) {
                    for (PieceType type : promotionPieces) {
                        moves.add(new ChessMove(start, oneStepPos, type));
                    }
                } else {
                    moves.add(new ChessMove(start, oneStepPos, null));

                    // ---- TWO STEP FORWARD ----
                    if (piece.getTeamColor() == ChessGame.TeamColor.WHITE && startRow == 2) {
                        ChessPosition twoStepPos = new ChessPosition(startRow + 2, startCol);
                        if (board.getPiece(twoStepPos) == null) {
                            moves.add(new ChessMove(start, twoStepPos, null));
                        }
                    }
                    if (piece.getTeamColor() == ChessGame.TeamColor.BLACK && startRow == 7) {
                        ChessPosition twoStepPos = new ChessPosition(startRow - 2, startCol);
                        if (board.getPiece(twoStepPos) == null) {
                            moves.add(new ChessMove(start, twoStepPos, null));
                        }
                    }
                }
            }
        }

        // CAPTURES
        int captureRow = startRow + direction;

        // Capture to left
        int leftCol = startCol - 1;
        if (captureRow >= 1 && captureRow <= 8 && leftCol >= 1) {
            ChessPosition leftCapturePos = new ChessPosition(captureRow, leftCol);
            ChessPiece leftPiece = board.getPiece(leftCapturePos);
            if (leftPiece != null && leftPiece.getTeamColor() != piece.getTeamColor()) {
                if (captureRow == promotionRow) {
                    for (PieceType type : promotionPieces) {
                        moves.add(new ChessMove(start, leftCapturePos, type));
                    }
                } else {
                    moves.add(new ChessMove(start, leftCapturePos, null));
                }
            }
        }

        // Capture to right
        int rightCol = startCol + 1;
        if (captureRow >= 1 && captureRow <= 8 && rightCol <= 8) {
            ChessPosition rightCapturePos = new ChessPosition(captureRow, rightCol);
            ChessPiece rightPiece = board.getPiece(rightCapturePos);
            if (rightPiece != null && rightPiece.getTeamColor() != piece.getTeamColor()) {
                if (captureRow == promotionRow) {
                    for (PieceType type : promotionPieces) {
                        moves.add(new ChessMove(start, rightCapturePos, type));
                    }
                } else {
                    moves.add(new ChessMove(start, rightCapturePos, null));
                }
            }
        }
        return moves;
    }

    public List<ChessMove> getQueenMoves(ChessBoard board, ChessPosition start){
        List<ChessMove> moves = new ArrayList<>();

        int startCol = start.getColumn();
        int startRow = start.getRow();

        ChessPiece piece = board.getPiece(start);

        int [][] directions = {
                {-1,1},
                {0,1},
                {1,1},
                {-1,0},
                {1,0},
                {-1,-1},
                {0,-1},
                {1,-1}
        };

        for(int[] dir : directions) {
            int col = startCol + dir[1];
            int row = startRow + dir[0];

            while (col >= 1 && col <= 8 && row >= 1 && row <= 8) {
                ChessPosition targetPos = new ChessPosition(row, col);
                ChessPiece targetPiece = board.getPiece(targetPos);

                if (targetPiece == null ) {
                    moves.add(new ChessMove(start, targetPos, null));
                } else if (targetPiece.getTeamColor() != piece.getTeamColor()) {
                    moves.add(new ChessMove(start, targetPos, null));
                    break;
                }
                else {
                    break;
                }
                col += dir[1];
                row += dir[0];
            }
        }
        return moves;
    }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ChessPiece piece = board.getPiece(myPosition);
        if (piece.getPieceType() == PieceType.BISHOP) {
            return getBishopMoves(board, myPosition);
        } else if (piece.getPieceType() == PieceType.KING) {
            return getKingMoves(board, myPosition);
        } else if (piece.getPieceType() == PieceType.ROOK){
            return getRookMoves(board, myPosition);
        } else if (piece.getPieceType() == PieceType.KNIGHT) {
            return getKnightMoves(board, myPosition);
        } else if (piece.getPieceType() == PieceType.PAWN) {
            return getPawnMoves(board, myPosition);
        } else if (piece.getPieceType() == PieceType.QUEEN){
            return getQueenMoves(board,myPosition);
        }



        return List.of();
    }

}
