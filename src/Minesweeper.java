import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

enum NewGameOption {SameAgain, Menu, Quit }

enum Difficulty {
    Easy(6, 6),
    Medium(9, 20),
    Hard(15, 90),
    Custom (-1, -1);

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

        JSlider gridSizeSlider = new JSlider(5,100,10);
        gridSizeSlider.setMajorTickSpacing(20);
        gridSizeSlider.setPaintLabels(true);
        gridSizeSlider.addChangeListener(
            e -> System.out.printf("gridSize changed to %d\n",gridSizeSlider.getValue())
        );
        gridSizeSlider.setEnabled(false);

        JComboBox<Difficulty> chooserCombo = new JComboBox<>(Difficulty.values());
        chooserCombo.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if( Difficulty.Custom.equals( e.getItem()) ){
                    //TODO: show advanced controls (size of grid, size of cells)
                    gridSizeSlider.setEnabled(true);
                }else{
                    //TODO: hide advanced controls
                    gridSizeSlider.setEnabled(false);
                    Minesweeper.selectedDifficulty = (Difficulty) e.getItem();
                }
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
        add("South", gridSizeSlider);


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
    private String[] hiddenFields;
    private ArrayList<Integer> bombIndexes = new ArrayList<>();
    private int mineAmount;
    private int[] mineDetectors;
    private int countMarked = 0;
    private boolean isGameOver = false;
    private boolean bombsRevealed =false;
    private String[] backupFields;

