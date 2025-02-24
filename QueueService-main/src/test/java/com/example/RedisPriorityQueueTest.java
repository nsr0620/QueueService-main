package com.example;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.resps.Tuple;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RedisPriorityQueueTest {
    private static RedisPriorityQueueService queue;

    @BeforeClass
    public static void setup() {
        queue = new RedisPriorityQueueService();
        queue.delete();
    }

    @Test
    public void testEnqueueAndDequeue() {
        queue.push("Task C", 10);
        queue.push("Task B", 5);
        queue.push("Task A", 1);

        // Expect Task A to be dequeued first (highest priority)
        assertEquals("Task A", queue.pull());
        assertEquals("Task B", queue.pull());
        assertEquals("Task C", queue.pull());

        assertNull(queue.pull());
    }

    @Test
    public void testFIFOWithSamePriority() {
        queue.push("First Task", 2);
        queue.push("Second Task", 2);

        assertEquals("First Task", queue.pull()); // FIFO order
        assertEquals("Second Task", queue.pull());
    }

    @Test
    public void testRetrieveAll() {
        queue.push("Urgent Task", 1);
        queue.push("Routine Task", 100);

        List<Tuple> tasks = queue.viewAll();
        assertEquals("Urgent Task", tasks.get(0).getElement());
        assertEquals("Routine Task", tasks.get(1).getElement());

        queue.delete(); // Clean up after test
    }

    @AfterClass
    public static void cleanup() {
        queue.delete();
    }
}
