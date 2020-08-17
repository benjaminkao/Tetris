package sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class GameController {

    public static final int numCols = 10;
    public static final int numRows = 23;
    private static final List<Integer[]> grid = new ArrayList<> ( numRows );
    private static final Random rand = new Random ();

    //All FXML variables
    private Stage stage;
    @FXML
    private Canvas gamePanel;
    @FXML
    private Canvas holdPanel;
    @FXML
    private Canvas nextPanel;
    @FXML
    private VBox gameOverScreen;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label levelLabel;
    @FXML
    private Label gameOverLabel;
    @FXML
    private Label gameOverScoreLabel;
    @FXML
    private Label scoreRowsLabel;
    @FXML
    private Label scoreTetrisLabel;
    @FXML
    private Button resetBtn;

    //Dimension variables
    public static double gameHeight;
    public static double gameWidth;
    private double holdHeight;
    private double holdWidth;

    //Graphics variables
    private GraphicsContext graphicsGame;
    private GraphicsContext graphicsHold;
    private GraphicsContext graphicsNext;

    //Game variables
    private boolean running = false;
    private int level = 1;
    private Timeline gameLoop;
    private boolean usedHold = false;
    private Tetromino current = new Tetromino ( rand.nextInt ( 7 ) + 1, numCols / 2, 1 );
    private Tetromino next = new Tetromino ( rand.nextInt ( 7 ) + 1, numCols / 2, 1 );
    private Tetromino hold;
    private int gameScore = 0;
    private int rowsCleared = 0;
    private int numTetris = 0;


    //Bot variables
    private Bot bot = new Bot ();
    private boolean botMoveHold = false;
    private int botMoveOrientation;
    private double botMoveLeftMostPosition;
    private Semaphore botMoveAvailable = new Semaphore(0);
    private Thread botThread;

    //This queue will hold the moves that the bot will use, producer is the background thread bot
    private ConcurrentLinkedQueue<Integer> botMoves = new ConcurrentLinkedQueue<> ();


    @FXML
    protected void initialize ()
    {
        gameHeight = gamePanel.getHeight ();
        gameWidth = gamePanel.getWidth ();
        graphicsGame = gamePanel.getGraphicsContext2D ();

        holdHeight = holdPanel.getHeight ();
        holdWidth = holdPanel.getWidth ();
        graphicsHold = holdPanel.getGraphicsContext2D ();

        graphicsNext = nextPanel.getGraphicsContext2D ();

        gameLoop = new Timeline ();
        gameLoop.setCycleCount ( Timeline.INDEFINITE );
    }

    @FXML
    protected void resetGame()
    {
        if (resetBtn.getText().equals("Play"))
        {
            level = 1;
            gameOverScreen.setVisible(false);
            gameOverLabel.setText("Game Over");
            resetBtn.setText("Play Again");

        }
        else if(resetBtn.getText ().equals("Play Again"))
        {
            //Reset game variables
            gameScore = 0;
            rowsCleared = 0;
            level = 1;

            //Reset grid
            grid.clear ();

            gameOverScreen.setVisible(false);
            gameOverScoreLabel.setVisible(false);
        }

        for (int i = 0; i < numRows; i++)
        {
            //Have to create new row every time, or else every index in arraylist will point to same row
            Integer[] row = new Integer[numCols];
            Arrays.fill ( row, 0 );

            //Fill in gridScreenshot for bot

            grid.add ( row );
        }

        //Reset score label
        scoreLabel.setText ( "Score: " + gameScore );
        scoreRowsLabel.setText ( "Rows Cleared: " + rowsCleared );
        levelLabel.setText("Level: " + level);

        //Reset hold tetromino
        hold = null;
        graphicsHold.clearRect ( 0, 0, holdWidth, holdHeight );

        //Reset current and next tetromino
        current = new Tetromino ( rand.nextInt ( 7 ) + 1, numCols / 2, 1 );
        next = new Tetromino ( rand.nextInt ( 7 ) + 1, numCols / 2, 1 );


        graphicsNext.clearRect ( 0, 0, holdWidth, holdHeight );
        Tetromino tmp = new Tetromino ( next.getType (), 1, 1 );
        setGraphicsNextColor ( next.getType () );
        for (Pieces piece : tmp.getPieces ())
        {
            graphicsNext.fillRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
            graphicsNext.strokeRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
        }
        bot = new Bot ();
        bot.setGridScreenshot ( grid );
        bot.setCurrentTetromino ( current );
        bot.setNextTetromino ( next );
        bot.sendController ( this );

        runGame ( !resetBtn.getText ().equals ( "Play" ) );
        running = true;
    }

    private void runGame (boolean restart)
    {

        KeyFrame gameFrame = new KeyFrame ( Duration.seconds ( 0.017 ), actionEvent -> {

            if(atTop())
            {
                bot.sendGameOverSignal ();
                gameLoop.stop();
                gameOverScoreLabel.setText("Final Score: " + (gameScore - 100));
                gameOverScreen.setVisible(true);
                gameOverScoreLabel.setVisible(true);
                running = false;
            }

            if ( atBottom ( 0 ) || hitGrid ( 0 ) )
            {       //If tetromino has reached the bottom
                usedHold = false;
                //Add tetromino points to grid
                addToGrid ();

                //Check if any rows have been filled
                checkGridRows ();


                //Set next tetromino to current tetromino
                current = next;

                //Get a new next tetromino
                next = new Tetromino ( rand.nextInt ( 7 ) + 1, numCols / 2, 1 );


                //Update bot variables
                bot.setHoldTetromino ( hold );
                bot.setGridScreenshot ( grid);
                bot.setCurrentTetromino ( current );
                bot.setNextTetromino ( next );

                //Signal bot that game variables are ready to be red
                bot.signalBot();

                graphicsNext.clearRect(0, 0, holdWidth, holdHeight);
                Tetromino tmp = new Tetromino(next.getType(), 1, 1);
                setGraphicsNextColor ( next.getType () );
                for (Pieces piece : tmp.getPieces ())
                {
                    graphicsNext.fillRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
                    graphicsNext.strokeRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
                }
            }

            if(botMoveAvailable.availablePermits () > 0)
            {       //Check to see if bot move is available first before acquiring to prevent blocking
                try
                {
                    botMoveAvailable.acquire();
                } catch (InterruptedException e)
                {
                    e.printStackTrace ();
                }
                if(botMoveHold && hold == null)
                {       //First hold, so bot hasn't predicted next piece
                    holdTetromino();
                    botMoveHold = false;

                    //Update bot variables
                    bot.setHoldTetromino ( hold );
                    bot.setGridScreenshot ( grid);
                    bot.setCurrentTetromino ( current );
                    bot.setNextTetromino ( next );

                    //Signal bot that it needs to make another move
                    bot.signalBot ();
                } else if(botMoveHold)
                {
                    holdTetromino ();
                    botMoveHold = false;
                    botMoveAvailable.release();
                }
                else
                {
                    if ( botMoveOrientation != -1 )
                    {       //Check that orientation doesn't indicate null hold
//                        if ( current.orientation > botMoveOrientation )
//                        {
//                            current.rotateLeft ();
//                        }
//                        else if ( current.orientation < botMoveOrientation )
//                        {
//                            current.rotateRight ();
//                        }
                        current.setOrientation ( botMoveOrientation );
                    }

                    while ( botMoveLeftMostPosition > current.getLeftMostPosition () && !atRightSide () )
                    {
                        current.incrementX ( Main.PIECESIZE );
                    }
                    while ( botMoveLeftMostPosition < current.getLeftMostPosition () && !atLeftSide () )
                    {
                        current.decrementX ( Main.PIECESIZE );
                    }

//                    if ( botMoveLeftMostPosition != current.getLeftMostPosition () || botMoveOrientation != current.orientation )
//                    {   //If bot hasn't moved tetromino enough, then signal that there is still a move available
//                        botMoveAvailable.release ();
//                    }
//                    else
//                    {
                        hardDropTetromino ();
//                    }
                }
            }


            graphicsGame.clearRect ( 0, 0, gameWidth, gameHeight );

            graphicsGame.setFill(Color.WHITE);
            graphicsGame.fillRect(0, 3 * Main.PIECESIZE - 1, gameWidth, 1);

            drawGrid ();

            drawTetromino ( current );

            if ( !atBottom ( 0 ) && !hitGrid ( 0 ) )
            {    //Make sure that tetromino hasn't hit bottom or any pieces due to user movement
                current.incrementY ( level / 5 + 2);
            }

        } );

//        botThread = new Thread ( bot );
//        botThread.setDaemon ( true );
//        botThread.start ();

        gameLoop.getKeyFrames ().add ( gameFrame );
        gameLoop.play ();

    }

    /**
     * This function rotates the tetromino right
     */
    private void rotateTetrominoRight ()
    {
        if ( current.getType () == Tetromino.OBLOCK )
        {       //Square block, no need to rotate
            return;
        }
        current.rotateRight ();
    }

    /**
     * This function rotates the tetromino left
     */
    private void rotateTetrominoLeft ()
    {
        if ( current.getType () == Tetromino.OBLOCK )
        {       //Square block, no need to rotate
            return;
        }
        current.rotateLeft ();
    }


    /**
     * This function hard drops the tetromino
     */
    private void hardDropTetromino ()
    {
        while (!atBottom ( 1 ) && !hitGrid ( 0 ))
        {
            current.incrementY ( 2 );
        }
    }


    /**
     * This function holds the tetromino and swaps out tetromino currently in hold if it exists
     */
    private void holdTetromino ()
    {
        if ( hold == null )
        {   //No hold piece
            //Store current in hold
            hold = current;

            //Set next Tetromino to current
            current = next;

            //Get a new next Tetromino
            next = new Tetromino ( rand.nextInt ( 7 ) + 1, numCols / 2, 1 );
            graphicsNext.clearRect(0, 0, holdWidth, holdHeight);
            Tetromino tmp = new Tetromino(next.getType(), 1, 2);
            setGraphicsNextColor ( next.getType () );
            for (Pieces piece : tmp.getPieces ())
            {
                graphicsNext.fillRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
                graphicsNext.strokeRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
            }
            usedHold = false;
        }
        else
        {   //Already have hold piece, need to swap out
            //Store hold in tmp Tetromino with coordinates at top of game
            Tetromino tmp = new Tetromino(hold.getType(), numCols / 2, 1);


            //Swap hold and current
            hold = current;
            hold.setOrientation ( Tetromino.FACINGUP);
            current = tmp;
        }

        graphicsHold.clearRect(0, 0, holdWidth, holdHeight);
        Tetromino tmp = new Tetromino(hold.getType (), 1, 2);
        setGraphicsHoldColor ( tmp.getType () );
        for (Pieces piece : tmp.getPieces ())
        {
            graphicsHold.fillRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
            graphicsHold.strokeRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
        }
    }


    /**
     * This function checks every row in grid to see if it is filled, if so, delete the rows and move everything down
     */
    private void checkGridRows ()
    {
        //Max of 4 rows can be deleted at a time
        int[] rowsToDelete = new int[4];
        int numDelete = 0;

        boolean delete = true;
        for (int i = 0; i < numRows; i++)
        {
            Integer[] row = grid.get ( i );
            for (int num : row)
            {
                if ( num == 0 )
                {       //No piece there, no need to delete
                    delete = false;
                    break;
                }
            }
            if ( delete )
            {
                rowsToDelete[numDelete++] = i;
                rowsCleared++;

                if(rowsCleared % 10 == 0)
                {
                    level++;
                    levelLabel.setText("Level: " + level);
                }

                scoreRowsLabel.setText("Rows Cleared: " + rowsCleared);
            }
            delete = true;
        }

        if ( numDelete > 0 )
        {
            switch(numDelete)
            {
                case 1:
                    gameScore+=100;
                    break;
                case 2:
                    gameScore+=400;
                    break;
                case 3:
                    gameScore+=800;
                    break;
                case 4:
                    gameScore+=1200;
                    numTetris++;
                    scoreTetrisLabel.setText ( "Tetrises: " + numTetris );
                    break;
            }
            scoreLabel.setText("Score: " + gameScore);

            /*Start deleting rows from the top because ArrayList automatically shifts every row after removal, so
             * deleting rows from the bottom causes the data in rowsToDelete to become invalid */
            for (int i = numDelete - 1; i >= 0; i--)
            {
                grid.remove ( rowsToDelete[i] );

                //Add new row to end of grid
                Integer[] row = new Integer[numCols];
                Arrays.fill ( row, 0 );

                grid.add ( row );
            }
        }
    }

    /**
     * This function adds the current tetromino to grid
     */
    private void addToGrid ()
    {
        //For each piece in the current Tetromino
        for (Pieces piece : current.getPieces ())
        {
            //Get row and column
            /* gridRow = numRows - 1 - (piece.y / Main.PIECESIZE) because if gridRow = piece.y / Main.PIECESIZE,
             * the rows would still delete, but the rows wouldn't move down since technically, the bottom of the grid
             * is also the end of the ArrayList, therefore, removing rows would not shift any rows "down"*/
            int gridRow = numRows - 1 - ((int) piece.y / Main.PIECESIZE);
            int gridCol = (int) piece.x / Main.PIECESIZE;

            grid.get ( gridRow )[gridCol] = current.getType ();
        }
    }

    /**
     * This function checks if the chosen rotation is possible, if not possible in current position, function will
     * move tetromino over left and right to check if rotation is possible in those positions, if yes, function
     * returns true without moving the tetromino back but rotates the piece back; if no, the function returns false,
     * moving the tetromino back
     * and rotating the piece back
     * @param right
     * @return
     */
    private boolean checkRotate ( boolean right )
    {
        //Rotate tetromino right so that pieces are in correct place for checking
        if ( right )
        {
            rotateTetrominoRight ();
        }
        else
        {
            rotateTetrominoLeft ();
        }

        if ( atLeftSide () )
        {   //If over left wall after rotate, move piece to the right until not at left side
            //Store current position for x just in case need to go back to original position
            int x = (int) current.x;
            while (atLeftSide ())
            {
                current.incrementX ( Main.PIECESIZE );
            }
            current.decrementX ( Main.PIECESIZE );
            //Now should be at left wall, now need to check that move hasn't hit any grid pieces
            if ( hitGrid ( 3 ) )
            {       //Move has hit grid pieces
                //Move piece back to original position
                current.x = x;

                //Rotate piece back to original position
                if ( right )
                {
                    rotateTetrominoLeft ();
                }
                else
                {
                    rotateTetrominoRight ();
                }
                return false;
            }
        }
        else if ( atRightSide () )
        {   //If over right wall after rotate, move piece to the left until not at right wall
            //Store current position for x just in case need to go back to original position
            int x = (int) current.x;
            while (atRightSide ())
            {
                current.decrementX ( Main.PIECESIZE );
            }
            current.incrementX ( Main.PIECESIZE );
            //Now should be at right wall, now need to check that move hasn't hit any grid pieces
            if ( hitGrid ( 3 ) )
            {       //Move has hit grid pieces
                //Move piece back to original position
                current.x = x;
                //Rotate piece back to original position
                if ( right )
                {
                    rotateTetrominoLeft ();
                }
                else
                {
                    rotateTetrominoRight ();
                }
                return false;
            }

        }
        else
        {   //Rotation hasn't spilled over sides, must mean piece is in center
            //Check that rotation hasn't hit any grid pieces
            if ( hitGrid ( 3 ) )
            {   //Rotation has hit grid piece
                System.out.println ( "No move: hit grid" );
                //Move piece to the left once
                current.decrementX ( Main.PIECESIZE );
                if ( atLeftSide () )
                {   //Move left has spilled over left side, move back
                    current.incrementX ( Main.PIECESIZE );
                }
                else
                {
                    //Check if hit grid
                    if ( hitGrid ( 3 ) )
                    {   //Move left has caused hit
                        System.out.println ( "Moved left: hit grid" );
                        //Move piece to the right
                        current.incrementX ( 2 * Main.PIECESIZE );

                        if ( hitGrid ( 3 ) )
                        {
                            System.out.println ( "Moved right: hit grid" );
                            //No safe rotation, rotate back to original
                            if ( right )
                            {
                                rotateTetrominoLeft ();
                            }
                            else
                            {
                                rotateTetrominoRight ();
                            }
                            //Move piece back to original
                            current.decrementX ( Main.PIECESIZE );
                            return false;
                        }
                        else
                        {   //Safe rotation, rotate back to original
                            System.out.println ( "Moved right: safe rotation" );
                            if ( right )
                            {
                                rotateTetrominoLeft ();
                            }
                            else
                            {
                                rotateTetrominoRight ();
                            }
                            return true;
                        }
                    }
                    else
                    {   //Haven't hit grid, safe rotation
                        System.out.println ( "Moved left: safe rotation" );
                        //Rotate piece back to original
                        if ( right )
                        {
                            rotateTetrominoLeft ();
                        }
                        else
                        {
                            rotateTetrominoRight ();

                        }
                        return true;
                    }
                }

                //Move piece to the right once
                current.incrementX ( Main.PIECESIZE );
                if ( atRightSide () )
                {   //Move left has spilled over right side, no safe rotation move back
                    if ( right )
                    {
                        rotateTetrominoLeft ();
                    }
                    else
                    {
                        rotateTetrominoRight ();

                    }
                    current.decrementX ( Main.PIECESIZE );
                    return false;
                }
                else
                {   //Hasn't spilled over, check if hit grid
                    if ( hitGrid ( 3 ) )
                    {   //No safe rotation move back
                        if ( right )
                        {
                            rotateTetrominoLeft ();
                        }
                        else
                        {
                            rotateTetrominoRight ();
                        }
                        current.decrementX ( Main.PIECESIZE );
                        return false;
                    }
                    else
                    {   //Safe rotation
                        //Rotate back
                        if ( right )
                        {
                            rotateTetrominoLeft ();
                        }
                        else
                        {
                            rotateTetrominoRight ();
                        }
                        return true;
                    }

                }
            }
        }
        if ( right )
        {
            rotateTetrominoLeft ();
        }
        else
        {
            rotateTetrominoRight ();
        }
        return true;
    }

    /**
     * This function checks to see if the tetromino has hit any pieces in the grid
     */
    private boolean hitGrid ( int num )
    {
        boolean hit = false;
        switch (num)
        {
            case 0:     //Increment y once
                current.incrementY ( Main.PIECESIZE );
                break;
            case 1:     //Decrement x once
                current.decrementX ( Main.PIECESIZE );
                current.incrementY ( (int) current.y % Main.PIECESIZE );
                break;
            case 2:     //Increment x once
                current.incrementX ( Main.PIECESIZE );
                current.incrementY ( (int) current.y % Main.PIECESIZE );
                break;
        }

        for (Pieces piece : current.getPieces ())
        {       //For each piece in tetromino, check if piece in grid exists at incremented positions
            int gridRow = numRows - 1 - ((int) piece.y / Main.PIECESIZE);
            if ( grid.get ( gridRow )[(int) piece.x / Main.PIECESIZE] != 0 )
            {   //Piece already exists in grid
                hit = true;
            }
        }
        switch (num)
        {
            case 0:     //Increment y once
                current.incrementY ( -(Main.PIECESIZE) );
                break;
            case 1:     //Decrement x once
                current.incrementX ( Main.PIECESIZE );
                break;
            case 2:     //Increment x once
                current.decrementX ( Main.PIECESIZE );
                break;
        }

        for (Pieces piece : current.getPieces ())
        {       //For each piece in tetromino, check if piece in grid exists at current positions
            int gridRow = numRows - 1 - ((int) piece.y / Main.PIECESIZE);
            int gridCol = (int) piece.x / Main.PIECESIZE;
            if ( grid.get ( gridRow )[gridCol] != 0 && grid.get ( gridRow - 1 )[gridCol] != 0 )
            {   //Piece already exists in grid
                hit = true;
            }
        }

        return hit;
    }

    /**
     * This function checks to see if the tetromino has reached the bottom
     *
     * @return
     */
    private boolean atBottom ( int num )
    {
        //If user is trying to lower the tetromino faster, check that incrementing tetromino won't go below bottom
        if ( num == 1 )
        {
            //Get smallest distance from tetromino
            double distanceFromBottom = Integer.MAX_VALUE;
            for (Pieces pieces : current.getPieces ())
            {
                double currentDistance = gameHeight - Main.PIECESIZE;
                if ( pieces.y < currentDistance )
                {
                    if ( currentDistance - pieces.y < distanceFromBottom )
                    {
                        distanceFromBottom = currentDistance - pieces.y;
                    }
                }
                else
                {
                    System.out.println ( "At bottom from user input" );
                    return true;
                }
            }
            /*If the distanceFromBottom of bottommost piece of tetromino is less than increment size, then return true
             * and let the game loop finish the rest of the descent*/
            return distanceFromBottom < (1.5 * Main.PIECESIZE);
        }
        else
        {

            for (Pieces pieces : current.getPieces ())
            {       //For all pieces in tetromino, check y coords
                if ( pieces.y >= gameHeight - Main.PIECESIZE )
                {   //If any of the pieces y coords are greater than or equal to height, then reached bottom
                    return true;
                }
            }
            //Current tetromino isn't touching bottom
            return false;
        }
    }

    private boolean atTop()
    {
        for(int i = numRows - 1; i > numRows - 4; i--)
        {       //If any cells in grid at or above row 20, gameOver
            Integer[] row = grid.get(i);
            for(int j = 0; j < numCols; j++)
            {
                if(row[j] != 0)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This function checks to see if the tetromino has reached the left wall
     *
     * @return
     */
    private boolean atLeftSide ()
    {
        for (Pieces pieces : current.getPieces ())
        {       //For all pieces in tetromino, check x coords
            if ( pieces.x <= 0 )
            {   //If any of the pieces x coords are less than or equal to width, then reached left side
                return true;
            }
        }
        //Current tetromino isn't touching any sides
        return false;
    }


    /**
     * This function checks to see if the tetromino has reached the right wall
     *
     * @return
     */
    private boolean atRightSide ()
    {
        for (Pieces pieces : current.getPieces ())
        {       //For all pieces in tetromino, check x coords
            if ( pieces.x >= gameWidth - Main.PIECESIZE )
            {   //If any of the pieces x coords are greater than or equal to width, then reached right side
                return true;
            }
        }
        //Current tetromino isn't touching any sides
        return false;
    }

    /**
     * This function draws any pieces that are in the grid
     */
    private void drawGrid ()
    {
        for (int i = 0; i < numRows; i++)
        {
            Integer[] row = grid.get ( i );
            for (int j = 0; j < numCols; j++)
            {
                if ( row[j] == 0 )
                {
                    continue;
                }

                setGraphicsGameColor ( row[j] );
                graphicsGame.fillRect ( j * Main.PIECESIZE, (numRows - 1 - i) * Main.PIECESIZE, Main.PIECESIZE, Main.PIECESIZE );
                graphicsGame.strokeRect ( j * Main.PIECESIZE, (numRows - 1 - i) * Main.PIECESIZE, Main.PIECESIZE, Main.PIECESIZE );
            }
        }
    }

    /**
     * This function draws the tetromino
     *
     * @param tetromino
     */

    private void drawTetromino ( Tetromino tetromino )
    {
        setGraphicsGameColor ( tetromino.getType () );
        for (Pieces piece : tetromino.getPieces ())
        {
            graphicsGame.fillRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
            graphicsGame.strokeRect ( piece.x, piece.y, Main.PIECESIZE, Main.PIECESIZE );
        }
    }


    /**
     * This function sets the fill color of the tetromino
     *
     * @param tetrominoType
     */
    private void setGraphicsGameColor ( int tetrominoType )
    {
        switch (tetrominoType)
        {
            case Tetromino.IBLOCK:
                graphicsGame.setFill ( Color.LIGHTBLUE );
                break;
            case Tetromino.JBLOCK:
                graphicsGame.setFill ( Color.BLUE );
                break;
            case Tetromino.LBLOCK:
                graphicsGame.setFill ( Color.ORANGE );
                break;
            case Tetromino.OBLOCK:
                graphicsGame.setFill ( Color.YELLOW );
                break;
            case Tetromino.SBLOCK:
                graphicsGame.setFill ( Color.GREEN );
                break;
            case Tetromino.ZBLOCK:
                graphicsGame.setFill ( Color.RED );
                break;
            case Tetromino.TBLOCK:
                graphicsGame.setFill ( Color.MAGENTA );
                break;
            default:
                System.out.println ("Invalid tetromino type");
        }
        graphicsGame.setStroke ( Color.BLACK );
    }

    private void setGraphicsHoldColor ( int tetrominoType )
    {
        switch (tetrominoType)
        {
            case Tetromino.IBLOCK:
                graphicsHold.setFill ( Color.LIGHTBLUE );
                break;
            case Tetromino.JBLOCK:
                graphicsHold.setFill ( Color.BLUE );
                break;
            case Tetromino.LBLOCK:
                graphicsHold.setFill ( Color.ORANGE );
                break;
            case Tetromino.OBLOCK:
                graphicsHold.setFill ( Color.YELLOW );
                break;
            case Tetromino.SBLOCK:
                graphicsHold.setFill ( Color.GREEN );
                break;
            case Tetromino.ZBLOCK:
                graphicsHold.setFill ( Color.RED );
                break;
            case Tetromino.TBLOCK:
                graphicsHold.setFill ( Color.MAGENTA );
                break;
            default:
                System.out.println ("Invalid tetromino type");
        }
        graphicsHold.setStroke ( Color.BLACK );
    }

    private void setGraphicsNextColor ( int tetrominoType )
    {
        switch (tetrominoType)
        {
            case Tetromino.IBLOCK:
                graphicsNext.setFill ( Color.LIGHTBLUE );
                break;
            case Tetromino.JBLOCK:
                graphicsNext.setFill ( Color.BLUE );
                break;
            case Tetromino.LBLOCK:
                graphicsNext.setFill ( Color.ORANGE );
                break;
            case Tetromino.OBLOCK:
                graphicsNext.setFill ( Color.YELLOW );
                break;
            case Tetromino.SBLOCK:
                graphicsNext.setFill ( Color.GREEN );
                break;
            case Tetromino.ZBLOCK:
                graphicsNext.setFill ( Color.RED );
                break;
            case Tetromino.TBLOCK:
                graphicsNext.setFill ( Color.MAGENTA );
                break;
            default:
                System.out.println ("Invalid tetromino type");
        }
        graphicsNext.setStroke ( Color.BLACK );
    }

    public void sendStage ( Stage stage )
    {
        this.stage = stage;
        //Add keylistener to canvas

        stage.getScene ().setOnKeyPressed ( keyEvent -> {
            if(running)
            {
                switch (keyEvent.getCode ())
                {
                    case UP:
                    case X:
                        if ( checkRotate ( true ) )
                        {
                            rotateTetrominoRight ();
                        }
                        break;
                    case DOWN:
                        if ( !atBottom ( 1 ) || !hitGrid ( 0 ) )
                        {
                            current.incrementY ( Main.PIECESIZE );
                        }
                        break;
                    case LEFT:
                        if ( !atLeftSide () && !hitGrid ( 1 ) )
                        {
                            current.decrementX ( Main.PIECESIZE );
                        }
                        break;
                    case RIGHT:
                        if ( !atRightSide () && !hitGrid ( 2 ) )
                        {
                            current.incrementX ( Main.PIECESIZE );
                        }
                        break;
                    case Z:
                        if ( checkRotate ( false ) )
                        {
                            rotateTetrominoLeft ();
                        }
                        break;
                    case SHIFT:
                        hardDropTetromino ();
                        break;
                    case SPACE:
                        if ( !usedHold )
                        {
                            usedHold = true;
                            holdTetromino ();
                        }
                        break;
                    default:
                        System.out.println ( "No key pressed" );
                }
            }
        } );
    }


    public void sendBotMove (boolean hold, int orientation, double leftMostCoordinate)
    {
        //Set botMove
        botMoveHold = hold;
        botMoveOrientation = orientation;

        botMoveLeftMostPosition = leftMostCoordinate;


        //Signal to JavaFX Application Thread to move, give semaphore the number of permits it will need to make each
        // move
        botMoveAvailable.release();
    }
}