    private Minesweeper() {
        addMouseListener(new NewMouseAdapter(this));
        addMouseMotionListener(new NewMouseMotionAdapter(this));

        testingAid();

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

    Point mouse2GridCoords(double xMouse, double yMouse) {
        if (xMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < xMouse ||
            yMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < yMouse) {
            throw new RuntimeException(OUTSIDE_GRID);
        }


        int gridX = (int) ((xMouse - GRID_BASE) / TILE_SIZE);
        int gridY = (int) ((yMouse - GRID_BASE) / TILE_SIZE);

        return new Point(gridX, gridY);
    }

    private int coordsToIndex(int column, int row) {
        if(column<0 || gridSize<=column) throw new IllegalArgumentException("Off grid col="+column);
        if(row<0    || gridSize<=row) throw new IllegalArgumentException("Off grid row="+row);
        return column + gridSize * row;
    }
    private int coordsToIndex(Point posColRow){
        return coordsToIndex(posColRow.x, posColRow.y);
    }

    void onClick(boolean isLeftMouse, int xMouse, int yMouse) {
        Point gridCoords = mouse2GridCoords(xMouse, yMouse);
        int index = coordsToIndex(gridCoords.x, gridCoords.y);

        mineAmount = selectedDifficulty.getMineCount();

        if (isLeftMouse) {
            onClickLeft(index);
        } else {
            onClickRight(index);
        }
        repaint();
    }

    private void onClickLeft(int index) {
        if (hiddenFields[index].equals(BOMB)) {
            userLost();
        } else {
            if (mineDetectors[index] == 0) {
                openCluster(index);
            } else {
                hiddenFields[index] = "open";
                openFields[index] = mineDetectors[index] + "";
            }
        }
    }

    private void onClickRight(int index) {
        if (!hiddenFields[index].equals("open") && openFields[index].equals("")) {
            openFields[index] = "X";
            if (openFields[index].equals("X") && hiddenFields[index].equals(BOMB)) {
                countMarked++;
            }
        } else if (!hiddenFields[index].equals("open") && openFields[index].equals("X")) {
            openFields[index] = "?";
            if (openFields[index].equals("?") && hiddenFields[index].equals(BOMB)) {
                countMarked--;
            }
        } else if (!hiddenFields[index].equals("open") && openFields[index].equals("?")) {
            openFields[index] = "";
        }
        if (countMarked == mineAmount && !isGameOver) {
            userWon();
        }
    }

    void newGame() {
        gridSize = selectedDifficulty.getSize();
        mineAmount = selectedDifficulty.getMineCount();

        final int N = gridSize*gridSize;
        if(N <= mineAmount){
            String error = String.format("mineAmount: %d >= %d squares :(", mineAmount, N);
            System.err.println(error);
            throw new IllegalStateException(error);
        }

        countMarked = 0;
        openFields = new String[N];
        backupFields = new String[N];
        bombsRevealed = false;
        Arrays.fill(openFields, "");
        Arrays.fill(backupFields, "");

        hiddenFields = new String[N];
        placeRandomMines(hiddenFields, bombIndexes);// TODO  disperesed vs clumped strategies ?

        // TODO understand the rest of this...
        mineDetectors = countAdjacents(hiddenFields);
        isGameOver = false;
        mouseLoc = null;
        gameWindow.pack();
//        repaint();
        gameWindow.setLocationRelativeTo(null); //center window - must call after size set!
        gameWindow.setVisible(true);
    }

    // Only for testing, honest !!
    private void revealBombs(boolean reveal){
        System.out.printf("Entered revealBombs(%b)\n", reveal);
        bombsRevealed = reveal;
        for (int i = 0; i < hiddenFields.length; i++) {
            if (hiddenFields[i].equals(BOMB)) {
                if(reveal){
                    backupFields[i] = openFields[i];
                    openFields[i] = BOMB;
                }else{
                    openFields[i] = backupFields[i];
                }
            }
        }
        repaint();
    }

    private void userLost() {

        isGameOver = true;

        revealBombs(true);        // Uncover remaining bombs

        int option = JOptionPane.showOptionDialog(null,
            "Better luck next time!\nPlay again?",
            "You Lost",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            gameOverIcon,
            NewGameOption.values(),
            NewGameOption.SameAgain
        );

        if(NewGameOption.values()[option] == NewGameOption.Quit){
            System.exit(0);  // crude but okay for app with no cleanup action needed
        }

        restart(option == 0);
    }

    private void userWon() {
        int option = JOptionPane.showOptionDialog( null,
            "Congratulations, You Won!\nWanna play again?",
            "You Won", //title
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            winnerIcon,
            NewGameOption.values(),
            NewGameOption.SameAgain
        );
        if(NewGameOption.values()[option] == NewGameOption.Quit){
            System.exit(0);  // crude but okay for app with no cleanup action needed
        }
        restart(option == 0);
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


    private Point index2Coords(int index) {
        if(index<0 || gridSize*gridSize<=index){
            throw new IllegalArgumentException("Index off grid: " + index);
        }
        return new Point(index % gridSize, index / gridSize);
    }

    private int[] countAdjacents(String[] field) {
        int[] adjacents = new int[gridSize * gridSize];
        Set<Integer> neighbourIndices = new TreeSet<>(); //ordered set
        for (int bombIndex : bombIndexes) {
            // for each mine , inc "adjacent mines" count in neighbours
            //
            for (int neighbIdx : getNeighbourIdxs( bombIndex, neighbourIndices ) ) {
//                if( !BOMB.equals(hiddenFields[neighbIdx] ) ??? TODO is this needed ???
                adjacents[neighbIdx]++;
            }
        }
        return adjacents;

    }

    private Set<Integer> getNeighbourIdxs(int index, Set<Integer> neighbours ){
        Point p = index2Coords(index);
        neighbours.clear();
        ArrayList<Point> nPoints = new ArrayList<>(); //for debug output
        for (int dx = -1; dx<2; ++dx){
            for (int dy = -1; dy<2; ++dy){
                try {
                    Point neighbour = new Point(p.x + dx, p.y + dy); //off grid -> throws
                    if (!neighbour.equals(p)) {  // neighbour, not self
                        int neighbourIndex = coordsToIndex(neighbour);
                        if(neighbours.contains(neighbourIndex)){
                            System.err.printf(
                                "\nLogic error: mine idx %d neigbIdx %d already in neighbours.\n",
                                index, neighbourIndex
                            );
                        }else {
                            neighbours.add(neighbourIndex);
                            nPoints.add(neighbour);
                        }
                    }
                }catch(IllegalArgumentException e){
                    //off grid neighbour, ignore
                }
            }
        }
        System.out.printf("%s neighbours: %s\nIdxs: %s", p, nPoints, neighbours);
        return neighbours;
    }


    private void placeRandomMines(String[] mineField, ArrayList<Integer> bombIndexes) {
        Random rand = new Random();
        Arrays.fill(mineField, "");
        bombIndexes.clear();

        final int N = gridSize*gridSize;
        final int mineAmountOrig = mineAmount;

//        // Takes about 800 iters to lay 90 mines
//        int attempts = 0;
//        for (int i = 0; i < N; i++) {
//            ++attempts;
//            if (mineField[i].isEmpty() && rand.nextInt(8) < 1) {
//                mineField[i] = BOMB;
//                if (--mineAmount == 0) {
//                    System.out.printf("Success: %d mines laid in %d iterations\n", mineAmountOrig, attempts);
//                    break;
//                }
//            }
//            if (mineAmount != 0 && i == (gridSize * gridSize) - 1) {
//                i = 0;
//            }
//        }

        // Takes about 100 iters to lay 90 mines
        for(int z = 1; ; ++z ){
            System.out.printf("Seed minefield iter: %d (outstanding %d mines)..  ", z, mineAmount);
            int row = rand.nextInt(gridSize);
            int col = rand.nextInt(gridSize);
            int index = row*gridSize + col;
            if(mineField[index].isEmpty()){
                mineField[index] = BOMB;
                bombIndexes.add(index);
                System.out.printf("Placed bomb at (%d, %d)\n", col, row);
                if (--mineAmount <= 0) {
                    System.out.printf("Success: %d mines laid in %d iterations\n", mineAmountOrig, z);
                    break;
                }
            }else{
                System.out.printf("\nAlready occupied: (%d, %d)\n", col, row);
            }
            if( 1000_000 < z ){
                System.err.printf("Failed to lay all mines after %d attempts!\n", z);
                System.err.printf("Quitting after %d mines laid!\n", mineAmountOrig-mineAmount);
                break;
            }
        }
        Collections.sort(bombIndexes);  // why not
        System.out.printf("\nRecorded %d mines:\n%s", bombIndexes.size(), bombIndexes);
        if(bombIndexes.size() != mineAmountOrig){
            System.err.printf("Error: Expected %d (not %d) mines!\n",
                mineAmountOrig,
                bombIndexes.size());
        }
    }

    // Player clicked an empty cell... try to open cluster of empty cells.
    //
    private void openCluster(int index) {
        if (index<0 || gridSize*gridSize<=index ) {
            throw new IllegalArgumentException("Off grid index: "+index);
        }
        for (int j = -1; j < 2; j++) {
            int range = gridSize * j;
            for (int i = -1; i < 2; i++) {
                try {
                    if (!hiddenFields[index + range + i].equals(BOMB)
                        && !hiddenFields[index + range + i].equals("open")
                        && (index2Coords(index).y + 1) * gridSize > index + i
                        && (index2Coords(index).y) * gridSize <= index + i) {
                        if (mineDetectors[index + range + i] == 0) {
                            hiddenFields[index + range + i] = "open";
                            openFields[index + range + i] = " ";
                            openCluster(index + range + i);
                        } else {
                            hiddenFields[index + range + i] = "open";
                            openFields[index + range + i] = mineDetectors[index + range + i] + "";
                        }

                    }
                } catch (Exception e) {
                    System.err.printf("openCluster threw %s\n", e.getMessage());
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


        if (hiddenFields[coordsToIndex(xTile, yTile)].equals("open")) {
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
        g.drawString(openFields[coordsToIndex(xTile, yTile)], topLeftCornerX + TILE_SIZE / 2 - 6, topLeftCornerY + TILE_SIZE / 2 + 5);
//        g.drawString(mineDetectors[coordsToIndex(xTile, yTile)] + "", topLeftCornerX + TILE_SIZE / 2, topLeftCornerY + TILE_SIZE / 2);

    }

    private void testingAid(){
        setFocusable(true);
        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar()=='r'){
                    revealBombs(!bombsRevealed);
                }
            }
            public void keyPressed(KeyEvent e) { }
            public void keyReleased(KeyEvent e) { }
        });
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
            game.mouseLoc = game.mouse2GridCoords(e.getX(), e.getY());
            game.repaint();
        } catch (Exception a) {
            if(!a.getMessage().equals(Minesweeper.OUTSIDE_GRID)) {
                a.printStackTrace();
            }
        }
        super.mouseMoved(e);
    }
}



