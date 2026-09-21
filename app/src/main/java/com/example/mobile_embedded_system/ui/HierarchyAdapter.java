package com.example.mobile_embedded_system.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_embedded_system.R;
import com.example.mobile_embedded_system.data.local.TelemetryEntity;
import com.example.mobile_embedded_system.domain.HierarchyNode;
import com.example.mobile_embedded_system.domain.TacticalStatusEvaluator;
import com.example.mobile_embedded_system.domain.UnitHierarchyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HierarchyAdapter extends RecyclerView.Adapter<HierarchyAdapter.HierarchyViewHolder> {

    public static class FlatNode {
        public final HierarchyNode node;
        public final int depth;

        public FlatNode(HierarchyNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    public interface OnNodeClickListener {
        void onNodeClick(HierarchyNode node);
        void onNodeLongClick(HierarchyNode node);
    }

    private final List<FlatNode> flatList = new ArrayList<>();
    private final UnitHierarchyManager hierarchyManager;
    private final Map<Long, TelemetryEntity> squadData;
    private final String networkRoot;
    private final OnNodeClickListener listener;

    public HierarchyAdapter(UnitHierarchyManager hierarchyManager, Map<Long, TelemetryEntity> squadData, String networkRoot, OnNodeClickListener listener) {
        this.hierarchyManager = hierarchyManager;
        this.squadData = squadData;
        this.networkRoot = networkRoot;
        this.listener = listener;
        rebuildFlatList();
    }

    public void updateData() {
        rebuildFlatList();
        notifyDataSetChanged();
    }

    private void rebuildFlatList() {
        flatList.clear();
        for (HierarchyNode root : hierarchyManager.getRootNodes()) {
            addNodeRecursive(root, 0);
        }
    }

    private void addNodeRecursive(HierarchyNode node, int depth) {
        flatList.add(new FlatNode(node, depth));
        if (node.isExpanded() && !node.getChildren().isEmpty()) {
            for (HierarchyNode child : node.getChildren()) {
                addNodeRecursive(child, depth + 1);
            }
        }
    }

    @NonNull
    @Override
    public HierarchyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hierarchy_node, parent, false);
        return new HierarchyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HierarchyViewHolder holder, int position) {
        holder.bind(flatList.get(position), hierarchyManager, squadData, networkRoot, listener, () -> updateData());
    }

    @Override
    public int getItemCount() {
        return flatList.size();
    }

    static class HierarchyViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout layoutNodeHeader;
        private final TextView textNodeExpandIcon;
        private final TextView textNodeTypeBadge;
        private final TextView textNodeTitle;
        private final TextView textNodeTopicPreview;
        private final TextView textNodeSummaryBadge;

        public HierarchyViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutNodeHeader = itemView.findViewById(R.id.layoutNodeHeader);
            textNodeExpandIcon = itemView.findViewById(R.id.textNodeExpandIcon);
            textNodeTypeBadge = itemView.findViewById(R.id.textNodeTypeBadge);
            textNodeTitle = itemView.findViewById(R.id.textNodeTitle);
            textNodeTopicPreview = itemView.findViewById(R.id.textNodeTopicPreview);
            textNodeSummaryBadge = itemView.findViewById(R.id.textNodeSummaryBadge);
        }

        public void bind(FlatNode flatNode, UnitHierarchyManager hierarchyManager, Map<Long, TelemetryEntity> squadData, String networkRoot, OnNodeClickListener listener, Runnable toggleCallback) {
            HierarchyNode node = flatNode.node;
            Context context = itemView.getContext();

            // Отступ по глубине
            int indentPx = (int) (flatNode.depth * 16 * context.getResources().getDisplayMetrics().density);
            layoutNodeHeader.setPadding(12 + (int) (flatNode.depth * 12), 6, 12, 6);

            // Иконка раскрытия
            if (node.getChildren().isEmpty()) {
                textNodeExpandIcon.setText("•");
            } else {
                textNodeExpandIcon.setText(node.isExpanded() ? "▾" : "▸");
            }

            // Тип узла
            textNodeTypeBadge.setText(getNodeTypeBadgeText(node.getType()));

            // Название
            textNodeTitle.setText(node.getName());

            // Топик MQTT
            String topic = hierarchyManager.generateMqttTelemetryTopic(node, networkRoot);
            textNodeTopicPreview.setText("TOPIC: " + topic);

            // Сводка по узлу
            UnitHierarchyManager.NodeSummary summary = hierarchyManager.evaluateNodeSummary(node, squadData);
            if (node.getType() == HierarchyNode.NodeType.SOLDIER) {
                textNodeSummaryBadge.setVisibility(View.GONE);
            } else {
                textNodeSummaryBadge.setVisibility(View.VISIBLE);
                textNodeSummaryBadge.setText(summary.onlineUnits + "/" + summary.totalUnits + " • " + summary.worstStatus.name());

                int statusColorAttr = R.attr.appStatusOk;
                if (summary.worstStatus == TacticalStatusEvaluator.Status.CRITICAL) {
                    statusColorAttr = R.attr.appStatusCritical;
                } else if (summary.worstStatus == TacticalStatusEvaluator.Status.WARNING) {
                    statusColorAttr = R.attr.appStatusWarning;
                }
                textNodeSummaryBadge.setTextColor(resolveColor(context, statusColorAttr));
            }

            layoutNodeHeader.setOnClickListener(v -> {
                if (!node.getChildren().isEmpty()) {
                    node.setExpanded(!node.isExpanded());
                    if (toggleCallback != null) toggleCallback.run();
                } else if (listener != null) {
                    listener.onNodeClick(node);
                }
            });

            layoutNodeHeader.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onNodeLongClick(node);
                    return true;
                }
                return false;
            });
        }

        private String getNodeTypeBadgeText(HierarchyNode.NodeType type) {
            switch (type) {
                case BATTALION: return "БН";
                case COMPANY: return "РОТА";
                case PLATOON: return "ВЗВОД";
                case SQUAD: return "ОТД";
                case SOLDIER: return "БОЕЦ";
                default: return "УЗЕЛ";
            }
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
