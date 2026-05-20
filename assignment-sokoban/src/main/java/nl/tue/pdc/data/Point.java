package nl.tue.pdc.data;

public record Point(int x, int y) implements Comparable<Point> {

    @Override
    public int compareTo(Point other) {
        int dx = x - other.x;
        int dy = y - other.y;
        return dx == 0 ? dy : dx;
    }

    public Point applyDirection(Direction d) {
        return new Point(x + d.dx, y + d.dy);
    }
}
