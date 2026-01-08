package salve.core;

import android.graphics.Path;

public class ParametricGlyph {

    public enum Style { ORB, SIGIL, SPIRAL }

    public static Path build(long seed, Style style, float sizePx) {
        SeedRand rnd = new SeedRand(seed);

        float rBig = 80f + rnd.nextFloat() * 60f;
        float rSmall = 20f + rnd.nextFloat() * 25f;
        float offset = 10f + rnd.nextFloat() * 70f;

        int steps = 1200;
        float turns = 6f + rnd.nextFloat() * 8f;
        float tMax = (float) (Math.PI * 2.0 * turns);

        if (style == Style.SIGIL) {
            offset *= 0.65f;
        }
        if (style == Style.SPIRAL) {
            rSmall *= 0.75f;
            turns *= 1.35f;
            tMax = (float) (Math.PI * 2.0 * turns);
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        float[] xs = new float[steps];
        float[] ys = new float[steps];

        for (int i = 0; i < steps; i++) {
            float t = (tMax * i) / (steps - 1f);

            float ratio = (rBig - rSmall) / rSmall;
            float x = (rBig - rSmall) * (float) Math.cos(t)
                    + offset * (float) Math.cos(ratio * t);
            float y = (rBig - rSmall) * (float) Math.sin(t)
                    - offset * (float) Math.sin(ratio * t);

            xs[i] = x;
            ys[i] = y;

            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        float width = maxX - minX;
        float height = maxY - minY;
        float scale = width > height ? (sizePx / width) : (sizePx / height);

        float centerX = (minX + maxX) / 2f;
        float centerY = (minY + maxY) / 2f;

        Path path = new Path();
        boolean first = true;
        for (int i = 0; i < steps; i++) {
            float x = (xs[i] - centerX) * scale;
            float y = (ys[i] - centerY) * scale;

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        path.close();
        return path;
    }

    static class SeedRand {
        private long s;

        SeedRand(long seed) {
            s = seed == 0 ? 1 : seed;
        }

        private int next() {
            s ^= (s << 13);
            s ^= (s >> 7);
            s ^= (s << 17);
            return (int) (s & 0x7fffffff);
        }

        float nextFloat() {
            return next() / (float) 0x7fffffff;
        }
    }
}
