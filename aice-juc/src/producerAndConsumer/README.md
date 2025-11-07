# producerAndConsumer：生产者-消费者模型

## Single（基础版：单生产者 + 单消费者）

### 1. 最大容量
设置 `maxCapacity` 限制队列最大长度，防止任务堆积导致 OOM。

### 2. 终止条件
- 生产者退出：依据 `isRunning`。
- 消费者退出：需满足 `!isRunning && queue.isEmpty()`，确保不丢数据（消费完队列中剩余元素）。

### 3. Lock 与 Condition
- `Lock` 是 `Condition` 的前提：调用 `await()`/`signal()` 前必须已持有对应的 `Lock`，否则抛出 `IllegalMonitorStateException`。
- `Condition` 负责等待/唤醒，不提供原子性：如“判断队满（`queue.size()==maxCapacity`）+ 入队（`queue.offer()`）”必须在同一把 `Lock` 的临界区内完成，才能构成原子操作。
- 职责类比：
  - `Lock`：守门人，保证同一时刻只有一个线程操作共享队列，避免数据竞争。
  - `Condition`：调度员，基于锁按“队列空/满”等条件精准地等待与唤醒。
- 结论：二者协同，既保障临界区原子性，又实现高效的线程协作。