package com.caojiantao.timewheel;

import lombok.Data;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Data
class TimeWheel {
    // 每个槽的时间间隔
    private long tickDuration;
    // 槽数组
    private TimeWheelSlot[] slots;
    // 当前槽指针
    private int currentSlot;
    // 当前级别
    private int level;
    // 低级别时间轮
    private TimeWheel lowerTimeWheel;
    // 高级别时间轮
    private TimeWheel higherTimeWheel;

    // 提交任务的线程池
    private static final ExecutorService scheduler = new ThreadPoolExecutor(64, 64, 1, TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(1024), (r, executor) -> {
        // 丢弃任务？
    });

    public TimeWheel(int slotNum, long tickDuration, int level) {
        this.tickDuration = tickDuration;
        this.slots = new TimeWheelSlot[slotNum];
        // slots 初始化
        for (int i = 0; i < slotNum; i++) {
            slots[i] = new TimeWheelSlot();
        }
        this.currentSlot = 0;
        this.level = level;
        this.lowerTimeWheel = null;
        this.higherTimeWheel = null;
    }

    public void addTask(TimeWheelSlotTask task) {
        long ticks = task.getDelay() % (slots.length * tickDuration) / tickDuration;
        int slotIndex = (int) ((currentSlot + ticks) % slots.length);
        TimeWheelSlot slot = slots[slotIndex];
        slot.getTaskSet().add(task);
    }

    public void tick() {
        currentSlot = (currentSlot + 1) % slots.length;
        System.out.println(LocalDateTime.now() + ": " + this);
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

    @Override
    public String toString() {
        String holder = "level:{0} slotNum:{1,number,#} tickDuration:{2,number,#} currentSlot:{3,number,#}";
        return MessageFormat.format(holder, level, slots.length, tickDuration, currentSlot);
    }
}
