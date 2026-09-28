package salve.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;

/** A shared illustrated bed, with its own duvet painted over the sleeping character. */
final class IllustratedBedRenderer {
    private static Bitmap shared;
    private final Bitmap artwork;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF(-104, -80, 104, 5);
    private final Path duvet = new Path();

    IllustratedBedRenderer(Context context) {
        if (shared == null) {
            try (InputStream input = context.getAssets().open("avatar/furniture/salve_bed.png")) {
                shared = BitmapFactory.decodeStream(input);
            } catch (IOException error) { throw new IllegalStateException("Missing Salve bed artwork", error); }
            if (shared == null) throw new IllegalStateException("Invalid Salve bed artwork");
        }
        artwork = shared;
        // Follow the duvet's folded left seam in the original sprite, not a rectangular blanket.
        duvet.moveTo(-13, -55); duvet.lineTo(-16, -50); duvet.lineTo(-14, -45);
        duvet.lineTo(-17, -38); duvet.lineTo(-16, -24); duvet.lineTo(-14, -12);
        duvet.lineTo(104, 5); duvet.lineTo(104, -80); duvet.lineTo(-13, -80); duvet.close();
    }

    void draw(Canvas canvas, float x, float floor, boolean foreground) {
        canvas.save(); canvas.translate(x, floor);
        if (foreground) canvas.clipPath(duvet);
        canvas.drawBitmap(artwork, null, bounds, paint);
        canvas.restore();
    }
}
