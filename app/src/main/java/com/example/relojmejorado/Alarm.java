package com.example.relojmejorado;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Alarm {
    private int id;
    private int hour;
    private int minute;
    private boolean enabled;
    private String label;

    public Alarm() {}

    public Alarm(int id, int hour, int minute, String label) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.enabled = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getLabel() { return (label != null && !label.isEmpty()) ? label : "Alarma"; }
    public void setLabel(String label) { this.label = label; }

    public String getFormattedTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        return sdf.format(cal.getTime());
    }
}
