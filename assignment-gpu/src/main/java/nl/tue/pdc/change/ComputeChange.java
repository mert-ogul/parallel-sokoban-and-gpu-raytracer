/*
 * Copyright (c) 2025 Eindhoven University of Technology (TU/e) / Thomas Neele
 */

package nl.tue.pdc.change;

import uk.ac.manchester.tornado.api.*;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.matrix.Matrix2DInt;

import java.util.Arrays;

public class ComputeChange {

    private static void changeKernel(KernelContext context, Matrix2DInt table, IntArray coins, int amount) {
        //TODO: create implementation
        
        // Amount column number = workerIdx 
        int workerIdx  = context.globalIdx;       
        
        int coinNr = coins.getSize();

        // Base case
        table.set(0, workerIdx , workerIdx  == 0 ? 0 : coinNr + 1);

        // All seperate workers need to finish row 0 before any of them can move (Synchronization)
        context.localBarrier();

        for (int Id = 0; Id < coinNr; Id++) {
            int result;
            int thisCoin = coins.get(Id);
            int notUseCoin = table.get(Id, workerIdx);  
            if (thisCoin <= workerIdx) {
                int useCoin = 1 + table.get(Id, workerIdx  - thisCoin);
                result = Math.min(notUseCoin, useCoin);
            } else {
                result = notUseCoin;
            }
            table.set(Id + 1, workerIdx , result);

            // All seperate workers need to finish row nr Id+1 before any of them can move (Synchronization)
            context.localBarrier();
        }
    }

    private static void changeKernelLessMemory(KernelContext context, Matrix2DInt table, IntArray coins, int amount) {
        //TODO: create implementation

        // Amount column number = workerIdx
        int workerIdx  = context.globalIdx;

        int coinNr = coins.getSize();

        // Base case
        table.set(0, workerIdx , workerIdx  == 0 ? 0 : coinNr + 1);

        // All seperate workers need to finish row 0 before any of them can move (Synchronization)
        context.localBarrier();

        for (int Id = 0; Id < coinNr; Id++) {
            int result;
            int thisCoin = coins.get(Id);
            int notUseCoin = table.get(Id % 2, workerIdx);
            if (thisCoin <= workerIdx) {
                int useCoin = 1 + table.get(Id % 2, workerIdx  - thisCoin);
                result = Math.min(notUseCoin, useCoin);
            } else {
                result = notUseCoin;
            }
            table.set((Id + 1) % 2, workerIdx , result);

            // All seperate workers need to finish row nr Id+1 before any of them can move (Synchronization)
            context.localBarrier();
        }
    }

