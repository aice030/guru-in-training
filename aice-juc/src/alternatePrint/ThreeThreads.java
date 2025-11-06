package alternatePrint;

public class ThreeThreads {
    private volatile int flag = 1;
    private volatile int count = 1;
    private final Object lock = new Object();

    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private final Object lock3 = new Object();

    public ThreeThreads() {}

    public void printThreeNum(int n) {
        Thread t1 = new Thread(() -> {
            while (true) {
                synchronized (lock) {
                    // 先检查是否已超范围，超了就唤醒其他线程并退出
                    if (count > n) {
                        lock.notifyAll(); // 唤醒所有可能等待的线程
                        return;
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
                        return;
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
                        return;
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

    public void printThreeNumWithLocks(int n) {
        Thread t1 = new Thread(() -> {
            while (true) {
                synchronized (lock1) {
                    try{
                        if (count > n) {
                            synchronized (lock2) {
                                lock2.notify();
                            }
                            synchronized (lock3) {
                                lock3.notify();
                            }
                            return;
                        }
                        while (flag != 1) {
                            lock1.wait();
                            if (count > n) {
                                synchronized (lock2) {
                                    lock2.notify();
                                }
                                synchronized (lock3) {
                                    lock3.notify();
                                }
                                return;
                            }
                        }
                        System.out.println("thread1: " + flag);
                        count++;
                        flag = 2;
                        // 唤醒下一个线程
                        synchronized (lock2) {
                            lock2.notify();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        Thread t2 = new Thread(() -> {
            while (true) {
                synchronized (lock2) {
                    try{
                        if (count > n) {
                            synchronized (lock1) {
                                lock1.notify();
                            }
                            synchronized (lock3) {
                                lock3.notify();
                            }
                            return;
                        }
                        while (flag != 2) {
                            lock2.wait();
                            if (count > n) {
                                synchronized (lock1) {
                                    lock1.notify();
                                }
                                synchronized (lock3) {
                                    lock3.notify();
                                }
                                return;
                            }
                        }
                        System.out.println("thread2: " + flag);
                        count++;
                        flag = 3;
                        // 唤醒下一个线程
                        synchronized (lock3) {
                            lock3.notify();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        Thread t3 = new Thread(() -> {
            while (true) {
                synchronized (lock3) {
                    try{
                        if (count > n) {
                            synchronized (lock1) {
                                lock1.notify();
                            }
                            synchronized (lock2) {
                                lock2.notify();
                            }
                            return;
                        }
                        while (flag != 3) {
                            lock3.wait();
                            if (count > n) {
                                synchronized (lock1) {
                                    lock1.notify();
                                }
                                synchronized (lock2) {
                                    lock2.notify();
                                }
                                return;
                            }
                        }
                        System.out.println("thread1: " + flag);
                        count++;
                        flag = 1;
                        // 唤醒下一个线程
                        synchronized (lock1) {
                            lock1.notify();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        t1.start();
        t2.start();
        t3.start();
    }
}
