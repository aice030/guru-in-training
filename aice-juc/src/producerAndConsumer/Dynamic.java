package producerAndConsumer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多生产者、多消费者
 * 可随时调整生产者、消费者数量
 * @author aice
 */
public class Dynamic {
    private final int M;
    private final int N;
    private final int maxCapacity;
    private final AtomicInteger data = new AtomicInteger(0);
    private final AtomicInteger producerNum = new AtomicInteger(0);
    private final AtomicInteger consumerNum = new AtomicInteger(0);

    private volatile boolean isRunning = true;
    private final Deque<Integer> queue = new ArrayDeque<>();
    private final Deque<Thread> producerQueue = new ArrayDeque<>();
    private final Deque<Thread> consumerQueue = new ArrayDeque<>();

    private final Lock lock = new ReentrantLock();
    private final Condition fullCondition = lock.newCondition();
    private final Condition emptyCondition = lock.newCondition();

    public Dynamic(int m, int n, int maxCapacity) {
        this.M = m;
        this.N = n;
        this.maxCapacity = maxCapacity;
    }

    private void produce(int threadIndex){
        lock.lock();
        try{
            while (queue.size() == maxCapacity) {
                fullCondition.await();
            }
            queue.offer(data.getAndIncrement());
            System.out.println(("producer" + threadIndex + ": " + data.get() + ", queue size: " + queue.size()));
            emptyCondition.signal();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void consume(int threadIndex){
        lock.lock();
        try{
            while (queue.isEmpty()) {
                emptyCondition.await();
            }
            int tempData = queue.poll();
            System.out.println(("consumer" + threadIndex + ": " + tempData + ", queue size: " + queue.size()));
            fullCondition.signal();
        }catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally {
            lock.unlock();
        }
    }

    public void addProducer(int num){
        lock.lock();
        try{
            while (num > 0) {
                int curProducerIndex = producerNum.incrementAndGet();
                Thread producer = new Thread(() -> {
                    while (isRunning) {
                        try {
                            Thread.sleep(500);
                            produce(curProducerIndex);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }, "Producer-" + curProducerIndex);
                producerQueue.offer(producer);
                producer.start();
                System.out.println("已添加生产者: " + curProducerIndex);
                num--;
            }
        }finally {
            lock.unlock();
        }
    }

    public void addConsumer(int num){
        lock.lock();
        try{
            while (num > 0) {
                int curConsumerIndex = consumerNum.incrementAndGet();
                Thread consumer = new Thread(() -> {
                   while (isRunning || !queue.isEmpty()) {
                       try{
                           Thread.sleep(1000);
                           consume(curConsumerIndex);
                       }catch (InterruptedException e){
                           Thread.currentThread().interrupt();
                           return;
                       }
                   }
                }, "Consumer-" + curConsumerIndex);
                consumerQueue.offer(consumer);
                consumer.start();
                System.out.println("已添加消费者: " + curConsumerIndex);
                num--;
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeProducer(int num){
        lock.lock();
        try {
            while (num > 0) {
                Thread producer = producerQueue.poll();
                if (producer != null) {
                    producer.interrupt();
                    System.out.println("已移除生产者: " + producer.getName() + "剩余生产者数量: " + producerQueue.size());
                }
                num--;
            }
        }finally {
            lock.unlock();
        }
    }

    public void removeConsumer(int num){
        lock.lock();
        try {
            while (num > 0) {
                Thread consumer = consumerQueue.poll();
                if (consumer != null) {
                    consumer.interrupt();
                    System.out.println("已移除消费者: " + consumer.getName() + "剩余消费者数量: " + consumerQueue.size());
                }
                num--;
            }
        }finally {
            lock.unlock();
        }
    }

    public void start() {
        addProducer(M);
        addConsumer(N);
    }

    public void shutdown() {
        isRunning = false;
        for (Thread producer : producerQueue) {
            producer.interrupt();
        }
        for (Thread consumer : consumerQueue) {
            consumer.interrupt();
        }
        producerQueue.clear();
        consumerQueue.clear();
        System.out.println("系统运行结束");
    }

}
