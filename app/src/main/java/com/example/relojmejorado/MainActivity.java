package com.example.relojmejorado;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_ACTIVE_FRAGMENT = "active_fragment_tag";

    private BottomNavigationView bottomNav;

    private Fragment clockFrag, stopwatchFrag, timerFrag, alarmFrag, activeFrag;
    private final String CLOCK_TAG = "clock";
    private final String STOPWATCH_TAG = "stopwatch";
    private final String TIMER_TAG = "timer";
    private final String ALARM_TAG = "alarm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK, false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            clockFrag = new ClockFragment();
            stopwatchFrag = new StopwatchFragment();
            timerFrag = new TimerFragment();
            alarmFrag = new AlarmFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, alarmFrag, ALARM_TAG).hide(alarmFrag)
                    .add(R.id.fragment_container, timerFrag, TIMER_TAG).hide(timerFrag)
                    .add(R.id.fragment_container, stopwatchFrag, STOPWATCH_TAG).hide(stopwatchFrag)
                    .add(R.id.fragment_container, clockFrag, CLOCK_TAG)
                    .commit();
            activeFrag = clockFrag;
            bottomNav.setSelectedItemId(R.id.nav_clock);
        } else {
            clockFrag = fm.findFragmentByTag(CLOCK_TAG);
            stopwatchFrag = fm.findFragmentByTag(STOPWATCH_TAG);
            timerFrag = fm.findFragmentByTag(TIMER_TAG);
            alarmFrag = fm.findFragmentByTag(ALARM_TAG);
            String activeTag = savedInstanceState.getString(KEY_ACTIVE_FRAGMENT);
            activeFrag = fm.findFragmentByTag(activeTag);

            int selectedItemId = R.id.nav_clock;
            if (activeTag != null) {
                if (activeTag.equals(STOPWATCH_TAG)) selectedItemId = R.id.nav_stopwatch;
                else if (activeTag.equals(TIMER_TAG)) selectedItemId = R.id.nav_timer;
                else if (activeTag.equals(ALARM_TAG)) selectedItemId = R.id.nav_alarm;
            }
            bottomNav.setSelectedItemId(selectedItemId);
        }

        setupBottomNav();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activeFrag != null) {
            outState.putString(KEY_ACTIVE_FRAGMENT, activeFrag.getTag());
        }
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment next = null;

            if (itemId == R.id.nav_clock) next = clockFrag;
            else if (itemId == R.id.nav_stopwatch) next = stopwatchFrag;
            else if (itemId == R.id.nav_timer) next = timerFrag;
            else if (itemId == R.id.nav_alarm) next = alarmFrag;

            if (next != null && next != activeFrag) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                if (activeFrag != null) ft.hide(activeFrag);
                ft.show(next).commit();
                activeFrag = next;
            }
            return true;
        });
    }

    public void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK, false);
        prefs.edit().putBoolean(KEY_DARK, !isDarkMode).apply();
        AppCompatDelegate.setDefaultNightMode(!isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public boolean isDarkMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK, false);
    }
}
