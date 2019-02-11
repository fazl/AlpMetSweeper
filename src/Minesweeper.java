import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class TileData {
    boolean isOpened = false;
    boolean hasMine = false;
    private TileLabel label = TileLabel.nil;
    private TileLabel backupLabel = TileLabel.nil;

    TileLabel getLabel() { return label; }
    void setLabel(TileLabel label) { this.label = label; }

    int detectedMines = 0;

    boolean isLive(){
        return label != TileLabel.marked && label != TileLabel.maybe;
    }

    void open() {
        if(!isOpened) {
            isOpened = true;
            label = TileLabel.values()[detectedMines];
        }
    }

    void backupLabel() { backupLabel = label;}
    void restoreLabel() { label = backupLabel;}
}

class GridPoint extends Point {
    GridPoint(int x, int y){ super(x,y); }

    @Override
    public String toString() { return String.format("GridPoint(x=%d,y=%d)", x, y); }
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
        chooserCombo.setSelectedItem(Minesweeper.selectedDifficulty);
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


    private static final String GAME_NAME = "Minesweeper 1.0";
    static final String OUTSIDE_GRID = "Ignore mouse outside grid";
    private static final Color COLOR_CELL_OPEN = Color.LIGHT_GRAY;
    private static final Color COLOR_CELL_HIGHLIGHT = Color.ORANGE;
    private static final Color COLOR_CELL_GAMEOVER = Color.DARK_GRAY;
    private static final Color COLOR_CELL_UNOPENED = Color.GRAY;
    private static final Color COLOR_CELL_TEXT = Color.BLUE;
    static Difficulty selectedDifficulty = Difficulty.Medium;
    private static JFrame gameWindow = null;
    private final int TILE_SIZE = 45;
    private final int GRID_BASE = 2;
    GridPoint mouseLoc=null;
    boolean isLeftMouse;
    private int gridSize = 15;
    private int BORDER = 2;
    private GameChooser gameChooser;
    private TileData[] fields;
    private ArrayList<Integer> bombIndexes = new ArrayList<>();
    private int mineAmount;
    private int countMarked = 0;
    private boolean isGameOver = false;
    private boolean showMines =false;

