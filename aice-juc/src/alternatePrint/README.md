# TwoThreads: 两线程交替打印奇数和偶数

## 1. synchronized锁住的对象

在TwoThreads类中创建一个lock对象，`synchronized(lock)` 锁住的是该类的某一个实例上的这把锁；如果创建了多个 TwoThreads 实例且每个实例各自持有不同的 `lock`，实例之间互不影响。
如果是 `synchronized(TwoThreads.class)`，则锁住的是类对象本身（类级别锁），所有实例都会竞争同一把锁。
常见做法：
- 使用同一个共享的 `lock` 对象（可为 `static final Object lock`），以保证多个线程间的互斥；
- 避免对可变对象或外部可见对象加锁，优先选择私有、不可变的锁对象以减少意外竞争。

## 2. start()和run()的区别

简单来说：`start()` 是“启动线程的开关”，负责创建新线程；`run()` 是“线程的任务内容”，本身不涉及线程的创建。要实现多线程，必须调用 `start()` 而非直接调用 `run()`。

### start() 方法：启动新线程，触发多线程执行
- 作用：启动一个新的线程（进入“就绪状态”），由 JVM 自动调用该线程的 `run()` 方法，实现真正的并发执行。
- 原理：调用 `start()` 后，JVM 会为线程分配独立的栈空间、程序计数器等资源，使其与其他线程并行执行。
- 限制：同一线程对象只能调用一次 `start()`，重复调用会抛出 `IllegalThreadStateException`。

### run() 方法：线程的执行体，仅普通方法调用
- 作用：定义线程要执行的任务逻辑（线程的“执行体”），但不会启动新线程，本质是一个普通成员方法。
- 原理：直接调用 `run()` 时，代码在当前调用者线程中执行，不会产生并发。
- 特点：可以重复调用（对象存在即可），因为它不改变线程的生命周期状态。


# ThreeThreads: 三线程交替打印 1、2、3
## 1. printThreeNum()方法
### 功能
使用 `notifyAll()` 唤醒等待的线程，通过 `while (flag != k)` 的条件循环确保只有轮到自身的线程才继续执行，从而实现交替打印。
### 问题
1. 打印结束后程序仍在运行 
原因：当 `count` 达到 `n` 退出循环后，其他线程可能仍在 `while (flag != k)` 的等待中，未被正确唤醒。
解决：在设置“完成”条件后，退出前先 `notifyAll()` 唤醒所有等待线程，确保它们能够检测到完成条件并安全退出。
2. 打印数量和预期不符
原因：当 `while (count <= n)` 作为外层条件时，可能存在线程已越过该判断、却在内层 `while (flag != k)` 中等待，唤醒后状态不一致导致越界或重复打印。
解决：将外层循环改为 `while (true)`，在临界区内统一判断“是否完成”，并在每次被唤醒时立即复查完成条件（防止虚假唤醒和状态漂移），确保 `count` 与 `flag` 的一致性。

## 2. printThreeNumWithLocks()方法
### return的作用域
线程体中的 `return` 终结的是“当前线程”的执行，不会终止整个 `printThreeNumWithLocks()` 方法中其他线程的执行逻辑。
### 为什么唤醒时要加锁synchronized(lock1){lock1.notify()}
唤醒其他线程（调用 `notify()` / `notifyAll()`）必须在同步块（`synchronized`）中进行。
#### 原因
Java 中，`wait()`、`notify()`、`notifyAll()` 的调用前提是：当前线程必须持有目标对象的监视器锁。只有持锁线程才有权操作该对象的等待队列；否则将抛出 `IllegalMonitorStateException`。
因此，`notify()`/`notifyAll()` 必须写在 `synchronized (lock)` 保护的临界区内。

# NThreads: 泛化，n 个线程打印 m 个数字
## 多线程交替打印核心步骤（模板）

### 通用模板（可复制）
```java
// 共享状态（根据你的工程实际封装到类中）
final Object lock = new Object();
volatile int nextId = 0;   // 轮到哪个线程（0..n-1）
volatile int count = 1;    // 已打印到的数字

void worker(int myId, int n, int m) {
    while (true) {
        synchronized (lock) {
            // 1) 完成条件统一判断（进入与被唤醒后都要检查）
            if (count > m) {
                lock.notifyAll();
                return; // 结束当前线程
            }

            // 2) 轮转判断：不该我执行就等待（用 while 应对虚假唤醒）
            while (nextId != myId) {
                try { lock.wait(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return; // 响应中断并退出
                }
                if (count > m) { // 被唤醒后立即复查完成条件
                    lock.notifyAll();
                    return;
                }
            }

            // 3) 轮到我：执行打印（业务逻辑放这里）
            // System.out.println(count);
            count++;

            // 4) 更新轮转标识并唤醒下一位（与打印处于同一临界区）
            nextId = (nextId + 1) % n;
            lock.notifyAll();
        }
    }
}
```

### 流程要点清单
- 使用 `while` + `wait()` 防止虚假唤醒导致越界/错序。
- 完成条件在临界区内统一判断，并在被唤醒后立刻复查。
- 打印与 `nextId` 更新在同一临界区，保证状态一致性。
- 退出前执行 `notifyAll()`，避免其他线程永久等待。
- 正确处理线程中断：`catch InterruptedException` 后设置中断标记并退出。

### 线程创建示例（可按需调整）
```java
int n = 3;       // 线程数量
int m = 100;     // 打印上限

for (int i = 0; i < n; i++) {
    final int id = i;
    new Thread(() -> worker(id, n, m)).start();
}
```
