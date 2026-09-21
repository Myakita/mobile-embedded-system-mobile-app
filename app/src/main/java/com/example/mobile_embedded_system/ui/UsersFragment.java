package com.example.mobile_embedded_system.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_embedded_system.R;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.domain.TacticalStatusEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Фрагмент состава и реестра участников группы по ТЗ (§1.81–1.84, §6.8.2).
 */
public class UsersFragment extends Fragment {

    private enum FilterMode { ALL, OK, ALERT, STALE }

    private TelemetryViewModel viewModel;
    private UsersAdapter adapter;
    private final Map<Long, TelemetryEntity> squadData = new HashMap<>();

    private TextView textUserCountSummary;
    private EditText editUserSearch;
    private TextView btnFilterAll;
    private TextView btnFilterOk;
    private TextView btnFilterAlert;
    private TextView btnFilterOffline;

    private FilterMode currentFilter = FilterMode.ALL;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TelemetryViewModel.class);

        initViews(view);
        setupRecyclerView(view);
        setupFilters();
        observeSquadTelemetry();
    }

    private void initViews(View view) {
        textUserCountSummary = view.findViewById(R.id.textUserCountSummary);
        editUserSearch = view.findViewById(R.id.editUserSearch);
        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterOk = view.findViewById(R.id.btnFilterOk);
        btnFilterAlert = view.findViewById(R.id.btnFilterAlert);
        btnFilterOffline = view.findViewById(R.id.btnFilterOffline);

        editUserSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase(Locale.US);
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new UsersAdapter(new UsersAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(UsersAdapter.UserItem userItem) {
                viewModel.setActiveUserId(userItem.userId);
                Toast.makeText(requireContext(), "ВЫБРАН БОЕЦ: " + userItem.callsign, Toast.LENGTH_SHORT).show();
                try {
                    NavHostFragment.findNavController(UsersFragment.this).navigate(R.id.mapFragment);
                } catch (Exception ignored) {}
            }

            @Override
            public void onUserLongClick(UsersAdapter.UserItem userItem) {
                showCommandSelectionDialog(userItem);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void showCommandSelectionDialog(UsersAdapter.UserItem userItem) {
        String[] commands = {
            "CHECK_IN — ЗАПРОС КВИТАНЦИИ",
            "HOLD — УДЕРЖАНИЕ ПОЗИЦИИ",
            "RETURN — ВОЗВРАТ НА БАЗУ"
        };
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("ПРИКАЗ -> " + userItem.callsign)
            .setItems(commands, (dialog, which) -> {
                boolean sent;
                String cmdName;
                switch (which) {
                    case 0:
                        sent = viewModel.dispatchCheckInCommand(userItem.userId);
                        cmdName = "CHECK_IN";
                        break;
                    case 1:
                        sent = viewModel.dispatchHoldCommand(userItem.userId);
                        cmdName = "HOLD";
                        break;
                    case 2:
                        sent = viewModel.dispatchReturnCommand(userItem.userId);
                        cmdName = "RETURN";
                        break;
                    default:
                        return;
                }
                if (sent) {
                    Toast.makeText(requireContext(), cmdName + " -> " + userItem.callsign, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "ОШИБКА ОТПРАВКИ " + cmdName, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("ОТМЕНА", null)
            .show();
    }

    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> setFilter(FilterMode.ALL));
        btnFilterOk.setOnClickListener(v -> setFilter(FilterMode.OK));
        btnFilterAlert.setOnClickListener(v -> setFilter(FilterMode.ALERT));
        btnFilterOffline.setOnClickListener(v -> setFilter(FilterMode.STALE));

        updateFilterButtonsUI();
    }

    private void setFilter(FilterMode mode) {
        this.currentFilter = mode;
        updateFilterButtonsUI();
        applyFilterAndSearch();
    }

    private void updateFilterButtonsUI() {
        int inkColor = resolveColor(R.attr.appInk);
        int surfaceColor = resolveColor(R.attr.appSurface);

        applyButtonStyle(btnFilterAll, currentFilter == FilterMode.ALL, inkColor, surfaceColor, surfaceColor, inkColor);
        applyButtonStyle(btnFilterOk, currentFilter == FilterMode.OK, inkColor, surfaceColor, surfaceColor, inkColor);
        applyButtonStyle(btnFilterAlert, currentFilter == FilterMode.ALERT, inkColor, surfaceColor, surfaceColor, inkColor);
        applyButtonStyle(btnFilterOffline, currentFilter == FilterMode.STALE, inkColor, surfaceColor, surfaceColor, inkColor);
    }

    private void applyButtonStyle(TextView btn, boolean isActive, int activeBg, int activeText, int inactiveBg, int inactiveText) {
        if (isActive) {
            btn.setBackgroundColor(activeBg);
            btn.setTextColor(activeText);
        } else {
            btn.setBackgroundColor(inactiveBg);
            btn.setTextColor(inactiveText);
        }
    }

    private void observeSquadTelemetry() {
        List<Long> squadIds = viewModel.getSquadUserIds();
        for (long userId : squadIds) {
            viewModel.getLatestTelemetry(userId).observe(getViewLifecycleOwner(), entity -> {
                if (entity != null) {
                    squadData.put(entity.userId, entity);
                    applyFilterAndSearch();
                }
            });
        }
        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        List<UsersAdapter.UserItem> filteredList = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        List<Long> squadIds = viewModel.getSquadUserIds();

        for (long userId : squadIds) {
            String callsign = getCallsignByUserId(userId);
            long serial = getSerialByUserId(userId);
            TelemetryEntity telemetry = squadData.get(userId);

            // 1. Поиск
            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || callsign.toLowerCase(Locale.US).contains(currentSearchQuery)
                    || String.valueOf(userId).contains(currentSearchQuery)
                    || String.valueOf(serial).contains(currentSearchQuery);

            if (!matchesSearch) {
                continue;
            }

            // 2. Фильтр по статусу
            boolean matchesFilter = true;
            if (telemetry != null) {
                long nowSec = nowMs / 1000L;
                com.example.mobile_embedded_system.domain.PositionStatusEvaluator.PositionState posState =
                        com.example.mobile_embedded_system.domain.PositionStatusEvaluator.evaluate(telemetry, nowSec);
                boolean isStale = (posState == com.example.mobile_embedded_system.domain.PositionStatusEvaluator.PositionState.STALE);
                TacticalStatusEvaluator.Status status = TacticalStatusEvaluator.evaluate(telemetry.pulseBpm, telemetry.temperatureCelsius);

                if (currentFilter == FilterMode.OK) {
                    matchesFilter = (!isStale && status == TacticalStatusEvaluator.Status.OK);
                } else if (currentFilter == FilterMode.ALERT) {
                    matchesFilter = (!isStale && (status == TacticalStatusEvaluator.Status.WARNING || status == TacticalStatusEvaluator.Status.CRITICAL));
                } else if (currentFilter == FilterMode.STALE) {
                    matchesFilter = isStale;
                }
            } else if (currentFilter != FilterMode.ALL) {
                matchesFilter = (currentFilter == FilterMode.STALE);
            }

            if (matchesFilter) {
                filteredList.add(new UsersAdapter.UserItem(userId, callsign, serial, telemetry));
            }
        }

        adapter.setItems(filteredList);
        textUserCountSummary.setText(String.format(Locale.US, "%d ИЗ %d ЧЕЛ.", filteredList.size(), squadIds.size()));
    }

    private String getCallsignByUserId(long userId) {
        return viewModel.getCallsignForUser(userId);
    }

    private long getSerialByUserId(long userId) {
        return viewModel.getSerialForUser(userId);
    }

    private int resolveColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return 0xFF888888;
    }
}
