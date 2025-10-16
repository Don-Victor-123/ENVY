
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 * Imprime un ticket térmico leyendo datos desde Console.* - Console.MENSAJE -
 * Console.FECHA_ISO (yyyy-MM-dd) - Console.LOGO_PATH (opcional) -
 * Console.PRINTER_HINT ("" = predeterminada) - Console.FOLIO_ACTUAL (long) ->
 * se imprime y luego se incrementa - Console.FOLIO_WIDTH (int) -> ancho con
 * ceros a la izquierda
 */
public class PruebaImpresion {

    private static final double ANCHO_MM = 80.0;   // usa 80.0 si tu rollo es 80mm
    private static final double ALTO_MM = 60.0;  // alto “largo”; ajusta si hace falta
    private static final double MARGIN_MM = 0.0;

    /**
     * Método que usa los valores que dejaste en Console.*
     */
    public static void imprimirDesdeConsole() throws Exception {
        // Variables leídas una sola vez (efectivamente finales para la lambda)
        final String mensaje = Console.MENSAJE;
        final String fechaIso = Console.FECHA_ISO;    // yyyy-MM-dd
        final String logoPath = Console.LOGO_PATH;    // opcional
        final String hint = Console.PRINTER_HINT; // "" = predeterminada
        final String fechaMostrar = toFechaDDMMYYYY(fechaIso);

        // Tomamos el folio actual y lo formateamos con ceros a la izquierda
        final long folioNumero = Console.FOLIO_ACTUAL;
        final String folio = padLeftZeros(Long.toString(folioNumero), Console.FOLIO_WIDTH);

        PageFormat pf = buildTicketPageFormatNoMargins(ANCHO_MM, ALTO_MM);

        PrintService svc = findService(hint);

        Printable contenido = (graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double w = pageFormat.getImageableWidth();
            double h = pageFormat.getImageableHeight();
            int y = 0, ls = 4;

// Origen ya está en imageableX,imageableY por tu translate previo
            AffineTransform old = g2.getTransform();

            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();

            String folioText = "FOLIO: LP" + folio;

// Pegado al borde derecho del área imprimible
            int topInsetPts = 60;         // distancia desde arriba
            int rightNudge = 10;         // 0 = justo al borde del área imprimible

            g2.translate(pageFormat.getImageableWidth() - rightNudge, 0);
            g2.rotate(Math.PI / 2);
            g2.drawString(folioText, topInsetPts, fm.getAscent());

            g2.setTransform(old);

// === 1) Logo (centrado, si existe) ===
            if (logoPath != null && !logoPath.isBlank()) {
                try {
                    BufferedImage img = ImageIO.read(new File(logoPath));
                    if (img != null) {
                        // Escalar a ancho del papel manteniendo proporción
                        int targetW = (int) Math.round(w);
                        int targetH = (int) Math.round(img.getHeight() * (targetW / (double) img.getWidth()));
                        Image scaled = img.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);

                        // CENTRAR horizontalmente
                        int ix = (int) Math.round((w - targetW) / 2.0);
                        g2.drawImage(scaled, ix, y, null);
                        y += targetH + 6;
                    }
                } catch (Exception ignore) {
                }
            }

// === 2) Título centrado ===
            g2.setFont(new Font("SansSerif", Font.BOLD, 55));
            String titulo = "ENVY";
            int tw = g2.getFontMetrics().stringWidth(titulo);
            g2.drawString(titulo, (int) Math.round((w - tw) / 2.0), y + g2.getFontMetrics().getAscent());
            y += g2.getFontMetrics().getHeight() + ls;

// === 3) Fecha centrada ===
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            String fechaLinea = "FECHA: " + fechaMostrar;
            int fw = g2.getFontMetrics().stringWidth(fechaLinea);
            g2.drawString(fechaLinea, (int) Math.round((w - fw) / 2.0), y + g2.getFontMetrics().getAscent());
            y += g2.getFontMetrics().getHeight() + ls;

// === 4) Mensaje principal centrado ===
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            int mw = g2.getFontMetrics().stringWidth(mensaje);
            g2.drawString(mensaje, (int) Math.round((w - mw) / 2.0), y + g2.getFontMetrics().getAscent());
            y += g2.getFontMetrics().getHeight() + 18;

            return Printable.PAGE_EXISTS;
        };

        PrinterJob job = PrinterJob.getPrinterJob();
        if (svc != null) {
            job.setPrintService(svc);
        }
        job.setPrintable(contenido, pf);
        job.print(); // ¡a imprimir!

        // Autoincrementa el folio para la próxima impresión
        Console.FOLIO_ACTUAL = folioNumero + 1;

        System.out.println("Impresión enviada " + (svc != null ? "a: " + svc.getName() : "(predeterminada)"));
        System.out.println("Siguiente folio será: " + padLeftZeros(Long.toString(Console.FOLIO_ACTUAL), Console.FOLIO_WIDTH));
    }

    // ===== Utilidades =====
    /**
     * Crea PageFormat para ticket térmico con márgenes mínimos.
     */
    // private static PageFormat buildTicketPageFormat(
    //         double anchoMM, double altoMM,
    //         double marginLeftMM, double marginTopMM,
    //         double marginRightMM, double marginBottomMM) {
    //     double ptPerMM = 72d / 25.4d;
    //     double w = anchoMM * ptPerMM;
    //     double h = altoMM * ptPerMM;
    //     double ml = marginLeftMM * ptPerMM;
    //     double mt = marginTopMM * ptPerMM;
    //     double mr = marginRightMM * ptPerMM;
    //     double mb = marginBottomMM * ptPerMM;
    //     Paper paper = new Paper();
    //     paper.setSize(w, h);
    //     paper.setImageableArea(ml, mt, w - (ml + mr), h - (mt + mb));
    //     PageFormat pf = new PageFormat();
    //     pf.setPaper(paper);
    //     return pf;
    // }
    private static PageFormat buildTicketPageFormatNoMargins(double anchoMM, double altoMM) {
        double ptPerMM = 72d / 25.4d;
        double w = anchoMM * ptPerMM;
        double h = altoMM * ptPerMM;

        Paper paper = new Paper();
        paper.setSize(w, h);
        paper.setImageableArea(0, 0, w, h); // ← sin márgenes (puede ser ignorado por el driver)

        PageFormat pf = new PageFormat();
        pf.setPaper(paper);
        return pf;
    }

    /**
     * Busca impresora por hint; si vacío o no encuentra, devuelve la
     * predeterminada.
     */
    private static PrintService findService(String hint) {
        try {
            if (hint == null || hint.isBlank()) {
                return PrintServiceLookup.lookupDefaultPrintService();
            }
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services != null) {
                for (PrintService ps : services) {
                    if (ps.getName().toLowerCase().contains(hint.toLowerCase())) {
                        return ps;
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    /**
     * Convierte yyyy-MM-dd a dd/MM/yyyy; si falla, regresa el texto original.
     */
    private static String toFechaDDMMYYYY(String iso) {
        try {
            LocalDate f = LocalDate.parse(iso, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return iso;
        }
    }

    /**
     * Padding con ceros a la izquierda hasta 'width' dígitos.
     */
    private static String padLeftZeros(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) {
            sb.append('0');
        }
        sb.append(s);
        return sb.toString();
    }
}
