package io.github.chatzikalymnios.lfds;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Elimination-Backoff Stack.
 *
 * @see <a href="http://doi.acm.org/10.1145/1007912.1007944">Danny Hendler, Nir
 *      Shavit, and Lena Yerushalmi. 2004. A scalable lock-free stack algorithm.
 *      In Proceedings of the sixteenth annual ACM symposium on Parallelism in
 *      algorithms and architectures (SPAA '04). ACM, New York, NY, USA,
 *      206-215.</a>
 */
public class EliminationBackoffStack<E> implements LockFreeStack<E> {
    private static final Logger logger = LoggerFactory.getLogger(EliminationBackoffStack.class);

    private static enum StackOp {
	PUSH, POP
    };

    private static final int EMPTY = -1;
    private static final int DELAY = 1000;

    /*
     * Atomic integer containing the next id to be assigned by the current EBStack
     * instance.
     */
    private final AtomicInteger nextId = new AtomicInteger(0);

    /*
     * Each updating thread is assigned a unique thread local id. The threadId
     * variable is instantiated for each thread at the moment it is first read by
     * that thread.
     */
    private final ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
	@Override
	protected Integer initialValue() {
	    return nextId.getAndIncrement();
	}
    };

    private Random random;

    private AtomicReference<Cell> top;
    private int concurrencyLevel;
    private ArrayList<AtomicReference<ThreadInfo>> location;
    private AtomicInteger[] collision;

    /**
     * Creates an empty EBStack with the specified <code>concurrencyLevel</code>.
     * The <code>concurrencyLevel</code> denotes the number of concurrently updating
     * threads and is used as the size of the internal collision array.
     *
     * @param concurrencyLevel
     *            the number of concurrently updating threads
     */
    public EliminationBackoffStack(int concurrencyLevel) {
	this.random = new Random();
	this.top = new AtomicReference<>(null);
	this.concurrencyLevel = concurrencyLevel;

	this.location = new ArrayList<>(concurrencyLevel);
	for (int i = 0; i < concurrencyLevel; i++) {
	    location.add(new AtomicReference<ThreadInfo>());
	}

	this.collision = new AtomicInteger[concurrencyLevel];
	for (int i = 0; i < concurrencyLevel; i++) {
	    collision[i] = new AtomicInteger(EMPTY);
	}

	logger.trace("Created EBStack with concurrencyLevel = " + concurrencyLevel);
    }

    /**
     * Creates a new cell containing the provided data and either pushes it
     * atomically onto the stack or eliminates a concurrent {@link #pop() pop}
     * operation.
     *
     * @param data
     *            the data to be pushed onto the stack
     */
    @Override
    public void push(E data) {
	Cell cell = new Cell(data);
	ThreadInfo myInfo = new ThreadInfo(threadId.get(), StackOp.PUSH, cell);

	while (true) {
	    if (tryPush(cell)) {
		logger.trace("Successful push: " + data);
		return;
	    } else {
		logger.trace("Failed push: " + data);

		if (tryEliminate(myInfo)) {
		    logger.trace("Eliminated push.");
		    return;
		}
	    }
	}
    }

    /**
     * Attempts to atomically push a cell onto the stack.
     *
     * @param cell
     *            the cell to be pushed onto the stack
     * @return <code>true</code> if the top of stack was changed; <code>false</code>
     *         otherwise
     */
    private boolean tryPush(Cell cell) {
	Cell oldTop = top.get();
	cell.next = oldTop;
	return top.compareAndSet(oldTop, cell);
    }

    /**
     * Atomically pops the top of stack or eliminates a concurrent
     * {@link #push(Object) push} operation.
     *
     * @return the old top of stack, or <code>null</code> if the stack was empty
     */
    @Override
    public E pop() {
	Cell cell = new Cell(null);
	ThreadInfo myInfo = new ThreadInfo(threadId.get(), StackOp.POP, cell);

	while (true) {
	    if (tryPop(cell)) {
		logger.trace("Successful pop: " + cell.data);
		return cell.data;
	    } else {
		logger.trace("Failed pop.");

		if (tryEliminate(myInfo)) {
		    logger.trace("Eliminated pop: " + myInfo.cell.data);
		    return myInfo.cell.data;
		}
	    }
	}
    }

    /**
     * Attempts to atomically pop the top of stack. The provided Cell object's data
     * field will contain the old top of stack if the stack was not empty and the
     * change was successful, and will be <code>null</code> otherwise.
     *
     * @return <code>true</code> if the top of stack was changed or the stack was
     *         empty; <code>false</code> otherwise
     */
    private boolean tryPop(Cell cell) {
	Cell oldTop = top.get();
	if (oldTop == null) {
	    cell.data = null;
	    return true;
	}

	Cell newTop = oldTop.next;
	if (top.compareAndSet(oldTop, newTop)) {
	    cell.data = oldTop.data;
	    return true;
	}

	return false;
    }

    /**
     * Attempts to eliminate the current thread's stack operation with a concurrent
     * opposing operation. The provided ThreadInfo object should be instantiated
     * with the current thread's id, the desired stack operation's opcode and the
     * Cell object to operate with. If the elimination is successful, the method
     * will return true and the provided cell's data field will contain
     * <code>null</code> in the case of a PUSH operation, or the eliminated PUSH
     * operation's data field in the case of a POP operation. Otherwise the method
     * will return false and the cell will remain unchanged.
     *
     * @param myInfo
     *            the ThreadInfo object describing the operation to eliminate
     * @return <code>true</code> if the elimination was successful;
     *         <code>false</code> otherwise
     */
    private boolean tryEliminate(ThreadInfo myInfo) {
	int myId = myInfo.id;
	location.get(myId).set(myInfo);
	int pos = random.nextInt(concurrencyLevel);
	int hisId = collision[pos].get();

	while (!collision[pos].compareAndSet(hisId, myId)) {
	    hisId = collision[pos].get();
	}

	if (hisId != EMPTY) {
	    ThreadInfo hisInfo = location.get(hisId).get();
	    if (hisInfo != null && hisInfo.id == hisId && hisInfo.op != myInfo.op) {
		if (location.get(myId).compareAndSet(myInfo, null)) {
		    return tryCollision(myInfo, hisInfo);
		} else {
		    finishCollision(myInfo);
		    return true;
		}
	    }
	}

	spin(DELAY);

	if (!location.get(myId).compareAndSet(myInfo, null)) {
	    finishCollision(myInfo);
	    return true;
	}

	return false;
    }

    /**
     * Attempts to atomically eliminate the two provided operations that have
     * collided.
     *
     * @param myInfo
     *            the current thread's operation
     * @param hisInfo
     *            the other thread's operation
     * @return <code>true</code> if the elimination was successful;
     *         <code>false</code> otherwise
     */
    private boolean tryCollision(ThreadInfo myInfo, ThreadInfo hisInfo) {
	if (myInfo.op == StackOp.PUSH) {
	    return location.get(hisInfo.id).compareAndSet(hisInfo, myInfo);
	}

	if (location.get(hisInfo.id).compareAndSet(hisInfo, null)) {
	    myInfo.cell = hisInfo.cell;
	    location.get(myInfo.id).set(null);
	    return true;
	}

	return false;
    }

    /**
     * Performs final operations to reset the elimination array after a successful
     * elimination.
     *
     * @param myInfo
     *            the current thread's operation
     */
    private void finishCollision(ThreadInfo myInfo) {
	if (myInfo.op == StackOp.POP) {
	    myInfo.cell = location.get(myInfo.id).get().cell;
	    location.get(myInfo.id).set(null);
	}
    }

    /**
     * Spins for <code>delay</code> nanoseconds.
     *
     * @param delay
     *            number of nanoseconds to spin for
     */
    private void spin(int delay) {
	long start = System.nanoTime();
	long end = start + delay;

	while (System.nanoTime() < end) {
	}
    }

    @Override
    public void reportStats() {
	System.out.println(getClass().getSimpleName() + " stats:");
    }

    private class Cell {
	E data;
	Cell next;

	Cell(E data) {
	    this.data = data;
	    this.next = null;
	}
    }

    private class ThreadInfo {
	int id;
	StackOp op;
	Cell cell;

	ThreadInfo(int threadId, StackOp op, Cell cell) {
	    this.id = threadId;
	    this.op = op;
	    this.cell = cell;
	}
    }
}
