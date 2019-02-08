import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

enum Difficulty {
    Easy(6, 6),
    Medium(9, 20),
    Hard(15, 90);

    private int size;
    private int mineCount;

    Difficulty(int size, int count) {
        this.size = size;
        mineCount = count;
    }
//    public static final String[] names=new String[values().length];
//    public static final String[] strings=new String[values().length];
//    static {
//        Difficulty[] values=values();
//        for(int i=0;i<values.length;i++){
//            names[i]=values[i].name();
//            strings[i]=values[i].toString();
//        }
//    }

    @Override
    public String toString() {
        //  e.g. "Easy (6x6 Grid, 6 Mines)"
        return String.format("%s (%dx%d Grid, %d Mines)",
            super.toString(), size, size, mineCount
        );
    }

    public int getSize() {
        return size;
    }

    public int getMineCount() {
        return mineCount;
    }

}

class GameChooser extends JFrame {
    GameChooser(String name, Minesweeper game) {
        super(name);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImage(Minesweeper.gameIcon.getImage());

        JLabel banner = new JLabel();
        banner.setIcon(Minesweeper.bannerIcon);
        banner.setPreferredSize(new Dimension(300, 120));

        JComboBox<Difficulty> chooserCombo = new JComboBox<>(Difficulty.values());
        chooserCombo.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Minesweeper.selectedDifficulty = (Difficulty) e.getItem();
            }
        });

        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> {
            game.newGame();
            setVisible(false);
        });


        add("North", banner);
        add("Center", chooserCombo);
        add("East", startButton);

        pack();
        setSize(315, 200);
        setLocationRelativeTo(null); //center in screen - call after size set!
    }

}

public class Minesweeper extends JPanel implements ActionListener {

    static final ImageIcon bannerIcon = new ImageIcon(Minesweeper.class.getResource("/title.jpg"));
    static final ImageIcon gameIcon = new ImageIcon(Minesweeper.class.getResource("/MS.png"));
    private static final ImageIcon gameOverIcon = new ImageIcon(Minesweeper.class.getResource("/gameover.png"));
    private static final ImageIcon winnerIcon = new ImageIcon(Minesweeper.class.getResource("/youwon.gif"));


    private static final String BOMB = "*";
    private static final String GAME_NAME = "Minesweeper 1.0";
    static final String OUTSIDE_GRID = "Ignore mouse outside grid";
    static Difficulty selectedDifficulty = Difficulty.Easy;
    private static JFrame gameWindow = null;
    private final int TILE_SIZE = 45;
    private final int GRID_BASE = 2;
    Point mouseLoc;
    boolean isLeftMouse;
    private int gridSize = 15;
    private int BORDER = 2;
    private GameChooser gameChooser;
    private String[] openFields;
    private String[] closedFields;
    private ArrayList<Integer> bombIndexes = new ArrayList<>();
    private int mineAmount;
    private int[] mineDetectors;
    private int countMarked = 0;
    private boolean isGameOver = false;

    private Minesweeper() {
        addMouseListener(new NewMouseAdapter(this));
        addMouseMotionListener(new NewMouseMotionAdapter(this));

        gameWindow = new JFrame(GAME_NAME);
        gameWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        gameWindow.setIconImage(gameIcon.getImage());
        gameWindow.add("Center", this);

        gameChooser = new GameChooser(GAME_NAME, this);
        gameChooser.setVisible(true);
    }

    public static void main(String[] args) {
        new Minesweeper();
    }

    @Override
    public void actionPerformed(ActionEvent click) {
        if (click.getActionCommand().equals("Random")) {
            JOptionPane.showMessageDialog(this, JOptionPane.YES_NO_OPTION);

        }
    }

    @Override
    public Dimension getPreferredSize() {
        int d = TILE_SIZE * gridSize + 2 * GRID_BASE;
        return new Dimension(d, d);
    }

    Point mouseToGridCoords(double xMouse, double yMouse) {
        if (xMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < xMouse ||
            yMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < yMouse) {
            throw new RuntimeException(OUTSIDE_GRID);
        }


        int gridX = (int) ((xMouse - GRID_BASE) / TILE_SIZE);
        int gridY = (int) ((yMouse - GRID_BASE) / TILE_SIZE);

        return new Point(gridX, gridY);
    }

