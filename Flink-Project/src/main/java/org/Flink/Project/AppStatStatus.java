package com.sling.dacoe.schema;

public class AppStatStatus {

    private int status;
    private int zeroEventsCount;
    private boolean emittedAttempt;
    private boolean emittedEnded;
    private boolean emittedStarted;
    private long previousLastModified;

    public AppStatStatus() {
        this.zeroEventsCount = 0;
        this.status = -1;
    }

    public boolean isEmittedStarted() {
        return emittedStarted;
    }

    public void setEmittedStarted(boolean emittedStarted) {
        this.emittedStarted = emittedStarted;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getZeroEventsCount() {
        return zeroEventsCount;
    }

    public void setZeroEventsCount(int zeroEventsCount) {
        this.zeroEventsCount = zeroEventsCount;
    }

    public boolean isEmittedAttempt() {
        return emittedAttempt;
    }

    public void setEmittedAttempt(boolean emittedAttempt) {
        this.emittedAttempt = emittedAttempt;
    }

    public boolean isEmittedEnded() {
        return emittedEnded;
    }

    public void setEmittedEnded(boolean emittedEnded) {
        this.emittedEnded = emittedEnded;
    }

    public long getPreviousLastModified() {
        return previousLastModified;
    }

    public void setPreviousLastModified(long previousLastModified) {
        this.previousLastModified = previousLastModified;
    }
}
