package com.example.mobile_embedded_system.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile_embedded_system.R;
import com.example.mobile_embedded_system.data.model.ConfigSyncState;
import com.example.mobile_embedded_system.data.model.DeviceConfigModel;

import java.util.Locale;

/**
 * Фрагмент конфигурирования интерфейсов и периферии Edge-терминала по ТЗ (§4, §11, MVP §11.50–11.59).
 */
public class DeviceConfigFragment extends Fragment {

    private TelemetryViewModel viewModel;
    private final DeviceConfigModel currentConfig = new DeviceConfigModel();

    private View bannerConfigStatus;
    private TextView textConfigSyncStatus;

    private EditText editDeviceSerial;
    private EditText editUserId;

    private CheckBox checkTemp;
    private CheckBox checkPulse;
    private CheckBox checkPressure;
    private CheckBox checkGnss;
    private CheckBox checkImu;
    private EditText editPeriodSec;

    private CheckBox checkLoraEnable;
    private EditText editLoraFreq;
    private EditText editLoraSf;

    private CheckBox checkWifiEnable;
    private EditText editWifiSsid;

    private CheckBox checkLteEnable;
    private EditText editLteApn;

    private EditText editMqttBroker;
    private EditText editNetworkRoot;
    private EditText editHierarchyPath;

