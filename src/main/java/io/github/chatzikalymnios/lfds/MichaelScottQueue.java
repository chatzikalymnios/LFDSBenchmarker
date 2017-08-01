package io.github.chatzikalymnios.lfds;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Michael Scott Queue.
 *
 * @see <a href="https://doi.org/10.1145/248052.248106">Simple, fast, and
 *      practical non-blocking and blocking concurrent queue algorithms</a>
 */
public class MichaelScottQueue<E> implements LockFreeQueue<E> {
	private static final Logger logger = LoggerFactory.getLogger(MichaelScottQueue.class);

	// Statistics
	private final AtomicInteger numEnqueues = new AtomicInteger(0);
	private final AtomicInteger numDequeues = new AtomicInteger(0);

	private final AtomicReference<Node> queueHead;
	private final AtomicReference<Node> queueTail;

	public MichaelScottQueue() {
		// Create sentinel node
		Node n = new Node();
		queueHead = new AtomicReference<>(n);
		queueTail = new AtomicReference<>(n);

		logger.trace("Created MSQueue");
	}

	/**
	 * Creates a new node containing the provided data and enqueues it atomically.
	 *
	 * @param data
	 *            the data to be enqueued
	 */
	@Override
	public void enqueue(E data) {
		Node n = new Node(data);
		Node tail = null;
		Node next = null;

		while (true) {
			tail = queueTail.get();
			next = tail.next.get();

			if (tail == queueTail.get()) {
				if (next == null) {
					if (tail.next.compareAndSet(next, n)) {
						break;
					}
				} else {
					// Tail has fallen behind
					// Try to advance it
					queueTail.compareAndSet(tail, next);
				}
			}
		}

		// Try to set tail to the inserted node
		// If unsuccessful, the tail was advanced by a concurrent thread
		queueTail.compareAndSet(tail, n);

		logger.trace("Successful enqueue: " + data);
		numEnqueues.incrementAndGet();
	}

	/**
	 * Atomically dequeues the head of the queue.
	 *
	 * @return the old head of queue, or <code>null</code> if the queue was empty
	 */
	@Override
	public E dequeue() {
		Node head = null;
		Node tail = null;
		Node next = null;
		E data = null;

		while (true) {
			head = queueHead.get();
			tail = queueTail.get();
			next = head.next.get();

			if (head == queueHead.get()) {
				if (head == tail) {
					if (next == null) {
						// Queue is empty
						return null;
					}

					// Tail has fallen behind
					// Try to advance it
					queueTail.compareAndSet(tail, next);
				} else {
					data = next.data;

					if (queueHead.compareAndSet(head, next)) {
						break;
					}
				}
			}
		}

		logger.trace("Successful dequeue: " + data);
		numDequeues.incrementAndGet();

		return data;
	}

	@Override
	public void printStats(PrintStream stream) {
		stream.println(getClass().getSimpleName() + " stats:");
		stream.println("Number of enqueues:    " + numEnqueues.get());
		stream.println("Number of dequeues:    " + numDequeues.get());
	}

	private class Node {
		E data;
		AtomicReference<Node> next;

		Node() {
			this(null);
		}

		Node(E data) {
			this.data = data;
			this.next = new AtomicReference<>();
		}
	}

}
