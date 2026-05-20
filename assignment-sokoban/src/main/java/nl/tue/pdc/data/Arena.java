package nl.tue.pdc.data;

import java.util.Arrays;

/**
 * Represents the static layout of the level.
 */
public record Arena(int width, int height, Item[] fields) {

    public enum Item {
        EMPTY, WALL, GOAL
    }

    public Arena {
        if (fields.length != width * height) {
            throw new IllegalArgumentException("Size of fields array (" + fields.length + ") is inconsistent with width " + width + " and height " + height);
        }
    }

    public Item getField(int x, int y) {
        return fields[x + y * width];
    }

    /**
     * Renders the arena combined with a specific game state into the standard text format.
     */
    public String toString(GameState st, boolean newlines) {
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < height; y++) {
            final int yFinal = y;
            for (int x = 0; x < width; x++) {
                final int xFinal = x;
                Item item = getField(x, y);
                boolean isPlayer = (st.player().x() == x && st.player().y() == y);
                boolean isCrate = Arrays.stream(st.crates())
                        .anyMatch(p -> p.x() == xFinal && p.y() == yFinal);

                char c = switch (item) {
                    case WALL -> '#';
                    case EMPTY -> {
                        if (isPlayer) yield '@';
                        if (isCrate) yield '$';
                        yield ' ';
                    }
                    case GOAL -> {
                        if (isPlayer) yield '+'; // Player on goal
                        if (isCrate) yield '*';  // Crate on goal
                        yield '.';
                    }
                };
                sb.append(c);
            }
            if (y < height - 1 && newlines) sb.append("\n");
        }
        return sb.toString();
    }

}
