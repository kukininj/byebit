package com.example.byebit.ui.home;

import androidx.work.WorkInfo;

import java.util.List;

public class Event {
    private final long timeMillis;
    private final List<WorkInfo> workInfos;

    public Event(long timeMillis, List<WorkInfo> workInfos) {
        this.timeMillis = timeMillis;
        this.workInfos = workInfos;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public List<WorkInfo> getWorkInfos() {
        return workInfos;
    }
}
