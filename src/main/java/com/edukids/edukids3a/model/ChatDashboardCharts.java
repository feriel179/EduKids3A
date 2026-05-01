package com.edukids.edukids3a.model;

import java.util.ArrayList;
import java.util.List;

public class ChatDashboardCharts {
    private List<ChatChartPoint> hourly = new ArrayList<>();
    private List<ChatChartPoint> daily = new ArrayList<>();
    private List<ChatChartPoint> activitySplit = new ArrayList<>();

    public List<ChatChartPoint> getHourly() {
        return hourly;
    }

    public void setHourly(List<ChatChartPoint> hourly) {
        this.hourly = hourly == null ? new ArrayList<>() : new ArrayList<>(hourly);
    }

    public List<ChatChartPoint> getDaily() {
        return daily;
    }

    public void setDaily(List<ChatChartPoint> daily) {
        this.daily = daily == null ? new ArrayList<>() : new ArrayList<>(daily);
    }

    public List<ChatChartPoint> getActivitySplit() {
        return activitySplit;
    }

    public void setActivitySplit(List<ChatChartPoint> activitySplit) {
        this.activitySplit = activitySplit == null ? new ArrayList<>() : new ArrayList<>(activitySplit);
    }
}
