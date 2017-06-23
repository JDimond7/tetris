
//TODO: Find a better and more consistent alternative to swing's timer.

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jdimond on 15/06/17.
 */


public class tetris {

    static Board board;

    public static void main(String[] args){
        JFrame frame = new JFrame();

        frame.setSize(10*25+25, 20*25+80); //not sure why these dimensions fit, but they do quite well.
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setTitle("Tetris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container,BoxLayout.Y_AXIS));

        board = new Board();
        Status stats = new Status();
        stats.setPreferredSize(new Dimension(11*25,5));

        container.add(board);
        container.add(stats);
        frame.add(container);

        board.timer.start();
        musicThread music = new musicThread();

        frame.setVisible(true); //after my while true in music thread, if these lines
        music.run();            //are the other way round, the frame won't load.
                                //also the music takes disgusting amount of cpu...!

        }
}

class Status extends JPanel{

    static int score = 0;
    static int level = 1;

    static JLabel scoreLabel = new JLabel();
    static JLabel pauseLabel = new JLabel();
    static JLabel levelLabel = new JLabel();

    static boolean paused = false;
    static boolean gameIsOver = false;

    public Status(){

        labelInit();
        this.add(levelLabel);
        this.add(scoreLabel);
        this.add(pauseLabel);
    }

    public static void labelInit(){
        levelLabel.setText("Level: " + level);
        scoreLabel.setText("      Score: " + 0);
    }

    public static void increaseScore(int n){
        score += Math.pow(2,n-1);
        scoreLabel.setText("      Score: " + score);
    }

    public static void levelUp(){
       level++;
       levelLabel.setText("Level: " + level);
    }

    public static void pause(){
        paused = true;
        pauseLabel.setText(" PAUSED ");
    }

    public static void unpause(){
        paused = false;
        pauseLabel.setText("");
    }

    public static void resetScoreAndLevel() {
        score = 0;
        scoreLabel.setText("      Score: " + score);
        level = 1;
        levelLabel.setText("Level: " + level);
    }
}

class Board extends JPanel implements ActionListener {

    int blocksize = 25; //size of blocks in pixels

    Tetronimo currentPiece;

    Timer timer = new Timer(600, this);
    static Random random = new Random();

    BufferedImage boardStateImage = new BufferedImage(11*25,27*20,BufferedImage.TYPE_INT_ARGB);
    static Color[][] boardState = new Color[10][20];

    public Board(){
        initBoardState();
        super.setPreferredSize(new Dimension(270, 500));
        currentPiece = new Tetronimo();
        addKeyListener(new keyboardInput());
        setFocusable(true);
        requestFocusInWindow(); //needed this line and the one above before the keyListener would work.

        //draw boundary.
        Graphics2D g = boardStateImage.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0,0,10,20*25+10);
        g.fillRect(10*25+10,0,10*25+10,20*25+10);
        g.fillRect(0,20*25+10,10*25+25,20*25+10);

    }

    @Override
    public void paint(Graphics g){
        super.paint(g);
        for (int[] pt: currentPiece.coords){
            g.setColor(currentPiece.color);
            g.fill3DRect(pt[0],pt[1],blocksize,blocksize,true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(currentPiece.moveIsPossible("DOWN", boardState)){
            currentPiece.lineDown(boardState);
        } else {
            for (int[] block : currentPiece.coords){
                boardState[(block[0]-10)/blocksize][(block[1]-10)/blocksize] = currentPiece.color;
            }
            Graphics2D g = boardStateImage.createGraphics();
            this.paint(g);
            removeFullLines(g);
            currentPiece = new Tetronimo();

            if (gameOver()){
                timer.stop();
                g.setColor(Color.BLACK);
                Font font = new Font("Courier", Font.BOLD, 45);
                g.setFont(font);
                g.drawString("GAME OVER", 13,200);
            }
        }
        repaint();
    }


    @Override
    public void paintComponent(Graphics g){
        g.drawImage(boardStateImage,0,0,Color.WHITE,this); //adding Colour.WHITE and making image typr ARGB above
                                                           //has fixed colour problems!
    }


    public static void initBoardState(){
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 20; j++) {
                boardState[i][j] = null;
            }
        }
    }

    public void levelUp(){
        Status.levelUp();
        if (Status.level < 16) {
            int delay = timer.getDelay();
            timer.setDelay((int) Math.floor(0.90 * delay)); //gets 10% faster until level 16.
        }
    }

    public boolean gameOver(){
        for (int[] block : currentPiece.coords){
            if (boardState[(block[0]-10)/blocksize][(block[1]-10)/blocksize] != null){
                Status.gameIsOver = true;
                timer.stop();
                return true;
            }
        }
        return false;
    }

    public void restart(){
        initBoardState(); //reset board
        Graphics2D g = (Graphics2D) boardStateImage.getGraphics();
        g.setColor(Color.WHITE); //reset background
        g.fillRect(10,0,10*25,20*25+10);
        repaint();
        Status.resetScoreAndLevel();
        timer.setDelay(600); //reset level
        timer.start(); //start over.
    }