    public static int runKernel(int[] coins, int amount, boolean lessMemory, boolean printMatrix) {
        // Initialize data structures
        IntArray coinsA = IntArray.fromArray(coins);
        Matrix2DInt table = new Matrix2DInt(lessMemory ? 2 : coins.length + 1, amount + 1); //TODO: set the dimensions of the table to operate on

        WorkerGrid workerGrid = new WorkerGrid1D(amount + 1);    //TODO: fill in the number of workers
        workerGrid.setLocalWork(amount + 1, 1, 1);
        GridScheduler gridScheduler = new GridScheduler("computeChange.t1", workerGrid);  // Attach the worker to the Grid
        KernelContext context = new KernelContext();

        TaskGraph taskGraph = new TaskGraph("computeChange")
                .transferToDevice(DataTransferMode.FIRST_EXECUTION, coinsA) // Transfer data from CPU to GPU
                .task("t1", lessMemory ? ComputeChange::changeKernelLessMemory : ComputeChange::changeKernel, context, table, coinsA, amount)
                .transferToHost(DataTransferMode.EVERY_EXECUTION, table);     // Transfer data from GPU to CPU

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph)) {
            // Run the execution plan on the default device
            executionPlan.withGridScheduler(gridScheduler)
                    .execute();
        } catch (TornadoExecutionPlanException e) {
            throw new RuntimeException(e);
        }

        if (printMatrix) {
            System.out.println(table);
        }
       
        if (lessMemory) {
            return table.get((coins.length) % 2, amount); 
        } else {
            return table.get(coins.length, amount);
        }//TODO: return the final result
    }

    /**
     *Compute how many coins from the list {@code coins} are needed to create the required total {@code amount}.
     * This is a recursive implementation that checks for each coin whether it's better to use it or not, starting
     * with the last coin in the list. The current coin to check is tracked through the parameter {@code coinId}
     * @param coins A list of available coins
     * @param amount The amount of change to create
     * @param coinId index of the current coin to check in the list {@code coins}
     * @return The least number of coins needed, or a value larger than {@code coins.length} if this is not possible
     */
    private static int computeRecursive(int[] coins, int amount, int coinId) {
        assert(amount >= 0);
        if (coinId < 0) {
            return amount == 0 ? 0 : coins.length + 1;
        }

        int thisCoin = coins[coinId];
        // How many coins do we need if we choose not to use this one?
        int dontUseThisCoin = computeRecursive(coins, amount, coinId - 1);
        if (thisCoin > amount) {
            // We cannot use this coin
            return dontUseThisCoin;
        } else {
            int useThisCoin = 1 + computeRecursive(coins, amount - thisCoin, coinId - 1);
            return Math.min(useThisCoin, dontUseThisCoin);
        }
    }

    /**
     * Compute how many coins from the list {@code coins} are needed to create the required total {@code amount}.
     * This is a recursive implementation that checks for each coin whether it's better to use it or not, starting
     * with the last coin in the list.
     * @param coins A list of coins available
     * @param amount The amount of change to create from the available coins
     * @return The least number of coins needed, or a value larger than {@code coins.length} if this is not possible
     */
    private static int computeRecursive(int[] coins, int amount) {
        return computeRecursive(coins, amount, coins.length - 1);
    }

    /**
     * Compute how many coins from the list {@code coins} are needed to create the required total {@code amount}.
     * This is a recursive implementation that checks for each coin whether it's better to use it or not, starting
     * with the last coin in the list. It applies dynamic programming to eliminate duplicate calls.
     * @param coins A list of coins available
     * @param amount The amount of change to create from the available coins
     * @param coinId index of the current coin to check in the list {@code coins}
     * @param cache Records values that have already been computed. -1 indicates result is not known yet.
     * @return The least number of coins needed, or a value larger than {@code coins.length} if this is not possible
     */
    private static int computeDynamic(int[] coins, int amount, int coinId, int[][] cache) {
        assert(amount >= 0);
        if (coinId < 0) {
            return amount == 0 ? 0 : coins.length + 1;
        }
        // Check if this has been computed before
        if (cache[coinId][amount] != -1) {
            return cache[coinId][amount];
        }

        int thisCoin = coins[coinId];
        // How many coins do we need if we choose not to use this one?
        int dontUseThisCoin = computeDynamic(coins, amount, coinId - 1, cache);
        int result;
        if (thisCoin > amount) {
            // We cannot use this coin
            result = dontUseThisCoin;
        } else {
            int useThisCoin = 1 + computeDynamic(coins, amount - thisCoin, coinId - 1, cache);
            result = Math.min(useThisCoin, dontUseThisCoin);
        }
        cache[coinId][amount] = result;
        return result;
    }

    /**
     * Compute how many coins from the list {@code coins} are needed to create the required total {@code amount}.
     * This is a recursive implementation that checks for each coin whether it's better to use it or not, starting
     * with the last coin in the list. It applies dynamic programming to eliminate duplicate calls.
     * @param coins A list of coins available
     * @param amount The amount of change to create from the available coins
     * @param printMatrix If set to true, the matrix used for storing intermediate results is printed
     * @return The least number of coins needed, or a value larger than {@code coins.length} if this is not possible
     */
    private static int computeDynamic(int[] coins, int amount, boolean printMatrix) {
        int[][] cache = new int[coins.length][amount+1];
        // initialise the cache with -1, indicating these values have not been computed yet.
        for (int i = 0; i < coins.length; i++) {
            for (int j = 0; j < amount+1; j++) {
                cache[i][j] = -1;
            }
        }

        int result = computeDynamic(coins, amount, coins.length-1, cache);

        // optionally print out the matrix of computed values
        if (printMatrix) {
            for (int i = 0; i < coins.length; i++) {
                for (int j = 0; j < amount+1; j++) {
                    System.out.printf("%2d ", cache[i][j]);
                }
                System.out.println();
            }
        }
        return result;
    }


    private static void run(int[] coins, int amount) {
        int recursiveResult = computeRecursive(coins, amount);
        int dynamicResult = computeDynamic(coins, amount, true);
        int gpuResult = runKernel(coins, amount,false, true);
        int gpuLessMemoryResult = runKernel(coins, amount,true, false);

        System.out.println("Amount " + amount + " with coins " + Arrays.toString(coins) + ":");
        System.out.println("- recursive: " + recursiveResult);
        System.out.println("- dynamic: " + dynamicResult);
        System.out.println("- gpu: " + gpuResult);
        System.out.println("- gpu-less-memory: " + gpuLessMemoryResult);
    }

    public static void run() {
        run(new int[]{1,1,2,5,10}, 14);
        run(new int[]{5,5,10,10,20}, 35);
        // Impossible cases
        run(new int[]{1,1,2,5,10}, 22); // amount is too high
        run(new int[]{1,2,5,10}, 4); // amount is impossible to achieve exactly
    }
}