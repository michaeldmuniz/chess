package client.ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.ChessMove;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BoardPrinter {

    private static final String RESET = "\u001B[0m";

    private static final String LIGHT_BG = "\u001B[48;5;180m";   // medium beige
    private static final String DARK_BG  = "\u001B[48;5;130m";   // Brown

    private static final String WHITE_FG = "\u001B[97m";   // bright white text
    private static final String BLACK_FG = "\u001B[30m";   // black text

    public void drawBoard(ChessGame game, boolean whitePerspective) {
        drawBoard(game, whitePerspective, null, null);
    }

    public void drawBoard(ChessGame game,
                          boolean whitePerspective,
                          ChessPosition highlightOrigin,
                          Collection<ChessMove> highlightMoves) {

        ChessBoard board = game.getBoard();

        int startRow = whitePerspective ? 8 : 1;
        int endRow   = whitePerspective ? 1 : 8;
        int stepRow  = whitePerspective ? -1 : 1;

        int startCol = whitePerspective ? 1 : 8;
        int endCol   = whitePerspective ? 8 : 1;
        int stepCol  = whitePerspective ? 1 : -1;

        Set<String> highlightSquares = new HashSet<>();
        if (highlightOrigin != null) {
            highlightSquares.add(key(highlightOrigin));
        }
        if (highlightMoves != null) {
            for (ChessMove m : highlightMoves) {
                highlightSquares.add(key(m.getEndPosition()));
            }
        }

        if (whitePerspective) {
            System.out.println("    a  b  c  d  e  f  g  h");
        } else {
            System.out.println("    h  g  f  e  d  c  b  a");
        }

        System.out.println("  +------------------------+");

        for (int row = startRow; row != endRow + stepRow; row += stepRow) {

            System.out.print(row + " |");

            for (int col = startCol; col != endCol + stepCol; col += stepCol) {

                String squareKey = row + "-" + col;
                boolean isHighlighted = highlightSquares.contains(squareKey);
                boolean isLight = ((row + col) % 2 != 0);
                
                String bg = isHighlighted
                        ? "\u001B[48;5;11m"  // yellow highlight
                        : (isLight ? LIGHT_BG : DARK_BG);

                ChessPiece piece = board.getPiece(new ChessPosition(row, col));

                String pieceText = (piece != null ? pieceToChar(piece) : " ");

                String cell = " " + pieceText + " ";

                System.out.print(bg + cell + RESET);

            }

            System.out.println("| " + row);
        }

        System.out.println("  +------------------------+");

        if (whitePerspective) {
            System.out.println("    a  b  c  d  e  f  g  h");
        } else {
            System.out.println("    h  g  f  e  d  c  b  a");
        }
    }

    private String key(ChessPosition pos) {
        return pos.getRow() + "-" + pos.getColumn();
    }

    private String pieceToChar(ChessPiece p) {
        char c = switch (p.getPieceType()) {
            case KING   -> 'K';
            case QUEEN  -> 'Q';
            case ROOK   -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
            case PAWN   -> 'P';
        };


        if (p.getTeamColor() == ChessGame.TeamColor.WHITE) {
            return WHITE_FG + c;
        } else {
            return BLACK_FG + Character.toLowerCase(c);
        }
    }
}