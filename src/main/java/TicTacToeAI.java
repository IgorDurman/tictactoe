import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;


/**
 * Main class for the Tic Tac Toe game with GUI and AI opponent.
 * Supports customizable board sizes (3x3, 5x5, 9x9), player symbol selection,
 * and multiple AI difficulty levels ranging from Easy to Impossible.
 */
public class TicTacToeAI extends JFrame implements ActionListener {
    // GUI buttons for each cell on the board
    JButton[][] buttons;
    // Internal 2D array representing the game board state
    char[][] board;
    // Characters representing the human player and AI
    char player = 'X', ai = 'O';
    // Difficulty level chosen for AI play
    String difficulty = "Hard";
    // Board size (e.g. 3, 5, 9)
    int boardSize = 3;
    // Number of consecutive marks required to win (3 for 3x3, 4 for 5x5, 5 for 9x9)
    int winLength = 3;

    // Dropdown selectors for board size, player symbol, and AI difficulty
    JComboBox<String> sizeBox;
    JComboBox<String> symbolBox;
    JComboBox<String> difficultyBox;
    // Panel holding the game board buttons
    JPanel boardPanel;

    /**
     * Constructor initializes the JFrame window, control panels,
     * and sets up the game board with initial settings.
     */
    public TicTacToeAI() {
        setTitle("Tic Tac Toe - AI");
        setSize(700, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel at top with dropdown selectors for difficulty, size, and player symbol
        JPanel topPanel = new JPanel();

        difficultyBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard", "Impossible"});
        difficultyBox.addActionListener(e -> difficulty = (String) difficultyBox.getSelectedItem());

        sizeBox = new JComboBox<>(new String[]{"3x3", "5x5", "9x9"});
        sizeBox.addActionListener(e -> {
            String selected = (String) sizeBox.getSelectedItem();
            boardSize = Integer.parseInt(selected.substring(0, 1));
            // Set the win length dynamically based on board size
            winLength = (boardSize == 3) ? 3 : (boardSize == 5 ? 4 : 5);
            initializeBoard();  // Re-initialize the board on size change
        });

        symbolBox = new JComboBox<>(new String[]{"X", "O"});
        symbolBox.addActionListener(e -> {
            player = ((String) symbolBox.getSelectedItem()).charAt(0);
            ai = (player == 'X') ? 'O' : 'X';
            initializeBoard();  // Reset board when player symbol changes
        });

        topPanel.add(new JLabel("AI Difficulty:"));
        topPanel.add(difficultyBox);
        topPanel.add(new JLabel("Board Size:"));
        topPanel.add(sizeBox);
        topPanel.add(new JLabel("You Are:"));
        topPanel.add(symbolBox);

        add(topPanel, BorderLayout.NORTH);

        boardPanel = new JPanel();
        add(boardPanel, BorderLayout.CENTER);

        initializeBoard();
        setVisible(true);
    }

    /**
     * Initializes or resets the game board GUI and internal data structures
     * according to the current board size and settings.
     * Creates a grid of buttons and clears all cell states.
     */
    void initializeBoard() {
        if (boardPanel != null) remove(boardPanel);
        boardPanel = new JPanel(new GridLayout(boardSize, boardSize));
        buttons = new JButton[boardSize][boardSize];
        board = new char[boardSize][boardSize];
        // Font size adapts to board size for readability
        Font font = new Font("Arial", Font.BOLD, Math.max(20, 300 / boardSize));

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(font);
                buttons[i][j].addActionListener(this);
                boardPanel.add(buttons[i][j]);
                board[i][j] = ' ';  // Empty cell
            }
        }
        add(boardPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Handles player's button click events.
     * Updates the board state if the move is valid, checks for win/draw,
     * and triggers AI's move accordingly.
     */
    public void actionPerformed(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (b == buttons[i][j] && board[i][j] == ' ') {
                    makeMove(i, j, player);
                    if (checkWin(player)) {
                        showMessage("You win!");
                        return;
                    }
                    if (isFull()) {
                        showMessage("Draw!");
                        return;
                    }
                    aiMove();
                    if (checkWin(ai)) {
                        showMessage("AI wins!");
                    } else if (isFull()) {
                        showMessage("Draw!");
                    }
                    return;
                }
            }
        }
    }

    /**
     * Executes AI's move based on the selected difficulty.
     * Adapts the search depth to the board size to maintain performance.
     */
    void aiMove() {
        int[] move;
        int depth;

        // Depth limit adjusted per board size for responsiveness
        switch (boardSize) {
            case 3: depth = 6; break;
            case 5: depth = 4; break;
            case 9: depth = 2; break;
            default: depth = 3;
        }

        switch (difficulty) {
            case "Easy":
                move = randomMove();
                break;
            case "Medium":
                move = mediumMove();
                break;
            case "Hard":
            case "Impossible":
                move = minimaxMoveWithAlphaBeta(depth);
                break;
            default:
                move = randomMove();
        }

        if (move != null) {
            makeMove(move[0], move[1], ai);
        }
    }

    /**
     * Marks the board and GUI button with the player's move.
     * Disables the button to prevent further input in that cell.
     *
     * @param row The row index where move is made.
     * @param col The column index where move is made.
     * @param ch  The player character ('X' or 'O').
     */
    void makeMove(int row, int col, char ch) {
        board[row][col] = ch;
        buttons[row][col].setText(String.valueOf(ch));
        buttons[row][col].setEnabled(false);
    }

    /**
     * Selects a random empty cell for AI's move (used in Easy difficulty).
     *
     * @return Coordinates [row, col] of the move.
     */
    int[] randomMove() {
        List<int[]> available = getAvailableMoves();
        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }

    /**
     * Medium difficulty AI attempts to win if possible,
     * blocks the player's immediate winning moves,
     * or falls back to a random move.
     *
     * @return Coordinates [row, col] of the move.
     */
    int[] mediumMove() {
        for (int[] move : getAvailableMoves()) {
            board[move[0]][move[1]] = ai;
            if (checkWin(ai)) {
                board[move[0]][move[1]] = ' ';
                return move;
            }
            board[move[0]][move[1]] = ' ';
        }
        for (int[] move : getAvailableMoves()) {
            board[move[0]][move[1]] = player;
            if (checkWin(player)) {
                board[move[0]][move[1]] = ' ';
                return move;
            }
            board[move[0]][move[1]] = ' ';
        }
        return randomMove();
    }

    /**
     * Uses minimax algorithm with alpha-beta pruning to find the best possible move for AI.
     * Search depth is limited to keep computations feasible on larger boards.
     *
     * @param maxDepth Maximum depth for minimax search.
     * @return Coordinates [row, col] of the best move.
     */
    int[] minimaxMoveWithAlphaBeta(int maxDepth) {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;
        for (int[] move : getAvailableMoves()) {
            board[move[0]][move[1]] = ai;
            int score = minimaxABLimited(false, 1, maxDepth, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board[move[0]][move[1]] = ' ';
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Implementation of the minimax algorithm with alpha-beta pruning and a depth limit.
     * This method recursively evaluates possible moves to optimize AI decisions based on difficulty.
     *
     * @param isMax   True if current level is maximizing (AI's turn), false for minimizing (player's turn).
     * @param depth   Current depth in the search tree.
     * @param maxDepth Maximum depth limit for search to control computation time.
     * @param alpha   Alpha value for pruning the maximizer's best option.
     * @param beta    Beta value for pruning the minimizer's best option.
     * @return Heuristic score of the board at this recursion level.
     */
    int minimaxABLimited(boolean isMax, int depth, int maxDepth, int alpha, int beta) {
        if (checkWin(ai)) return 10 - depth;
        if (checkWin(player)) return depth - 10;
        if (isFull() || depth == maxDepth) return 0;

        int best = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int[] move : getAvailableMoves()) {
            board[move[0]][move[1]] = isMax ? ai : player;
            int score = minimaxABLimited(!isMax, depth + 1, maxDepth, alpha, beta);
            board[move[0]][move[1]] = ' ';

            if (isMax) {
                best = Math.max(best, score);
                alpha = Math.max(alpha, best);
            } else {
                best = Math.min(best, score);
                beta = Math.min(beta, best);
            }

            if (beta <= alpha) break; // Prune the branch
        }
        return best;
    }

    /**
     * Returns a list of all currently empty cells on the board.
     *
     * @return List of [row, col] pairs indicating free spots.
     */
    List<int[]> getAvailableMoves() {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++)
                if (board[i][j] == ' ')
                    moves.add(new int[]{i, j});
        return moves;
    }

    /**
     * Checks if the specified player has achieved a winning sequence
     * of the required length in any direction (horizontal, vertical, diagonal).
     *
     * @param ch Player character ('X' or 'O').
     * @return True if player has won, false otherwise.
     */
    boolean checkWin(char ch) {
        // Check rows for winning sequence
        for (int i = 0; i < boardSize; i++) {
            int count = 0;
            for (int j = 0; j < boardSize; j++) {
                count = (board[i][j] == ch) ? count + 1 : 0;
                if (count >= winLength) return true;
            }
        }

        // Check columns for winning sequence
        for (int j = 0; j < boardSize; j++) {
            int count = 0;
            for (int i = 0; i < boardSize; i++) {
                count = (board[i][j] == ch) ? count + 1 : 0;
                if (count >= winLength) return true;
            }
        }

        // Check diagonals (top-left to bottom-right) for winning sequence
        for (int i = 0; i <= boardSize - winLength; i++) {
            for (int j = 0; j <= boardSize - winLength; j++) {
                int count = 0;
                for (int k = 0; k < winLength; k++) {
                    if (board[i + k][j + k] == ch) count++;
                    else break;
                }
                if (count == winLength) return true;
            }
        }

        // Check anti-diagonals (top-right to bottom-left) for winning sequence
        for (int i = 0; i <= boardSize - winLength; i++) {
            for (int j = winLength - 1; j < boardSize; j++) {
                int count = 0;
                for (int k = 0; k < winLength; k++) {
                    if (board[i + k][j - k] == ch) count++;
                    else break;
                }
                if (count == winLength) return true;
            }
        }

        // No winning sequence found
        return false;
    }

    /**
     * Checks if the board is completely filled without any empty cells,
     * indicating a draw if no player has won.
     *
     * @return True if the board is full, false otherwise.
     */
    boolean isFull() {
        for (int i = 0; i < boardSize; i++)
            for (int j = 0; j < boardSize; j++)
                if (board[i][j] == ' ')
                    return false;
        return true;
    }

    /**
     * Displays a message dialog to notify players of game outcomes,
     * then resets the game board to start a new match.
     *
     * @param message Message string to display.
     */
    void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
        initializeBoard();
    }

    /**
     * Main method launches the Tic Tac Toe AI GUI application.
     *
     * @param args Command line arguments (unused).
     */
    public static void main(String[] args) {
        // Schedule GUI creation on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> new TicTacToeAI());
    }
}