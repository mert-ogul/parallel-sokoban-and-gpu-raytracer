package nl.tue.pdc.sampling;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.KernelContext;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.WorkerGrid;
import uk.ac.manchester.tornado.api.WorkerGrid2D;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;

public class RayBounceBenchmark {

    private static final int TRIANGLE_REJECTION = 0;
    private static final int TRIANGLE_FAST = 1;
    private static final int CIRCLE_REJECTION = 2;
    private static final int CIRCLE_FAST = 3;
    private static final int SIZE = 256;
    private static final int SAMPLES_PER_THREAD = 10000;
    private static final int MEASURED_RUNS = 3;

    public static void main(String[] args) {
        run();
    }

    public static void triangleRejectionKernel(KernelContext context, FloatArray results, final int samplesPerThread) {
        int globalId = context.globalIdx + context.globalIdy * context.globalGroupSizeX;
        GPURandom random = new GPURandom(globalId);
        float intermediateResult = 0.0f;

        for (int i = 0; i < samplesPerThread; i++) {
            intermediateResult += RayBounce.bounceTriangleRejection(context, random);
        }

        results.set(globalId, intermediateResult / samplesPerThread);
    }

    public static void triangleFastKernel(KernelContext context, FloatArray results, final int samplesPerThread) {
        int globalId = context.globalIdx + context.globalIdy * context.globalGroupSizeX;
        GPURandom random = new GPURandom(globalId);
        float intermediateResult = 0.0f;

        for (int i = 0; i < samplesPerThread; i++) {
            intermediateResult += RayBounce.bounceTriangleFast(context, random);
        }

        results.set(globalId, intermediateResult / samplesPerThread);
    }

    public static void circleRejectionKernel(KernelContext context, FloatArray results, final int samplesPerThread) {
        int globalId = context.globalIdx + context.globalIdy * context.globalGroupSizeX;
        GPURandom random = new GPURandom(globalId);
        float intermediateResult = 0.0f;

        for (int i = 0; i < samplesPerThread; i++) {
            intermediateResult += RayBounce.bounceCircleRejection(context, random);
        }

        results.set(globalId, intermediateResult / samplesPerThread);
    }

    public static void circleFastKernel(KernelContext context, FloatArray results, final int samplesPerThread) {
        int globalId = context.globalIdx + context.globalIdy * context.globalGroupSizeX;
        GPURandom random = new GPURandom(globalId);
        float intermediateResult = 0.0f;

        for (int i = 0; i < samplesPerThread; i++) {
            intermediateResult += RayBounce.bounceCircleFast(context, random);
        }

        results.set(globalId, intermediateResult / samplesPerThread);
    }

    public static void run() {
        int size = Math.max(1, SIZE);
        int samplesPerThread = Math.max(1, SAMPLES_PER_THREAD);
        int measuredRuns = Math.max(1, MEASURED_RUNS);

        RayTracer.printDevices();

        runCase("b0", "triangle rejection", TRIANGLE_REJECTION, size, samplesPerThread, measuredRuns);
        runCase("b1", "triangle fast", TRIANGLE_FAST, size, samplesPerThread, measuredRuns);
        runCase("b2", "circle rejection", CIRCLE_REJECTION, size, samplesPerThread, measuredRuns);
        runCase("b3", "circle fast", CIRCLE_FAST, size, samplesPerThread, measuredRuns);
    }

    private static void runCase(String graphId, String name, int benchmarkCase, int size, int samplesPerThread, int measuredRuns) {
        WorkerGrid workerGrid = new WorkerGrid2D(size, size);
        GridScheduler gridScheduler = new GridScheduler(graphId + ".t0", workerGrid);
        KernelContext context = new KernelContext();
        FloatArray results = new FloatArray(size * size);

        TaskGraph taskGraph = new TaskGraph(graphId);
        if (benchmarkCase == TRIANGLE_REJECTION) {
            taskGraph.task("t0", RayBounceBenchmark::triangleRejectionKernel, context, results, samplesPerThread);
        } else if (benchmarkCase == TRIANGLE_FAST) {
            taskGraph.task("t0", RayBounceBenchmark::triangleFastKernel, context, results, samplesPerThread);
        } else if (benchmarkCase == CIRCLE_REJECTION) {
            taskGraph.task("t0", RayBounceBenchmark::circleRejectionKernel, context, results, samplesPerThread);
        } else {
            taskGraph.task("t0", RayBounceBenchmark::circleFastKernel, context, results, samplesPerThread);
        }
        taskGraph.transferToHost(DataTransferMode.EVERY_EXECUTION, results);

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph)) {
            executionPlan.withGridScheduler(gridScheduler).execute();

            long totalTime = 0L;
            float checksum = 0.0f;
            for (int i = 0; i < measuredRuns; i++) {
                long start = System.nanoTime();
                executionPlan.withGridScheduler(gridScheduler).execute();
                long stop = System.nanoTime();
                totalTime += stop - start;
                checksum += checksum(results);
            }

            double averageSeconds = totalTime / (measuredRuns * 1_000_000_000.0);
            double totalSamples = (double) size * size * samplesPerThread;
            double samplesPerSecond = totalSamples / averageSeconds;
            System.out.printf("%-20s %8.4f s/run  %12.0f samples/s  checksum=%f%n",
                    name, averageSeconds, samplesPerSecond, checksum);
        } catch (TornadoExecutionPlanException e) {
            e.printStackTrace();
        }
    }

    private static float checksum(FloatArray results) {
        float sum = 0.0f;
        for (int i = 0; i < results.getSize(); i++) {
            sum += results.get(i);
        }
        return sum;
    }
}
