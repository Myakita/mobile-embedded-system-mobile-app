package com.example.mobile_embedded_system.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_embedded_system.R;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.data.model.PacketDiagnosticsModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Фрагмент диагностики сетевого транспорта и статистики пакетов по ТЗ (§21, MVP §1.98–1.101).
 */
public class DiagnosticsFragment extends Fragment {

    private TelemetryViewModel viewModel;
    private long selectedUnitId = 1001L;

    private TextView textMqttStatusBadge;
    private TextView textDiagBroker;
    private TextView textDiagLastReconnect;
    private TextView textDiagSubscription;

    private TextView textStatTotal;
    private TextView textStatTelemetry;
    private TextView textStatCommand;
    private TextView textStatDuplicates;
    private TextView textStatCryptoErrors;
    private TextView textStatMalformed;

    private TextView btnDiag1001;
    private TextView btnDiag1002;
    private TextView btnDiag1003;

    private TextView textDiagDeviceSerial;
    private TextView textDiagLastSeq;
    private TextView textDiagLastTimestamp;
    private TextView textDiagLastSeen;

    private TextView btnResetDiagCounters;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diagnostics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TelemetryViewModel.class);

        initViews(view);
        setupUnitButtons();
        observeDiagnosticsAndTelemetry();
    }

    private void initViews(View view) {
        textMqttStatusBadge = view.findViewById(R.id.textMqttStatusBadge);
        textDiagBroker = view.findViewById(R.id.textDiagBroker);
        textDiagLastReconnect = view.findViewById(R.id.textDiagLastReconnect);
        textDiagSubscription = view.findViewById(R.id.textDiagSubscription);

        textStatTotal = view.findViewById(R.id.textStatTotal);
        textStatTelemetry = view.findViewById(R.id.textStatTelemetry);
        textStatCommand = view.findViewById(R.id.textStatCommand);
        textStatDuplicates = view.findViewById(R.id.textStatDuplicates);
        textStatCryptoErrors = view.findViewById(R.id.textStatCryptoErrors);
        textStatMalformed = view.findViewById(R.id.textStatMalformed);

        btnDiag1001 = view.findViewById(R.id.btnDiag1001);
        btnDiag1002 = view.findViewById(R.id.btnDiag1002);
        btnDiag1003 = view.findViewById(R.id.btnDiag1003);

        textDiagDeviceSerial = view.findViewById(R.id.textDiagDeviceSerial);
        textDiagLastSeq = view.findViewById(R.id.textDiagLastSeq);
        textDiagLastTimestamp = view.findViewById(R.id.textDiagLastTimestamp);
        textDiagLastSeen = view.findViewById(R.id.textDiagLastSeen);

        btnResetDiagCounters = view.findViewById(R.id.btnResetDiagCounters);
        btnResetDiagCounters.setOnClickListener(v -> {
            PacketDiagnosticsModel model = viewModel.getDiagnosticsModel();
            if (model != null) {
                model.resetCounters();
                updateDiagnosticsUI();
                Toast.makeText(requireContext(), "СЧЁТЧИКИ ПАКЕТОВ СБРОШЕНЫ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUnitButtons() {
        btnDiag1001.setOnClickListener(v -> selectUnit(1001L));
        btnDiag1002.setOnClickListener(v -> selectUnit(1002L));
        btnDiag1003.setOnClickListener(v -> selectUnit(1003L));
        updateUnitButtonsUI();
    }

    private void selectUnit(long userId) {
        this.selectedUnitId = userId;
        updateUnitButtonsUI();
        observeSelectedUnitTelemetry();
    }

    private void updateUnitButtonsUI() {
        int inkColor = requireContext().getColor(R.color.ink);
        int surfaceColor = requireContext().getColor(R.color.surface);
        int bgColor = requireContext().getColor(R.color.bg);

        applyButtonStyle(btnDiag1001, selectedUnitId == 1001L, inkColor, surfaceColor, bgColor, inkColor);
        applyButtonStyle(btnDiag1002, selectedUnitId == 1002L, inkColor, surfaceColor, bgColor, inkColor);
        applyButtonStyle(btnDiag1003, selectedUnitId == 1003L, inkColor, surfaceColor, bgColor, inkColor);
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

    private void observeDiagnosticsAndTelemetry() {
        updateDiagnosticsUI();
        observeSelectedUnitTelemetry();

        viewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> updateDiagnosticsUI());
    }

    private void observeSelectedUnitTelemetry() {
        viewModel.getLatestTelemetry(selectedUnitId).observe(getViewLifecycleOwner(), entity -> {
            if (entity != null) {
                updateUnitDiagnosticsUI(entity);
            } else {
                textDiagDeviceSerial.setText("DEVICE SERIAL: --");
                textDiagLastSeq.setText("LAST SEQUENCE: --");
                textDiagLastTimestamp.setText("LAST TIMESTAMP: --");
                textDiagLastSeen.setText("LAST SEEN: --");
            }
        });
    }

    private void updateDiagnosticsUI() {
        PacketDiagnosticsModel model = viewModel.getDiagnosticsModel();
        if (model == null) return;

        textMqttStatusBadge.setText(model.getConnectionState());
        textDiagBroker.setText(String.format(Locale.US, "БРОКЕР: %s", model.getBrokerUrl()));
        textDiagLastReconnect.setText(String.format(Locale.US, "ПОСЛ. ПЕРЕПОДКЛЮЧЕНИЕ: %s",
                timeFormat.format(new Date(model.getLastReconnectTimestampMs()))));
        textDiagSubscription.setText(String.format(Locale.US, "ПОДПИСКА: %s", model.getActiveSubscriptionTopic()));

        textStatTotal.setText(String.valueOf(model.getTotalPacketsReceived()));
        textStatTelemetry.setText(String.valueOf(model.getTelemetryPacketsCount()));
        textStatCommand.setText(String.valueOf(model.getCommandPacketsCount()));
        textStatDuplicates.setText(String.valueOf(model.getDuplicatesDroppedCount()));
        textStatCryptoErrors.setText(String.valueOf(model.getDecryptionErrorsCount()));
        textStatMalformed.setText(String.valueOf(model.getMalformedPacketsCount()));
    }

    private void updateUnitDiagnosticsUI(TelemetryEntity entity) {
        textDiagDeviceSerial.setText(String.format(Locale.US, "DEVICE SERIAL: %d", entity.deviceSerial));
        textDiagLastSeq.setText(String.format(Locale.US, "LAST SEQUENCE: %d", entity.sequence));
        textDiagLastTimestamp.setText(String.format(Locale.US, "LAST TIMESTAMP: %s (sec: %d)",
                timeFormat.format(new Date(entity.timestamp * 1000L)), entity.timestamp));
        textDiagLastSeen.setText(String.format(Locale.US, "LAST SEEN: %s",
                timeFormat.format(new Date(entity.receivedAtMs))));
    }
}