    private Minesweeper() {
        addMouseListener(new MouseClickAdapter(this));
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

    GridPoint mouse2GridCoords(double xMouse, double yMouse) {
        if (xMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < xMouse ||
            yMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < yMouse) {
            throw new RuntimeException(OUTSIDE_GRID);
        }


        int gridX = (int) ((xMouse - GRID_BASE) / TILE_SIZE);
        int gridY = (int) ((yMouse - GRID_BASE) / TILE_SIZE);

        return new GridPoint(gridX, gridY);
    }

    private int coordsToIndex(int column, int row) {
        if(column<0 || gridSize<=column) throw new IllegalArgumentException("Off grid col="+column);
        if(row<0    || gridSize<=row) throw new IllegalArgumentException("Off grid row="+row);
        return column + gridSize * row;
    }
    private int coordsToIndex(GridPoint posColRow){
        return coordsToIndex(posColRow.x, posColRow.y);
    }

    void onClick(boolean isLeftMouse, int xMouse, int yMouse) {
        int index = coordsToIndex( mouse2GridCoords(xMouse, yMouse) );

        if (isLeftMouse) {
            onClickLeft(index);
        } else {
            onClickRight(index);
        }
        repaint();
    }

    // Opens cells (maybe cluster). Check for loser
    //
    private void onClickLeft(int index) {
        TileData field = fields[index];
        // X or ? protects cell against accidental clicks
        if(field.isLive()){
            if (field.hasMine) {
                userLost();                             // game over
            } else {
                if (field.detectedMines == 0) {
                    openCluster(index);
                } else {
                    field.open(); //also sets label
                }
            }
        }
    }

    // Cycle unopened cell label "" -> X -> ? -> "" (and check for winner)
    //
    private void onClickRight(int index) {
        final TileData field = fields[index];
        if (field.isOpened || isGameOver )
            return;

        switch (field.getLabel()) {
            case nil:
                field.setLabel( TileLabel.marked );
                if (field.hasMine) {
                    if (++countMarked == mineAmount) {
                        userWon();                      // game over
                    }
                }
                break;
            case marked:
                field.setLabel( TileLabel.maybe );
                if (field.hasMine) {
                    --countMarked;
                }
                break;
            case maybe:
                field.setLabel(TileLabel.nil);
                break;
        }
    }

    void newGame() {
        gridSize = selectedDifficulty.getSize();

        final int N = gridSize*gridSize;
        if(N <= mineAmount){
            String error = String.format("mineAmount: %d >= %d squares :(", mineAmount, N);
            System.err.println(error);
            throw new IllegalStateException(error);
        }

        countMarked = 0;
        fields = new TileData[N];
        for(int i = 0; i<N; ++i){ fields[i]=new TileData(); }
                                                    // TODO  dispersed vs clumped strategies ?
        mineAmount = placeRandomMines(selectedDifficulty.getMineCount(), fields, bombIndexes);

        showMines = false;
        isGameOver = false;
        mouseLoc = null;
        gameWindow.pack();
        gameWindow.setLocationRelativeTo(null); //center window - must call after size set!
        gameWindow.setVisible(true);
    }

    // Only for testing, honest !!
    private void revealBombs(boolean reveal){
        System.out.printf("Entered revealBombs(%b)\n", reveal);
        showMines = reveal;
        for (TileData field : fields) {
            if (field.hasMine) {
                if (reveal) {
                    field.backupLabel();
                    field.setLabel(TileLabel.bomb);               //temporary reveal
                } else {
                    field.restoreLabel();
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
            "Kaboom! You Lost",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            gameOverIcon,
            NewGameOption.values(),
            NewGameOption.SameAgain
        );
        restart(NewGameOption.values()[option]);
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
        restart(NewGameOption.values()[option]);
    }

    private void restart(NewGameOption option){
        switch (option){
            case SameAgain:
                newGame();
                break;
            case Menu:
                gameChooser.setVisible(true);
                gameWindow.dispose();
                isGameOver = false;
                break;
            case Quit:
                System.exit(0);  // crude but okay for app with no cleanup action needed
                break;
        }
    }


    private GridPoint index2Coords(int index) {
        if(index<0 || gridSize*gridSize<=index){
            throw new IllegalArgumentException("Index off grid: " + index);
        }
        return new GridPoint(index % gridSize, index / gridSize);
    }



    private Set<Integer> getNeighbourIdxs(int index ){
        GridPoint p = index2Coords(index);
        Set<Integer> neighbours = new TreeSet<>(); //auto sorts
        for (int dx = -1; dx<2; ++dx){
            for (int dy = -1; dy<2; ++dy){
                try {
                    GridPoint neighbour = new GridPoint(p.x + dx, p.y + dy); //off grid -> throws
                    if (!neighbour.equals(p)) {  // neighbour, not self
                        int neighbourIndex = coordsToIndex(neighbour);
                        if(neighbours.contains(neighbourIndex)){
                            System.err.printf(
                                "\nLogic error: mine idx %d neigbIdx %d already in neighbours.\n",
                                index, neighbourIndex
                            );
                        }else {
                            neighbours.add(neighbourIndex);
                        }
                    }
                }catch(IllegalArgumentException e){
                    //off grid neighbour, ignore
                }
            }
        }
        System.out.printf("%s's neighbours: Idxs: %s\n", p, neighbours);
        return neighbours;
    }


    // Try to place requested nr of mines.  Return nr of mines placed.
    private int placeRandomMines(int nMines, TileData[] mineField, ArrayList<Integer> bombIndexes) {
        bombIndexes.clear();
        final int N = gridSize*gridSize;
        if(N!=mineField.length){throw new IllegalStateException("Logic error: mineField size mismatches grid size");}
        final int origMines = nMines;

        Random rand = new Random();

//        // Takes about 800 iters to lay 90 mines
//        int attempts = 0;
//        for (int i = 0; i < N; i++) {
//            ++attempts;
//            if (mineField[i].isEmpty() && rand.nextInt(8) < 1) {
//                mineField[i] = BOMB;
//                if (--nMines == 0) {
//                    System.out.printf("Success: %d mines laid in %d iterations\n", origMines, attempts);
//                    break;
//                }
//            }
//            if (nMines != 0 && i == (gridSize * gridSize) - 1) {
//                i = 0;
//            }
//        }

        // Takes about 100 iters to lay 90 mines
        int col, row, index;
        for(int attempt = 1; ; ++attempt ){
            System.out.printf("Lay mines iter: %d (outstanding %d mines)..  ", attempt, nMines);
            index = coordsToIndex(col=rand.nextInt(gridSize), row=rand.nextInt(gridSize));
            if(!mineField[index].hasMine){
                mineField[index].hasMine = true;
                bombIndexes.add(index);
                for (int nIdx : getNeighbourIdxs( index ) ) {
                    mineField[nIdx].detectedMines++;
                }

                System.out.printf("Placed bomb at (%d, %d)\n", col, row);
                if (--nMines <= 0) {
                    System.out.printf("Success: %d mines laid in %d iterations\n", origMines, attempt);
                    break;
                }
            }else{
                System.out.printf("\nAlready occupied: (%d, %d)\n", col, row);
            }
            if( 1000_000 < attempt ){
                System.err.printf("Failed to lay all mines after %d attempts!\n", attempt);
                System.err.printf("Quitting after %d mines laid!\n", bombIndexes.size());
                break;
            }
        }
        Collections.sort(bombIndexes);  // why not
        System.out.printf("\nRecorded %d mines:\n%s\n", bombIndexes.size(), bombIndexes);
        if(bombIndexes.size() != origMines){
            System.err.printf("Error: Expected %d (not %d) mines!\n",
                origMines,
                bombIndexes.size());
        }
        return bombIndexes.size();

    }

    // Player clicked an empty cell... try to open cluster of empty cells.
    // Recurses on neighbours that have no detected mines
    //
    private void openCluster(int index) {
        fields[index].open();

        for( int nIdx : getNeighbourIdxs(index) ){
            TileData field = fields[nIdx];
            if ( !field.isOpened && !field.hasMine ) {
                field.open();
                if (field.detectedMines == 0) {
                    openCluster(nIdx);  // NB recursion
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

    //TODO When game lost, highlight mines player did not find
    private void drawTile(Graphics g, int yTile, int xTile) {
        int RECT_SIZE = TILE_SIZE - BORDER;

        int topLeftCornerX = xTile * TILE_SIZE + (GRID_BASE + BORDER);
        int topLeftCornerY = yTile * TILE_SIZE + (GRID_BASE + BORDER);

        // shading opened cells
        Color color=COLOR_CELL_OPEN;

        // shading unopened cells
        TileData field = fields[coordsToIndex(xTile, yTile)];
        if (!field.isOpened) {
            color = COLOR_CELL_UNOPENED;

            if (isGameOver) {
                color=COLOR_CELL_GAMEOVER;
            }else if (field.isLive() && new GridPoint(xTile, yTile).equals(mouseLoc)){
                color = COLOR_CELL_HIGHLIGHT;
            }
        }
        g.setColor(color);
        g.fillRect(topLeftCornerX, topLeftCornerY, RECT_SIZE, RECT_SIZE);

        // draw label on tile
        String text = field.getLabel().text;
        g.setFont(new Font("Sans", Font.BOLD, 20));
        g.setColor(field.hasMine ? COLOR_CELL_HIGHLIGHT : COLOR_CELL_TEXT);
        g.drawString(
            text,
            topLeftCornerX + TILE_SIZE / 2 - 6,
            topLeftCornerY + TILE_SIZE / 2 + 5);
    }

    private void testingAid(){
        setFocusable(true);
        addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                if ("rR".contains(""+e.getKeyChar())){
                    revealBombs(!showMines);
                }
            }
            public void keyPressed(KeyEvent e) { }
            public void keyReleased(KeyEvent e) { }
        });
    }

}

class MouseClickAdapter extends MouseAdapter {
    private Minesweeper game;

    MouseClickAdapter(Minesweeper p) {
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



