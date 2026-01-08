package salve.presentation.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.salve.app.R;

import java.util.List;

import salve.core.GlifoReliquia;
import salve.core.MemoriaEmocional;

public class InventarioReliquiasBottomSheet extends BottomSheetDialogFragment
        implements ReliquiaAdapter.OnReliquiaClickListener {

    private MemoriaEmocional memoria;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getContext();
        if (ctx != null) {
            memoria = new MemoriaEmocional(ctx.getApplicationContext());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventario_reliquias, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_reliquias);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<GlifoReliquia> reliquias = memoria == null ? java.util.Collections.emptyList() : memoria.getReliquias();
        ReliquiaAdapter adapter = new ReliquiaAdapter(requireContext(), reliquias, this);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onReliquiaClick(GlifoReliquia reliquia) {
        Context ctx = getContext();
        if (ctx != null) {
            ObjetoCreativoActivity.openFromReliquia(ctx, reliquia);
        }
        dismiss();
    }
}
