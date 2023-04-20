package com.caojiantao.timewheel;


import java.time.LocalDateTime;

public class MultiLevelTimeWheelDemo {

    public static void main(String[] args) {
        MultiLevelTimeWheel multiLevelTimeWheel = new MultiLevelTimeWheel(60, 1000, 60, 24);

        TimeWheelSlotTask task1 = new TimeWheelSlotTask();
        task1.setRunnable(() -> {
            System.out.println(LocalDateTime.now() + ": ================ task1 执行 ================");
        });
        task1.setDelay(32000L);

        TimeWheelSlotTask task2 = new TimeWheelSlotTask();
        task2.setRunnable(() -> {
            System.out.println(LocalDateTime.now() + ": ================ task2 执行 ================");
        });
        task2.setDelay(2000L);

        TimeWheelSlotTask task3 = new TimeWheelSlotTask();
        task3.setRunnable(() -> {
            System.out.println(LocalDateTime.now() + ": ================ task3 执行 ================");
        });
        task3.setDelay(5000L);

        TimeWheelSlotTask task4 = new TimeWheelSlotTask();
        task4.setRunnable(() -> {
            System.out.println(LocalDateTime.now() + ": ================ task4 执行 ================");
        });
        task4.setDelay(12000L);

        TimeWheelSlotTask task5 = new TimeWheelSlotTask();
        task5.setRunnable(() -> {
            System.out.println(LocalDateTime.now() + ": ================ task5 立即执行 ================");
        });
        task5.setDelay(120L);

        multiLevelTimeWheel.addTask(task1);
        multiLevelTimeWheel.addTask(task2);
        multiLevelTimeWheel.addTask(task3);
        multiLevelTimeWheel.addTask(task4);
        multiLevelTimeWheel.addTask(task5);

        System.out.println(LocalDateTime.now() + ": start=========================================");
        multiLevelTimeWheel.start();
    }
}
