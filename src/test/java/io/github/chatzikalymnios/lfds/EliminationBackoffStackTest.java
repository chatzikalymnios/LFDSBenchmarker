package io.github.chatzikalymnios.lfds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EliminationBackoffStackTest {

	private EliminationBackoffStack<Integer> stack;

	@Before
	public void setUp() throws Exception {
		// Single-threaded operation
		stack = new EliminationBackoffStack<>(1);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPushPop() {
		assertNull(stack.pop());

		stack.push(1);
		stack.push(2);

		assertEquals((long) 2, (long) stack.pop());

		stack.push(3);
		assertEquals((long) 3, (long) stack.pop());
		assertEquals((long) 1, (long) stack.pop());

		assertNull(stack.pop());
	}

}