    public void removeFullLines(Graphics2D g){
        ArrayList<Integer> fullLines = new ArrayList();
        boolean lineIsFull = true;

        for (int j = 0; j < 20; j++){
            lineIsFull = true;
            for (int i = 0; i < 10; i++){
                if (boardState[i][j] == null){
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull){
                fullLines.add(j);
            }
        }

        Status.increaseScore(fullLines.size());
        if (Status.score != 0 && Status.score%15 == 0){ //level for each 15 points. not working at all for some reason.
            //this current method means that skips in points via 3 or 4 line clears can cause
            //a levelup to be missed.
            levelUp();
            Status.score++; //total hack, should think of something better.
        }
        for (int n : fullLines){
            for (int j = n; j > 0; j--){
                for (int i = 0; i < 10; i++){
                    if (boardState[i][j-1] == null){
                        boardState[i][j] = null;
                        g.setColor(Color.white);
                        g.fillRect((i*blocksize)+10, (j*blocksize)+10, blocksize, blocksize);
                        continue;
                    }
                    boardState[i][j] = boardState[i][j-1]; //THIS should be what was missing. Sorted it!!!
                    g.setColor(boardState[i][j-1]);
                    g.fill3DRect((i*blocksize)+10, (j*blocksize)+10, blocksize, blocksize, true);
                }
            }
        }
        repaint();
    }


    class keyboardInput extends KeyAdapter{
        public void keyPressed(KeyEvent e){
            switch (e.getKeyCode()){
                case KeyEvent.VK_RIGHT:
                    if (Status.paused || Status.gameIsOver){
                        break;
                    }
                    currentPiece.moveRight(boardState);
                    repaint();
                    break;

                case KeyEvent.VK_LEFT:
                    if (Status.paused || Status.gameIsOver){
                        break;
                    }
                    currentPiece.moveLeft(boardState);
                    repaint();
                    break;

                case KeyEvent.VK_DOWN:
                    if (Status.paused || Status.gameIsOver){
                        break;
                    }
                    currentPiece.moveDown(boardState);
                    repaint();
                    break;

                case KeyEvent.VK_SPACE:
                    if (Status.paused || Status.gameIsOver){
                        break;
                    }
                    while(currentPiece.lineDown(boardState)){}
                    repaint();
                    break;

                case KeyEvent.VK_UP:
                    if (Status.paused || Status.gameIsOver){
                        break;
                    }
                    currentPiece.rotate();
                    repaint();
                    break;

                case KeyEvent.VK_P:
                    if (timer.isRunning()) {
                        Status.pause();
                        timer.stop();
                    } else {
                        Status.unpause();
                        timer.start();
                    }
                    break;

                case KeyEvent.VK_R:
                    if (Status.gameIsOver){
                        Status.gameIsOver = false;
                        restart();
                    }
                    break;

                case KeyEvent.VK_Q:
                    System.exit(0);

                default:
                    break;
            }
        }
    }
}

class Tetronimo {

    public enum Shape {LINE, S, Z, L, REVERSE_L, T, SQUARE, NONE};
    Shape shape;

    int blocksize = 25;
    int boardWidth = 10*blocksize;
    int boardHeight = 20*blocksize;

    int[][] coords;

    Shape[] values = Shape.values();

    Color color = Color.white;
    Random random = new Random();

    int center = boardWidth/2;

    int[][] lineShape = new int[][]{ {center+10,10},
            {center+10,10+blocksize},
            {center+10,10+2*blocksize},
            {center+10,10+3*blocksize} };

    int[][] tShape = new int[][]{ {center+10-blocksize, blocksize+10},
            {center+10, blocksize+10},
            {center+10,10},
            {center+10+blocksize,blocksize+10}};

    int[][] lShape = new int[][]{ {center+10, 10},
            {center+10,10+blocksize},
            {center+10, 10+2*blocksize},
            {center+10+blocksize,2*blocksize+10}};

    int[][] sShape = new int[][]{ {center+10-blocksize, blocksize+10},
            {center+10,10},
            {center+10+blocksize,10},
            {center+10, blocksize+10}};

    int[][] zShape = new int[][]{ {center+10-blocksize, 10},
            {center+10,10},
            {center+10+blocksize,blocksize+10},
            {center+10, blocksize+10}};

    int[][] SqShape = new int[][]{ {center+10-blocksize, 10},
            {center+10,10},
            {center+10-blocksize,blocksize+10},
            {center+10, blocksize+10}};

    int[][] revLShape = new int[][]{ {center+10, 10},
            {center+10,10+blocksize},
            {center+10-blocksize,10+2*blocksize},
            {center+10,10+2*blocksize}};


