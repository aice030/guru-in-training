package alternatePrint;

public class ThreeNum {
    private volatile int flag = 1;
    private volatile int count = 1;
    private final Object lock = new Object();

    public  ThreeNum() {}

    public void printThreeNum(int n) {
        Thread t1 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    // 先检查是否已超范围，超了就唤醒其他线程并退出
                    if (count > n) {
                        lock.notifyAll(); // 唤醒所有可能等待的线程
                        break;
                    }
                    while (flag != 1) {
                        try {
                            lock.wait();
                            if (count > n) {
                                lock.notifyAll();
                                return;
                            }
                        }catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("thread1: " + flag);
                    count++;
                    flag = 2;
                    lock.notifyAll();
                }
            }
        });
        Thread t2 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    // 先检查是否已超范围，超了就唤醒其他线程并退出
                    if (count > n) {
                        lock.notifyAll(); // 唤醒所有可能等待的线程
                        break;
                    }
                    while (flag != 2) {
                        try {
                            lock.wait();
                            // 被唤醒后再次检查是否已结束
                            if (count > n) {
                                lock.notifyAll();
                                return;
                            }
                        }catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("thread2: " + flag);
                    count++;
                    flag = 3;
                    lock.notifyAll();
                }
            }
        });
        Thread t3 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    // 先检查是否已超范围，超了就唤醒其他线程并退出
                    if (count > n) {
                        lock.notifyAll(); // 唤醒所有可能等待的线程
                        break;
                    }
                    while (flag != 3) {
                        try {
                            lock.wait();
                            if (count > n) {
                                lock.notifyAll();
                                return;
                            }
                        }catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println("thread3: " + flag);
                    count++;
                    flag = 1;
                    lock.notifyAll();
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
    }
}
