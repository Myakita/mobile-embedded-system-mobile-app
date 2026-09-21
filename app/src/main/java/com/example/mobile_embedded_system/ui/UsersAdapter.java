package com.example.mobile_embedded_system.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_embedded_system.R;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.domain.TacticalStatusEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public static class UserItem {
        public final long userId;
        public final String callsign;
        public final long serial;
        public final TelemetryEntity telemetry;

        public UserItem(long userId, String callsign, long serial, TelemetryEntity telemetry) {
            this.userId = userId;
            this.callsign = callsign;
            this.serial = serial;
            this.telemetry = telemetry;
        }
    }

    public interface OnUserClickListener {
        void onUserClick(UserItem userItem);
    }

    private final List<UserItem> items = new ArrayList<>();
    private final OnUserClickListener listener;

    public UsersAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<UserItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_row, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        private final View viewUserStatusIndicator;
        private final TextView textUserCallsign;
        private final TextView textUserSubInfo;
        private final TextView textUserVitals;
        private final TextView textUserAgeAndPressure;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            viewUserStatusIndicator = itemView.findViewById(R.id.viewUserStatusIndicator);
            textUserCallsign = itemView.findViewById(R.id.textUserCallsign);
            textUserSubInfo = itemView.findViewById(R.id.textUserSubInfo);
            textUserVitals = itemView.findViewById(R.id.textUserVitals);
            textUserAgeAndPressure = itemView.findViewById(R.id.textUserAgeAndPressure);
        }

        public void bind(UserItem item, OnUserClickListener listener) {
            textUserCallsign.setText(item.callsign);
            textUserSubInfo.setText(String.format(Locale.US, "ID: %d  |  S/N: %d", item.userId, item.serial));

            Context context = itemView.getContext();
            long nowMs = System.currentTimeMillis();

            if (item.telemetry != null) {
                long ageSec = (nowMs - item.telemetry.receivedAtMs) / 1000L;
                boolean isStale = ageSec > 120L;

                textUserVitals.setText(String.format(Locale.US, "%d BPM  •  %.1f °C",
                        item.telemetry.pulseBpm, item.telemetry.temperatureCelsius));

                String pressureStr = item.telemetry.pressureSys + "/" + item.telemetry.pressureDia;
                String ageText = isStale ? String.format(Locale.US, "%s  |  %d с (STALE)", pressureStr, ageSec)
                        : String.format(Locale.US, "%s  |  %d с назад", pressureStr, ageSec);
                textUserAgeAndPressure.setText(ageText);

                TacticalStatusEvaluator.Status status = TacticalStatusEvaluator.evaluate(
                        item.telemetry.pulseBpm, item.telemetry.temperatureCelsius
                );

                int statusColorAttr;
                if (isStale) {
                    statusColorAttr = R.attr.appInk2;
                } else if (status == TacticalStatusEvaluator.Status.CRITICAL) {
                    statusColorAttr = R.attr.appStatusCritical;
                } else if (status == TacticalStatusEvaluator.Status.WARNING) {
                    statusColorAttr = R.attr.appStatusWarning;
                } else {
                    statusColorAttr = R.attr.appStatusOk;
                }

                viewUserStatusIndicator.setBackgroundColor(resolveColor(context, statusColorAttr));
            } else {
                textUserVitals.setText("НЕТ ДАННЫХ");
                textUserAgeAndPressure.setText("-- / --  |  ОФЛАЙН");
                viewUserStatusIndicator.setBackgroundColor(resolveColor(context, R.attr.appInk2));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(item);
            });
        }

        private int resolveColor(Context context, int attrResId) {
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(attrResId, typedValue, true)) {
                return typedValue.data;
            }
            return 0xFF888888;
        }
    }
}
