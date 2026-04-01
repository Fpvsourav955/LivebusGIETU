package com.sourav.livebusgietu;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BusStop {
    private String name;
    private boolean isReached;
    private Long estimatedTime;
    private double estInMinutes;
    private String displayTime;

    public BusStop(String name, boolean isReached, double estInMinutes) {
        this.name = name;
        this.isReached = isReached;
        this.estInMinutes = estInMinutes;
    }

    public String getName() {
        return name;
    }

    public boolean isReached() {
        return isReached;
    }

    public Long getEstimatedTime() {
        return estimatedTime;
    }

    public String getDisplayTime() {
        return displayTime;
    }

    public double getEstInMinutes() {
        return estInMinutes;
    }

    public void setEstimatedTime(Long time) {
        this.estimatedTime = time;
    }

    public void setDisplayTime(String displayTime) {
        this.displayTime = displayTime;
    }

    public void calculateETA(long startTimeMillis) {
        if (startTimeMillis > 0) {
            long eta = startTimeMillis + (long)(estInMinutes * 60 * 1000);
            this.estimatedTime = eta;
            this.displayTime = formatTime(eta);
        }
    }

    public void adjustETA(long deltaMillis) {
        if (estimatedTime != null) {
            estimatedTime += deltaMillis;
            displayTime = formatTime(estimatedTime);
        }
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(millis));
    }
}
