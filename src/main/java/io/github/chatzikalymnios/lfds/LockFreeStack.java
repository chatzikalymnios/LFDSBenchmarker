package io.github.chatzikalymnios.lfds;

import java.io.PrintStream;

public interface LockFreeStack<E> {
	/**
	 * Atomically pushes the provided data onto the stack.
	 *
	 * @param data
	 *            the data to be pushed onto the stack
	 */
	void push(E data);

	/**
	 * Atomically pops the top of stack.
	 *
	 * @return the old top of stack, or <code>null</code> if the stack was empty
	 */
	E pop();

	/**
	 * Prints statistics relevant to each LockFreeStack implementation to the
	 * specified stream.
	 *
	 * @param stream
	 *            PrintStream to print statistics to
	 */
	void printStats(PrintStream stream);
}
