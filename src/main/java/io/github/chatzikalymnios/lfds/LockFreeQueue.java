package io.github.chatzikalymnios.lfds;

import java.io.PrintStream;

public interface LockFreeQueue<E> {
	/**
	 * Atomically enqueues the provided data.
	 *
	 * @param data
	 *            the data to be enqueued
	 */
	void enqueue(E data);

	/**
	 * Atomically dequeues the head of the queue.
	 *
	 * @return the old head of queue, or <code>null</code> if the queue was empty
	 */
	E dequeue();

	/**
	 * Prints statistics relevant to each LockFreeQueue implementation to the
	 * specified stream.
	 *
	 * @param stream
	 *            PrintStream to print statistics to
	 */
	void printStats(PrintStream stream);
}
