package com.example.relojmejorado;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relojmejorado.databinding.DialogSaveTimeBinding;
import com.example.relojmejorado.databinding.FragmentStopwatchBinding;
import com.example.relojmejorado.databinding.ItemSavedTimeBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StopwatchFragment extends Fragment {

    private static final String PREFS_SW = "sw_prefs";
    private static final String KEY_TIMES = "saved_times";

    private FragmentStopwatchBinding binding;

    private boolean showAnalog = true;
    private boolean isRunning = false;
    private boolean sortDesc = true; // longest first

    private long elapsedMs = 0;
    private long startTime = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (isRunning && binding != null) {
                elapsedMs = System.currentTimeMillis() - startTime;
                updateDisplay();
                handler.postDelayed(this, 30);
            }
        }
    };

    private List<SavedTime> savedList = new ArrayList<>();
    private SavedAdapter adapter;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStopwatchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.analogStopwatch.setDarkMode(((MainActivity) requireActivity()).isDarkMode());

        loadSaved();
        setupRecyclerView();
        setupButtons();
        updateDisplay();

        binding.btnSave.setVisibility(View.GONE);
        binding.btnReset.setEnabled(false);
    }

    private void setupButtons() {
        binding.btnStartStop.setOnClickListener(v -> {
            if (isRunning) pauseSW();
            else startSW();
        });
        binding.btnReset.setOnClickListener(v -> resetSW());
        binding.btnSave.setOnClickListener(v -> showSaveDialog());
        binding.btnToggleViewSw.setOnClickListener(v -> {
            showAnalog = !showAnalog;
            binding.analogStopwatch.setVisibility(showAnalog ? View.VISIBLE : View.GONE);
            binding.tvDigitalStopwatch.setVisibility(showAnalog ? View.GONE : View.VISIBLE);
            binding.btnToggleViewSw.setImageResource(showAnalog ? R.drawable.ic_digital : R.drawable.ic_analog);
        });
        binding.btnToggleSort.setOnClickListener(v -> {
            sortDesc = !sortDesc;
            binding.btnToggleSort.setImageResource(sortDesc ? R.drawable.ic_sort_desc : R.drawable.ic_sort_asc);
            sortAndRefresh();
        });
    }

    private void startSW() {
        startTime = System.currentTimeMillis() - elapsedMs;
        isRunning = true;
        binding.btnStartStop.setText("Pausar");
        binding.btnStartStop.setBackgroundTintList(requireContext().getColorStateList(R.color.ios_orange));
        binding.btnSave.setVisibility(View.GONE);
        binding.btnReset.setEnabled(true);
        handler.post(ticker);
    }

    private void pauseSW() {
        isRunning = false;
        handler.removeCallbacks(ticker);
        binding.btnStartStop.setText("Continuar");
        binding.btnStartStop.setBackgroundTintList(requireContext().getColorStateList(R.color.ios_green));
        if (elapsedMs > 0) binding.btnSave.setVisibility(View.VISIBLE);
    }

    private void resetSW() {
        isRunning = false;
        handler.removeCallbacks(ticker);
        elapsedMs = 0;
        startTime = 0;
        binding.btnStartStop.setText("Iniciar");
        binding.btnStartStop.setBackgroundTintList(requireContext().getColorStateList(R.color.ios_blue));
        binding.btnSave.setVisibility(View.GONE);
        binding.btnReset.setEnabled(false);
        updateDisplay();
    }

    private void updateDisplay() {
        binding.analogStopwatch.setStopwatchTime(elapsedMs);
        binding.tvDigitalStopwatch.setText(fmt(elapsedMs));
    }

    private String fmt(long ms) {
        long h = ms / 3_600_000; ms %= 3_600_000;
        long m = ms / 60_000; ms %= 60_000;
        long s = ms / 1_000;
        long cs = (ms % 1_000) / 10;
        if (h > 0) return String.format("%02d:%02d:%02d.%02d", h, m, s, cs);
        return String.format("%02d:%02d.%02d", m, s, cs);
    }

    private void showSaveDialog() {
        DialogSaveTimeBinding dialogBinding = DialogSaveTimeBinding.inflate(LayoutInflater.from(requireContext()));
        dialogBinding.tvDialogTime.setText(fmt(elapsedMs));

        new AlertDialog.Builder(requireContext())
                .setTitle("Guardar tiempo")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Guardar", (d, w) -> {
                    String name = dialogBinding.etName.getText().toString().trim();
                    if (name.isEmpty()) name = "Tiempo #" + (savedList.size() + 1);
                    savedList.add(new SavedTime(name, elapsedMs, System.currentTimeMillis()));
                    persistSaved();
                    sortAndRefresh();
                    resetSW();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setupRecyclerView() {
        adapter = new SavedAdapter(savedList, pos -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar registro")
                    .setMessage("¿Eliminar \"" + savedList.get(pos).getName() + "\"?")
                    .setPositiveButton("Sí", (d, w) -> {
                        savedList.remove(pos);
                        persistSaved();
                        adapter.notifyItemRemoved(pos);
                    })
                    .setNegativeButton("No", null).show();
        });
        binding.rvSavedTimes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSavedTimes.setAdapter(adapter);
    }

    private void sortAndRefresh() {
        if (sortDesc) savedList.sort((a, b) -> Long.compare(b.getDurationMs(), a.getDurationMs()));
        else savedList.sort((a, b) -> Long.compare(a.getDurationMs(), b.getDurationMs()));
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadSaved() {
        SharedPreferences p = requireContext().getSharedPreferences(PREFS_SW, Context.MODE_PRIVATE);
        String json = p.getString(KEY_TIMES, null);
        if (json != null) {
            Type t = new TypeToken<List<SavedTime>>() {}.getType();
            List<SavedTime> loaded = gson.fromJson(json, t);
            if (loaded != null) savedList = loaded;
        }
        sortAndRefresh();
    }

    private void persistSaved() {
        requireContext().getSharedPreferences(PREFS_SW, Context.MODE_PRIVATE)
                .edit().putString(KEY_TIMES, gson.toJson(savedList)).apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isRunning) handler.post(ticker);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Corrected Inner Adapter
    private static class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.VH> {
        interface OnDelete { void onDelete(int pos); }

        private final List<SavedTime> items;
        private final OnDelete onDelete;

        SavedAdapter(List<SavedTime> items, OnDelete onDelete) {
            this.items = items;
            this.onDelete = onDelete;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSavedTimeBinding itemBinding = ItemSavedTimeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SavedTime st = items.get(position);
            holder.binding.tvRank.setText(String.valueOf(position + 1));
            holder.binding.tvSavedName.setText(st.getName());
            holder.binding.tvSavedTime.setText(st.getFormattedDuration());
            holder.binding.btnDelete.setOnClickListener(v -> {
                if(holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onDelete.onDelete(holder.getAdapterPosition());
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ItemSavedTimeBinding binding;
            VH(@NonNull ItemSavedTimeBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
