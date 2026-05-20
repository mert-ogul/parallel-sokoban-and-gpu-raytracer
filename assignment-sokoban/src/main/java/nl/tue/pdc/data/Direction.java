package nl.tue.pdc.data;

public enum Direction {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    final int dx;
    final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public char toChar() {
        return switch (this) {
            case UP -> 'u';
            case DOWN -> 'd';
            case LEFT -> 'l';
            case RIGHT -> 'r';
        };
    }

    public String toString() {
        return String.valueOf(toChar());
    }

    public Direction fromChar(char d) {
        return switch (d) {
            case 'u' -> UP;
            case 'd' -> DOWN;
            case 'l' -> LEFT;
            case 'r' -> RIGHT;
            default -> throw new IllegalArgumentException("Unexpected direction character: " + d);
        };
    }
}
