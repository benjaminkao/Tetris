package sample;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Bot extends Task<Void> {
    private Semaphore startTask = new Semaphore(1);
    private int[][] gridScreenshot = new int[GameController.numRows][GameController.numCols];
    private Tetromino hold;
    private Tetromino current;
    private Tetromino next;
    private boolean gameOver = false;

    private Point[] positionChanged = new Point[4];

    private Queue<Move> priorityQueue =
            new PriorityQueue<Move> ( (x, y) -> Integer.compare(y.score, x.score));

    public static final int[] EMPTY_ROW = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int[] rowsToDelete = new int[4];
    private boolean deleteRows = false;


    private final String RED = "\u001B[31m";
    private final String BLUE = "\u001B[34m";
    private final String NORMAL = "\u001B[0m";

    //Scoring biases
    private final int scoreAdjacentTiles = 100;             //Score for each adjacent piece
    private final int scoreAdjacentWalls = 100;          //Score for adjacent walls or bottom
    private final int scoreHeight = -200;                   //Score for additional height
    private final int scoreRowClear = 5000;                  //Score for each number of rows cleared
    private final int scoreNumHoles = -2000;                 //Score for each number of holes
    private final int scoreCliff = -100;                    //Score for each cliff, multiplied by how big cliff is
    private final int scoreHolesBelow = -500;               //Score for each hole below a move
    private final int scoreLower = -100;                    //Score for how low tetromino is placed

    private GameController gameController;

    /**
     * This function signals the semaphore that a new grid snapshot is available and the bot needs to start the task
     * of getting the best move
     */
    public void signalBot()
    {
        startTask.release();
    }

    /**
     * Updates gridScreenshot, used by GameController to send the most current snapshot of the tetris board
     * @param grid
     */
    public void setGridScreenshot( List<Integer[]> grid)
    {
        gridScreenshot = new int[GameController.numRows][GameController.numCols];
        for(int i = 0; i < GameController.numRows; i++)
        {
            Integer[] row = grid.get(GameController.numRows - 1 - i);
            for(int j = 0; j < GameController.numCols; j++)
            {
                gridScreenshot[i][j] = row[j];
            }
        }
    }

    public void setHoldTetromino(Tetromino hold)
    {
        if(hold == null)
        {
            this.hold = null;
        } else
        {
            this.hold = new Tetromino ( hold.getType (), GameController.numCols / 2, 3 );
        }
    }

    public void setCurrentTetromino(Tetromino current)
    {
        this.current = new Tetromino(current);
        //Increment current tetromino down so that when setTetrominoLeft is called, it is possible to hit a piece, if
        // not, bot will fail once pieces get too high on board due to ArrayIndexOutOfBoundsError
        this.current.incrementY ( 50 );
    }

    public void setNextTetromino(Tetromino next)
    {
        this.next = new Tetromino ( next );
    }


    public void sendController(GameController gameController)
    {
        this.gameController = gameController;
    }

    public void sendGameOverSignal()
    {
        this.gameOver = true;
    }

    @Override
    protected Void call () throws Exception
    {
        while(!gameOver)
        {       //Until gameOverSignal is received from GameController

            //Wait until startTask signal is received from GameController
            startTask.acquire();

            //Get best move
            Move bestMove = getBestDrop();

            //Set current tetromino to bestMove
            if(bestMove.tetromino != null)
            {
                this.current = new Tetromino ( bestMove.tetromino );
                this.current.setOrientation(bestMove.orientation);
//                addToGridScreenshot ();
            }
            Move finalBestMove = bestMove;

            Platform.runLater ( () -> {
                gameController.sendBotMove( finalBestMove.hold, finalBestMove.orientation,
                        (finalBestMove.tetromino != null ? this.current.getLeftMostPosition () : -1));
            } );

//            int height= getHeight();
//            int numHoles = 0, holesBelow = 0, cliffs = 0;
//            if(!bestMove.hold)
//            {
//                numHoles = getNumHoles ( height );
//
//                holesBelow = getHolesBelow ();
//                cliffs = getCliffs ( height );
//            }
//
//            for(int[] row : gridScreenshot)
//            {
//
//                for(int col : row)
//                {
//                    if(col < 0)
//                    {
//                        System.out.print (BLUE + col + ", " + NORMAL);
//                    }
//                    else if(col > 0 && col < 10)
//                    {
//                        System.out.print (RED + col + ", " + NORMAL);
//                    } else if(col > 10){
//                        System.out.print ( BLUE + col + ", " + NORMAL);
//                    } else {
//                        System.out.print (col + ", ");
//                    }
//                }
//                System.out.println ();
//            }
//            System.out.println ("Used Hold: " + bestMove.hold);
//            System.out.println ("Best Score: " + bestMove.score);
//
//            if(!bestMove.hold)
//            {
//                System.out.println ( "Adjacent Tiles:\t" + getAdjacentTiles () );
//                System.out.println ( "Adjacent Walls:\t" + getAdjacentWalls () );
//
//                System.out.println ("Best Coord: " + bestMove.tetromino.getLeftMostPosition ());
//                System.out.println ("Best Orientation: " + bestMove.orientation);
//                System.out.println ("Best Height: " + height);
//
//                System.out.println ( "Best numHoles: " + numHoles );
//                System.out.println ( "Best belowHoles: " + holesBelow );
//                System.out.println ( "Best Cliffs: " + cliffs );
//            }
//
//            System.out.println ("-----------------------------------------------------------------------------");
//            if(!bestMove.hold)
//            {
//                //Reset grid screenshot
//                resetGridScreenshot ();
//            }

            //Reset priorityQueue for new move
            priorityQueue.clear();
        }
        return null;
    }

    @Override
    protected void failed() {
        Throwable throwable = this.getException ();
        throwable.printStackTrace ();
    }


    /**
     * This function iterates through every possible orientation, move, position of the current tetromino; gets the
     * score of each of the resulting boards and returns the best scoring board
     * @return
     */
    private Move getBestDrop()
    {
        boolean tryHold = false;
        //Increment row to skip all empty rows
        for(int i = 0; i < 2; i++)
        {       //Need to run through twice if hold piece exists
            if(i == 1)
            {       //Second run through
                tryHold = true;

                if(hold == null)
                {   //No hold piece, so add move with score 0 to priorityQueue
                    priorityQueue.add(new Move(tryHold, -1, null, 0));
                    return priorityQueue.poll();
                }
                this.current = new Tetromino(hold);
            }

            for (int orientation = 0; orientation < 4; orientation++)
            {       //For every tetromino orientation
                //Reset rowsToDelete array
                rowsToDelete = new int[4];
                deleteRows = false;

                //Set orientation of tetromino
                this.current.setOrientation ( orientation );

                //Move tetromino to gridScreenshot column 0
                setTetrominoToLeftWall ();
                while (!atRightWall ())
                {       //While tetromino hasn't spilled over right side of grid
                    //Drop tetromino
                    drop ();

                    //Get board score
                    int score = getBoardScore ();


                    //Add move to priorityQueue
                    priorityQueue.add ( new Move (tryHold, orientation, this.current, score ) );


                    //Reset gridScreenshot
                    resetGridScreenshot ();

                    //Move piece over one
                    this.current.incrementX ( Main.PIECESIZE );

                    //Move piece back to top
                    setTetrominoToTop ();
                }
            }
        }
        return priorityQueue.poll();
    }

    /**
     * This function hard drops the current tetromino into gridScreenshot so that bot can check the move in relation
     * to the board
     */
    private void drop()
    {
        while(!atBottom() && !hitGrid(false))
        {
            this.current.incrementY ( Main.PIECESIZE );
        }

        addToGridScreenshot ();
    }

    /**
     * This function is used to move the current tetromino to the left wall, this is used when getting the best move
     * because need to start getting moves at left wall then incrementing right and repeating for every orientation
     * of the current tetromino
     */
    private void setTetrominoToLeftWall()
    {
        if(atRightWall())
        {
            this.current.decrementX ( Main.PIECESIZE );
        }
        while(!atLeftWall() && !hitGrid(true))
        {
            this.current.decrementX ( Main.PIECESIZE );
        }
        if(this.current.getLeftMostPosition () < 0)
        {
            System.out.println (this.current.orientation);
            System.out.println (this.current.getLeftMostPosition ());
            this.current.incrementX(Main.PIECESIZE);
            System.out.println (this.current.getLeftMostPosition ());

        }
    }

    /**
     * This function moves the current tetromino back up to the top of the grid board, used to reset current
     * tetromino when iterating through all possible moves
     */
    private void setTetrominoToTop()
    {
        while (this.current.getTopPosition () > 4)
        {
            this.current.incrementY ( -Main.PIECESIZE );
        }
    }

    /**
     * This function checks to see if current tetromino has hit the bottom of the grid board
     * @return
     */
    private boolean atBottom()
    {
        for(Pieces piece : this.current.getPieces ())
        {
            if(piece.y >= GameController.gameHeight - Main.PIECESIZE)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * This function checks to make sure that the current tetromino doesn't hit any pieces in gridScreenshot if
     * current tetromino is moved left, right or down
     * @param left
     * @return
     */
    private boolean hitGrid(boolean left)
    {
        if(!left)
        {   //Checking for drop() function
            //Increment tetromino
            this.current.incrementY ( Main.PIECESIZE );
        }
        else
        {   //Checking for setLeftToWall() function, need to check if hits any pieces
            this.current.decrementX ( Main.PIECESIZE );
        }

        for(Pieces piece : this.current.getPieces())
        {
            int gridRow = (int) piece.y / Main.PIECESIZE;
            int gridCol = (int) piece.x / Main.PIECESIZE;

            if(gridRow < 0)
            {
                return false;
            }

            if(gridScreenshot[gridRow][gridCol] > 0)
            {       //Hit piece
                this.current.incrementY(-Main.PIECESIZE);
                return true;
            }
        }


        //Decrement tetromino back to original position
        if(!left)
        {   //Checking for drop() function
            //Increment tetromino
            this.current.incrementY ( -Main.PIECESIZE );
        }
        else
        {   //Checking for setLeftToWall() function, need to check if hits any pieces
            this.current.incrementX ( Main.PIECESIZE );
        }
        return false;
    }

    /**
     * This function checks to see if current tetromino is hitting the right wall
     * @return
     */
    private boolean atRightWall()
    {
      for(Pieces pieces : this.current.getPieces ())
      {
          if(pieces.x >= GameController.gameWidth)
          {
              return true;
          }
      }

      return false;
    }

    /**
     * This function checks to see if current tetromino is hitting the left wall
     * @return
     */
    private boolean atLeftWall()
    {
        this.current.decrementX ( Main.PIECESIZE );

        for(Pieces pieces : this.current.getPieces ())
        {
            if(pieces.x < 0)
            {
                this.current.incrementX ( Main.PIECESIZE );
                return true;
            }
        }
        this.current.incrementX ( Main.PIECESIZE );
        return false;
    }

    /**
     * This function gets the total score of the current board which is used for sorting and getting the relatively
     * best move possible
     * @return
     */
    private int getBoardScore()
    {
        int score = 0;

        //Get scoreHeight
        int height = getHeight();
        int scoreHeight = height * this.scoreHeight;

        //Get scoreRowClear
        int scoreRowClear = getRowsCleared(height) * this.scoreRowClear;

        //Get scoreNumHoles
        int scoreNumHoles = getNumHoles (height) * this.scoreNumHoles;
        int scoreHolesBelow = getHolesBelow() * this.scoreHolesBelow;

        int scoreAdjacentTiles = getAdjacentTiles () * this.scoreAdjacentTiles;
        int scoreAdjacentWalls = getAdjacentWalls () * this.scoreAdjacentWalls;

        //Multiply by e ^ (height / 2) so that it really discourages placing pieces too high
        int scoreCliffs = getCliffs(height) * this.scoreCliff * (int) Math.exp(height / 2);

        //Get highest piece from placed tetromino
        int scoreLower = (GameController.numRows - (int) this.current.getTopPosition ()) * this.scoreLower;



        score =
                scoreHeight + scoreNumHoles + scoreRowClear + scoreAdjacentTiles + scoreAdjacentWalls + scoreCliffs + scoreHolesBelow + scoreLower;
        return score;
    }


    /**
     * This function gets the number of holes that the current gridScreenshot contains
     *
     * A hole is defined as a cell that has a ceiling above it or the cells to the left or right have a ceiling
     * @param height
     * @return
     */
    private int getNumHoles(int height)
    {
        if(height == 0)
        {
            return 0;
        }
        //Use height to find row where grid isn't empty
        int row = GameController.numRows - height - 1, numHoles = 0;
        if(row < 0)
        {
            return GameController.numRows;
        }

        while(row < GameController.numRows)
        {
            //Instead of deleting the rows physically in memory, just pretend they are deleted and increment over them
            if(deleteRows)
            {
                while(row < GameController.numRows && checkForDeletedRow ( row ))
                {
                    row++;
                }
            }
            for(int col = 0; col < GameController.numCols; col++)
            {
                if(gridScreenshot[row][col] <= 0)
                {       //If position in grid equals zero, then gap has started

                    /*Hole is found, but also consider the height of the hole, if hole is higher that is bad. If hole
                     is at wall, that is much better than having a hole in the middle*/

                    if(hasCeiling(row, col, height))
                    {       //If empty cell has a ceiling, then it is a hole
                        gridScreenshot[row][col] = -1;
                        numHoles+=(GameController.numRows - row);

                        //This encourages the bot to keep the holes towards the walls instead of the middle
                        if(col <= GameController.numCols / 2)
                        {
                            numHoles+=col;
                        } else {
                            numHoles+=GameController.numCols - col - 1;
                        }
                    } else
                    {       //Empty cell doesn't have ceiling
                        //Check if left cell isn't empty
                        if(col - 1 > 0 && gridScreenshot[row][col - 1] > 0)
                        {   //If cell to the left of empty cell is filled, check for ceiling
                            if(hasCeiling(row, col - 1, height))
                            {
                                gridScreenshot[row][col] = -1;
                                numHoles+=(GameController.numRows - row);
                                //This encourages the bot to keep the holes towards the walls instead of the middle
                                if(col <= GameController.numCols / 2)
                                {
                                    numHoles+=col;
                                } else {
                                    numHoles+=GameController.numCols - col - 1;
                                }
                            }
                        } else if(col + 1 < GameController.numCols && gridScreenshot[row][col + 1] > 0)
                        {   //If cell to the right of empty cell is filled, check for ceiling
                            if(hasCeiling(row, col + 1, height))
                            {
                                gridScreenshot[row][col] = -1;
                                numHoles+=(GameController.numRows - row);
                                //This encourages the bot to keep the holes towards the walls instead of the middle
                                if(col <= GameController.numCols / 2)
                                {
                                    numHoles+=col;
                                } else {
                                    numHoles+=GameController.numCols - col - 1;
                                }
                            }
                        }

                    }

                }
            }
            row++;
        }
        return numHoles;
    }

    /**
     * This function checks to see if there are any holes below any of the pieces in the current tetromino that were
     * placed
     * @return
     */
    private int getHolesBelow()
    {
        int numHolesBelow = 0;
        for(Pieces pieces: this.current.getPieces ())
        {
            int gridRow = (int) pieces.y / Main.PIECESIZE;
            int gridCol = (int) pieces.x / Main.PIECESIZE;


            for(int row = gridRow; row < GameController.numRows; row++)
            {
                if(deleteRows)
                {
                    while(row < GameController.numRows && checkForDeletedRow ( row ))
                    {
                        row++;
                    }
                }
                if(gridScreenshot[row][gridCol] == -1)
                {
                    /*Increment numHolesBelow by 100 divided by the distance between the piece last placed and the
                    hole it is above. This discourages placement of pieces over holes that are right below.

                    Why 100? I just tried 100 and it worked out really well
                    */
                    gridScreenshot[row][gridCol] = -2;
                    numHolesBelow += 100 / (row - gridRow);
                }
            }
        }
        return numHolesBelow;
    }


    /**
     * This function returns the number of rows that will be cleared by the main JavaFX Application Thread after
     * the tetromino is placed
     * @param height
     * @return
     */
    private int getRowsCleared(int height)
    {

        //Only need to check rows that aren't empty
        int row = (int) GameController.numRows - 1 - height, rowsCleared = 0;
        if(row < 0)
        {
            return 0;
        }
        boolean filled = true;
        while(row < GameController.numRows)
        {
            for(int i = 0; i < GameController.numCols; i++)
            {
                if(gridScreenshot[row][i] == 0)
                {       //Row is not filled
                    filled = false;
                    break;
                }
            }

            if(filled)
            {
                deleteRows = true;
                rowsToDelete[rowsCleared] = row;
                rowsCleared++;
            }
            row++;
        }

        //If 3 or more rows are cleared, increment by 10 to tell bot that it is a lot better than just clearing a couple

        if ( rowsCleared == 3 )
        {
            rowsCleared += 5;
        }
        else if ( rowsCleared == 4 )
        {
            rowsCleared += 10;
        }


        return rowsCleared;
    }


    /**
     * This function gets the number of adjacent tiles that the placed tetromino has
     * @return
     */
    private int getAdjacentTiles()
    {
        //positionChanged should hold the indexes of gridScreenshot where piece was placed
        int adjacentTiles = 0;
        boolean left = false, right = false, down = false;
        for(int i = 0; i < 4; i++)
        {
            Point currentPoint = positionChanged[i];
            boolean same = false;
            if(currentPoint.y - 1 >= 0 && gridScreenshot[currentPoint.x][currentPoint.y - 1] != 0)
            {   //Piece exists to the left
                //Check if piece to the left is part of the same tetromino
                for(Point point : positionChanged)
                {
                    if(currentPoint.x == point.x && currentPoint.y - 1 == point.y)
                    {       //Not same tetromino
                        same = true;
                    }
                }

                if(!same)
                {
                    adjacentTiles++;
                    left = true;
                }
            }
            if(currentPoint.y + 1 < GameController.numCols - 1 && gridScreenshot[currentPoint.x][currentPoint.y + 1] != 0)
            {   //Piece exists to the right
                //Check if piece to the right is part of the same tetromino
                for(Point point : positionChanged)
                {
                    if(currentPoint.x == point.x && currentPoint.y + 1 == point.y)
                    {       //Not same tetromino
                        same = true;
                    }
                }

                if(!same)
                {
                    adjacentTiles++;
                    right = true;
                }
            }
            if(currentPoint.x + 1 < GameController.numRows - 1 && gridScreenshot[currentPoint.x + 1][currentPoint.y] != 0)
            {       //Piece exists below
                //CHeck if piece below is part of the same tetromino
                for(Point point : positionChanged)
                {
                    if(currentPoint.x + 1 == point.x && currentPoint.y == point.y)
                    {       //Not same tetromino
                        same = true;
                    }
                }

                if(!same)
                {
                    adjacentTiles++;
                    down = true;
                }
            }
        }

        if(left && right && down)
        {       //If tile locked, count as more
            adjacentTiles += 3;
        }

        return adjacentTiles;
    }


    /**
     * This function gets the number of adjacent wall pieces that the placed tetromino has
     * @return
     */
    private int getAdjacentWalls()
    {
        int adjacentWalls = 0;

        for(Point point : positionChanged)
        {
            if(point.y == 0)
            {   //At left wall
                adjacentWalls++;
            }
            if(point.y == GameController.numCols - 1)
            {
                adjacentWalls++;
            }
            if(point.x == GameController.numRows - 1)
            {   //At bottom
                adjacentWalls++;
            }
        }

        return adjacentWalls;
    }

    /**
     * This is a helper function for getNumHoles(), it checks that for a given cell, does it have a piece above
     * @param row
     * @param col
     * @param height
     * @return
     */
    private boolean hasCeiling(int row, int col, int height)
    {
        for(int i = row - 1; i > GameController.numRows - height - 1; i--)
        {       //For every cell at height or below and above current cell

            if(gridScreenshot[i][col] > 0)
            {   //Has ceiling
                return true;
            }
        }
        return false;
    }


    /**
     * This function returns the max height of the board after the tetromino is placed
     * @return
     */
    private int getHeight()
    {
        int num = 0;
        for(int i = 0; i < GameController.numRows; i++)
        {
            if( Arrays.equals(gridScreenshot[i], EMPTY_ROW))
            {
                num++;
            }
        }
        return GameController.numRows - num;
    }

    /**
     * This function checks for the number of cliffs and how steep each cliff is (for both sides)
     * This function uses the sliding window algorithm to check for cliffs
     * @param height - Since we know max height of board currently, only need to check rows below height to save time
     * @return
     */
    private int getCliffs(int height)
    {
        //Edge case, if height is 0, no cliffs possible
        if(height == 0)
        {
            return 0;
        }

        int previousHeight = 0;
        int currentHeight = 0;
        int nextHeight = 0;
        int numCliffs = 0;

        int row;
        int steepness;
        //Get currentHeight for column 0 first to save time, so no need to check currentHeight for every column
        for(row = GameController.numRows - height; row < GameController.numRows; row++)
        {
            if(gridScreenshot[row][0] > 0)
            {
                currentHeight = GameController.numRows - row;
                break;
            }
        }


        for(int col = 1; col < GameController.numCols; col++)
        {       //For each column, get height of column and compare with columns next to it

            for(row = GameController.numRows - height; row < GameController.numRows; row++)
            {   //Get height of column
                if(gridScreenshot[row][col] > 0)
                {       //Found piece
                    //Store nextHeight
                    nextHeight = GameController.numRows - row;
                    break;
                }
            }

            if(col > 1)
            {   //If not at second column, then previousHeight exits
                steepness = currentHeight - previousHeight - 1;
                if(steepness > 0)
                {       //If steepness is greater than 1, requires L, J, or I block to fill, so that is bad
                    numCliffs += steepness;

                    for(int i = GameController.numRows - previousHeight - 1; i < GameController.numRows - currentHeight - 1; i++)
                    {
                        gridScreenshot[i][col - 2] = -3;
                    }
                }
            }

            steepness = currentHeight - nextHeight - 1;
            if(steepness > 0)
            {
                numCliffs += steepness;
                for(int i = GameController.numRows - previousHeight - 1; i < GameController.numRows - currentHeight - 1; i++)
                {
                    gridScreenshot[i][col] = -3;
                }
            }


            //Slide window over
            previousHeight = currentHeight;
            currentHeight = nextHeight;
        }

        return numCliffs;
    }

    /**
     * This function adds the current tetromino into the gridScreenshot
     */
    private void addToGridScreenshot()
    {
        int i = 0;
        for(Pieces piece : this.current.getPieces ())
        {
            int gridRow = (int) piece.y / Main.PIECESIZE;
            int gridCol = (int) piece.x / Main.PIECESIZE;

            //Store positions of tetromino added to gridScreenshot
            positionChanged[i] = new Point(gridRow, gridCol);

            gridScreenshot[gridRow][gridCol] = this.current.getType();
            i++;
        }
    }

    //Resets gridScreenshot to grid that was sent by JavaFX Application Thread
    private void resetGridScreenshot()
    {
        for(Point point : positionChanged)
        {
            gridScreenshot[point.x][point.y] = 0;
        }

        for(int i = 0; i < GameController.numRows; i++)
        {
            while(i + 1< GameController.numRows && Arrays.equals(gridScreenshot[i], EMPTY_ROW))
            {
                i++;
            }

            for(int j = 0; j < GameController.numCols; j++)
            {
                if(gridScreenshot[i][j] < 0)
                {
                    gridScreenshot[i][j] = 0;
                }
            }
        }
    }

    private boolean checkForDeletedRow(int testRow)
    {
        for(int row : rowsToDelete)
        {
            if(testRow == row)
            {
                return true;
            }
        }
        return false;
    }


    /**
     * This Move class is used to store the important information about each move so it can be easily stored and sorted
     * in a priority queue
     */
    protected class Move {
        protected final int orientation, score;
        protected final boolean hold;
        protected Tetromino tetromino;

        protected Move(boolean hold, int orientation, Tetromino tetromino, int score)
        {
            this.hold = hold;
            this.orientation = orientation;
            if(tetromino != null)
            {
                this.tetromino = new Tetromino ( tetromino );
            } else {
                this.tetromino = null;
            }
            this.score = score;
        }
    }

}
