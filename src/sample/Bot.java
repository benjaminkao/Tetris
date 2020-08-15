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
    private Tetromino last;
    private Tetromino current;
    private Tetromino next;
    private boolean gameOver = false;

    private Point[] positionChanged = new Point[4];

    private Queue<Move> priorityQueue =
            new PriorityQueue<Move> ( (x, y) -> Integer.compare(y.score, x.score));

    public static final int[] EMPTY_ROW = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};


    private final String RED = "\u001B[31m";
    private final String BLUE = "\u001B[34m";
    private final String NORMAL = "\u001B[0m";

    //Scoring biases
    private final int scoreAdjacentTiles = 200;             //Score for each adjacent piece
    private final int scoreAdjacentWalls = 100;          //Score for adjacent walls or bottom
    private final int scoreHeight = -200;                   //Score for additional height
    private final int scoreRowClear = 5000;                  //Score for each number of rows cleared
    private final int scoreNumHoles = -2000;                 //Score for each number of holes
    private final int scoreCliff = -100;                    //Score for each cliff, multiplied by how big cliff is
    private final int scoreHolesBelow = -300;               //Score for each hole below a move

    private GameController gameController;

    public void signalBot()
    {
        startTask.release();
    }

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


    public void setCurrentTetromino(Tetromino current)
    {
        this.current = new Tetromino(current);
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
        {
            int i = 0;
            startTask.acquire();

//            drop();

            Move bestMove = getBestDrop();

            this.current = new Tetromino(bestMove.tetromino);
            this.current.setOrientation(bestMove.orientation);
            Move finalBestMove = bestMove;
            Platform.runLater ( () -> {
                gameController.sendBotMove( finalBestMove.orientation, this.current.getLeftMostPosition ());
            } );

            addToGridScreenshot ();
            int height= getHeight();
            int numHoles = getNumHoles ( height );
            int holesBelow = getHolesBelow ();

            for(int[] row : gridScreenshot)
            {

                for(int col : row)
                {
                    if(col < 0)
                    {
                        System.out.print (BLUE + col + ", " + NORMAL);
                    }
                    else if(col > 0 && col < 10)
                    {
                        System.out.print (RED + col + ", " + NORMAL);
                    } else if(col > 10){
                        System.out.print ( BLUE + col + ", " + NORMAL);
                    } else {
                        System.out.print (col + ", ");
                    }
                }
                System.out.println ();
            }

            System.out.println ( "Adjacent Tiles:\t" + getAdjacentTiles () );
            System.out.println ( "Adjacent Walls:\t" + getAdjacentWalls () );

            System.out.println ("Best Coord: " + bestMove.tetromino.getLeftMostPosition ());
            System.out.println ("Best Orientation: " + bestMove.orientation);
            System.out.println ("Best Score: " + bestMove.score);
            System.out.println ("Best Height: " + height);
            System.out.println ("Best numHoles: " + numHoles);
            System.out.println ("Best belowHoles: " + holesBelow);

            System.out.println ("-----------------------------------------------------------------------------");

            while(!priorityQueue.isEmpty())
            {
                bestMove = priorityQueue.poll ();
                this.current = new Tetromino(bestMove.tetromino);
                this.current.setOrientation(bestMove.orientation);

                addToGridScreenshot ();
                height= getHeight();
                numHoles = getNumHoles ( height );
                holesBelow = getHolesBelow ();

                for(int[] row : gridScreenshot)
                {

                    for(int col : row)
                    {
                        if(col < 0)
                        {
                            System.out.print (BLUE + col + ", " + NORMAL);
                        }
                        else if(col > 0 && col < 10)
                        {
                            System.out.print (RED + col + ", " + NORMAL);
                        } else if(col > 10){
                            System.out.print ( BLUE + col + ", " + NORMAL);
                        } else {
                            System.out.print (col + ", ");
                        }
                    }
                    System.out.println ();
                }

                System.out.println ( "Adjacent Tiles:\t" + getAdjacentTiles () );
                System.out.println ( "Adjacent Walls:\t" + getAdjacentWalls () );

                System.out.println ("Coord: " + bestMove.tetromino.getLeftMostPosition ());
                System.out.println ("Orientation: " + bestMove.orientation);
                System.out.println ("Score: " + bestMove.score);
                System.out.println ("Height: " + height);
                System.out.println ("numHoles: " + numHoles);
                System.out.println ("belowHoles: " + holesBelow);

                resetGridScreenshot ();
            }
            System.out.println ("-----------------------------------------------------------------------------");




            priorityQueue.clear();

            resetGridScreenshot ();
        }
        return null;
    }

    @Override
    protected void failed() {
        Throwable throwable = this.getException ();
        throwable.printStackTrace ();
    }


    private Move getBestDrop()
    {
        //Increment row to skip all empty rows

        for(int orientation = 0; orientation < 4; orientation++)
        {       //For every tetromino orientation
            //Set orientation of tetromino
            this.current.setOrientation(orientation);

            //Move tetromino to gridScreenshot column 0
            setTetrominoToLeftWall ();
            while(!atRightWall ())
            {       //While tetromino hasn't spilled over right side of grid
                //Drop tetromino
                drop();

                //Get board score
                int score = getBoardScore();


                //Add move to priorityQueue
                priorityQueue.add(new Move(orientation, this.current, score));



                //Reset gridScreenshot
                resetGridScreenshot ();

                //Move piece over one
                this.current.incrementX ( Main.PIECESIZE );

                //Move piece back to top
                setTetrominoToTop();
            }
        }
        return priorityQueue.poll();
    }

    private void drop()
    {


        while(!atBottom() && !hitGrid())
        {
            this.current.incrementY ( Main.PIECESIZE );
        }

        addToGridScreenshot ();
    }

    private void setTetrominoToLeftWall()
    {

        while(!atLeftWall())
        {

            this.current.decrementX ( Main.PIECESIZE );
        }
    }

    private void setTetrominoToTop()
    {
        while (this.current.getTopPosition () > 0)
        {
            this.current.incrementY ( -Main.PIECESIZE );
        }
    }


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

    private boolean hitGrid()
    {
        //Increment tetromino
        this.current.incrementY ( Main.PIECESIZE );

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
        this.current.incrementY(-Main.PIECESIZE);
        return false;
    }

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

    private boolean atLeftWall()
    {
        for(Pieces pieces : this.current.getPieces ())
        {
            if(pieces.x <= 0)
            {
                return true;
            }
        }
        return false;
    }


    private int getBoardScore()
    {
        int score = 0;

        //Get scoreHeight
        int height = getHeight();
        int scoreHeight = height * this.scoreHeight;

        //Get scoreNumHoles
        int scoreNumHoles = getNumHoles (height) * this.scoreNumHoles;
        int scoreHolesBelow = getHolesBelow() * this.scoreHolesBelow;

        //Get scoreRowClear
        int scoreRowClear = getRowsCleared(height) * this.scoreRowClear;

        int scoreAdjacentTiles = getAdjacentTiles () * this.scoreAdjacentTiles;
        int scoreAdjacentWalls = getAdjacentWalls () * this.scoreAdjacentWalls;
        int scoreCliffs = getCliffs(height) * this.scoreCliff;





        score =
                scoreHeight + scoreNumHoles + scoreRowClear + scoreAdjacentTiles + scoreAdjacentWalls + scoreCliffs + scoreHolesBelow;
        return score;
    }


    private int getNumHoles(int height)
    {
        if(height == 0)
        {
            return 0;
        }
        //Use height to find row where grid isn't empty
        int row = GameController.numRows - height - 1, numHoles = 0;


        while(row < GameController.numRows)
        {
            for(int col = 0; col < GameController.numCols; col++)
            {
                if(gridScreenshot[row][col] <= 0)
                {       //If position in grid equals zero, then gap has started

                    if(hasCeiling(row, col, height))
                    {       //If empty cell has a ceiling, then it is a hole
                        //Hole is found, but also consider the height of the hole, if hole is higher that is bad
                        gridScreenshot[row][col] = -1;
                        numHoles+=(GameController.numRows - row);
                    } else
                    {       //Empty cell doesn't have ceiling
                        //Check if left cell isn't empty
                        if(col - 1 > 0 && gridScreenshot[row][col - 1] > 0)
                        {   //If cell to the left of empty cell is filled, check for ceiling
                            if(hasCeiling(row, col - 1, height))
                            {
                                gridScreenshot[row][col] = -1;
                                numHoles+=(GameController.numRows - row);
                            }
                        } else if(col + 1 < GameController.numCols && gridScreenshot[row][col + 1] > 0)
                        {   //If cell to the right of empty cell is filled, check for ceiling
                            if(hasCeiling(row, col + 1, height))
                            {
                                gridScreenshot[row][col] = -1;
                                numHoles+=(GameController.numRows - row);
                            }
                        }

                    }

                }
            }
            row++;
        }
        return numHoles;
    }

    private int getHolesBelow()
    {
        int numHolesBelow = 0;
        for(Pieces pieces: this.current.getPieces ())
        {
            int gridRow = (int) pieces.y / Main.PIECESIZE;
            int gridCol = (int) pieces.x / Main.PIECESIZE;


            for(int row = gridRow; row < GameController.numRows; row++)
            {
                if(gridScreenshot[row][gridCol] == -1)
                {
                    //Increment numHolesBelow by the distance between the piece last placed and the hole it is above
                    //This discourages placement of pieces over holes that are lower down
                    gridScreenshot[row][gridCol] = -2;
                    numHolesBelow += GameController.numRows - row;
                }
            }
        }
        return numHolesBelow;
    }

    private int getRowsCleared(int height)
    {
        //Only need to check rows that aren't empty
        int row = (int) GameController.numRows - 1 - height, rowsCleared = 0;
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
                rowsCleared++;
            }
            row++;
        }

        return rowsCleared;
    }

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
        int previousHeight = 0;
        int currentHeight = 0;
        int nextHeight = 0;
        int numCliffs = 0;



        int row;
        int steepness;
        //Get currentHeight for column 0 first to save time, so no need to check currentHeight for every column
        for(row = GameController.numRows - height - 1; row >= 0; row--)
        {
            if(gridScreenshot[row][1] > 0)
            {
                currentHeight = GameController.numRows - row;
            }
        }

        for(int col = 1; col < GameController.numCols; col++)
        {       //For each column, get height of column and compare with columns next to it

            for(row = GameController.numRows - height - 1; row >= 0; row--)
            {   //Get height of column
                if(gridScreenshot[row][col] > 0)
                {       //Found piece
                    //Store nextHeight
                    nextHeight = GameController.numRows - row;
                    break;
                }
            }

            if(col != 1)
            {   //If not at second column, then previousHeight exits
                steepness = currentHeight - previousHeight - 1;
                if(steepness > 0)
                {       //If steepness is greater than 1, requires L, J, or I block to fill, so that is bad
                    numCliffs += steepness;
                }
            }

            steepness = currentHeight - nextHeight - 1;
            if(steepness > 0)
            {
                numCliffs += steepness;
            }


            //Slide window over
            previousHeight = currentHeight;
            currentHeight = nextHeight;
        }

        return numCliffs;
    }

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





    protected class Move {
        protected final int orientation, score;
        protected Tetromino tetromino;

        protected Move(int orientation, Tetromino tetromino, int score)
        {
            this.orientation = orientation;
            this.tetromino = new Tetromino(tetromino);
            this.score = score;
        }
    }

}
