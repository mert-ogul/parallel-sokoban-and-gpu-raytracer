package nl.tue.pdc;

import nl.tue.pdc.concurrent.ConcurrentHashSet;
import nl.tue.pdc.concurrent.UnsafeHashSet;
import nl.tue.pdc.data.Arena;
import nl.tue.pdc.data.Direction;
import nl.tue.pdc.data.GameState;

import java.util.*;

public class SequentialSokobanSolver extends SokobanSolver {

    public SequentialSokobanSolver(Arena arena, GameState initialState) {
        super(arena, initialState, new ConcurrentHashSet<>(30_000_000));
    }

    public void solve() {
        Deque<GameState> todo = new ArrayDeque<>();

        todo.add(initialState);
        visited.add(initialState);

        while (!todo.isEmpty()) {
            // By taking the first element of the deque, we use it as a FIFO queue
            // Thus we implement a BFS search order, and we are guaranteed to find a shortest solution
            GameState current = todo.pollFirst();

            if (solution == null & isSolved(current)) {
                solution = current.getLurdString();
            }

            for (Direction d: Direction.values()) {
                GameState next = nextState(current, d);
                if (next != null && visited.add(next)) {
                    todo.add(next);
                }
            }
        }

        numStates = visited.size();
    }
}
