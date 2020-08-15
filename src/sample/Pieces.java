package sample;

public class Pieces {
    public double x, y;

    public Pieces(double x, double y) {

        this.x = x * Main.PIECESIZE;
        this.y = y * Main.PIECESIZE;
    }

    public void movePiece(double x, double y)
    {
        this.x = x;
        this.y = y;
    }


    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

}
