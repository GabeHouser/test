import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Enum to represent game modes and difficulty levels
enum Mode { REGULAR, KILLER }
enum Difficulty { EASY, MEDIUM, HARD, EXTREME }

class Cell extends JPanel {
    JTextField inputField; // For the cell value
    JTextField pencilField;
    JLabel cageSumLabel;   // For the cage total
    int[][] cellNum;

    public Cell() {

        setLayout(new BorderLayout());
        inputField = new JTextField();
        inputField.setHorizontalAlignment(SwingConstants.CENTER);
        inputField.setFont(new Font("Arial", Font.BOLD, 20));
        add(inputField, BorderLayout.CENTER);

        pencilField = new JTextField();
        pencilField.setHorizontalAlignment(SwingConstants.CENTER);
        pencilField.setFont(new Font("Arial", Font.BOLD, 12));

        add(pencilField, BorderLayout.CENTER);

        // Cage total in the top-left corner
        cageSumLabel = new JLabel();
        cageSumLabel.setFont(new Font("Arial", Font.BOLD, 12));
        cageSumLabel.setHorizontalAlignment(SwingConstants.LEFT);
        cageSumLabel.setVerticalAlignment(SwingConstants.TOP);
        cageSumLabel.setForeground(Color.black);
        cageSumLabel.setOpaque(true);
        cageSumLabel.setBackground(null);
        cageSumLabel.setBorder(BorderFactory.createEmptyBorder());
        add(cageSumLabel, BorderLayout.NORTH);
    }
}

class Cage {
    List<int[]> cells; // List of cells in this cage
    Cage() {
        cells = new ArrayList<>();
    }
    void addCell(int row, int col) {
        cells.add(new int[]{row, col});
    }
}

public class Sudoku extends Component {
    // Game state variables
    public int[][] board; // 2D array for the Sudoku board
    public int[][] solution; // 2D array for the solved Sudoku board
    public boolean[][] cageAssigned; // Tracks cells already part of a cage
    public List<Cage> cages;
    public int mistakes = 0;
    public int maxMistakes = 3;
    public int generationAttempts = 0;
    Random random = null;
    public JFrame frame;
    public JTextField[][] disBoard;
    public JPanel cellGrid;
    public JLabel statusLabel;
    public Mode mode;

    public Sudoku() {
        // Initialize board and solution arrays
        board = new int[9][9];
        solution = new int[9][9];
        cageAssigned = new boolean[9][9];
        cages = new ArrayList<>();
        random = new Random();
        startMenu();

    }

    public static void main(String[] args) {
        new Sudoku();
    }

    public void startMenu() {
        mistakes = 0;
        // Create the main frame
        frame = new JFrame("Sudoku");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // Title at the top
        JLabel title = new JLabel("Sudoku", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 36));
        frame.add(title, BorderLayout.NORTH);

