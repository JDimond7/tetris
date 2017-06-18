
//TODO: Make fallen pieces persistent on board. DONE.
//TODO: implement piece rotation. -- pieces rotate, but weird outofbounds errors sometimes occur.
//TODO: Collision detection with the board. DONE.
//TODO: Delete completed lines -- DONE!!
//TODO: Colour the blocks -- DONE!!
//TODO: Final game details; scoring, levels, etc.
//TODO: Make the music not a computational aneurysm for the cpu.
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
    public static void main(String[] args){
        JFrame frame = new JFrame();
        //frame.setSize(400,700);
        frame.setSize(10*25+25, 20*25+40); //not sure why these dimensions fit, but they do quite well.
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setTitle("Tetris");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Board board = new Board();
        frame.add(board);
        board.timer.start();
        //musicThread music = new musicThread();

        frame.setVisible(true); //after my while true in music thread, if these lines
        //music.run();            //are the other way round, the frame won't load.
                                //also the music takes disgusting amount of cpu...!

    }
}

class Board extends JPanel implements ActionListener {
    int blocksize = 25; //size of blocks in pixels
    int boardWidth = 10*blocksize;
    int boardHeight= 20*blocksize; //std size of board relative to blocks
    Tetronimo testShape;
    Timer timer = new Timer(500, this);
    static Random random = new Random();
    BufferedImage boardStateImage = new BufferedImage(11*25,27*20,BufferedImage.TYPE_INT_ARGB);

    boolean[][] boardState = new boolean[boardWidth][boardHeight]; //auto init to all false
    Color[][] boardState2 = new Color[boardWidth][boardHeight];

    void initBoardState(){ //init board to null just for peace of mind.
        for (Color[] row : boardState2){
            for (Color col : row){
                col = null;
            } //Actually, maybe make one larger and colour boundary white, so that this provides edges of board nicely.
        }
    }

    public Board(){
        initBoardState();
        testShape = new Tetronimo();
        addKeyListener(new keyboardInput());
        setFocusable(true);
        requestFocusInWindow(); //needed this line and the one above before the keyListener would work.
    }

    @Override
    public void paint(Graphics g){
        super.paint(g);
        for (int[] pt: testShape.coords){
            g.setColor(testShape.color);
            g.fill3DRect(pt[0],pt[1],blocksize,blocksize,true);//much better!
        }
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        //if (testShape.moveDownPossible("DOWN",boardState2)){
        if(testShape.moveIsPossible("DOWN",boardState2)){
            testShape.lineDown(boardState2);
        } else {
            for (int[] block : testShape.coords){
                //boardState[block[0]][block[1]] = true;
                boardState2[block[0]][block[1]] = testShape.color;
            }
            Graphics2D g = boardStateImage.createGraphics();
            this.paint(g);
            removeFullLines(g);
            testShape = new Tetronimo();
        }
        repaint();
    }

    public void paintComponent(Graphics g){
        //super.paintComponent(g); //removing this fixes non-persistent pieces - but the colours are flipped
        g.drawImage(boardStateImage,0,0,Color.WHITE,this); //adding Colour.WHITE and making image typr ARGB above
                                                           //has fixed colour problems!
    }

    public void removeFullLines(Graphics2D g){ //not recognising a full line...
        ArrayList<Integer> fullLines = new ArrayList();
        boolean lineIsFull = true;

        for (int j = 0; j < boardHeight; j+=blocksize) {
            lineIsFull = true;
            for (int i = 0; i < boardWidth; i+=blocksize) {
               if (null == boardState2[10+i][10+j]){ //this offset worked to make it recognise full lines;
                   lineIsFull = false;
                   break;
               }
            }
            if (lineIsFull){
                fullLines.add(j);
            }
        }

        for (int n : fullLines){ //this doens't work though.
            for (int j = n; j > blocksize; j-=blocksize) {
                for (int i = 0; i < boardWidth; i+=blocksize) {
                    if (null == boardState2[10+i][10+j-blocksize]){
                        boardState2[10+i][10+j] = null;
                        g.setColor(Color.white);
                        g.fillRect(10+i,10+j,blocksize,blocksize);
                        continue;
                    }
                    g.setColor(boardState2[10+i][10+j-blocksize]);
                    g.fill3DRect(10+i,10+j,blocksize,blocksize,true);
                }
            }
        }
        System.out.println("full lines: " + fullLines.size());
        repaint();
    }

