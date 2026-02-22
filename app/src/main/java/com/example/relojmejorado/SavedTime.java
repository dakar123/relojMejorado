package com.example.relojmejorado;

public class SavedTime {
    private String name;
    private long durationMs;
    private long savedAt;

    public SavedTime() {}

    public SavedTime(String name, long durationMs, long savedAt) {
        this.name = name;
        this.durationMs = durationMs;
        this.savedAt = savedAt;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public long getSavedAt() { return savedAt; }
    public void setSavedAt(long savedAt) { this.savedAt = savedAt; }

    public String getFormattedDuration() {
        long ms = durationMs;
        long hours   = ms / 3_600_000; ms %= 3_600_000;
        long minutes = ms / 60_000;    ms %= 60_000;
        long seconds = ms / 1_000;
        long centis  = (ms % 1_000) / 10;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centis);
        }
        return String.format("%02d:%02d.%02d", minutes, seconds, centis);
    }
}
