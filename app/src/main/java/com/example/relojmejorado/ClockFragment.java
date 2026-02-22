package com.example.relojmejorado;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.relojmejorado.databinding.FragmentClockBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockFragment extends Fragment {

    private FragmentClockBinding binding;

    private boolean showAnalog = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable digitalTick = new Runnable() {
        @Override public void run() {
            if (binding != null) {
                updateDigital();
                handler.postDelayed(this, 500);
            }
        }
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentClockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean dark = ((MainActivity) requireActivity()).isDarkMode();
        binding.analogClock.setDarkMode(dark);
        updateThemeIcon(dark);

        binding.btnToggleView.setOnClickListener(v -> toggleView());
        binding.btnToggleTheme.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleDarkMode();
            }
        });
    }

    private void toggleView() {
        showAnalog = !showAnalog;
        binding.analogClock.setVisibility(showAnalog ? View.VISIBLE : View.GONE);
        binding.tvDigitalTime.setVisibility(showAnalog ? View.GONE : View.VISIBLE);
        binding.btnToggleView.setImageResource(showAnalog ? R.drawable.ic_digital : R.drawable.ic_analog);
    }

    private void updateDigital() {
        binding.tvDigitalTime.setText(new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(new Date()));
        binding.tvDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date()));
    }

    private void updateThemeIcon(boolean dark) {
        binding.btnToggleTheme.setImageResource(dark ? R.drawable.ic_sun : R.drawable.ic_moon);
    }

    @Override public void onResume() {
        super.onResume();
        boolean dark = ((MainActivity) requireActivity()).isDarkMode();
        binding.analogClock.setDarkMode(dark);
        updateThemeIcon(dark);
        binding.analogClock.startClock();
        handler.post(digitalTick);
    }

    @Override public void onPause() {
        super.onPause();
        handler.removeCallbacks(digitalTick);
        binding.analogClock.stopClock();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
