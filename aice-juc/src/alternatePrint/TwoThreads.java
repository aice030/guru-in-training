package alternatePrint;

public class TwoThreads {
    private volatile int count = 1;
    private final Object lock = new Object();

    public TwoThreads() {}

    // 双线程交替打印
    public void printTwoNum(int n) {
        // 线程t2，打印偶数
        Thread t1 = new Thread(() -> {
            while (count <= n) {
                synchronized (lock) {
                    if (count % 2 == 1) {
                        System.out.println("odd thread: " + count++);
                        lock.notify();
                    }else {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });

        // 线程t2，打印偶数
        Thread t2 = new Thread(() -> {
            while (count <= n) {
                synchronized (lock) {
                    if (count % 2 == 0) {
                        System.out.println("even thread: " + count++);
                        lock.notify();
                    }else {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });
        t1.start();
        t2.start();
    }
}