    private int gridCoordsToArrayIndex(int column, int row) {
        return column + gridSize * row;
    }

    void onClick(boolean isLeftMouse, int xMouse, int yMouse) {
        Point gridCoords = mouseToGridCoords(xMouse, yMouse);
        int index = gridCoordsToArrayIndex(gridCoords.x, gridCoords.y);

        mineAmount = selectedDifficulty.getMineCount();

        if (isLeftMouse) {
            if (closedFields[index].equals(BOMB)) {
                gameOver();
            } else if (closedFields[index].equals("")) {
                if (mineDetectors[index] == 0) {
                    openNoMineFields(index);
                } else {
                    closedFields[index] = "open";
                    openFields[index] = mineDetectors[index] + "";
                }
            }
        } else {
            if (!closedFields[index].equals("open") && openFields[index].equals("")) {
                openFields[index] = "X";
                if (openFields[index].equals("X") && closedFields[index].equals(BOMB)) {
                    countMarked++;
                }
            } else if (!closedFields[index].equals("open") && openFields[index].equals("X")) {
                openFields[index] = "?";
                if (openFields[index].equals("?") && closedFields[index].equals(BOMB)) {
                    countMarked--;
                }
            } else if (!closedFields[index].equals("open") && openFields[index].equals("?")) {
                openFields[index] = "";
            }
            if (countMarked == mineAmount && !isGameOver) {
                winner();
            }
        }
        repaint();
    }

    void newGame() {
        gridSize = selectedDifficulty.getSize();

        countMarked = 0;
        openFields = new String[gridSize * gridSize];
        closedFields = new String[gridSize * gridSize];
        Arrays.fill(closedFields, "");
        Arrays.fill(openFields, "");
        closedFields = placeRandomMines();
        bombIndexes.clear();
        for (int i = 0; i < closedFields.length; i++) {
            if (closedFields[i].equals(BOMB)) {
                bombIndexes.add(i);
            }
        }

        mineDetectors = new int[gridSize * gridSize];
        mineDetectors = findAdjacent(closedFields);
        isGameOver = false;
        mouseLoc = null;
        gameWindow.pack();
//        repaint();
        gameWindow.setLocationRelativeTo(null); //center window - must call after size set!
        gameWindow.setVisible(true);
    }

    private void gameOver() {

        isGameOver = true;

        // Uncover remaning bombs
        for (int i = 0; i < closedFields.length; i++) {
            if (closedFields[i].equals(BOMB)) {
                openFields[i] = BOMB;
            }
        }
        repaint();

        int option = JOptionPane.showConfirmDialog(null,
            "Game Over\nWanna play again?",
            "Game Over",
            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
            gameOverIcon
        );

        restart(option == JOptionPane.YES_OPTION);
    }

    private void winner() {
        int option = JOptionPane.showConfirmDialog( null,
            "Congratulations, You Won!\nWanna play again?",
            "You Won",
            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
            winnerIcon
        );
        restart(option == JOptionPane.YES_OPTION);
    }

    private void restart(boolean again){
        if ( again) {
            newGame();
        } else {
            gameChooser.setVisible(true);
            gameWindow.dispose();
            isGameOver = false;
        }
    }


    private Point arrayIndex2GridCoords(int index) {
        return new Point(index % gridSize, index / gridSize);
    }