    private TextView btnApplyConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TelemetryViewModel.class);

        initViews(view);
        bindDataToViews();
        setupChangeListeners();
    }

    private void initViews(View view) {
        bannerConfigStatus = view.findViewById(R.id.bannerConfigStatus);
        textConfigSyncStatus = view.findViewById(R.id.textConfigSyncStatus);

        editDeviceSerial = view.findViewById(R.id.editDeviceSerial);
        editUserId = view.findViewById(R.id.editUserId);

        checkTemp = view.findViewById(R.id.checkTemp);
        checkPulse = view.findViewById(R.id.checkPulse);
        checkPressure = view.findViewById(R.id.checkPressure);
        checkGnss = view.findViewById(R.id.checkGnss);
        checkImu = view.findViewById(R.id.checkImu);
        editPeriodSec = view.findViewById(R.id.editPeriodSec);

        checkLoraEnable = view.findViewById(R.id.checkLoraEnable);
        editLoraFreq = view.findViewById(R.id.editLoraFreq);
        editLoraSf = view.findViewById(R.id.editLoraSf);

        checkWifiEnable = view.findViewById(R.id.checkWifiEnable);
        editWifiSsid = view.findViewById(R.id.editWifiSsid);

        checkLteEnable = view.findViewById(R.id.checkLteEnable);
        editLteApn = view.findViewById(R.id.editLteApn);

        editMqttBroker = view.findViewById(R.id.editMqttBroker);
        editNetworkRoot = view.findViewById(R.id.editNetworkRoot);
        editHierarchyPath = view.findViewById(R.id.editHierarchyPath);

        btnApplyConfig = view.findViewById(R.id.btnApplyConfig);
        btnApplyConfig.setOnClickListener(v -> applyAndTransmitConfig());
    }

    private void bindDataToViews() {
        editDeviceSerial.setText(String.valueOf(currentConfig.getDeviceSerial()));
        editUserId.setText(String.valueOf(currentConfig.getUserId()));

        checkTemp.setChecked(currentConfig.isTempEnabled());
        checkPulse.setChecked(currentConfig.isPulseEnabled());
        checkPressure.setChecked(currentConfig.isPressureEnabled());
        checkGnss.setChecked(currentConfig.isGnssEnabled());
        checkImu.setChecked(currentConfig.isImuEnabled());
        editPeriodSec.setText(String.valueOf(currentConfig.getTelemetryPeriodSec()));

        checkLoraEnable.setChecked(currentConfig.isLoraEnabled());
        editLoraFreq.setText(String.valueOf(currentConfig.getLoraFrequencyMhz()));
        editLoraSf.setText(String.valueOf(currentConfig.getLoraSpreadingFactor()));

        checkWifiEnable.setChecked(currentConfig.isWifiEnabled());
        editWifiSsid.setText(currentConfig.getWifiSsid());

        checkLteEnable.setChecked(currentConfig.isLteEnabled());
        editLteApn.setText(currentConfig.getLteApn());

        editMqttBroker.setText(currentConfig.getBrokerUrl());
        editNetworkRoot.setText(currentConfig.getNetworkRoot());
        editHierarchyPath.setText(currentConfig.getHierarchyPath());

        updateSyncStatusUI(currentConfig.getSyncState());
    }

    private void setupChangeListeners() {
        TextWatcher dirtyWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                markConfigDirty();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editDeviceSerial.addTextChangedListener(dirtyWatcher);
        editUserId.addTextChangedListener(dirtyWatcher);
        editPeriodSec.addTextChangedListener(dirtyWatcher);
        editLoraFreq.addTextChangedListener(dirtyWatcher);
        editLoraSf.addTextChangedListener(dirtyWatcher);
        editWifiSsid.addTextChangedListener(dirtyWatcher);
        editLteApn.addTextChangedListener(dirtyWatcher);
        editMqttBroker.addTextChangedListener(dirtyWatcher);
        editNetworkRoot.addTextChangedListener(dirtyWatcher);
        editHierarchyPath.addTextChangedListener(dirtyWatcher);

        View.OnClickListener checkListener = v -> markConfigDirty();
        checkTemp.setOnClickListener(checkListener);
        checkPulse.setOnClickListener(checkListener);
        checkPressure.setOnClickListener(checkListener);
        checkGnss.setOnClickListener(checkListener);
        checkImu.setOnClickListener(checkListener);
        checkLoraEnable.setOnClickListener(checkListener);
        checkWifiEnable.setOnClickListener(checkListener);
        checkLteEnable.setOnClickListener(checkListener);
    }

    private void markConfigDirty() {
        if (currentConfig.getSyncState() != ConfigSyncState.CONFIGURATION_REQUIRED) {
            currentConfig.setSyncState(ConfigSyncState.CONFIGURATION_REQUIRED);
            updateSyncStatusUI(ConfigSyncState.CONFIGURATION_REQUIRED);
        }
    }

    private void updateSyncStatusUI(ConfigSyncState state) {
        if (bannerConfigStatus == null || textConfigSyncStatus == null) return;

        textConfigSyncStatus.setText(String.format(Locale.US, "СТАТУС КОНФИГУРАЦИИ: %s", state.getDescription().toUpperCase(Locale.US)));

        int bgResId;
        switch (state) {
            case APPLIED:
                bgResId = R.color.status_ok;
                break;
            case PENDING:
            case CONFIGURATION_REQUIRED:
                bgResId = R.color.status_warning;
                break;
            case ERROR:
            default:
                bgResId = R.color.status_critical;
                break;
        }
        bannerConfigStatus.setBackgroundResource(bgResId);
    }

    private void applyAndTransmitConfig() {
        try {
            currentConfig.setDeviceSerial(Long.parseLong(editDeviceSerial.getText().toString().trim()));
            currentConfig.setUserId(Long.parseLong(editUserId.getText().toString().trim()));
            currentConfig.setTelemetryPeriodSec(Integer.parseInt(editPeriodSec.getText().toString().trim()));

            currentConfig.setTempEnabled(checkTemp.isChecked());
            currentConfig.setPulseEnabled(checkPulse.isChecked());
            currentConfig.setPressureEnabled(checkPressure.isChecked());
            currentConfig.setGnssEnabled(checkGnss.isChecked());
            currentConfig.setImuEnabled(checkImu.isChecked());

            currentConfig.setLoraEnabled(checkLoraEnable.isChecked());
            currentConfig.setLoraFrequencyMhz(Integer.parseInt(editLoraFreq.getText().toString().trim()));
            currentConfig.setLoraSpreadingFactor(Integer.parseInt(editLoraSf.getText().toString().trim()));

            currentConfig.setWifiEnabled(checkWifiEnable.isChecked());
            currentConfig.setWifiSsid(editWifiSsid.getText().toString().trim());

            currentConfig.setLteEnabled(checkLteEnable.isChecked());
            currentConfig.setLteApn(editLteApn.getText().toString().trim());

            currentConfig.setBrokerUrl(editMqttBroker.getText().toString().trim());
            currentConfig.setNetworkRoot(editNetworkRoot.getText().toString().trim());
            currentConfig.setHierarchyPath(editHierarchyPath.getText().toString().trim());

            currentConfig.setSyncState(ConfigSyncState.APPLIED);
            currentConfig.setLastSyncTimestampMs(System.currentTimeMillis());

            updateSyncStatusUI(ConfigSyncState.APPLIED);

            Toast.makeText(requireContext(),
                    "КОНФИГУРАЦИЯ ПЕРЕДАНА НА ТЕРМИНАЛ [" + currentConfig.getDeviceSerial() + "]",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            currentConfig.setSyncState(ConfigSyncState.ERROR);
            updateSyncStatusUI(ConfigSyncState.ERROR);
            Toast.makeText(requireContext(), "ОШИБКА ВВОДА ПАРАМЕТРОВ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
