package com.example;

import java.util.*;
import java.util.concurrent.*;

public class InMemoryPriorityQueue implements QueueService {
    private final Map<String, PriorityBlockingQueue<PriorityMessage>> queues;
    private final long visibilityTimeout;

    public InMemoryPriorityQueue(long visibilityTimeout) {
        this.queues = new ConcurrentHashMap<>();
        this.visibilityTimeout = visibilityTimeout;
    }

    @Override
    public void push(String queueUrl, String msgBody) {
        push(queueUrl, msgBody, 0); // Default priority is 0
    }

    public void push(String queueUrl, String msgBody, int priority) {
        queues.computeIfAbsent(queueUrl, k -> new PriorityBlockingQueue<>())
                .add(new PriorityMessage(msgBody, priority, System.currentTimeMillis()));
    }

    @Override
    public Message pull(String queueUrl) {
        PriorityBlockingQueue<PriorityMessage> queue = queues.get(queueUrl);
        if (queue == null)
            return null;

        long now = System.currentTimeMillis();
        List<PriorityMessage> visibleMessages = new ArrayList<>();

        for (PriorityMessage msg : queue) {
            if (msg.isVisibleAt(now)) {
                visibleMessages.add(msg);
            }
        }

        if (visibleMessages.isEmpty())
            return null;

        visibleMessages.sort(Comparator.comparingInt(PriorityMessage::getPriority).reversed()
                .thenComparingLong(PriorityMessage::getCreatedAt));

        PriorityMessage selectedMsg = visibleMessages.get(0);
        selectedMsg.setReceiptId(UUID.randomUUID().toString());
        selectedMsg.incrementAttempts();
        selectedMsg.setVisibleFrom(now + TimeUnit.SECONDS.toMillis(visibilityTimeout));
        return new Message(selectedMsg.getBody(), selectedMsg.getReceiptId());
    }

    @Override
    public void delete(String queueUrl, String receiptId) {
        PriorityBlockingQueue<PriorityMessage> queue = queues.get(queueUrl);
        if (queue == null)
            return;

        queue.removeIf(msg -> !msg.isVisibleAt(System.currentTimeMillis()) &&
                msg.getReceiptId().equals(receiptId));
    }

    private static class PriorityMessage extends Message implements Comparable<PriorityMessage> {
        private final int priority;
        private final long createdAt;

        public PriorityMessage(String msgBody, int priority, long createdAt) {
            super(msgBody);
            this.priority = priority;
            this.createdAt = createdAt;
        }

        public int getPriority() {
            return priority;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        @Override
        public int compareTo(PriorityMessage other) {
            return other.priority != this.priority ? Integer.compare(other.priority, this.priority) :

                    Long.compare(this.createdAt, other.createdAt); // FCFS within same priority
        }
    }
}
