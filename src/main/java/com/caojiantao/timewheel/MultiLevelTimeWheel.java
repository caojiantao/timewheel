package com.caojiantao.timewheel;

import java.util.Timer;
import java.util.TimerTask;

public class MultiLevelTimeWheel {

    private TimeWheel lowestTimeWheel;

    private Timer timer = new Timer();

    public MultiLevelTimeWheel(int slotNum, long tickDuration, int... higherSlotNumArray) {
        lowestTimeWheel = new TimeWheel(slotNum, tickDuration, 1);
        doHandleHigherTimeWheel(lowestTimeWheel, higherSlotNumArray);
    }

    private void doHandleHigherTimeWheel(TimeWheel lowestTimeWheel, int[] higherSlotNumArray) {
        if (higherSlotNumArray == null) {
            return;
        }
        TimeWheel timeWheel = lowestTimeWheel;
        for (int slotNum : higherSlotNumArray) {
            long tickDuration = timeWheel.getSlots().length * timeWheel.getTickMs();
            TimeWheel higherTimeWheel = new TimeWheel(slotNum, tickDuration, timeWheel.getLevel() + 1);
            higherTimeWheel.setLowerTimeWheel(timeWheel);
            timeWheel.setHigherTimeWheel(higherTimeWheel);
            timeWheel = higherTimeWheel;
        }
    }

    public void addTask(TimeWheelSlotTask task) {
        TimeWheel timeWheel = lowestTimeWheel;
        while (timeWheel != null) {
            if (task.getDelay() < timeWheel.getTickCycleMs()) {
                timeWheel.addTask(task);
                return;
            }
            timeWheel = timeWheel.getHigherTimeWheel();
        }
        throw new RuntimeException("提交的延时任务超过了最大延时时间");
    }

    public void start() {
        long tickDuration = lowestTimeWheel.getTickMs();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                lowestTimeWheel.tick();
            }
        }, tickDuration, tickDuration);
    }
}
