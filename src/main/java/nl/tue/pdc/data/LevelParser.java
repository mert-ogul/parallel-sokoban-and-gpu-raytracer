package nl.tue.pdc.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LevelParser {

    public record ParseResult(Arena arena, GameState state) {}

    public static ParseResult parse(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));
        final int height = lines.size();
        final int width = lines.stream().mapToInt(String::length).max().orElse(0);

        Arena.Item[] fields = new Arena.Item[height * width];
        Arrays.fill(fields, Arena.Item.EMPTY);
        List<Point> crates = new ArrayList<>();
        Point player = null;

        for (int y = 0; y < height; y++) {
            String line = lines.get(y);
            for (int x = 0; x < line.length(); x++) {
                char c = line.charAt(x);
                int index = x + y * width;

                // Process character using Java 21 switch syntax
                switch (c) {
                    case '#' -> fields[index] = Arena.Item.WALL;
                    case '.' -> fields[index] = Arena.Item.GOAL;
                    case '$' -> crates.add(new Point(x, y));
                    case '@' -> player = new Point(x, y);
                    case '*' -> { // Crate on goal
                        crates.add(new Point(x, y));
                        fields[index] = Arena.Item.GOAL;
                    }
                    case '+' -> { // Player on goal
                        player = new Point(x, y);
                        fields[index] = Arena.Item.GOAL;
                    }
                    case ' ' -> {} // Empty space
                    default -> throw new RuntimeException("Found invalid character " + c + " on at position " + y + ":" + x);
                }
            }
        }

        if (player == null) {
            throw new IllegalStateException("No player (@) found in the level file.");
        }

        if (crates.size() != Arrays.stream(fields).filter(f -> f == Arena.Item.GOAL).count()) {
            throw new IllegalArgumentException("Number of crates is different from the number of goals.");
        }

        return new ParseResult(
                new Arena(width, height, fields),
                new GameState(player, crates.toArray(new Point[0]), null, null)
        );
    }
}
