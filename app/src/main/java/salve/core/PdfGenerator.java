package salve.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PdfGenerator unificado: crea PDFs desde IMÁGENES y desde TEXTOS.
 * - Mantiene el método nuevo que devuelve File (imágenes)
 * - Mantiene compatibilidad con firmas antiguas (boolean)
 * - Añade creación de PDF desde texto y desde lista de párrafos
 */
public class PdfGenerator {
    private final Context ctx;

    public PdfGenerator(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    // ==========================
    //      IMÁGENES → PDF
    // ==========================
    public File crearPdfDesdeImagenes(List<Bitmap> imagenes, String nombreBase) throws IOException {
        if (imagenes == null || imagenes.isEmpty()) {
            throw new IllegalArgumentException("No hay imágenes");
        }

        PdfDocument pdf = new PdfDocument();
        int page = 1;
        for (Bitmap bmp : imagenes) {
            if (bmp == null) continue;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                    bmp.getWidth(), bmp.getHeight(), page++
            ).create();
            PdfDocument.Page p = pdf.startPage(info);
            Canvas c = p.getCanvas();
            c.drawBitmap(bmp, 0, 0, null);
            pdf.finishPage(p);
        }

        File out = ensureOutputFile(nombreBase);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            pdf.writeTo(fos);
        }
        pdf.close();
        return out;
    }

    // Compatibilidad con versiones anteriores (si existían)
    public boolean crearPdfDesdeImagenes(Boolean compatibilityFlag,
                                         List<Bitmap> imagenes,
                                         String nombreBase) {
        try {
            File f = crearPdfDesdeImagenes(imagenes, nombreBase);
            return f != null && f.exists();
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================
    //        TEXTO → PDF
    // ==========================
    /**
     * Crea un PDF a partir de un texto largo (con salto de página automático).
     * Tamaño de página por defecto: A4 595x842 px (72 dpi aprox.).
     */
    public File crearPdfDesdeTexto(String contenido, String nombreBase) throws IOException {
        if (contenido == null || contenido.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido está vacío");
        }
        List<String> parrafos = new ArrayList<>();
        // Normaliza saltos de línea tipo Windows/Mac
        for (String p : contenido.replace("\r\n", "\n").replace('\r','\n').split("\n\n")) {
            parrafos.add(p);
        }
        return crearPdfDesdeParrafos(parrafos, nombreBase);
    }

    /**
     * Crea un PDF a partir de una lista de párrafos.
     */
    public File crearPdfDesdeParrafos(List<String> parrafos, String nombreBase) throws IOException {
        if (parrafos == null || parrafos.isEmpty()) {
            throw new IllegalArgumentException("La lista de párrafos está vacía");
        }

        final int pageW = 595; // A4 aprox.
        final int pageH = 842;
        final int margin = 40; // 40px por lado
        final int usableW = pageW - margin * 2;
        final int lineHeight = 18; // 12pt aprox.

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(12f);
        paint.setColor(0xFF000000);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));

        PdfDocument pdf = new PdfDocument();
        int pageNum = 1;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create();
        PdfDocument.Page page = pdf.startPage(info);
        Canvas canvas = page.getCanvas();
        int x = margin;
        int y = margin + 20;

        for (int pi = 0; pi < parrafos.size(); pi++) {
            List<String> lineas = wrapText(parrafos.get(pi), paint, usableW);
            for (String linea : lineas) {
                // Salto de página si no cabe la siguiente línea
                if (y + lineHeight > pageH - margin) {
                    pdf.finishPage(page);
                    info = new PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNum).create();
                    page = pdf.startPage(info);
                    canvas = page.getCanvas();
                    y = margin + 20;
                }
                canvas.drawText(linea, x, y, paint);
                y += lineHeight;
            }
            // Espacio entre párrafos
            y += lineHeight;
        }

        pdf.finishPage(page);
        File out = ensureOutputFile(nombreBase);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            pdf.writeTo(fos);
        }
        pdf.close();
        return out;
    }

    // ==========================
    //        HELPERS
    // ==========================
    private File ensureOutputFile(String nombreBase) {
        File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        return new File(dir, nombreBase + "-" + System.currentTimeMillis() + ".pdf");
    }

    /**
     * Envuelve texto a líneas que quepan en el ancho disponible usando Paint.measureText.
     */
    // Pega este método en tu PdfGenerator (reemplaza el wrapText actual)
    private static List<String> wrapText(String texto, Paint paint, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (texto == null) return out;

        // Separar por saltos de línea simples (sin etiquetas ni "regex:" en el código)
        String[] paragraphs = texto.split("\n");
        for (String p : paragraphs) {
            String[] words = p.trim().split("\\s+");
            StringBuilder line = new StringBuilder();
            for (String w : words) {
                if (line.length() == 0) {
                    line.append(w);
                } else {
                    String candidate = line + " " + w;
                    if (paint.measureText(candidate) <= maxWidth) {
                        line.append(" ").append(w);
                    } else {
                        out.add(line.toString());
                        line = new StringBuilder(w);
                    }
                }
            }
            if (line.length() > 0) out.add(line.toString());
            // Línea en blanco entre párrafos
            out.add("");
        }
        return out;
    }

}

