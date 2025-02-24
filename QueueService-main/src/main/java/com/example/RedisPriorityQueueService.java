package com.example;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.resps.Tuple;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class RedisPriorityQueueService {
    private static final String QUEUE_NAME = "myPriorityQueue";
    private final JedisPooled jedis;
    private final AtomicLong counter = new AtomicLong();

    public RedisPriorityQueueService() {
        String redisUrl = "rediss://default:ATswAAIjcDE1ZmM0ODdlZGNlMjE0MmE4OTUzNjhkMGFlY2RhNDM4MXAxMA@workable-hare-15152.upstash.io:6379";
        jedis = new JedisPooled(redisUrl);
    }

    // Push a task with priority
    public void push(String task, double priority) {
        long insertionOrder = counter.incrementAndGet(); // Get a unique sequence number for FCFS
        double compositeScore = priority + (insertionOrder / 1000000.0);

        jedis.zadd(QUEUE_NAME, compositeScore, task);
    }

    // Pull the highest-priority task (lowest score)
    public String pull() {
        List<Tuple> tasks = jedis.zpopmin(QUEUE_NAME, 1);
        return tasks.isEmpty() ? null : tasks.get(0).getElement();
    }

    // View all tasks in priority order
    public List<Tuple> viewAll() {
        return jedis.zrangeWithScores(QUEUE_NAME, 0, -1);
    }

    // Clear the queue
    public void delete() {
        jedis.del(QUEUE_NAME);
    }

    // Close Jedis Connection
    public void close() {
        jedis.close();
    }
}
