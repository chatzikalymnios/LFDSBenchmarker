package io.github.chatzikalymnios.lfdsbenchmarker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.chatzikalymnios.lfds.LockFreeStack;

public class StackBenchmark implements Benchmark {
    private static final Logger logger = LoggerFactory.getLogger(StackBenchmark.class);

    private static final int NANO_IN_MICROSECONDS = 1000;
    private static final int NANO_IN_MILLISECONDS = 1000000;

    private ExecutorService executorService;
    private LockFreeStack<Integer> stack;
    private int numThreads;
    private int numItems;
    private int workload;
    private List<Integer> items;

    public StackBenchmark(LockFreeStack<Integer> stack, int numThreads, int numItems, int workload) {
	this.executorService = Executors.newFixedThreadPool(numThreads);
	this.stack = stack;
	this.numThreads = numThreads;
	this.numItems = numItems;
	this.workload = workload;

	this.items = createItems(numItems);
    }

    private List<Integer> createItems(int numItems) {
	Random random = new Random(0);
	List<Integer> items = new ArrayList<>(numItems);

	// Fill list (no duplicates)
	for (int i = 0; i < numItems; i++) {
	    items.add(i);
	}

	// Shuffle list
	for (int i = 0; i < numItems; i++) {
	    int pos = random.nextInt(numItems);
	    int temp = items.get(i);
	    items.set(i, items.get(pos));
	    items.set(pos, temp);
	}

	return items;
    }

    @Override
    public void run() throws InterruptedException, ExecutionException {
	Collection<Callable<List<Integer>>> workers = createWorkers();

	System.out.println("StackBenchmark [" + stack.getClass().getSimpleName() + "] running...");

	long startTime = System.nanoTime();

	List<Future<List<Integer>>> futures = executorService.invokeAll(workers);

	long endTime = System.nanoTime();
	long elapsedTime = (endTime - startTime) / NANO_IN_MILLISECONDS;

	executorService.shutdown();

	System.out.println("StackBenchmark [" + stack.getClass().getSimpleName() + "] completed");
	System.out.println("---------------------------------------------------");
	System.out.println("Elapsed time: " + elapsedTime + " milliseconds");
	System.out.println("---------------------------------------------------");

	System.out.println("Errors:");

	if (stack.pop() != null) {
	    System.out.println("\tThe stack is not empty after test");
	}

	List<Integer> poppedItems = gatherPoppedItems(futures);

	if (poppedItems.size() != numItems) {
	    System.out.println("\tPushed " + numItems + " items but popped " + poppedItems.size() + " items");
	}

	if (hasDuplicates(poppedItems)) {
	    System.out.println("\tDuplicate element(s) were popped from the stack");
	}

	System.out.println("---------------------------------------------------");

	stack.reportStats();
    }

    private Collection<Callable<List<Integer>>> createWorkers() {
	Collection<Callable<List<Integer>>> workers = new ArrayList<>();

	int numUsedItems = 0;
	int itemsPerWorker = numItems / numThreads;

	for (int i = 0; i < numThreads; i++) {
	    int from = i * itemsPerWorker;
	    int to = Math.min((i + 1) * itemsPerWorker, numItems);
	    numUsedItems += to - from;
	    workers.add(new StackWorker(items.subList(from, to), itemsPerWorker));
	}

	numItems = numUsedItems;

	return workers;
    }

    private List<Integer> gatherPoppedItems(List<Future<List<Integer>>> futures)
	    throws InterruptedException, ExecutionException {
	List<Integer> poppedItems = new ArrayList<>();

	for (Future<List<Integer>> future : futures) {
	    poppedItems.addAll(future.get());
	}

	return poppedItems;
    }

    private boolean hasDuplicates(List<Integer> items) {
	Set<Integer> set = new HashSet<>();
	set.addAll(items);
	return set.size() < items.size();
    }

    private class StackWorker implements Callable<List<Integer>> {
	private Random random;
	private List<Integer> items;
	private int numOps;

	StackWorker(List<Integer> items, int numOps) {
	    this.random = new Random();
	    this.items = items;
	    this.numOps = numOps;
	}

	@Override
	public List<Integer> call() throws Exception {
	    List<Integer> popped = new ArrayList<>();

	    for (int i = 0; i < numOps; i++) {
		stack.push(items.get(i));
		spin();
	    }

	    for (int i = 0; i < numOps; i++) {
		popped.add(stack.pop());
		spin();
	    }

	    return popped;
	}

	private void spin() {
	    long start = System.nanoTime();
	    long end = start + workload * NANO_IN_MICROSECONDS;

	    while (System.nanoTime() < end) {
	    }
	}

    }

}
