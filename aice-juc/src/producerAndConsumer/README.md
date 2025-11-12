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

## Dynamic（进阶版：多生产者 + 多消费者，可动态扩缩容）

### 1. 锁设计
- `produce` / `consume` 以及 `addProducer` / `removeProducer` 等管理方法共用同一把 `lock`。这样可以确保对共享队列和线程池的所有修改都具备一致的内存可见性与原子性。
- 影响：扩容或缩容时会短暂阻塞生产/消费操作，但在典型场景下“调整线程数量”的频率远低于生产/消费频率，因此实现简单、状态一致性最好。
- 若需在扩缩容期间保持生产/消费不中断，可拆分锁（例如：队列锁 + 管理锁），但需要额外关注状态同步与竞态条件。

### 2. 异常处理策略
- 阻塞点（`await`、`sleep` 等）捕获 `InterruptedException` 时使用 `Thread.currentThread().interrupt()` 恢复中断标记，并尽快返回，从而允许上层控制逻辑根据中断信号做收尾处理（Dynamic 类当前采用的模式）。
- `throw new RuntimeException(e)`：在无法在当前层处理受检异常时，可将其转换并上抛，交给调用方统一处理。使用时务必保留原始异常 `e` 以便排查。
- `e.printStackTrace()`：仅适合临时调试。在线上环境应替换为日志记录（如 `log.error("...", e)`），避免异常被“打印即忽略”。
- 关键点：要么妥善记录并上抛，要么显式恢复中断状态；不要静默吞掉异常。

### 3. 原子变量与可见性
- `data`、`producerNum`、`consumerNum` 使用 `AtomicInteger`，保证递增操作的原子性与可见性，也让线程命名更直观。
- 等价实现：这类状态的修改都发生在 `lock` 的临界区内，即使改为普通 `int` 也能保证线程安全；使用 `AtomicInteger` 主要为了便捷地执行自增并保持数值有序。
- `isRunning` 标记为 `volatile`，确保线程在调整运行状态时能即时读取最新值，避免退出条件迟滞。
