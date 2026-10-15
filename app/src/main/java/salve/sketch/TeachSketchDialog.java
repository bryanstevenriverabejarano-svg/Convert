package salve.sketch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;

/** Lienzo para enseñar a Salve un dibujo con el dedo: ella lo aprende y lo redibuja con sus epiciclos. */
public final class TeachSketchDialog {
    public interface Callback { void onTaught(String name, List<float[]> strokes, float aspect); }

    private TeachSketchDialog() { }

    public static void show(Activity activity, String suggestedName, Callback callback) {
        float d = activity.getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * d);
        root.setPadding(pad, pad / 2, pad, 0);
        EditText name = new EditText(activity);
        name.setHint("¿Qué es? (p. ej. un mueble)");
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (suggestedName != null) name.setText(suggestedName);
        root.addView(name);
        FingerSketchView canvas = new FingerSketchView(activity);
        canvas.setContentDescription("Lienzo: dibuja con el dedo");
        root.addView(canvas, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(300 * d)));
        LinearLayout row = new LinearLayout(activity);
        Button undo = new Button(activity), clear = new Button(activity);
        undo.setText("Deshacer");
        clear.setText("Borrar");
        undo.setOnClickListener(v -> canvas.undo());
        clear.setOnClickListener(v -> canvas.clear());
        row.addView(undo, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(clear, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(row);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Enséñale a dibujar")
                .setMessage("Dibuja con el dedo y dile qué es. Salve lo aprenderá y lo trazará con sus círculos de luz.")
                .setView(root)
                .setPositiveButton("Enseñar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("Dile qué es"); return; }
            if (canvas.strokes().isEmpty()) { Toast.makeText(activity, "Dibuja algo primero", Toast.LENGTH_SHORT).show(); return; }
            HandDrawing.Result drawing = HandDrawing.normalize(canvas.strokes());
            dialog.dismiss();
            callback.onTaught(value, drawing.strokes, drawing.aspect);
        }));
        dialog.show();
    }

    /** Captura los trazos del dedo tal cual, en píxeles. */
    static final class FingerSketchView extends View {
        private final List<float[]> strokes = new ArrayList<>();
        private float[] current = new float[0];
        private int count;
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG), back = new Paint();
        private final Path path = new Path();

        FingerSketchView(Context context) {
            super(context);
            float d = context.getResources().getDisplayMetrics().density;
            ink.setStyle(Paint.Style.STROKE);
            ink.setStrokeWidth(3 * d);
            ink.setStrokeCap(Paint.Cap.ROUND);
            ink.setStrokeJoin(Paint.Join.ROUND);
            ink.setColor(0xFFD8FBFF);
            back.setColor(0xFF0B1628);
        }

        List<float[]> strokes() {
            List<float[]> all = new ArrayList<>(strokes);
            if (count >= 2) all.add(java.util.Arrays.copyOf(current, 2 * count));
            return all;
        }

        void undo() { if (!strokes.isEmpty()) strokes.remove(strokes.size() - 1); invalidate(); }
        void clear() { strokes.clear(); count = 0; invalidate(); }

        @Override public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: count = 0; add(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_MOVE:
                    for (int h = 0; h < e.getHistorySize(); h++) add(e.getHistoricalX(h), e.getHistoricalY(h));
                    add(e.getX(), e.getY());
                    break;
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                    add(e.getX(), e.getY());
                    if (count >= 1 && strokes.size() < 200) strokes.add(java.util.Arrays.copyOf(current, 2 * Math.max(count, 1)));
                    count = 0;
                    break;
                default: break;
            }
            invalidate();
            return true;
        }

        private void add(float x, float y) {
            if (count > 0 && Math.hypot(x - current[2 * count - 2], y - current[2 * count - 1]) < 2) return;
            if (2 * count + 2 > current.length) current = java.util.Arrays.copyOf(current, Math.max(64, current.length * 2));
            current[2 * count] = x; current[2 * count + 1] = y; count++;
        }

        @Override protected void onDraw(Canvas c) {
            c.drawRect(0, 0, getWidth(), getHeight(), back);
            List<float[]> all = strokes();
            for (float[] s : all) {
                path.rewind();
                path.moveTo(s[0], s[1]);
                if (s.length == 2) path.lineTo(s[0] + .1f, s[1]);
                for (int i = 2; i < s.length; i += 2) path.lineTo(s[i], s[i + 1]);
                c.drawPath(path, ink);
            }
        }
    }
}