    class keyboardInput extends KeyAdapter{
        public void keyPressed(KeyEvent e){
            switch (e.getKeyCode()){
                case KeyEvent.VK_RIGHT:
                    testShape.moveRight(boardState2);
                    repaint();
                    break;
                case KeyEvent.VK_LEFT:
                    testShape.moveLeft(boardState2);
                    repaint();
                    break;
                case KeyEvent.VK_DOWN:
                    testShape.moveDown(boardState2);
                    repaint();
                    break;
                case KeyEvent.VK_SPACE:
                    while(testShape.lineDown(boardState2)){} //strange but works.
                    repaint();
                    break;
                case KeyEvent.VK_UP:
                    testShape.rotate();
                    repaint();
                    break;
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

    //shapes to be written with top-left most block first, bottom-right most block last.
    //top takes priority over right, bottom takes priority over right.
    int center = boardWidth/2;

    int[][] lineShape = new int[][]{ {center+10,10},
                                     {center+10,10+blocksize},
                                     {center+10,10+2*blocksize},
                                     {center+10,10+3*blocksize} };

    int[][] tShape = new int[][]{ {center+10-blocksize, blocksize+10},
                                  {center+10,10},
                                  {center+10, blocksize+10},
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

    public boolean moveIsPossible(String move, Color[][] boardState){ //might it be best to check all blocks?
        if (move.equals("RIGHT")){
            if (this.coords[0][0] + blocksize < boardWidth &&
                    this.coords[1][0] + blocksize < boardWidth &&
                    this.coords[2][0] + blocksize < boardWidth &&
                    this.coords[3][0] + blocksize < boardWidth){
                for (int[] block : this.coords){
                    if (null != boardState[block[0]+blocksize][block[1]]){
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
                    if (null != boardState[block[0]-blocksize][block[1]]){
                        return false;
                    }
                }
                return true;
            }
            return false;
        } else if (move.equals("DOWN")){
            if (this.coords[0][1] + blocksize < boardHeight &&
                    this.coords[1][1] + blocksize < boardHeight &&
                    this.coords[2][1] + blocksize < boardHeight &&
                    this.coords[3][1] + blocksize < boardHeight){
                for (int[] block : this.coords){
                    if (null != boardState[block[0]][block[1]+blocksize]){
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

    void rotate(){//like how it does L and Reverse-L. S and Z might want a change, as might T.
        if (this.shape == Shape.SQUARE){
            return;
        }
        //int[][] rotmat = new int[][]{{0,-1},{1,0}};
        int centerx = this.coords[1][0];
        int centery = this.coords[1][1];
        int temp;
        for (int[] point : this.coords){
            temp = point[0]-centerx;
            point[0] = -(point[1] - centery) + centerx;
            point[1] = temp + centery;
        }
    }
}

class musicThread extends Thread { //provides music thread - but how to get it to loop? --will the curent thing be ok forever?

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

        keepPlaying();
    }

    private void keepPlaying(){
        seq.start();
        while(seq.isRunning()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Does this happen?");
        keepPlaying();
    }
    //keepPlaying fails to make it loop correctly. After seq finishes, it no longer has a set
    //sequence, so has nothing to play. Tried to make it reset the sequence as the first line of keepPlaying
    //however this just made a strange exception get thrown. Not ideal.
}


//for (int[] pt : testShape.coords){
//    g.drawLine(pt[0],          pt[1],          pt[0]+blocksize,pt[1]);
//    g.drawLine(pt[0],          pt[1],          pt[0],          pt[1]+blocksize);
//    g.drawLine(pt[0]+blocksize,pt[1],          pt[0]+blocksize,pt[1]+blocksize);
//    g.drawLine(pt[0],          pt[1]+blocksize,pt[0]+blocksize,pt[1]+blocksize);
//}

//public boolean moveDownPossible(String move, Color[][] boardState){
//    if (this.coords[3][1] + blocksize < boardHeight){
//        for (int[] block : this.coords){
//            //if (boardState[block[0]][block[1]+blocksize]){
//            //    return false;
//            //}
//            if (null != boardState[block[0]][block[1] + blocksize]){
//                return false;
//            }
//        }
//        return true;
//    }
//    return false;
//}
