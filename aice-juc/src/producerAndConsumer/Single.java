package producerAndConsumer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * 单生产者、单消费者
 */
public class Single {
    private final Deque<Integer> queue = new ArrayDeque<>();
    // 限制最大容量，防止任务堆积造成OOM
    private final int maxCapacity;
    private volatile boolean isRunning = true;
    private final Lock lock = new ReentrantLock();
    // 队列不满的条件（生产者等待）
    private final Condition notFull = lock.newCondition();
    // 队列不空的条件（消费者等待）
    private final Condition notEmpty = lock.newCondition();

    public Single(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void produce(int data) {
        lock.lock();
        try{
            while (queue.size() == maxCapacity) {
                notFull.await();
            }
            queue.offer(data);
            System.out.println(("produce: " + data + ", queue size: " + queue.size()));
            notEmpty.signal();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public  void consume() {
        lock.lock();
        try{
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            int data = queue.poll();
            System.out.println(("consume: " + data + ", queue size: " + queue.size()));
            notFull.signal();
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }finally {
            lock.unlock();
        }
    }

    public void startRunning() {
        Thread producer = new Thread(() -> {
            int data = 1;
           while (isRunning) {
               try{
                   produce(data++);
                   TimeUnit.MILLISECONDS.sleep(500);
               } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
               }
           }
        });

        Thread consumer = new Thread(() -> {
            while (isRunning || !queue.isEmpty()) {
                try{
                    consume();
                    TimeUnit.MILLISECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        producer.start();
        consumer.start();
    }

    public  void stopRunning() {
        isRunning = false;
    }

}
