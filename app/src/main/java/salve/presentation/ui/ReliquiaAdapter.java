package salve.presentation.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import salve.core.GlifoReliquia;
import salve.core.ParametricGlyph;
import com.salve.app.R;

public class ReliquiaAdapter extends RecyclerView.Adapter<ReliquiaAdapter.ReliquiaViewHolder> {

    public interface OnReliquiaClickListener {
        void onReliquiaClick(GlifoReliquia reliquia);
    }

    private final Context context;
    private final List<GlifoReliquia> reliquias;
    private final OnReliquiaClickListener listener;

    public ReliquiaAdapter(Context context,
                           List<GlifoReliquia> reliquias,
                           OnReliquiaClickListener listener) {
        this.context = context;
        this.reliquias = reliquias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReliquiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reliquia, parent, false);
        return new ReliquiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReliquiaViewHolder holder, int position) {
        GlifoReliquia reliquia = reliquias.get(position);
        String titulo = reliquia.getTitulo();
        String significado = reliquia.getSignificado();
        holder.titulo.setText(titulo == null ? "Reliquia" : titulo);
        holder.significado.setText(significado == null ? "" : significado);
        holder.icon.setImageBitmap(generarMiniatura(reliquia));
        holder.itemView.setOnClickListener(v -> listener.onReliquiaClick(reliquia));
    }

    @Override
    public int getItemCount() {
        return reliquias.size();
    }

    private Bitmap generarMiniatura(GlifoReliquia reliquia) {
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = Math.round(64 * density);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * density);
        paint.setColor(reliquia.getColorArgb());

        ParametricGlyph.Style style = ParametricGlyph.Style.ORB;
        String styleRaw = reliquia.getStyle();
        if (styleRaw != null) {
            try {
                style = ParametricGlyph.Style.valueOf(styleRaw.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                style = ParametricGlyph.Style.ORB;
            }
        }

        float sizeGlyph = sizePx * 0.8f;
        Path path = ParametricGlyph.build(reliquia.getSeed(), style, sizeGlyph);
        canvas.save();
        canvas.translate(sizePx / 2f, sizePx / 2f);
        canvas.drawPath(path, paint);
        canvas.restore();
        return bitmap;
    }

    static class ReliquiaViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView titulo;
        final TextView significado;

        ReliquiaViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.reliquia_icon);
            titulo = itemView.findViewById(R.id.reliquia_titulo);
            significado = itemView.findViewById(R.id.reliquia_significado);
        }
    }
}
