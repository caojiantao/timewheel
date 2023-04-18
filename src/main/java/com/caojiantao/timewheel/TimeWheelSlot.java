package com.caojiantao.timewheel;

import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class TimeWheelSlot {

    /**
     * 线程安全
     */
    private Set<TimeWheelSlotTask> taskSet = ConcurrentHashMap.newKeySet();
}
