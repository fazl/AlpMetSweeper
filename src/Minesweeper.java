import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

enum Difficulty{
    Easy(6,6),
    Medium(9,20),
    Hard(15,90);

    private int size;
    private int mineCount;
    Difficulty(int size, int count){
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

    public int getSize() {return size;}
    public int getMineCount() {return mineCount;}

}

class GameChooser extends JFrame{

}

public class Minesweeper extends JPanel implements ActionListener {

    private static final String BOMB = "*";
    private  int gridSize = 15;
    private final int TILE_SIZE = 45;
    private final int GRID_BASE = 2;
    private int BORDER = 2;

    Point mouseLoc;
    boolean isLeftMouse;
    
    static Difficulty selectedDifficulty = Difficulty.Easy;

    private String[] openFields;
    private String[] closedFields;
    private ArrayList<Integer> bombIndexes = new ArrayList<>();


    private int mineAmount;
    private int[] mineDetectors;

    private int countMarked = 0;
    
    private static JFrame chooserWindow;
    private static JFrame gameWindow = null;


    private boolean isGameOver=false;

    private Minesweeper() {
        addMouseListener(new NewMouseAdapter(this));
        addMouseMotionListener(new NewMouseMotionAdapter(this));

    }


    public void actionPerformed(ActionEvent click) {
        if (click.getActionCommand().equals("Random")) {
            JOptionPane.showMessageDialog(this,JOptionPane.YES_NO_OPTION);

        }
    }

    public Dimension getPreferredSize() {
        int d = TILE_SIZE * gridSize + 2 * GRID_BASE;
        return new Dimension(d, d);
    }

    Point mouseToGridCoords(double xMouse, double yMouse) {
        if (xMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < xMouse ||
                yMouse < GRID_BASE || gridSize * TILE_SIZE + GRID_BASE - BORDER < yMouse) {
            throw new RuntimeException("Ignore mouse outside grid: ");
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
                    openFields[index] = mineDetectors[index]+"";
                }
            }
        }
        else {
            if (!closedFields[index].equals("open") && openFields[index].equals("")) {
                openFields[index] = "X";
                if (openFields[index].equals("X") && closedFields[index].equals(BOMB)) {
                    countMarked++;
                }
            }
            else if (!closedFields[index].equals("open") && openFields[index].equals("X")) {
                openFields[index] = "?";
                if (openFields[index].equals("?") && closedFields[index].equals(BOMB)) {
                    countMarked--;
                }
            }
            else if (!closedFields[index].equals("open") && openFields[index].equals("?")) {
                openFields[index] = "";
            }
            if(countMarked == mineAmount && !isGameOver){
                winner();
            }
        }
        repaint();
    }

    private void newGame(boolean restart) {
        gridSize = selectedDifficulty.getSize();

        countMarked = 0;
//        if(gameWindow == null){gameWindow = new JFrame("Minesweeper 1.0");}
        if (!restart) gameWindow = new JFrame("Minesweeper 1.0");
        openFields = new String[gridSize * gridSize];
        closedFields = new String[gridSize * gridSize];
        Arrays.fill(closedFields, "");
        Arrays.fill(openFields, "");
        closedFields=placeRandomMines();
        bombIndexes.clear();
        for (int i = 0; i < closedFields.length; i++) {
            if (closedFields[i].equals(BOMB)) {
                bombIndexes.add(i);
            }
        }

        mineDetectors=new int[gridSize * gridSize];
        mineDetectors = findAdjacent(closedFields);
        isGameOver=false;
        mouseLoc=null;
        repaint();
    }

    private void gameOver() {
        ImageIcon icon = new ImageIcon(getClass().getResource("/gameover.png"));

        isGameOver=true;

        for (int i=0; i<closedFields.length; i++) {
            if (closedFields[i].equals(BOMB)) {
                openFields[i]= BOMB;
            }
        }
        repaint();
        if (JOptionPane.showConfirmDialog(null, "Game Over\nWanna play again?", "Game Over", JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE, icon) == JOptionPane.YES_OPTION) {
            newGame(true);
        } else {
            chooserWindow.setVisible(true);
            gameWindow.dispose();
            isGameOver = false;
        }
    }

    private void winner() {
        ImageIcon icon = new ImageIcon(getClass().getResource("/youwon.gif"));



        if (JOptionPane.showConfirmDialog(null, "Congratulations, You Won!\nWanna play again?",
                "You Won", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, icon) == JOptionPane.YES_OPTION) {
            newGame(true);
        }
        else{
            chooserWindow.setVisible(true);
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
                        System.out.printf("closedFields[%d] but length=%d\n",(index + range + i), closedFields.length );
                        System.out.printf("index=%d,range=%d,i=%d\n",index, range, i);
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
            if(newField[i].equals("")){
                newField[i] = (rand.nextInt(8) < 1 ? BOMB : "");
                if(newField[i].equals(BOMB)){
                    mineAmount--;
                }
            }
            if(mineAmount != 0 && i == (gridSize * gridSize) - 1 ){
                i = 0;
            }
            if(mineAmount == 0){
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


        if (closedFields[gridCoordsToArrayIndex(xTile, yTile)].equals("open"))  {
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

    private static String getFirstWord(Object obj){
        String text = ""+obj;
        return text.substring(0,text.indexOf(" "));
    }

    public static void main(String[] args) {
        JComboBox<Difficulty> gameChooser = new JComboBox<>(Difficulty.values());
        gameChooser.addItemListener((ItemEvent e) -> {
            if(e.getStateChange()==ItemEvent.SELECTED){
                selectedDifficulty = (Difficulty)e.getItem();
            }
        });

        ImageIcon icon = new ImageIcon(Minesweeper.class.getResource("/MS.png"));
        JButton start = new JButton("Start");
        start.addActionListener(e -> {
            Minesweeper game = new Minesweeper();

            game.newGame(false);


            gameWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            gameWindow.setIconImage((icon.getImage()));

            gameWindow.add("Center", game);

            gameWindow.pack();
            gameWindow.setLocationRelativeTo(null);
            gameWindow.setVisible(true);
            chooserWindow.setVisible(false);

        });


        ImageIcon explanationIcon = new ImageIcon(Minesweeper.class.getResource("/title.jpg"));
        JLabel explanation = new JLabel();
        explanation.setIcon(explanationIcon);
        explanation.setPreferredSize(new Dimension(300,120));

        chooserWindow = new JFrame("Minesweeper 1.0");
        chooserWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chooserWindow.setIconImage((icon.getImage()));

        chooserWindow.add("North", explanation);
        chooserWindow.add("Center", gameChooser);
        chooserWindow.add("East", start);

        chooserWindow.pack();
        chooserWindow.setSize(315,200);
        chooserWindow.setLocationRelativeTo(null);
        chooserWindow.setVisible(true);
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
//            System.out.println(a);
        }
        super.mouseMoved(e);
    }
}



