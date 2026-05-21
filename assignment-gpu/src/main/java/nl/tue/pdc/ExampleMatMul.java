/*
Copyright 2026 TornadoVM team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package nl.tue.pdc;

import uk.ac.manchester.tornado.api.*;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.types.matrix.Matrix2DFloat;

/**
 * An example implementation of matrix multiplication
 * From the <a href="https://tornadovm.readthedocs.io/en/latest/programming.html#kernel-api">TornadoVM documentation</a>
 * To run this code:
 * - First generate an argfile using the command {@code tornado --generate-argfile}.
 * - Supply the argfil as a JVM option, for example {@code java @/opt/tornado/tornado-argfil -cp . nl.tue.pdc.ExampleMatMul}
 */
public class ExampleMatMul {
    private static void mxmKernel(KernelContext context, Matrix2DFloat A, Matrix2DFloat B, Matrix2DFloat C, final int size) {
        int idx = context.globalIdx;
        int jdx = context.globalIdy;
        float sum = 0;
        for (int k = 0; k < size; k++) {
            sum += A.get(idx, k) * B.get(k, jdx);
        }
        C.set(idx, jdx, sum);
    }

    public void run(Matrix2DFloat A, Matrix2DFloat B, Matrix2DFloat C, final int size) {
        // When using the kernel-parallel API, we need to create a Grid and a Worker

        WorkerGrid workerGrid = new WorkerGrid2D(size, size);    // Create a 2D Worker
        GridScheduler gridScheduler = new GridScheduler("s0.t0", workerGrid);  // Attach the worker to the Grid
        KernelContext context = new KernelContext();             // Create a context
        workerGrid.setLocalWork(32, 32, 1);                      // Set the local-group size

        TaskGraph taskGraph = new TaskGraph("s0")
                .transferToDevice(DataTransferMode.FIRST_EXECUTION, A, B) // Transfer data from host to device only during the first execution
                .task("t0", ExampleMatMul::mxmKernel, context, A, B, C, size)   // Each task points to an existing Java method
                .transferToHost(DataTransferMode.EVERY_EXECUTION, C);     // Transfer data from device to host in every execution.
        // Create an immutable task-graph
        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();

        // Create an execution plan from an immutable task-graph
        TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph);

        // Execute the execution plan
        executionPlan.withGridScheduler(gridScheduler)
                .execute();

        System.out.println(C);
    }

    static void main() {
        Matrix2DFloat A = new Matrix2DFloat(2,2);
        Matrix2DFloat B = new Matrix2DFloat(2,2);
        Matrix2DFloat C = new Matrix2DFloat(2,2);
        A.set(0,0, 3);
        A.set(1, 1, 5);
        B.set(0,0, 3);
        B.set(1, 0, 4);

        (new ExampleMatMul()).run(A, B, C, 2);
    }
}
