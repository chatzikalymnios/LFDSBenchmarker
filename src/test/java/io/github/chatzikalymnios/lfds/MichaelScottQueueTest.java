package io.github.chatzikalymnios.lfds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MichaelScottQueueTest {

	private MichaelScottQueue<Integer> queue;

	@Before
	public void setUp() throws Exception {
		queue = new MichaelScottQueue<>();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEnqueueDequeue() {
		assertNull(queue.dequeue());

		queue.enqueue(1);
		queue.enqueue(2);

		assertEquals(1, (long) queue.dequeue());

		queue.enqueue(3);
		assertEquals(2, (long) queue.dequeue());
		assertEquals(3, (long) queue.dequeue());

		assertNull(queue.dequeue());
	}

}
