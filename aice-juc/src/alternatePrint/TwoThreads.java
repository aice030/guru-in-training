package alternatePrint;

/**
 * @author aice
 * 双线程交替打印
 */
public class TwoThreads {
    private volatile int count = 1;
    private final Object lock = new Object();

    public TwoThreads() {}

    // 双线程交替打印
    public void printTwoNum(int n) {
        // 线程t1，打印奇数
        Thread t1 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    try{
                        if (count > n) {
                            lock.notifyAll();
                            return;
                        }
                        while (count % 2 != 1) {
                            lock.wait();
                            if (count > n) {
                                lock.notifyAll();
                                return;
                            }
                        }
                        System.out.println("Tread1: " + count);
                        count++;
                        lock.notifyAll();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        // 线程t2，打印偶数
        Thread t2 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    try{
                        if (count > n) {
                            lock.notifyAll();
                            return;
                        }
                        while (count % 2 != 0) {
                            lock.wait();
                            if (count > n) {
                                lock.notifyAll();
                                return;
                            }
                        }
                        System.out.println("Tread2: " + count);
                        count++;
                        lock.notifyAll();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        t1.start();
        t2.start();
    }
}
