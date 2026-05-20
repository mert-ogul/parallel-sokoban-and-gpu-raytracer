package nl.tue.pdc;

import nl.tue.pdc.concurrent.WriteSet;
import nl.tue.pdc.data.*;

import java.util.*;

/**
 * A solver class for Sokoban puzzles.
 */
public abstract class SokobanSolver {

    private final Arena arena;
    protected final GameState initialState;
    protected final WriteSet<GameState> visited;

    protected volatile String solution = null;
    protected int numStates = 0;

    public SokobanSolver(Arena arena, GameState initialState, WriteSet<GameState> visited) {
        this.arena = arena;
        this.initialState = initialState;
        this.visited = visited;
    }

    /**
     * Solves the puzzle starting from the initial state.
     * returns a list of states representing the path to the solution, or empty if no solution.
     */
    public abstract void solve();

    /**
     * Obtain the solution after solving has completed as a lurd String.
     * @return The solution to the Sokoban puzzle the constructor, or null if none has been found (yet).
     */
    public String getSolution() {
        return solution;
    }

    /**
     * The number of game states found during solving.
     * @return The number of unique states observed.
     */
    public int getNumStates() {
        return numStates;
    }

    /**
     * Checks if all goals are covered by crates.
     */
    public boolean isSolved(GameState state) {
        return Arrays.stream(state.crates())
                .allMatch(p -> arena.getField(p.x(), p.y()) == Arena.Item.GOAL);
    }

    /**
     * Computes all possible valid states reachable in one move (move or push).
     */
    public List<GameState> nextStates(GameState state) {
        List<GameState> nextStates = new ArrayList<>();

        for (Direction d: Direction.values()) {
            GameState next = nextState(state, d);
            if (next != null) {
                nextStates.add(next);
            }
        }

        return nextStates;
    }

    public GameState nextState(GameState state, Direction d) {
        Point nextPos = state.player().applyDirection(d);
        int nx = nextPos.x(); int ny = nextPos.y();

        // 1. Check if the next position is a wall
        if (arena.getField(nx, ny) == Arena.Item.WALL) {
            return null;
        }

        // 2. Check if there is a crate at the next position
        int crateIndex = -1;
        for (int i = 0; i < state.crates().length; i++) {
            if (state.crates()[i].x() == nx && state.crates()[i].y() == ny) {
                crateIndex = i;
                break;
            }
        }

        if (crateIndex != -1) {
            // Try to push the crate
            Point crateNextPos = nextPos.applyDirection(d);
            int cnx = crateNextPos.x(); int cny = crateNextPos.y();

            // Crate can move if the target is not a wall and not another crate
            boolean isCrateAt = Arrays.stream(state.crates()).noneMatch(c -> c.equals(crateNextPos));
            if (arena.getField(cnx, cny) != Arena.Item.WALL && isCrateAt) {
                Point[] newCrates = state.crates().clone();
                newCrates[crateIndex] = crateNextPos;
                Arrays.sort(newCrates);
                return new GameState(nextPos, newCrates, state, d);
            }
        } else {
            // Just move the player to empty space/goal
            return new GameState(nextPos, state.crates(), state, d);
        }

        return null;
    }

}
