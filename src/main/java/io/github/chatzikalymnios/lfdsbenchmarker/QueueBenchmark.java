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

import io.github.chatzikalymnios.lfds.LockFreeQueue;

public class QueueBenchmark implements Benchmark {
	private static final Logger logger = LoggerFactory.getLogger(QueueBenchmark.class);

	private static final int NANO_IN_MICROSECONDS = 1000;
	private static final int NANO_IN_MILLISECONDS = 1000000;

	private ExecutorService executorService;
	private LockFreeQueue<Integer> queue;
	private int numThreads;
	private int numItems;
	private int workload;
	private List<Integer> items;

	public QueueBenchmark(LockFreeQueue<Integer> queue, int numThreads, int numItems, int workload) {
		this.executorService = Executors.newFixedThreadPool(numThreads);
		this.queue = queue;
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

		System.out.println("QueueBenchmark [" + queue.getClass().getSimpleName() + "] running...");

		long startTime = System.nanoTime();

		List<Future<List<Integer>>> futures = executorService.invokeAll(workers);

		long endTime = System.nanoTime();
		long elapsedTime = (endTime - startTime) / NANO_IN_MILLISECONDS;

		executorService.shutdown();

		System.out.println("QueueBenchmark [" + queue.getClass().getSimpleName() + "] completed");
		System.out.println("---------------------------------------------------");
		System.out.println("Elapsed time: " + elapsedTime + " milliseconds");
		System.out.println("---------------------------------------------------");

		System.out.println("Errors:");

		if (queue.dequeue() != null) {
			System.out.println("\tThe stack is not empty after test");
		}

		List<Integer> dequeuedItems = gatherDequeuedItems(futures);

		if (dequeuedItems.size() != numItems) {
			System.out.println("\tEnqueued " + numItems + " items but dequeued " + dequeuedItems.size() + " items");
		}

		if (hasDuplicates(dequeuedItems)) {
			System.out.println("\tDuplicate element(s) were dequeued from the stack");
		}

		System.out.println("---------------------------------------------------");

		queue.printStats(System.out);
	}

	private Collection<Callable<List<Integer>>> createWorkers() {
		Collection<Callable<List<Integer>>> workers = new ArrayList<>();

		int numUsedItems = 0;
		int itemsPerWorker = numItems / numThreads;

		for (int i = 0; i < numThreads; i++) {
			int from = i * itemsPerWorker;
			int to = Math.min((i + 1) * itemsPerWorker, numItems);
			numUsedItems += to - from;
			workers.add(new QueueWorker(items.subList(from, to), itemsPerWorker));
		}

		numItems = numUsedItems;

		return workers;
	}

	private List<Integer> gatherDequeuedItems(List<Future<List<Integer>>> futures)
			throws InterruptedException, ExecutionException {
		List<Integer> dequeuedItems = new ArrayList<>();

		for (Future<List<Integer>> future : futures) {
			dequeuedItems.addAll(future.get());
		}

		return dequeuedItems;
	}

	private boolean hasDuplicates(List<Integer> items) {
		Set<Integer> set = new HashSet<>();
		set.addAll(items);
		return set.size() < items.size();
	}

	private class QueueWorker implements Callable<List<Integer>> {
		private List<Integer> items;
		private int numOps;

		QueueWorker(List<Integer> items, int numOps) {
			this.items = items;
			this.numOps = numOps;
		}

		@Override
		public List<Integer> call() throws Exception {
			List<Integer> dequeued = new ArrayList<>();

			for (int i = 0; i < numOps; i++) {
				queue.enqueue(items.get(i));
				spin();
			}

			for (int i = 0; i < numOps; i++) {
				dequeued.add(queue.dequeue());
				spin();
			}

			return dequeued;
		}

		private void spin() {
			long start = System.nanoTime();
			long end = start + workload * NANO_IN_MICROSECONDS;

			while (System.nanoTime() < end) {
			}
		}

	}
}
