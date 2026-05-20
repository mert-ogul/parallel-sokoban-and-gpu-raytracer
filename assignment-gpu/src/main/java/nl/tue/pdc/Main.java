package nl.tue.pdc;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import nl.tue.pdc.change.ComputeChange;
import nl.tue.pdc.sampling.RayTracer;

public class Main {

    private static final ArgumentParser parser = ArgumentParsers.newFor("GPU-App").build();

    static {
        parser.description("Run code for computing change or simplified ray tracing.");

        MutuallyExclusiveGroup mode = parser.addMutuallyExclusiveGroup("mode")
                .required(true)
                .description("Whether to run ComputeChange or RayTracer.");
        mode.addArgument("--change")
                .help("Run ComputeChange")
                .action(Arguments.storeFalse())
                .dest("mode");;
        mode.addArgument("--raytracer")
                .help("Run RayTracer")
                .action(Arguments.storeTrue())
                .dest("mode");
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

        if (namespace.getBoolean("mode")) {
            RayTracer.run();
        } else {
            ComputeChange.run();
        }
    }
}
