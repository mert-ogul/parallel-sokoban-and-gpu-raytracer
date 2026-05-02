package nl.tue.pdc;

import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.*;
import nl.tue.pdc.data.Arena;
import nl.tue.pdc.data.GameState;
import nl.tue.pdc.data.LevelParser;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;

import java.io.IOException;
import java.util.Base64;

public class SokobanApp {

    private static final ArgumentParser parser = ArgumentParsers.newFor("SokobanApp").build();

    static {
        parser.description("Run the Sokoban solver on the provided level.");

        MutuallyExclusiveGroup mode = parser.addMutuallyExclusiveGroup("mode")
                .description("Whether to run sequentially or in parallel. Default: sequential.");
        mode.addArgument("-s", "--seq")
                .help("Run sequentially")
                .action(Arguments.storeFalse())
                .dest("mode");;
        mode.addArgument("-p", "--par")
                .help("Run in parallel")
                .action(Arguments.storeTrue())
                .dest("mode");

        parser.addArgument("-t", "--threads")
                .type(Integer.class)
                .dest("threads")
                .setDefault(Runtime.getRuntime().availableProcessors())
                .help("""
                        Number of threads to use for the parallel solver. \
                        Has no effect when using the sequential solver. \
                        Default: number of available processors.""");

        parser.addArgument("-c", "--custom-set")
                .dest("custom-set")
                .action(Arguments.storeTrue())
                .help("""
                        Use a custom thread-safe implementation for the hash set of explored game states. \
                        Has no effect when using the sequential solver.""");

        parser.addArgument("--async-pool")
                .dest("async-pool")
                .action(Arguments.storeTrue())
                .help("""
                        Use ForkJoinPool async mode for the parallel solver. \
                        Has no effect when using the sequential solver.""");

        parser.addArgument("level")
                .type(Arguments.fileType().acceptSystemIn().verifyCanRead().verifyIsFile())
                .setDefault("-")
                .metavar("LEVEL")
                .help("The Sokoban level file to solve. To read from STDIN, provide the argument \"-\". Default: -");
    }

    public static void main(String[] args) {
        Namespace namespace;
        try {
            namespace = parser.parseArgs(args);
        } catch (HelpScreenException e) {
            return;
        } catch (ArgumentParserException e) {
            System.err.println("Error: failed to parse arguments: " + e.getMessage());
            parser.printUsage();
            return;
        }

        int numThreads = namespace.getInt("threads");
        if (numThreads < 1) {
            System.err.println("Error: number of threads must be greater than 0.");
            return;
        }
        if (numThreads > Runtime.getRuntime().availableProcessors()) {
            System.err.println("Warning: number of threads is greater than number of processors.");
        }

        boolean useCustomSet = namespace.getBoolean("custom-set");
        boolean asyncPool = namespace.getBoolean("async-pool");

        String levelFile = namespace.getString("level");
        Arena arena; GameState state;
        try {
            LevelParser.ParseResult result = LevelParser.parse(levelFile);
            arena = result.arena();
            state = result.state();
        } catch (IOException e) {
            System.err.println("Error: failed to parser level file " + levelFile + ": " + e.getMessage());
            return;
        }

        System.err.println("Level Loaded Successfully!");
        System.err.printf("Dimensions: %d x %d%n", arena.width(), arena.height());
        System.err.println("Player Position: " + state.player());
        System.err.println("Number of Crates: " + state.crates().length);
        System.err.println("Link: https://dangarfield.github.io/sokoban-solver/?name=test-app" +
                "&w=" + arena.width() +
                "&h=" + arena.height() +
                "&grid=" + Base64.getEncoder().encodeToString(arena.toString(state, false).getBytes())
        );

        SokobanSolver solver = namespace.get("mode") ?
                new ParallelSokobanSolver(arena, state, useCustomSet, numThreads, asyncPool) :
                new SequentialSokobanSolver(arena, state);

        long start = System.currentTimeMillis();
        solver.solve();
        long end = System.currentTimeMillis();

        System.out.println("level: " + levelFile);
        System.out.println("runtime: " + (end - start));
        System.out.println("num_states: " + solver.getNumStates());
        if (solver.getSolution() != null) {
            System.out.println("solution:");
            System.out.println("    length: " + solver.getSolution().length());
            System.out.println("    path: " + solver.getSolution());
        } else {
            System.out.println("solution: null");
        }
    }
}
