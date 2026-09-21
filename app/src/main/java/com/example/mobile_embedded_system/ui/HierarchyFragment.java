package com.example.mobile_embedded_system.ui;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.example.mobile_embedded_system.domain.HierarchyNode;
import com.example.mobile_embedded_system.domain.UnitHierarchyManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Фрагмент дерева иерархии и автоматической генерации топиков по ТЗ (§1.8–1.12, §5.8–5.12).
 */
public class HierarchyFragment extends Fragment {

    private static final long[] SQUAD_IDS = {1001L, 1002L, 1003L};

    private TelemetryViewModel viewModel;
    private UnitHierarchyManager hierarchyManager;
    private HierarchyAdapter adapter;
    private final Map<Long, TelemetryEntity> squadData = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hierarchy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TelemetryViewModel.class);
        hierarchyManager = viewModel.getHierarchyManager();

        initViews(view);
        setupRecyclerView(view);
        observeSquadTelemetry();
    }

    private void initViews(View view) {
        TextView btnAddNode = view.findViewById(R.id.btnAddNode);
        btnAddNode.setOnClickListener(v -> showAddNodeDialog());
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerHierarchy);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new HierarchyAdapter(hierarchyManager, squadData, "mesh-a", new HierarchyAdapter.OnNodeClickListener() {
            @Override
            public void onNodeClick(HierarchyNode node) {
                if (node.getUserId() != null) {
                    viewModel.setActiveUserId(node.getUserId());
                    Toast.makeText(requireContext(), "ВЫБРАН ЮНИТ: " + node.getName(), Toast.LENGTH_SHORT).show();
                    try {
                        NavHostFragment.findNavController(HierarchyFragment.this).navigate(R.id.mapFragment);
                    } catch (Exception ignored) {}
                } else {
                    Toast.makeText(requireContext(), "УЗЕЛ ИЕРАРХИИ: " + node.getName(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNodeLongClick(HierarchyNode node) {
                showDeleteNodeDialog(node);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void observeSquadTelemetry() {
        for (long userId : SQUAD_IDS) {
            viewModel.getLatestTelemetry(userId).observe(getViewLifecycleOwner(), entity -> {
                if (entity != null) {
                    squadData.put(entity.userId, entity);
                    if (adapter != null) {
                        adapter.updateData();
                    }
                }
            });
        }
    }

    private void showAddNodeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("ДОБАВИТЬ УЗЕЛ ИЕРАРХИИ");

        final EditText input = new EditText(requireContext());
        input.setHint("Название узла (например, 2-е Отделение)");
        builder.setView(input);

        builder.setPositiveButton("СОЗДАТЬ", (dialog, which) -> {
            String nodeName = input.getText().toString().trim();
            if (!nodeName.isEmpty()) {
                HierarchyNode squad = hierarchyManager.addNode(nodeName, HierarchyNode.NodeType.SQUAD, 3L, null);
                if (adapter != null) {
                    adapter.updateData();
                }
                String topic = hierarchyManager.generateMqttTelemetryTopic(squad, "mesh-a");
                Toast.makeText(requireContext(), "УЗЕЛ СОЗДАН. МАРШРУТ: " + topic, Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("ОТМЕНА", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteNodeDialog(HierarchyNode node) {
        new AlertDialog.Builder(requireContext())
                .setTitle("УДАЛЕНИЕ УЗЛА")
                .setMessage("Удалить узел '" + node.getName() + "' из структуры подразделения?")
                .setPositiveButton("УДАЛИТЬ", (dialog, which) -> {
                    hierarchyManager.removeNode(node.getId());
                    if (adapter != null) {
                        adapter.updateData();
                    }
                    Toast.makeText(requireContext(), "УЗЕЛ УДАЛЕН. ТОПИКИ ПЕРЕСЧИТАНЫ.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }
}