    private int[] findAdjacent(String[] field) {
//        ArrayList<Integer> bombIndexes = new ArrayList<>();
//        for (int i = 0; i < field.length; i++) {
//            if (field[i].equals(BOMB)) {
//                bombIndexes.add(i);
//            }
//        }
        int[] adjacents = new int[gridSize * gridSize];
        for (int index : bombIndexes)
            for (int j = -1; j < 2; j++) {
                int range = gridSize * j;
                for (int i = -1; i < 2; i++) {
                    try {
                        if (!closedFields[index + range + i].equals(BOMB)
                            && index / gridSize * gridSize + gridSize > (index + i)
                            && index / gridSize * gridSize <= (index + i)) {
                            adjacents[index + range + i] += 1;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.printf("closedFields[%d] but length=%d\n", (index + range + i), closedFields.length);
                        System.out.printf("index=%d,range=%d,i=%d\n", index, range, i);
                    }
                }
            }
        return adjacents;

    }

    private String[] placeRandomMines() {
        Random rand = new Random();
        String[] newField = new String[gridSize * gridSize];
        Arrays.fill(newField, "");

        mineAmount = selectedDifficulty.getMineCount();
        for (int i = 0; i < gridSize * gridSize; i++) {
            if (newField[i].equals("")) {
                newField[i] = (rand.nextInt(8) < 1 ? BOMB : "");
                if (newField[i].equals(BOMB)) {
                    mineAmount--;
                }
            }
            if (mineAmount != 0 && i == (gridSize * gridSize) - 1) {
                i = 0;
            }
            if (mineAmount == 0) {
                break;
            }
        }
        return newField;
    }

    private void openNoMineFields(int index) {
        if (index >= gridSize * gridSize) {
            return;
        }
        for (int j = -1; j < 2; j++) {
            int range = gridSize * j;
            for (int i = -1; i < 2; i++) {
                try {
                    if (!closedFields[index + range + i].equals(BOMB)
                        && !closedFields[index + range + i].equals("open")
                        && (arrayIndex2GridCoords(index).y + 1) * gridSize > index + i
                        && (arrayIndex2GridCoords(index).y) * gridSize <= index + i) {
                        if (mineDetectors[index + range + i] == 0) {
                            closedFields[index + range + i] = "open";
                            openFields[index + range + i] = " ";
                            openNoMineFields(index + range + i);
                        } else {
                            closedFields[index + range + i] = "open";
                            openFields[index + range + i] = mineDetectors[index + range + i] + "";
                        }

                    }
                } catch (Exception ignored) {
                }
            }

        }
    }

    @Override
    public void paint(Graphics g) {
        for (int row = 0; row < gridSize; ++row) {
            for (int column = 0; column < gridSize; ++column) {
                drawTile(g, row, column);
            }
        }

    }

    private void drawTile(Graphics g, int yTile, int xTile) {
        int RECT_SIZE = TILE_SIZE - BORDER;

        int topLeftCornerX = xTile * TILE_SIZE + (GRID_BASE + BORDER);
        int topLeftCornerY = yTile * TILE_SIZE + (GRID_BASE + BORDER);


        if (closedFields[gridCoordsToArrayIndex(xTile, yTile)].equals("open")) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(topLeftCornerX, topLeftCornerY, RECT_SIZE, RECT_SIZE);
        } else {
            if (isGameOver) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(topLeftCornerX, topLeftCornerY, RECT_SIZE, RECT_SIZE);
            } else {
                g.setColor(Color.GRAY);
                g.fillRect(topLeftCornerX, topLeftCornerY, RECT_SIZE, RECT_SIZE);
            }
        }
        if (mouseLoc != null && !isGameOver) {
            if (mouseLoc.equals(new Point(xTile, yTile))) {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(topLeftCornerX, topLeftCornerY, RECT_SIZE, RECT_SIZE);
            }
        }
        g.setFont(new Font("Sans", Font.BOLD, 20));
        g.setColor(Color.BLUE);
        g.drawString(openFields[gridCoordsToArrayIndex(xTile, yTile)], topLeftCornerX + TILE_SIZE / 2 - 6, topLeftCornerY + TILE_SIZE / 2 + 5);
//        g.drawString(mineDetectors[gridCoordsToArrayIndex(xTile, yTile)] + "", topLeftCornerX + TILE_SIZE / 2, topLeftCornerY + TILE_SIZE / 2);

    }
}

class NewMouseAdapter extends MouseAdapter {
    private Minesweeper game;

    NewMouseAdapter(Minesweeper p) {
        game = p;
    }

    public void mouseClicked(MouseEvent e) {
        try {
            game.isLeftMouse = e.getButton() == 1;
            game.onClick(game.isLeftMouse, e.getX(), e.getY());
        } catch (RuntimeException exc) {
            if (!exc.getMessage().startsWith("Ignor")) {
                throw exc;
            }
        }
    }
}

class NewMouseMotionAdapter extends MouseMotionAdapter {
    private Minesweeper game;

    NewMouseMotionAdapter(Minesweeper g) {
        game = g;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            game.mouseLoc = game.mouseToGridCoords(e.getX(), e.getY());
            game.repaint();
        } catch (Exception a) {
            if(!a.getMessage().equals(Minesweeper.OUTSIDE_GRID)) {
                a.printStackTrace();
            }
        }
        super.mouseMoved(e);
    }
}



