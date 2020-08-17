package sample;

import java.util.ArrayList;
import java.util.List;

public class Tetromino {

    public static final int IBLOCK = 1;
    public static final int JBLOCK = 2;
    public static final int LBLOCK = 3;
    public static final int OBLOCK = 4;
    public static final int SBLOCK = 5;
    public static final int ZBLOCK = 6;
    public static final int TBLOCK = 7;

    //Allocate memory for tetromino, every tetromino contains 4 pieces
    Pieces[] pieces = new Pieces[4];


    //Position of piece
    protected double x;
    protected double y;


    //Orientation variables
    public static final int FACINGUP = 0;
    public static final int FACINGRIGHT = 1;
    public static final int FACINGDOWN = 2;
    public static final int FACINGLEFT = 3;
    public int orientation = FACINGUP;

    private int tetrominoType = -1;

    public Tetromino(int tetrominoType, double x, double y) {
        this.x = x * Main.PIECESIZE;
        this.y = y * Main.PIECESIZE;
        this.tetrominoType = tetrominoType;
        //Get tetrominoType and fill in array of pieces to represent tetromino
        switch (tetrominoType)
        {
            case IBLOCK:
                pieces[0] = new Pieces(x - 1, y);
                pieces[1] = new Pieces(x, y);
                pieces[2] = new Pieces(x + 1, y);
                pieces[3] = new Pieces(x + 2, y);
                break;
            case  JBLOCK:
                pieces[0] = new Pieces(x - 1, y);
                pieces[1] = new Pieces(x, y);
                pieces[2] = new Pieces(x + 1, y);
                pieces[3] = new Pieces(x + 1, y - 1);
                break;
            case LBLOCK:
                pieces[0] = new Pieces(x - 1, y - 1);
                pieces[1] = new Pieces(x - 1, y);
                pieces[2] = new Pieces(x, y);
                pieces[3] = new Pieces(x + 1, y);
                break;
            case OBLOCK:
                pieces[0] = new Pieces(x - 1, y - 1);
                pieces[1] = new Pieces(x, y - 1);
                pieces[2] = new Pieces(x - 1, y);
                pieces[3] = new Pieces(x, y);
                break;
            case SBLOCK:
                pieces[0] = new Pieces(x - 1, y);
                pieces[1] = new Pieces(x, y);
                pieces[2] = new Pieces(x, y - 1);
                pieces[3] = new Pieces(x + 1, y - 1);
                break;
            case ZBLOCK:
                pieces[0] = new Pieces(x - 1, y - 1);
                pieces[1] = new Pieces(x, y - 1);
                pieces[2] = new Pieces(x, y);
                pieces[3] = new Pieces(x + 1, y);
                break;
            case TBLOCK:
                pieces[0] = new Pieces(x - 1, y);
                pieces[1] = new Pieces(x, y);
                pieces[2] = new Pieces(x, y - 1);
                pieces[3] = new Pieces(x + 1, y);
                break;
            default:
                System.out.println ("CONSTRUCTOR ERROR - Invalid Tetromino Type: " + tetrominoType);
        }
    }

