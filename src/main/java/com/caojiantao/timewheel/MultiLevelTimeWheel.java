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
            long tickDuration = timeWheel.getSlots().length * timeWheel.getTickDuration();
            TimeWheel higherTimeWheel = new TimeWheel(slotNum, tickDuration, timeWheel.getLevel() + 1);
            higherTimeWheel.setLowerTimeWheel(timeWheel);
            timeWheel.setHigherTimeWheel(higherTimeWheel);
            timeWheel = higherTimeWheel;
        }
    }

    public void addTask(TimeWheelSlotTask task) {
        TimeWheel timeWheel = lowestTimeWheel;
        while (timeWheel != null) {
            long delayLimit = timeWheel.getSlots().length * timeWheel.getTickDuration();
            if (task.getDelay() < delayLimit) {
                timeWheel.addTask(task);
                return;
            }
            timeWheel = timeWheel.getHigherTimeWheel();
        }
        throw new IllegalArgumentException();
    }

    public void start() {
        long tickDuration = lowestTimeWheel.getTickDuration();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                lowestTimeWheel.tick();
            }
        }, tickDuration, tickDuration);
    }
}
