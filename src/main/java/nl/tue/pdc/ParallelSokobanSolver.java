package nl.tue.pdc;

import nl.tue.pdc.concurrent.ConcurrentHashSet;
import nl.tue.pdc.concurrent.CustomConcurrentHashSet;
import nl.tue.pdc.data.Arena;
import nl.tue.pdc.data.Direction;
import nl.tue.pdc.data.GameState;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicReference;

public class ParallelSokobanSolver extends SokobanSolver {

    public static final int TIMEOUT = 60;

    private final int numThreads;
    private final boolean asyncMode;

    public ParallelSokobanSolver(Arena arena, GameState initialState, boolean useCustomSet, int numThreads, boolean asyncMode) {
        super(arena, initialState, useCustomSet ?
                new CustomConcurrentHashSet<>(30_000) :
                new ConcurrentHashSet<>(30_000_000, numThreads));
        this.numThreads = numThreads;
        this.asyncMode = asyncMode;
    }

    @Override
    public void solve() {
        visited.add(initialState);

        int wait = 0;
        ExplorationExceptionHandler ueh = new ExplorationExceptionHandler();
        System.err.println("pool_async_mode: " + asyncMode);
        try (ForkJoinPool pool = new ForkJoinPool(numThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, ueh, asyncMode)) {
            pool.setParallelism(numThreads);
            pool.execute(new ExplorationAction(initialState));
            System.err.println("Invoked task");
            while (!pool.isQuiescent() && wait < TIMEOUT) {
                Thread.sleep(1000);
                wait++;
                System.err.println("waited " + wait + " seconds. Pool: " + pool);
                System.err.printf(
                        "pool_status elapsed_s=%d async=%b threads=%d parallelism=%d size=%d active=%d running=%d steals=%d queued_tasks=%d queued_submissions=%d%n",
                        wait,
                        asyncMode,
                        numThreads,
                        pool.getParallelism(),
                        pool.getPoolSize(),
                        pool.getActiveThreadCount(),
                        pool.getRunningThreadCount(),
                        pool.getStealCount(),
                        pool.getQueuedTaskCount(),
                        pool.getQueuedSubmissionCount());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Execution of the thread pool was interrupted.", e);
        }
        if (ueh.firstException.get() != null) {
            throw new RuntimeException("Exception while executing parallel tasks.", ueh.firstException.get());
        }

        if (wait == TIMEOUT) {
            System.err.println("Hit timeout");
            return;
        }
        numStates = visited.size();
    }

    public class ExplorationAction extends RecursiveAction {

        private final GameState current;

        public ExplorationAction(GameState current) {
            this.current = current;
        }

        @Override
        protected void compute() {
            if (isSolved(current) && solution == null) {
                solution = current.getLurdString();
            }

            for (Direction d: Direction.values()) {
                GameState next = nextState(current, d);

                if (next == null) {
                    continue;
                }

                boolean isNewState;
                try {
                    isNewState = visited.add(next);
                } catch (Exception e) {
                    Thread.UncaughtExceptionHandler ueh = getPool().getUncaughtExceptionHandler();
                    if (ueh != null) {
                        ueh.uncaughtException(Thread.currentThread(), e);
                    }
                    return;
                }

                if (isNewState) {
                    (new ExplorationAction(next)).fork();
                }
            }
        }
    }

    private static class ExplorationExceptionHandler implements Thread.UncaughtExceptionHandler {

        public AtomicReference<Throwable> firstException = new AtomicReference<>();

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            firstException.compareAndSet(null, throwable);
        }
    }
}