    public Tetronimo(){
        this.shape = setShape();
        switch (shape){
            case LINE:
                this.coords = lineShape;
                this.color = Color.cyan;
                break;

            case T:
                this.coords = tShape;
                this.color = Color.magenta;
                break;

            case L:
                this.coords = lShape;
                this.color = Color.orange;
                break;

            case S:
                this.coords = sShape;
                this.color = Color.red;
                break;

            case Z:
                this.coords = zShape;
                this.color = Color.green;
                break;

            case SQUARE:
                this.coords = SqShape;
                this.color = Color.yellow;
                break;

            case REVERSE_L:
                this.coords = revLShape;
                this.color = Color.blue;
                break;
        }
    }

    Shape setShape(){ //chooses shape of new tetronimo.
        int r = random.nextInt(7);
        return values[r];
    }

    public void moveRight(Color[][] boardState){
        if (moveIsPossible("RIGHT", boardState)) {
            for (int[] pt : this.coords) {
                pt[0] += blocksize;
            }
        }
    }

    public void moveDown(Color[][] boardState){
        if (moveIsPossible("DOWN", boardState)) {
            for (int[] pt : this.coords) {
                pt[1] += blocksize;
            }
        }
    }

    public void moveLeft(Color[][] boardState){
        if (moveIsPossible("LEFT", boardState)) {
            for (int[] pt : this.coords) {
                pt[0] -= blocksize;
            }
        }
    }

    public boolean lineDown(Color[][] boardState){ //the line down enforced by time. Is boolean, not void, hence
        if (moveIsPossible("DOWN", boardState)) {  // separate from moveDown.
            for (int[] pt : this.coords) {
                     pt[1] += blocksize;
            }
            return true;
        }
        return false;
    }

    public boolean moveIsPossible(String move, Color[][] boardState){
        if (move.equals("RIGHT")){
            if (this.coords[0][0] + blocksize < boardWidth &&
                    this.coords[1][0] + blocksize < boardWidth &&
                    this.coords[2][0] + blocksize < boardWidth &&
                    this.coords[3][0] + blocksize < boardWidth){
                for (int[] block : this.coords){
                    if (null != boardState[(block[0]-10)/blocksize + 1][(block[1]-10)/blocksize]){
                            return false;
                    }
                }
                return true;
            }
            return false;
        }
        else if (move.equals("LEFT")){
            if (this.coords[0][0] - blocksize > 0 &&
                    this.coords[1][0] - blocksize > 0 &&
                    this.coords[2][0] - blocksize > 0 &&
                    this.coords[3][0] - blocksize > 0){
                for (int[] block : this.coords){
                    if (null != boardState[(block[0]-10)/blocksize - 1][(block[1]-10)/blocksize]){
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        else if (move.equals("DOWN")){
            if (this.coords[0][1] + blocksize < boardHeight &&
                    this.coords[1][1] + blocksize < boardHeight &&
                    this.coords[2][1] + blocksize < boardHeight &&
                    this.coords[3][1] + blocksize < boardHeight){
                for (int[] block : this.coords){
                    if (null != boardState[(block[0]-10)/blocksize][(block[1]-10)/blocksize + 1]){
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        else {
            System.out.println("WEIRD ISSUE");
            return false;
        }
    }

    void rotate(){//like how it does L and Reverse-L. S and Z might want a change.
        if (this.shape == Shape.SQUARE){
            return;
        }
        int centerx = this.coords[1][0];
        int centery = this.coords[1][1];
        boolean shoveRight = false;
        boolean shoveLeft = false;
        boolean shoveRightTwice = false;
        boolean shoveLeftTwice = false;

        int temp;
        for (int[] point : this.coords){
            temp = point[0]-centerx;
            point[0] = -(point[1] - centery) + centerx;
            point[1] = temp + centery;
            if (point[0] < 0){
                shoveRight = true;
            } else if (point [0] > 10*blocksize){
                shoveLeft = true;
            }

            if (point[0] <= -2*blocksize+10){
                shoveRightTwice = true;
            } else if (point[0] >= 11*blocksize+10){
                shoveLeftTwice = true;
            }
        }

        if (shoveRightTwice){ //to handle line piece
            for (int[] point : this.coords){
                point[0] += 2*blocksize;
            }
            return;
        } else if (shoveLeftTwice){
            for (int[] point : this.coords){
                point[0] -= 2*blocksize;
            }
            return;
        }

        if (shoveRight){ //for everything else.
            for (int[] point : this.coords){
                point[0] += blocksize;
            }
        } else if (shoveLeft){
            for (int[] point : this.coords){
                point[0] -= blocksize;
            }
        }
    }
}

class musicThread extends Thread { //provides music thread

    Sequencer seq = null;

    @Override
    public void run() {

        try {
            seq = MidiSystem.getSequencer();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }

        try {
            seq.open();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }

        InputStream musicFile = null;
        try {
            musicFile = new BufferedInputStream(new FileInputStream(new File("ThemeA.mid")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            seq.setSequence(musicFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }

        seq.setLoopCount(100);
        seq.start();
    }
}
