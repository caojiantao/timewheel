package com.caojiantao.timewheel;

import lombok.Data;

@Data
public class TimeWheelSlotTask {

    private Runnable runnable;
    private Long delay;
}
