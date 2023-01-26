import java.util.Objects;


public class Point {

    public int x,y;

    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString(){
        return "x=" + x + " y=" + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public Point clone(){
        return new Point(x,y);
    }

    public static int manhattanDistance(Point one, Point two) {
        return Math.abs(one.x - two.x) + Math.abs(one.y - two.y);
    }


}