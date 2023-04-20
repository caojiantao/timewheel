## 业务背景

有很多业务场景需要定时或者延时执行某个任务，例如“每天 9 点给用户发送活动提醒”、“创建订单 30 分钟未支付自动取消”等。

简单点能用 JDK 中的 Timer 直接实现，由于单线程局限性可以升级为 ScheduledThreadPoolExecutor 进行任务调度。

企业级应用中还会依赖 quartz、xxl-job 等第三方框架来高效管理定时任务。

## 什么是时间轮

时间轮是一种实现高效定时器的数据结构。其原理与时钟类似，将时间划分成多个槽（slot），每个槽代表时间精度（tickMs）并维护一个定时任务列表。另外有一个时针指向当前槽（currentSlot），并以固定的速度移动（tick）。

当时针移动到某个槽时，就遍历当前槽内的任务，执行已到期的任务。通常任务列表是基于优先级队列实现的，按照任务的执行时间排序。

![](http://image.caojiantao.site:38080/af76574e-ab72-4dad-b1b5-7416d9836825.jpg)

相比传统的定时器，基于数组+优先级队列的时间轮在处理大量任务时显得非常高效。

## 多级时间轮

由于使用了优先级队列，所以单级时间轮在动态新增删除任务时的时间复杂度是 O(logn)。当然可以通过增加槽位数量达到 O(1) 但会浪费大量的空间。

可以用多个时间轮，低级时间轮走完一圈，高级时间轮走一个槽位。结合时钟的秒针、分针、时针就非常容易理解了。

![](http://image.caojiantao.site:38080/107da51d-c37d-4df0-9c52-8b3917c66e06.jpg)

这样做的好处能优化任务动态新增删除的时间复杂度是 O(1)，但是整个时间轮会有一个最大延时执行时间，例如这里的 1 天。

## Java 实践

[查看完整代码](https://github.com/caojiantao/timewheel)

```java
@Data
class TimeWheel {
    // 滴答精度
    private long tickMs;
    // 槽数组
    private TimeWheelSlot[] slots;
    // 当前槽指针
    private int currentSlot;
    // 低级别时间轮
    private TimeWheel lowerTimeWheel;
    // 高级别时间轮
    private TimeWheel higherTimeWheel;

    // 提交任务的线程池
    private static final ExecutorService scheduler = new ThreadPoolExecutor(64, 64, 1, TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(1024), (r, executor) -> {
        // 丢弃任务？
    });

    public TimeWheel(int slotNum, long tickMs, int level) {
        this.tickMs = tickMs;
        this.slots = new TimeWheelSlot[slotNum];
        // slots 初始化
        for (int i = 0; i < slotNum; i++) {
            slots[i] = new TimeWheelSlot();
        }
        this.currentSlot = 0;
        this.lowerTimeWheel = null;
        this.higherTimeWheel = null;
    }

    /**
     * 获取当前时间轮，tick 一圈的时间
     */
    public long getTickCycleMs() {
        return slots.length * tickMs;
    }

    /**
     * 向当前时间轮添加延时任务，会对 tickCycleMs 取余
     */
    public void addTask(TimeWheelSlotTask task) {
        long ticks = task.getDelay() % getTickCycleMs() / tickMs;
        int slotIndex = (int) ((currentSlot + ticks) % slots.length);
        if (slotIndex == currentSlot) {
            // 立即执行
            scheduler.execute(task.getRunnable());
            return;
        }
        TimeWheelSlot slot = slots[slotIndex];
        slot.getTaskSet().add(task);
    }

    /**
     * 滴答前进
     */
    public void tick() {
        currentSlot = (currentSlot + 1) % slots.length;
        TimeWheelSlot slot = slots[currentSlot];
        // 如果走完一圈切高级轮存在，触发高级轮的 tick
        if (currentSlot == 0 && higherTimeWheel != null) {
            higherTimeWheel.tick();
        }
        // 如果有低级轮，需要将当前槽的任务转移到低级
        if (lowerTimeWheel != null) {
            for (TimeWheelSlotTask task : slot.getTaskSet()) {
                lowerTimeWheel.addTask(task);
            }
            slot.getTaskSet().clear();
            return;
        }
        // 已经是最低级，直接处理完当前槽内任务
        for (TimeWheelSlotTask task : slot.getTaskSet()) {
            scheduler.execute(task.getRunnable());
        }
        // 清空
        slot.getTaskSet().clear();
    }
}
```
