package nl.tue.pdc.sampling;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.KernelContext;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoBackend;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.WorkerGrid;
import uk.ac.manchester.tornado.api.WorkerGrid2D;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;

public class RayTracer {

	private static final int SAMPLES_PER_THREAD = 100000;

	public static void printDevices() {
		int numBackEnds = TornadoRuntimeProvider.getTornadoRuntime().getNumBackends();
		System.out.println("Number of backends found: " + numBackEnds);

		for (int i = 0; i < numBackEnds; i++){
			TornadoBackend backEnd = TornadoRuntimeProvider.getTornadoRuntime().getBackend(i);
			int numDevices = backEnd.getNumDevices();
			System.out.println("Number of devices found: " + numDevices);

			for (int j = 0; j < numDevices; j++) {
				TornadoDevice firstDevice = backEnd.getDevice(j);
				System.out.println("Devices: " + firstDevice);
			}
		}
	}

	public static void rayTracingKernel(KernelContext context, FloatArray results) {
		int globalId = context.globalIdx + context.globalIdy * context.globalGroupSizeX;

		GPURandom r = new GPURandom(globalId);
		float intermediate_result = 0.0f;

		for (int i = 0; i < SAMPLES_PER_THREAD; i++) {
			intermediate_result += RayBounce.bounce(context, r);
		}

		results.set(globalId, intermediate_result / SAMPLES_PER_THREAD);
	}

    public static void run() {
		printDevices();

		int width = 256;
		int height = 256;

		WorkerGrid workerGrid = new WorkerGrid2D(width, height);    // Create a 2D Worker
        GridScheduler gridScheduler = new GridScheduler("g0.t0", workerGrid);  // Attach the worker to the Grid
		KernelContext context = new KernelContext();

		FloatArray results = new FloatArray(width * height);

		TaskGraph tg = new TaskGraph("g0")
			.task("t0", RayTracer::rayTracingKernel, context, results)
			.transferToHost(DataTransferMode.EVERY_EXECUTION, results);

		ImmutableTaskGraph itg = tg.snapshot();
		TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(itg);

		long timerStart = System.nanoTime();
		executionPlan.withGridScheduler(gridScheduler).execute();
		try {
			executionPlan.close();
		} catch (TornadoExecutionPlanException e) {
			e.printStackTrace();
		}
		long timerStop = System.nanoTime();

		RenderImage img = new RenderImage(results, width, height);
		img.show();

		System.out.println("Elapsed time: " + ((timerStop - timerStart) / 1000000000.0) + "s");
    }
}