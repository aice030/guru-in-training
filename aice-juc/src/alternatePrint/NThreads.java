package alternatePrint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 泛化，n个线程交替打印m个数字
public class NThreads {
    // 通过Condition控制并发
    private final Lock lock = new ReentrantLock();
    private final List<Condition> conditionList = new ArrayList<Condition>();
    private volatile int curThreadIndex = 0;
    private volatile int count = 1;
    private final int m;
    private final int n;

    public NThreads(int m, int n){
        this.m = m;
        this.n = n;
    }

    // 用n个线程交替打印m个数字
    public void printMNumWithNThreads() {
        // 为每个线程添加对应的锁控制等待与唤醒
        for (int i = 0; i < n; i++) {
            conditionList.add(lock.newCondition());
        }
        start();
    }

    public void start() {
        for (int i = 0; i < n; i++) {
            int threadIndex = i;
            Thread t = new Thread(() -> {
                print(threadIndex);
            });
            t.start();
        }
    }

    public void print(int threadIndex){
        while (true) {
            lock.lock();
            try{
                if (count > m) {
                    // 若打印已经结束，唤醒所有线程，避免有线程一直等待
                    for (Condition c : conditionList) {
                        c.signal();
                    }
                    return;
                }
                // 如果不是目标index的线程，则等待
                while (threadIndex != curThreadIndex) {
                    try{
                        Condition condition = conditionList.get(threadIndex);
                        condition.await();
                        if (count > m) {
                            for (Condition c : conditionList) {
                                c.signal();
                            }
                            return;
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
                System.out.println("Thread" + (curThreadIndex + 1) + ": " + count);
                count++;
                curThreadIndex = (curThreadIndex + 1) % n;
                conditionList.get(curThreadIndex).signal();
            }finally {
                lock.unlock();
            }
        }
    }
}
