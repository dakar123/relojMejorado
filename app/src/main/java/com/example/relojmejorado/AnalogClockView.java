package com.example.relojmejorado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class AnalogClockView extends View {

    public enum Mode { CLOCK, STOPWATCH }

    private Mode mode = Mode.CLOCK;
    private boolean isDarkMode = false;

    private final Paint paintFace   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintHour   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintMin    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSec    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTick   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintNumber = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerDot    = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float drawnHours, drawnMinutes, drawnSeconds;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            if (mode == Mode.CLOCK) {
                Calendar cal = Calendar.getInstance();
                drawnSeconds = cal.get(Calendar.SECOND);
                drawnMinutes = cal.get(Calendar.MINUTE) + drawnSeconds / 60f;
                drawnHours   = cal.get(Calendar.HOUR) + drawnMinutes / 60f;
                invalidate();
                handler.postDelayed(this, 500);
            }
        }
    };

    public AnalogClockView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(6f);

        paintHour.setStyle(Paint.Style.STROKE);
        paintHour.setStrokeCap(Paint.Cap.ROUND);
        paintHour.setStrokeWidth(10f);

        paintMin.setStyle(Paint.Style.STROKE);
        paintMin.setStrokeCap(Paint.Cap.ROUND);
        paintMin.setStrokeWidth(7f);

        paintSec.setStyle(Paint.Style.STROKE);
        paintSec.setStrokeCap(Paint.Cap.ROUND);
        paintSec.setStrokeWidth(3f);

        paintCenter.setStyle(Paint.Style.FILL);

        paintTick.setStyle(Paint.Style.STROKE);
        paintTick.setStrokeCap(Paint.Cap.ROUND);

        paintNumber.setTextAlign(Paint.Align.CENTER);
        paintNumber.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        applyColors();
    }

    public void setDarkMode(boolean dark) {
        isDarkMode = dark;
        applyColors();
        invalidate();
    }

    private void applyColors() {
        int faceColor   = isDarkMode ? ContextCompat.getColor(getContext(), R.color.clock_face_dark) : Color.WHITE;
        int borderColor = ContextCompat.getColor(getContext(), R.color.ios_blue);
        int handColor   = isDarkMode ? Color.WHITE : ContextCompat.getColor(getContext(), R.color.clock_hands_light);
        int tickColor   = isDarkMode ? ContextCompat.getColor(getContext(), R.color.clock_ticks_dark) : ContextCompat.getColor(getContext(), R.color.clock_ticks_light);
        int numColor    = isDarkMode ? ContextCompat.getColor(getContext(), R.color.clock_numbers_dark) : ContextCompat.getColor(getContext(), R.color.clock_numbers_light);

        paintFace.setColor(faceColor);
        paintBorder.setColor(borderColor);
        paintHour.setColor(handColor);
        paintMin.setColor(handColor);
        paintSec.setColor(ContextCompat.getColor(getContext(), R.color.ios_red));
        paintCenter.setColor(ContextCompat.getColor(getContext(), R.color.ios_red));
        paintTick.setColor(tickColor);
        paintNumber.setColor(numColor);
    }

    public void startClock() {
        mode = Mode.CLOCK;
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    public void stopClock() {
        handler.removeCallbacks(clockTick);
    }

    public void setStopwatchTime(long elapsedMs) {
        mode = Mode.STOPWATCH;
        float totalSec = elapsedMs / 1000f;
        drawnSeconds = totalSec % 60f;
        drawnMinutes = (totalSec / 60f) % 60f;
        drawnHours   = (totalSec / 3600f) % 12f;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w  = getWidth();
        int h  = getHeight();
        if (w == 0 || h == 0) return;
        
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - paintBorder.getStrokeWidth();

        // Face & border
        canvas.drawCircle(cx, cy, radius, paintFace);
        canvas.drawCircle(cx, cy, radius, paintBorder);

        // Tick marks & hour numbers
        paintNumber.setTextSize(radius * 0.13f);
        int[] nums = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6.0 - 90);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            if (i % 5 == 0) {
                paintTick.setStrokeWidth(3f);
                canvas.drawLine(cx + cos * radius * 0.82f, cy + sin * radius * 0.82f,
                        cx + cos * radius * 0.95f, cy + sin * radius * 0.95f, paintTick);
                float nx = cx + cos * radius * 0.70f;
                float ny = cy + sin * radius * 0.70f + paintNumber.getTextSize() / 3f;
                canvas.drawText(String.valueOf(nums[i / 5]), nx, ny, paintNumber);
            } else {
                paintTick.setStrokeWidth(1.5f);
                canvas.drawLine(cx + cos * radius * 0.90f, cy + sin * radius * 0.90f,
                        cx + cos * radius * 0.95f, cy + sin * radius * 0.95f, paintTick);
            }
        }

        // Hour hand
        float ha = (float) Math.toRadians(drawnHours * 30f - 90f);
        canvas.drawLine(cx - (float) Math.cos(ha) * radius * 0.12f,
                cy - (float) Math.sin(ha) * radius * 0.12f,
                cx + (float) Math.cos(ha) * radius * 0.50f,
                cy + (float) Math.sin(ha) * radius * 0.50f, paintHour);

        // Minute hand
        float ma = (float) Math.toRadians(drawnMinutes * 6f - 90f);
        canvas.drawLine(cx - (float) Math.cos(ma) * radius * 0.15f,
                cy - (float) Math.sin(ma) * radius * 0.15f,
                cx + (float) Math.cos(ma) * radius * 0.70f,
                cy + (float) Math.sin(ma) * radius * 0.70f, paintMin);

        // Second hand
        float sa = (float) Math.toRadians(drawnSeconds * 6f - 90f);
        canvas.drawLine(cx - (float) Math.cos(sa) * radius * 0.20f,
                cy - (float) Math.sin(sa) * radius * 0.20f,
                cx + (float) Math.cos(sa) * radius * 0.82f,
                cy + (float) Math.sin(sa) * radius * 0.82f, paintSec);

        // Center dot
        canvas.drawCircle(cx, cy, radius * 0.045f, paintCenter);
        innerDot.setColor(paintFace.getColor());
        canvas.drawCircle(cx, cy, radius * 0.022f, innerDot);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopClock();
    }
}