    public Tetromino(Tetromino tetromino)
    {

        this.x = tetromino.x;
        this.y = tetromino.y;
        this.orientation = FACINGUP;
        this.tetrominoType = tetromino.tetrominoType;
        //Get tetrominoType and fill in array of pieces to represent tetromino
        switch (tetromino.getType())
        {
            case IBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE));
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE));
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE) + 2, (tetromino.y / Main.PIECESIZE));
                break;
            case  JBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE));
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE));
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE) - 1);
                break;
            case LBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE) - 1);
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE));
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE));
                break;
            case OBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE) - 1);
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE) - 1);
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE));
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                break;
            case SBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE));
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE) - 1);
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE) - 1);
                break;
            case ZBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1, (tetromino.y / Main.PIECESIZE) - 1);
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE) - 1);
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE));
                break;
            case TBLOCK:
                pieces[0] = new Pieces((tetromino.x / Main.PIECESIZE) - 1,(tetromino.y / Main.PIECESIZE));
                pieces[1] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE));
                pieces[2] = new Pieces((tetromino.x / Main.PIECESIZE), (tetromino.y / Main.PIECESIZE) - 1);
                pieces[3] = new Pieces((tetromino.x / Main.PIECESIZE) + 1, (tetromino.y / Main.PIECESIZE));
                break;
            default:
                System.out.println ("COPY ERROR - Invalid Tetromino Type: " + tetrominoType);
                break;
        }
    }


    public double getLeftMostPosition()
    {
        double mostLeft = Integer.MAX_VALUE;

        for(Pieces piece : pieces)
        {
            if(piece.x < mostLeft)
            {
                mostLeft = piece.x;
            }
        }

        return  mostLeft / GameController.numCols;
    }

    public double getTopPosition()
    {
        double mostTop = Integer.MAX_VALUE;

        for(Pieces piece : pieces)
        {
            if(piece.y < mostTop)
            {
                mostTop = piece.y;
            }
        }

        return mostTop / GameController.numRows;
    }

    public void setOrientation(int orientation)
    {
        this.orientation = orientation;
        rotate();
    }

    public void incrementY(double increment)
    {
        y += increment;
        for(Pieces piece : pieces)
        {
            piece.y += increment;

        }
    }

    public void incrementX(double increment)
    {
        x += increment;
        for(Pieces piece : pieces)
        {
            piece.x += increment;

        }
    }

    public void decrementX(double decrement)
    {
        x -= decrement;
        for(Pieces piece : pieces)
        {
            piece.x -= decrement;
        }
    }

    public void rotateRight()
    {
        //Change orientation of piece
        orientation = (orientation + 1) % 4;
        rotate();
    }

    public void rotateLeft()
    {
        //Change orientation of piece
        orientation = (orientation + 3) % 4;
        rotate();
    }

    /**
     * General rotate function
     */
    private void rotate()
    {
        if(tetrominoType == OBLOCK)
        {
            return;
        }
        //Rotate piece to new orientation
        switch (tetrominoType)
        {
            case IBLOCK:
                rotateI ();
                break;
            case  JBLOCK:
                rotateJ ();
                break;
            case LBLOCK:
                rotateL ();
                break;
            case SBLOCK:
                rotateS();
                break;
            case ZBLOCK:
                rotateZ();
                break;
            case TBLOCK:
                rotateT();
                break;
            default:
                System.out.println ("ROTATE ERROR - Invalid Tetromino Type: " + tetrominoType);
        }
    }

    private void rotateI()
    {
        switch (orientation)
        {
            case FACINGUP:
                pieces[0].movePiece(x - Main.PIECESIZE, y);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x + Main.PIECESIZE, y);
                pieces[3].movePiece(x + (2 * Main.PIECESIZE), y);
                break;
            case FACINGRIGHT:
                pieces[0].movePiece(x + Main.PIECESIZE, y - (2 * Main.PIECESIZE));
                pieces[1].movePiece(x + Main.PIECESIZE, y - Main.PIECESIZE);
                pieces[2].movePiece(x + Main.PIECESIZE, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y + Main.PIECESIZE);
                break;
            case FACINGDOWN:
                pieces[0].movePiece(x + (2 * Main.PIECESIZE), y);
                pieces[1].movePiece (x + Main.PIECESIZE, y);
                pieces[2].movePiece (x, y);
                pieces[3].movePiece (x - Main.PIECESIZE, y);
                break;
            case FACINGLEFT:
                pieces[0].movePiece(x, y + Main.PIECESIZE);
                pieces[1].movePiece (x,  y );
                pieces[2].movePiece (x,  y - Main.PIECESIZE );
                pieces[3].movePiece (x,  y - (2 * Main.PIECESIZE) );
                break;
        }
    }

    private void rotateJ()
    {
        switch (orientation)
        {
            case FACINGUP:
                pieces[0].movePiece(x - Main.PIECESIZE, y);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x + Main.PIECESIZE, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y - Main.PIECESIZE);
                break;
            case FACINGRIGHT:
                pieces[0].movePiece(x, y - Main.PIECESIZE);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x, y + Main.PIECESIZE);
                pieces[3].movePiece(x + Main.PIECESIZE, y + Main.PIECESIZE);
                break;
            case FACINGDOWN:
                pieces[0].movePiece(x - Main.PIECESIZE, y + Main.PIECESIZE);
                pieces[1].movePiece (x - Main.PIECESIZE, y);
                pieces[2].movePiece (x, y);
                pieces[3].movePiece (x + Main.PIECESIZE, y);
                break;
            case FACINGLEFT:
                pieces[0].movePiece(x - Main.PIECESIZE, y - Main.PIECESIZE);
                pieces[1].movePiece (x,  y - Main.PIECESIZE );
                pieces[2].movePiece (x,  y);
                pieces[3].movePiece (x,  y + Main.PIECESIZE );
                break;
        }
    }

    private void rotateL()
    {
        switch (orientation)
        {
            case FACINGUP:
                pieces[0].movePiece(x - Main.PIECESIZE, y - Main.PIECESIZE);
                pieces[1].movePiece(x - Main.PIECESIZE, y);
                pieces[2].movePiece(x, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y);
                break;
            case FACINGRIGHT:
                pieces[0].movePiece(x + Main.PIECESIZE, y - Main.PIECESIZE);
                pieces[1].movePiece(x, y - Main.PIECESIZE);
                pieces[2].movePiece(x, y);
                pieces[3].movePiece(x, y + Main.PIECESIZE);
                break;
            case FACINGDOWN:
                pieces[0].movePiece(x + Main.PIECESIZE, y + Main.PIECESIZE);
                pieces[1].movePiece (x + Main.PIECESIZE, y);
                pieces[2].movePiece (x, y);
                pieces[3].movePiece (x - Main.PIECESIZE, y);
                break;
            case FACINGLEFT:
                pieces[0].movePiece(x - Main.PIECESIZE, y + Main.PIECESIZE);
                pieces[1].movePiece (x,  y + Main.PIECESIZE );
                pieces[2].movePiece (x,  y);
                pieces[3].movePiece (x,  y - Main.PIECESIZE );
                break;
        }
    }

    private void rotateS()
    {
        switch (orientation)
        {
            case FACINGUP:
                pieces[0].movePiece(x - Main.PIECESIZE, y);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x, y - Main.PIECESIZE);
                pieces[3].movePiece(x + Main.PIECESIZE, y - Main.PIECESIZE);
                break;
            case FACINGRIGHT:
                pieces[0].movePiece(x, y - Main.PIECESIZE);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x + Main.PIECESIZE, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y + Main.PIECESIZE);
                break;
            case FACINGDOWN:
                pieces[0].movePiece(x - Main.PIECESIZE, y + Main.PIECESIZE);
                pieces[1].movePiece (x, y + Main.PIECESIZE);
                pieces[2].movePiece (x, y);
                pieces[3].movePiece (x + Main.PIECESIZE, y);
                break;
            case FACINGLEFT:
                pieces[0].movePiece(x - Main.PIECESIZE, y - Main.PIECESIZE);
                pieces[1].movePiece (x - Main.PIECESIZE,  y );
                pieces[2].movePiece (x,  y);
                pieces[3].movePiece (x,  y + Main.PIECESIZE );
                break;
        }
    }

    private void rotateZ()
    {
        switch (orientation)
        {
            case FACINGUP:
                pieces[0].movePiece(x - Main.PIECESIZE, y - Main.PIECESIZE);
                pieces[1].movePiece(x, y - Main.PIECESIZE);
                pieces[2].movePiece(x, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y);
                break;
            case FACINGRIGHT:
                pieces[0].movePiece(x, y + Main.PIECESIZE);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x + Main.PIECESIZE, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y - Main.PIECESIZE);
                break;
            case FACINGDOWN:
                pieces[0].movePiece(x - Main.PIECESIZE, y);
                pieces[1].movePiece (x, y);
                pieces[2].movePiece (x, y + Main.PIECESIZE);
                pieces[3].movePiece (x + Main.PIECESIZE, y + Main.PIECESIZE);
                break;
            case FACINGLEFT:
                pieces[0].movePiece(x - Main.PIECESIZE, y + Main.PIECESIZE);
                pieces[1].movePiece (x - Main.PIECESIZE,  y );
                pieces[2].movePiece (x,  y);
                pieces[3].movePiece (x,  y - Main.PIECESIZE );
                break;
        }
    }

    private void rotateT()
    {
        switch (orientation)
        {
            case FACINGUP:
                pieces[0].movePiece(x - Main.PIECESIZE, y);
                pieces[1].movePiece(x, y - Main.PIECESIZE);
                pieces[2].movePiece(x, y);
                pieces[3].movePiece(x + Main.PIECESIZE, y);
                break;
            case FACINGRIGHT:
                pieces[0].movePiece(x, y - Main.PIECESIZE);
                pieces[1].movePiece(x, y);
                pieces[2].movePiece(x + Main.PIECESIZE, y);
                pieces[3].movePiece(x, y + Main.PIECESIZE);
                break;
            case FACINGDOWN:
                pieces[0].movePiece(x - Main.PIECESIZE, y);
                pieces[1].movePiece (x, y);
                pieces[2].movePiece (x, y + Main.PIECESIZE);
                pieces[3].movePiece (x + Main.PIECESIZE, y);
                break;
            case FACINGLEFT:
                pieces[0].movePiece(x, y - Main.PIECESIZE);
                pieces[1].movePiece (x - Main.PIECESIZE,  y );
                pieces[2].movePiece (x,  y);
                pieces[3].movePiece (x,  y + Main.PIECESIZE );
                break;
        }
    }

    public Pieces[] getPieces ()
    {
        return this.pieces;
    }

    public int getType()
    {
        return this.tetrominoType;
    }
}
