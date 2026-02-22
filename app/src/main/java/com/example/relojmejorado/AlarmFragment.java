package com.example.relojmejorado;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojmejorado.databinding.DialogAlarmLabelBinding;
import com.example.relojmejorado.databinding.FragmentAlarmBinding;
import com.example.relojmejorado.databinding.ItemAlarmBinding;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmFragment extends Fragment {

    private static final String PREFS_ALARM = "alarm_prefs";
    private static final String KEY_ALARMS = "alarms";

    private FragmentAlarmBinding binding;

    private List<Alarm> alarms = new ArrayList<>();
    private AlarmAdapter adapter;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlarmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadAlarms();
        setupRV();
        binding.fabAddAlarm.setOnClickListener(v -> showTimePicker());
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(now.get(Calendar.HOUR_OF_DAY))
                .setMinute(now.get(Calendar.MINUTE))
                .setTitleText("Seleccionar hora de la alarma")
                .build();
        
        picker.addOnPositiveButtonClickListener(v -> {
            showLabelDialog(picker.getHour(), picker.getMinute());
        });

        picker.show(getChildFragmentManager(), "time_picker");
    }

    private void showLabelDialog(int hour, int minute) {
        DialogAlarmLabelBinding dialogBinding = DialogAlarmLabelBinding.inflate(LayoutInflater.from(requireContext()));
        new AlertDialog.Builder(requireContext())
                .setTitle("Nombre de la alarma")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Agregar", (d, w) -> {
                    String label = dialogBinding.etAlarmLabel.getText().toString().trim();
                    if (label.isEmpty()) label = "Alarma";
                    addAlarm(hour, minute, label);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addAlarm(int hour, int minute, String label) {
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        Alarm a = new Alarm(id, hour, minute, label);
        alarms.add(a);
        scheduleAlarm(a);
        persistAlarms();
        adapter.notifyItemInserted(alarms.size() - 1);
        Toast.makeText(requireContext(), "Alarma: " + a.getFormattedTime(), Toast.LENGTH_SHORT).show();
    }

    private void scheduleAlarm(Alarm alarm) {
        if (!alarm.isEnabled()) return;
        AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildPI(alarm);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        cal.set(Calendar.MINUTE, alarm.getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_YEAR, 1);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am != null && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } catch (SecurityException e) {
            if (am != null) {
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }

    private void cancelAlarm(Alarm alarm) {
        AlarmManager am = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(buildPI(alarm));
        }
    }

    private PendingIntent buildPI(Alarm alarm) {
        Intent intent = new Intent(requireContext(), AlarmReceiver.class);
        intent.setAction("com.example.relojmejorado.ALARM_ACTION");
        intent.putExtra(AlarmReceiver.EXTRA_LABEL, alarm.getLabel());
        intent.putExtra(AlarmReceiver.EXTRA_ID, alarm.getId());
        return PendingIntent.getBroadcast(requireContext(), alarm.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void setupRV() {
        adapter = new AlarmAdapter(alarms, pos -> {
            Alarm a = alarms.get(pos);
            a.setEnabled(!a.isEnabled());
            if (a.isEnabled()) scheduleAlarm(a); else cancelAlarm(a);
            persistAlarms();
            adapter.notifyItemChanged(pos);
        }, pos -> new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar alarma")
                .setMessage("¿Eliminar \"" + alarms.get(pos).getLabel() + "\"?")
                .setPositiveButton("Sí", (d, w) -> {
                    cancelAlarm(alarms.get(pos));
                    alarms.remove(pos);
                    persistAlarms();
                    adapter.notifyItemRemoved(pos);
                })
                .setNegativeButton("No", null).show());
        binding.rvAlarms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAlarms.setAdapter(adapter);
    }

    private void loadAlarms() {
        SharedPreferences p = requireContext().getSharedPreferences(PREFS_ALARM, Context.MODE_PRIVATE);
        String json = p.getString(KEY_ALARMS, null);
        if (json != null) {
            Type t = new TypeToken<List<Alarm>>() {}.getType();
            List<Alarm> loaded = gson.fromJson(json, t);
            if (loaded != null) alarms = loaded;
        }
    }

    private void persistAlarms() {
        requireContext().getSharedPreferences(PREFS_ALARM, Context.MODE_PRIVATE)
                .edit().putString(KEY_ALARMS, gson.toJson(alarms)).apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Corrected Inner Adapter
    private static class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.VH> {
        interface OnToggle { void onToggle(int pos); }
        interface OnDelete { void onDelete(int pos); }

        private final List<Alarm> items;
        private final OnToggle toggle;
        private final OnDelete delete;

        AlarmAdapter(List<Alarm> items, OnToggle toggle, OnDelete delete) {
            this.items = items;
            this.toggle = toggle;
            this.delete = delete;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAlarmBinding itemBinding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Alarm a = items.get(position);
            holder.binding.tvAlarmTime.setText(a.getFormattedTime());
            holder.binding.tvAlarmLabel.setText(a.getLabel());
            holder.binding.switchAlarm.setOnCheckedChangeListener(null);
            holder.binding.switchAlarm.setChecked(a.isEnabled());
            holder.binding.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    toggle.onToggle(holder.getBindingAdapterPosition());
                }
            });
            holder.binding.btnDeleteAlarm.setOnClickListener(v -> {
                 if(holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    delete.onDelete(holder.getBindingAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ItemAlarmBinding binding;
            VH(@NonNull ItemAlarmBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
