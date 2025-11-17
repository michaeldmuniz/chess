package client.ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;


public class BoardPrinter {

    public void drawBoard(ChessGame game) {
        ChessBoard board = game.getBoard();

        // Print ranks 8 down to 1 (white perspective only for now)
        for (int row = 8; row >= 1; row--) {
            System.out.print(row + " ");  // row label

            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = board.getPiece(new ChessPosition(row, col));

                if (piece == null) {
                    System.out.print(". ");
                } else {
                    System.out.print(pieceToChar(piece) + " ");
                }
            }

            System.out.println();
        }

        // Column labels
        System.out.println("  a b c d e f g h");
    }


    private char pieceToChar(ChessPiece p) {
        return switch (p.getPieceType()) {
            case KING -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'K' : 'k';
            case QUEEN -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'Q' : 'q';
            case ROOK -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'R' : 'r';
            case BISHOP -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'B' : 'b';
            case KNIGHT -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'N' : 'n';
            case PAWN -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'P' : 'p';
        };
    }
}
