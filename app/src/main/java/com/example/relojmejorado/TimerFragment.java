package com.example.relojmejorado;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.relojmejorado.databinding.FragmentTimerBinding;

public class TimerFragment extends Fragment {

    private static final String TIMER_CHANNEL = "timer_channel";

    private FragmentTimerBinding binding;

    private CountDownTimer countDown;
    private long totalMs = 0;
    private long remainingMs = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTimerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupPickers();
        createNotificationChannel();

        binding.btnTimerStartPause.setOnClickListener(v -> {
            if (!isRunning && !isPaused) startTimer();
            else if (isRunning) pauseTimer();
            else resumeTimer();
        });
        binding.btnTimerReset.setOnClickListener(v -> resetTimer());
    }

    private void setupPickers() {
        binding.npHours.setMinValue(0);
        binding.npHours.setMaxValue(23);
        binding.npHours.setFormatter(v -> String.format("%02d", v));
        binding.npMinutes.setMinValue(0);
        binding.npMinutes.setMaxValue(59);
        binding.npMinutes.setFormatter(v -> String.format("%02d", v));
        binding.npSeconds.setMinValue(0);
        binding.npSeconds.setMaxValue(59);
        binding.npSeconds.setFormatter(v -> String.format("%02d", v));
    }

    private void startTimer() {
        long h = binding.npHours.getValue();
        long m = binding.npMinutes.getValue();
        long s = binding.npSeconds.getValue();
        totalMs = (h * 3600L + m * 60L + s) * 1000L;
        if (totalMs == 0) return;

        remainingMs = totalMs;
        isRunning = true;
        isPaused = false;

        binding.cardPickers.setVisibility(View.GONE);
        binding.tvTimerDisplay.setVisibility(View.VISIBLE);
        binding.btnTimerStartPause.setText("Pausar");
        binding.btnTimerStartPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ios_orange));

        launchCountDown(remainingMs);
    }

    private void pauseTimer() {
        if (countDown != null) countDown.cancel();
        isRunning = false;
        isPaused = true;
        binding.btnTimerStartPause.setText("Continuar");
        binding.btnTimerStartPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ios_green));
    }

    private void resumeTimer() {
        isRunning = true;
        isPaused = false;
        binding.btnTimerStartPause.setText("Pausar");
        binding.btnTimerStartPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ios_orange));
        launchCountDown(remainingMs);
    }

    private void resetTimer() {
        if (countDown != null) countDown.cancel();
        isRunning = false;
        isPaused = false;
        remainingMs = 0;
        binding.cardPickers.setVisibility(View.VISIBLE);
        binding.tvTimerDisplay.setVisibility(View.GONE);
        binding.tvTimerDisplay.setTextColor(ContextCompat.getColor(requireContext(), R.color.ios_green));
        binding.btnTimerStartPause.setText("Iniciar");
        binding.btnTimerStartPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ios_blue));
    }

    private void launchCountDown(long ms) {
        countDown = new CountDownTimer(ms, 100) {
            @Override
            public void onTick(long left) {
                if (binding == null) return;
                remainingMs = left;
                binding.tvTimerDisplay.setText(fmtTimer(left));
                float ratio = (float) left / totalMs;
                int color;
                if (ratio > 0.5f) color = R.color.ios_green;
                else if (ratio > 0.2f) color = R.color.ios_orange;
                else color = R.color.ios_red;
                binding.tvTimerDisplay.setTextColor(ContextCompat.getColor(requireContext(), color));
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
                remainingMs = 0;
                isRunning = false;
                binding.tvTimerDisplay.setText("00:00:00");
                binding.tvTimerDisplay.setTextColor(ContextCompat.getColor(requireContext(), R.color.ios_red));
                onTimerDone();
            }
        };
        countDown.start();
    }

    private void onTimerDone() {
        if (getContext() == null) return;
        vibrateFinish();
        sendNotification();
        if (binding != null) {
            binding.btnTimerStartPause.setText("Iniciar");
            binding.btnTimerStartPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.ios_blue));
        }
    }

    private String fmtTimer(long ms) {
        long h = ms / 3_600_000; ms %= 3_600_000;
        long m = ms / 60_000; ms %= 60_000;
        long s = ms / 1_000;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    TIMER_CHANNEL, "Temporizador", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void sendNotification() {
        Intent i = new Intent(requireContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(requireContext(), 0, i, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(requireContext(), TIMER_CHANNEL)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("⏱ ¡Tiempo!")
                .setContentText("Tu temporizador ha finalizado")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(2001, b.build());
    }

    private void vibrateFinish() {
        Vibrator v = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null) return;
        long[] pat = {0, 400, 200, 400, 200, 400};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createWaveform(pat, -1));
        else
            v.vibrate(pat, -1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDown != null) countDown.cancel();
        binding = null;
    }
}
