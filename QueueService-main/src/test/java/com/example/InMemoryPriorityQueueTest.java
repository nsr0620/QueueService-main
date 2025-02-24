package com.example;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class InMemoryPriorityQueueTest {
    private InMemoryPriorityQueue queue;
    private String queueUrl = "https://sqs.ap-1.amazonaws.com/007/MyQueue";

    @Before
    public void setup() {
        queue = new InMemoryPriorityQueue(4); // 4 seconds visibility timeout
    }

    @Test
    public void testEnqueueAndDequeue() {
        queue.push(queueUrl, "Test message!", 2);
        Message msg = queue.pull(queueUrl);

        assertNotNull(msg);
        assertEquals("Test message!", msg.getBody());
    }

    @Test
    public void testPriorityHandling() {
        queue.push(queueUrl, "Low Priority", 1);
        queue.push(queueUrl, "High Priority", 4); // Highest priority
        queue.push(queueUrl, "Medium Priority", 3);

        assertEquals("High Priority", queue.pull(queueUrl).getBody()); // Highest priority first
        assertEquals("Medium Priority", queue.pull(queueUrl).getBody()); // Next highest priority
        assertEquals("Low Priority", queue.pull(queueUrl).getBody()); // Lowest priority
    }

    @Test
    public void testFIFOWithSamePriority() {
        queue.push(queueUrl, "First Message", 3);
        queue.push(queueUrl, "Second Message", 3);

        assertEquals("First Message", queue.pull(queueUrl).getBody()); // FIFO order
        assertEquals("Second Message", queue.pull(queueUrl).getBody());
    }

    @Test
    public void testPullFromEmptyQueue() {
        Message msg = queue.pull(queueUrl);
        assertNull(msg);
    }

    @Test
    public void testPullTwice() {
        queue.push(queueUrl, "Single Message", 2);
        queue.pull(queueUrl);
        Message msg = queue.pull(queueUrl);
        assertNull(msg);
    }

    @Test
    public void testRemoveMessage() {
        queue.push(queueUrl, "Delete Me", 3);
        Message msg = queue.pull(queueUrl);
        queue.delete(queueUrl, msg.getReceiptId());
        msg = queue.pull(queueUrl);
        assertNull(msg);
    }

    @Test
    public void testVisibilityTimeoutFeature() throws InterruptedException {
        queue.push(queueUrl, "Temporary Hidden", 4);
        Message msg = queue.pull(queueUrl);

        assertNotNull(msg);
        assertNull(queue.pull(queueUrl)); // Message should be invisible

        Thread.sleep(5000); // Wait for timeout expiration
        assertNotNull(queue.pull(queueUrl)); // Message becomes visible again
    }
}