        // Main panel for game options
        JPanel startPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        startPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create panels for Regular Sudoku and Killer Sudoku
        for (String mode : new String[]{"Regular", "Killer"}) {
            JPanel modePanel = new JPanel(new BorderLayout());
            modePanel.setBorder(BorderFactory.createTitledBorder(mode + " Sudoku"));

            // Buttons for difficulty levels
            JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 10, 10));
            for (String difficulty : new String[]{"Easy", "Medium", "Hard", "Extreme"}) {
                String actionCommand = mode + "-" + difficulty; // Unique identifier
                JButton button = new JButton(difficulty);
                button.setActionCommand(actionCommand);
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gameSelectInput(e.getActionCommand());
                    }
                });
                buttonPanel.add(button);
            }
            modePanel.add(buttonPanel, BorderLayout.CENTER);
            startPanel.add(modePanel);
        }
        frame.add(startPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void sudokuBoardGUI(Mode mode) {
        frame = new JFrame("Sudoku Board");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(650, 625);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground( Color.GRAY );
        frame.setBackground(Color.DARK_GRAY);

        cellGrid = new JPanel(new GridLayout(9, 9)); // 9x9 grid for Sudoku
        cellGrid.setBackground(Color.BLACK);

        disBoard = new JTextField[9][9]; // Text fields for user input

        // Map cage IDs to unique colors and sums
        Map<Integer, Color> cageColors = generateCageColors();
        Set<Integer> displayedCageSums = new HashSet<>(); // Track cage sums already displayed

        // Customize grid lines
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                JLayeredPane cellLayer = new JLayeredPane();
                cellLayer.setLayout(null);

                // Create the input field
                JTextField inputField = new JTextField();
                inputField.setBounds(0, 0, 70, 60); // Adjust size to fit the grid
                inputField.setHorizontalAlignment(JTextField.CENTER);
                inputField.setFont(new Font("Arial", Font.PLAIN, 16));

                // Apply cage coloring based on cage ID
                int cageId = getCageId(row, col);
                if (cageId != -1) {
                    inputField.setBackground(cageColors.get(cageId));
                }

                // Pre-fill with numbers if available
                if (board[row][col] != 0) {
                    inputField.setText(String.valueOf(board[row][col]));
                    inputField.setForeground(Color.BLACK);
                    inputField.setFont(new Font("Arial", Font.BOLD, 17));
                    inputField.setEditable(false);
                } else {
                    inputField.setEditable(true);
                    int finalRow = row;
                    int finalCol = col;
                    inputField.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyReleased(KeyEvent e) {
                            handlePlayerInput(finalRow, finalCol, inputField);
                        }
                    });
                }

                // Create the cage sum label
                JLabel cageSumLabel = new JLabel("", SwingConstants.LEFT);
                cageSumLabel.setBounds(0, 0, 80, 20); // Position it above the input field
                cageSumLabel.setFont(new Font("Arial", Font.BOLD, 12));
                cageSumLabel.setForeground(Color.black); // Set the color for the cage sum
                cageSumLabel.setOpaque(false); // Transparent background

                // Display cage sum if this cell is the top-left of the cage
                if (cageId != -1 && isTopLeftOfCage(row, col, cageId)) {
                    int cageSum = getCageSum(cageId);
                    cageSumLabel.setText(String.valueOf(cageSum));
                    displayedCageSums.add(cageId);
                }

                int top=0;
                int bottom=0;
                int left=0;
                int right=0;
                if(row == 0 || row == 3 || row == 6){
                    top = 2;
                } else { top = 1; }
                if(row == 2 || row == 5 || row == 8){
                    bottom = 2;
                } else { bottom = 1; }
                if(col == 0 || col == 3 || col == 6){
                    left = 2;
                } else { left = 1; }
                if(col == 2 || col == 5 || col == 8){
                    right = 2;
                } else { right = 1; }
                if(mode == Mode.KILLER){
                    inputField.setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));
                } else {
                    inputField.setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));
                }

                cellLayer.add(inputField, JLayeredPane.DEFAULT_LAYER);
                cellLayer.add(cageSumLabel, JLayeredPane.PALETTE_LAYER);
                disBoard[row][col] = inputField;
                cellGrid.add(cellLayer);
            }
        }

        // Add a button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(Color.DARK_GRAY);

        // Add components to the frame
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.add(cellGrid, BorderLayout.CENTER);

        // Add a status label at the top
        statusLabel = new JLabel("Mistakes: " + mistakes + "/" + maxMistakes, SwingConstants.CENTER);
        frame.add(statusLabel, BorderLayout.NORTH);

        frame.setFocusable(true);
        frame.setVisible(true);
    }

    public boolean isTopLeftOfCage(int row, int col, int cageId) {
        // Get all cells in the cage
        Cage cage = cages.get(cageId);

        // Initialize variables to track the top-left cell
        int topRow = Integer.MAX_VALUE;
        int leftCol = Integer.MAX_VALUE;

        // Find the top-most and left-most cell in the cage
        for (int[] cell : cage.cells) {
            int cellRow = cell[0];
            int cellCol = cell[1];

            // Update the topRow and leftCol if a higher-priority cell is found
            if (cellRow < topRow || (cellRow == topRow && cellCol < leftCol)) {
                topRow = cellRow;
                leftCol = cellCol;
            }
        }

        // Return true if the current cell matches the top-left cell
        return row == topRow && col == leftCol;
    }

    public void handlePlayerInput(int row, int col, JTextField cell) {
        String input = cell.getText();

        // Validate input (optional): Ensure only numbers 1-9 are allowed
        if (input.isEmpty() || !input.matches("[1-9]")) {
            return; // Ignore invalid input
        }

        // Parse input as an integer
        int numInput = Integer.parseInt(input);

        // Update the board with valid input
        board[row][col] = numInput;

        // Check if the input creates a conflict (mistake detection)
        if (isMistake(row, col, numInput)) {
            // If there's a mistake, mark it in red
            cell.setForeground(Color.RED);
            cell.setFont(new Font("Arial", Font.BOLD, 16));
            // Increase the mistake count
            mistakes++;

            // Optionally, update the status label with the mistake count
            statusLabel.setText("Mistakes: " + mistakes + "/" + maxMistakes);

            // If the number of mistakes exceeds the maximum allowed, alert the player
            if (mistakes >= maxMistakes) {
                JOptionPane.showMessageDialog(frame, "You have made too many mistakes!", "Game Over", JOptionPane.ERROR_MESSAGE);

                // Lock the board: Disable all input fields
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        disBoard[i][j].setEditable(false);
                    }
                }

                JButton returnToMainMenuButton = new JButton("Return to Main Menu");
                returnToMainMenuButton.addActionListener(e -> {
                    frame.dispose(); // Close the current game window
                    startMenu(); // Show the main menu
                });

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(returnToMainMenuButton);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.revalidate();

            }
        } else {
            // If no mistake, keep the number in black (or reset color if it was a mistake before)
            cell.setForeground(Color.BLACK);
            cell.setEditable(false);
        }

        // After valid input, check if the board is solved
        if (isBoardSolved()) {
            // Show win message and give the player the option to return to the main menu
            int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "Congratulations! You have solved the puzzle! Return to main menu?",
                    "You Win!",
                    JOptionPane.YES_NO_OPTION
            );

            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    disBoard[i][j].setEditable(false);
                }
            }

            JButton returnToMainMenuButton = new JButton("Return to Main Menu");
            returnToMainMenuButton.addActionListener(e -> {
                frame.dispose();
                startMenu();
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(returnToMainMenuButton);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.revalidate();
        }
    }

    public boolean isMistake(int row, int col, int numInput) {
        // Check the row for duplicates
        for (int c = 0; c < 9; c++) {
            if (c != col && board[row][c] == numInput) {
                return true; // Duplicate found in the same row
            }
        }

        // Check the column for duplicates
        for (int r = 0; r < 9; r++) {
            if (r != row && board[r][col] == numInput) {
                return true; // Duplicate found in the same column
            }
        }

        return false;
    }

    public void gameSelectInput(String actionCommand) {
        frame.dispose(); // Close the menu window

        // Use switch to handle the action command
        switch (actionCommand) {
            case "Regular-Easy":
                generateBoard(Mode.REGULAR, Difficulty.EASY);
                System.out.println("Starting Regular Sudoku on Easy difficulty!");
                mode = Mode.REGULAR;
                sudokuBoardGUI(Mode.REGULAR);
                break;
            case "Regular-Medium":
                generateBoard(Mode.REGULAR, Difficulty.MEDIUM);
                System.out.println("Starting Regular Sudoku on Medium difficulty!");
                mode = Mode.REGULAR;
                sudokuBoardGUI(Mode.REGULAR);
                break;
            case "Regular-Hard":
                generateBoard(Mode.REGULAR, Difficulty.HARD);
                System.out.println("Starting Regular Sudoku on Hard difficulty!");
                mode = Mode.REGULAR;
                sudokuBoardGUI(Mode.REGULAR);
                break;
            case "Regular-Extreme":
                generateBoard(Mode.REGULAR, Difficulty.EXTREME);
                System.out.println("Starting Regular Sudoku on Extreme difficulty!");
                mode = Mode.REGULAR;
                sudokuBoardGUI(Mode.REGULAR);
                break;
            case "Killer-Easy":
                generateBoard(Mode.KILLER, Difficulty.EASY);
                System.out.println("Starting Killer Sudoku on Easy difficulty!");
                mode = Mode.KILLER;
                sudokuBoardGUI(Mode.KILLER);
                break;
            case "Killer-Medium":
                generateBoard(Mode.KILLER, Difficulty.MEDIUM);
                System.out.println("Starting Killer Sudoku on Medium difficulty!");
                mode = Mode.KILLER;
                sudokuBoardGUI(Mode.KILLER);
                break;
            case "Killer-Hard":
                generateBoard(Mode.KILLER, Difficulty.HARD);
                System.out.println("Starting Killer Sudoku on Hard difficulty!");
                mode = Mode.KILLER;
                sudokuBoardGUI(Mode.KILLER);
                break;
            case "Killer-Extreme":
                generateBoard(Mode.KILLER, Difficulty.EXTREME);
                System.out.println("Starting Killer Sudoku on Extreme difficulty!");
                mode = Mode.KILLER;
                sudokuBoardGUI(Mode.KILLER);
                break;
            default:
                System.out.println("Invalid selection!");
                break;
        }

    }

    public boolean isBoardSolved() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != solution[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isNumberInRow(int[][] board, int number, int row) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == number) {
                return true;
            }
        }
        return false;
    }

    public boolean isNumberInColumn(int[][] board, int number, int column) {
        for (int i = 0; i < 9; i++) {
            if (board[i][column] == number) {
                return true;
            }
        }
        return false;
    }

    public boolean isNumberInBox(int[][] board, int number, int row, int column) {
        int localBoxRow = row - row % 3;
        int localBoxColumn = column - column % 3;

        for (int i = localBoxRow; i < localBoxRow + 3; i++) {
            for (int j = localBoxColumn; j < localBoxColumn + 3; j++) {
                if (board[i][j] == number) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isValidPlacement(int[][] board, int number, int row, int column) {
        return !isNumberInRow(board, number, row) &&
                !isNumberInColumn(board, number, column) &&
                !isNumberInBox(board, number, row, column);
    }

    public boolean solveBoard(int[][] board) {
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                if (board[row][column] == 0) {
                    for (int numberToTry = 1; numberToTry <= 9; numberToTry++) {
                        if (isValidPlacement(board, numberToTry, row, column)) {
                            board[row][column] = numberToTry;
                            generationAttempts++;
                            if (solveBoard(board)) {
                                return true;
                            } else {
                                board[row][column] = 0;
                            }
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    void generateBoard(Mode mode, Difficulty diff) {
        int triedBoards = 0;
        ArrayList<Integer> rand = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            rand.add(i);
        }
        //reset board
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                solution[i][j] = 0;
            }
        }
        while(true){
            //generate random diagonal
            Collections.shuffle(rand);
            for (int i = 0; i < 9; i++) {
                solution[i][i] = rand.get(i);
            }
            //generate random left column
            Collections.shuffle(rand);
            for (int i = 1; i < 9; i++){
                if(isNumberInRow(board, rand.get(i), i)){
                    Collections.shuffle(rand);
                }
            }
            //fill in solution with solution
            System.out.println("Generating...");
            if (solveBoard(solution)) {
                System.out.println("Loaded after "+generationAttempts+" permutations tried" );
                break;
            } else {
                triedBoards++;
                System.out.println("Failed Boards:" + triedBoards);
            }
        }
        // Save the generated puzzle to solution (since it's assumed to be valid now)
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                board[i][j] = solution[i][j];
            }
        }
        // Depending on mode and difficulty, remove numbers to create the puzzle
        if (mode == Mode.REGULAR) {
            switch (diff) {
                case EASY:
                    // Remove cells to match easy difficulty
                    removeCellsForDifficulty(5, board);
                    break;
                case MEDIUM:
                    removeCellsForDifficulty(15, board);
                    break;
                case HARD:
                    removeCellsForDifficulty(30, board);
                    break;
                case EXTREME:
                    removeCellsForDifficulty(40, board);
                    break;
            }
        }
        if (mode == Mode.KILLER) {
            generateCages();
            switch (diff) {
                case EASY:
                    // Remove cells to match easy difficulty
                    removeCellsForDifficulty(5, board);
                    break;
                case MEDIUM:
                    removeCellsForDifficulty(30, board);
                    break;
                case HARD:
                    removeCellsForDifficulty(40, board);
                    break;
                case EXTREME:
                    removeCellsForDifficulty(50, board);
                    break;
            }
        }
    }

    public void removeCellsForDifficulty(int cellsToRemove, int[][] board) {
        int count = 0;
        while (count < cellsToRemove) {
            int row = random.nextInt(9);
            int col = random.nextInt(9);
            if (board[row][col] != 0) {
                board[row][col] = 0;
                count++;
            }
        }
    }

    public int getCageSum(int cageId) {
        // Check if the cageId is valid
        if (cageId < 0 || cageId >= cages.size()) {
            return -1; // Invalid cageId
        }

        Cage cage = cages.get(cageId); // Get the cage based on cageId
        int cageSum = 0;

        // Iterate over the cells in the cage and calculate the sum
        for (int[] cell : cage.cells) {
            int row = cell[0];
            int col = cell[1];
            cageSum += solution[row][col]; // Add the value of the cell from the board
        }

        return cageSum; // Return the total sum
    }

    public void generateCages() {
        Random rand = new Random();

        // Clear any existing cages
        cages.clear(); // Clear the previous cages
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                cageAssigned[i][j] = false; // Reset the cageAssigned grid
            }
        }

        // Loop through the grid to assign cages
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                // Skip if this cell is already assigned to a cage
                if (cageAssigned[i][j]) continue;

                // Start a new cage
                Cage cage = new Cage();
                cages.add(cage);  // Add the cage to the list of cages

                // Assign the initial cell
                assignCellToCage(i, j, cage);

                // Determine the cage size (between 4 and 7 cells to avoid very small cages)
                int cageSize = rand.nextInt(4);  // Cage size between 4 and 7

                // Use a queue for breadth-first assignment of cells to cages
                Queue<int[]> cellsToAssign = new LinkedList<>();
                cellsToAssign.add(new int[]{i, j});

                // Expand the cage until it reaches the desired size or no more neighbors
                while (!cellsToAssign.isEmpty() && cage.cells.size() < cageSize) {
                    int[] currentCell = cellsToAssign.poll();
                    int currentRow = currentCell[0];
                    int currentCol = currentCell[1];

                    // Get all valid neighbors
                    List<int[]> neighbors = getUnassignedNeighbors(currentRow, currentCol);

                    // Add valid neighbors to the queue and assign them to the cage
                    for (int[] neighbor : neighbors) {
                        int neighborRow = neighbor[0];
                        int neighborCol = neighbor[1];

                        assignCellToCage(neighborRow, neighborCol, cage);
                        cellsToAssign.add(neighbor);  // Add this neighbor to the list of cells to process
                    }
                }
            }
        }
    }

    public void assignCellToCage(int row, int col, Cage cage) {
        cage.addCell(row, col);
        cageAssigned[row][col] = true;
    }

    public int getCageId(int row, int col) {
        for (int i = 0; i < cages.size(); i++) {
            Cage cage = cages.get(i);
            for (int[] cell : cage.cells) {
                if (cell[0] == row && cell[1] == col) {
                    return i;
                }
            }
        }
        return -1;
    }

    public Map<Integer, Color> generateCageColors() {
        Map<Integer, Color> cageColors = new HashMap<>();
        Random random = new Random();
        int colorThreshold = 3225;  // Define a threshold for color similarity (larger values = stricter similarity checks)
        int minBrightness = 90;   // Minimum brightness for the color (lower = darker)

        // Assign random colors to cages, avoiding white, gray, black, and too similar colors
        for (int i = 0; i < cages.size(); i++) {
            int red, green, blue;
            Color newColor;
            do {
                // Generate random RGB values between 0 and 255
                red = random.nextInt(256);
                green = random.nextInt(256);
                blue = random.nextInt(256);

                // Create the color with the generated RGB values
                newColor = new Color(red, green, blue);

            } while (isColorInvalid(newColor, cageColors, colorThreshold, minBrightness));

            // Assign the valid color to the cage
            cageColors.put(i, newColor);
        }

        return cageColors;
    }

    public boolean isColorInvalid(Color newColor, Map<Integer, Color> cageColors, int colorThreshold, int minBrightness) {
        // Check if the color is too dark
        if (getColorBrightness(newColor) < minBrightness) {
            return true;  // Too dark
        }

        // Check if the color is white, gray, or black
        if (newColor.getRed() == newColor.getGreen() && newColor.getGreen() == newColor.getBlue() ||  // Gray
                (newColor.getRed() == 255 && newColor.getGreen() == 255 && newColor.getBlue() == 255) ||  // White
                (newColor.getRed() == 0 && newColor.getGreen() == 0 && newColor.getBlue() == 0)) {         // Black
            return true;  // Avoid gray, white, and black
        }

        // Check for similarity to previously assigned colors
        for (Color existingColor : cageColors.values()) {
            if (calculateColorDistance(newColor, existingColor) < colorThreshold) {
                return true;  // Colors are too similar
            }
        }

        // If no issues, the color is valid
        return false;
    }

    public int getColorBrightness(Color color) {
        // Calculate brightness as the average of RGB values
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }

    public int calculateColorDistance(Color color1, Color color2) {
        int rDiff = color1.getRed() - color2.getRed();
        int gDiff = color1.getGreen() - color2.getGreen();
        int bDiff = color1.getBlue() - color2.getBlue();
        return rDiff * rDiff + gDiff * gDiff + bDiff * bDiff;  // Squared Euclidean distance (no square root for efficiency)
    }

    public List<int[]> getUnassignedNeighbors(int row, int col) {
        List<int[]> neighbors = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Up, Down, Left, Right

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // Check bounds and unassigned status
            if (newRow >= 0 && newRow < 9 && newCol >= 0 && newCol < 9 && !cageAssigned[newRow][newCol]) {
                neighbors.add(new int[]{newRow, newCol});
            }
        }
        return neighbors;
    }

}
