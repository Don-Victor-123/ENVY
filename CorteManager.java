import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

public class CorteManager {

    /** 0 = imprimir corte al cerrar; 1 = NO imprimir corte al cerrar */
    public static int BANDERA_IMPRESION_AL_CERRAR = 0;

    private static final DateTimeFormatter ISO  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Path BASE = Paths.get("cortes");
    private static final String TICKETS_FILE     = "tickets.jsonl";
    private static final String ULTIMO_FOLIO_FILE= "ultimo_folio.txt";
    private static final String CORTE_FLAG       = "corte_hecho.flag";
    private static final String CORTE_TXT        = "corte.txt";

    private static final String PASSWORD_POR_DEFECTO = "842867";
    private static final Path PASSWORD_FILE = Paths.get("corte_password.txt");

    /** Llamar al arrancar. */
    public static void init(String fechaIso) {
        try { Files.createDirectories(dirDia(fechaIso)); }
        catch (IOException e) { System.err.println("No se pudo crear carpeta de cortes: " + e.getMessage()); }
    }

    private static Path dirDia(String fechaIso) { return BASE.resolve(fechaIso); }

    /** Registrar SIEMPRE tras imprimir un ticket. */
    public static void registrarTicket(long folio, String mensaje, String fechaIso) {
        String hora = HORA.format(LocalDateTime.now());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("folio", folio);
        data.put("fecha", fechaIso);
        data.put("hora", hora);
        data.put("mensaje", mensaje == null ? "" : mensaje);

        String lineaJson = toJsonLine(data);
        Path p = dirDia(fechaIso).resolve(TICKETS_FILE);
        try {
            appendLineFsync(p, lineaJson);
            writeTextFsync(dirDia(fechaIso).resolve(ULTIMO_FOLIO_FILE), Long.toString(folio));
        } catch (IOException e) {
            System.err.println("No se pudo registrar el ticket: " + e.getMessage());
        }
    }

    /** Siguiente folio a partir del registro. Null si no hay. */
    public static Long recuperarSiguienteFolio(String fechaIso) {
        Path p = dirDia(fechaIso).resolve(ULTIMO_FOLIO_FILE);
        if (!Files.exists(p)) return null;
        try {
            String s = Files.readString(p).trim();
            long ultimo = Long.parseLong(s);
            return ultimo + 1;
        } catch (Exception e) {
            System.err.println("No se pudo leer " + ULTIMO_FOLIO_FILE + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean estaCorteHecho(String fechaIso) { return Files.exists(dirDia(fechaIso).resolve(CORTE_FLAG)); }

    public static boolean hayTicketsHoy(String fechaIso) {
        Path p = dirDia(fechaIso).resolve(TICKETS_FILE);
        try { return Files.exists(p) && Files.size(p) > 0; } catch (IOException e) { return false; }
    }

    public static boolean validarContrasena(Scanner scanner) {
        String esperada = leerPassword();
        System.out.print("Ingrese la contraseña del corte: ");
        String ingresada = scanner.nextLine();
        return esperada.equals(ingresada);
    }

    private static String leerPassword() {
        try { if (Files.exists(PASSWORD_FILE)) return Files.readString(PASSWORD_FILE).trim(); }
        catch (IOException ignored) {}
        return PASSWORD_POR_DEFECTO;
    }

    /** Ejecuta corte + impresión y marca bandera. */
    public static boolean hacerCorteConImpresion(String fechaIso, String printerHint) {
        try {
            CorteResumen r = calcularResumen(fechaIso);
            if (r.totalTickets == 0) { System.out.println("No hay tickets para cortar."); return false; }

            Path txt = dirDia(fechaIso).resolve(CORTE_TXT);
            String cuerpo = formatearCorte(r);
            writeTextFsync(txt, cuerpo);

            imprimirTexto(cuerpo, printerHint);

            Files.writeString(dirDia(fechaIso).resolve(CORTE_FLAG), "ok", StandardCharsets.UTF_8,
                    CREATE, WRITE, TRUNCATE_EXISTING);
            return true;
        } catch (Exception e) {
            System.err.println("Error al hacer el corte: " + e.getMessage());
            return false;
        }
    }

    /** Lógica al salir: intenta corte (si bandera=0 y hay tickets). Devuelve true si se permite salir. */
    public static boolean manejarCierreConCorte(Scanner scanner, String fechaIso, String printerHint) {
        if (BANDERA_IMPRESION_AL_CERRAR != 0) return true;  // no imprimir automáticamente
        if (estaCorteHecho(fechaIso) || !hayTicketsHoy(fechaIso)) return true;

        if (validarContrasena(scanner)) {
            boolean ok = hacerCorteConImpresion(fechaIso, printerHint);
            if (!ok) { System.out.println("No se pudo completar el corte."); return false; }
            return true;
        } else {
            System.out.println("Contraseña incorrecta.");
            System.out.print("Estás a punto de salir SIN hacer el corte. ¿Continuar? (s/n): ");
            String r1 = scanner.nextLine().trim().toLowerCase();
            if (!r1.equals("s") && !r1.equals("si") && !r1.equals("sí")) return false;

            System.out.print("¿Seguro que quieres salir sin hacer el corte? (s/n): ");
            String r2 = scanner.nextLine().trim().toLowerCase();
            if (!r2.equals("s") && !r2.equals("si") && !r2.equals("sí")) return false;

            return true; // el usuario aceptó salir sin corte
        }
    }

    // =================== Internos ===================

    private static class CorteResumen {
        String fechaIso;
        int totalTickets = 0;
        long folioInicial = 0;
        long folioFinal = 0;
    }

    private static CorteResumen calcularResumen(String fechaIso) throws IOException {
        Path p = dirDia(fechaIso).resolve(TICKETS_FILE);
        CorteResumen r = new CorteResumen();
        r.fechaIso = fechaIso;
        if (!Files.exists(p)) return r;

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                Map<String, Object> obj = parseJsonLine(line);
                if (obj == null) continue;
                long folio = ((Number) obj.getOrDefault("folio", 0)).longValue();
                if (r.totalTickets == 0) r.folioInicial = folio;
                r.folioFinal = folio;
                r.totalTickets++;
            }
        }
        return r;
    }

    private static void imprimirTexto(String texto, String printerHint) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();

        PrintService svc = elegirImpresora(printerHint);
        if (svc != null) {
            try { job.setPrintService(svc); }
            catch (PrinterException e) { System.err.println("No se pudo seleccionar la impresora: " + e.getMessage()); }
        }

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double width  = mmToPoints(80);
        double height = mmToPoints(200);
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        pf.setPaper(paper);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            int x = 10, y = 14;
            for (String line : texto.split("\\R")) { g2.drawString(line, x, y); y += 12; }
            return Printable.PAGE_EXISTS;
        }, pf);

        job.print();
    }

    private static double mmToPoints(double mm) { return (mm / 25.4) * 72.0; }

    private static PrintService elegirImpresora(String hint) {
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services == null || services.length == 0) return null;
            if (hint == null || hint.isBlank()) return PrintServiceLookup.lookupDefaultPrintService();
            String h = hint.toLowerCase(Locale.ROOT);
            for (PrintService s : services) {
                String name = s.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains(h)) return s;
            }
            return PrintServiceLookup.lookupDefaultPrintService();
        } catch (Exception e) { return null; }
    }

    private static String formatearCorte(CorteResumen r) {
        StringBuilder sb = new StringBuilder();
        sb.append(center("CORTE YO MERENGUES", 25)).append('\n');
        sb.append(center("CORTE DEL DÍA", 25)).append('\n');
        sb.append(center(r.fechaIso, 25)).append('\n');
        sb.append(repeat("-", 32)).append('\n');
        sb.append("Tickets: ").append(r.totalTickets).append('\n');
        if (r.totalTickets > 0) {
            sb.append("Desde folio: ").append(r.folioInicial).append('\n');
            sb.append("Hasta folio: ").append(r.folioFinal).append('\n');
        }
        sb.append(repeat("-", 25)).append('\n');
        sb.append("Generado: ").append(HORA.format(LocalDateTime.now())).append('\n');
        return sb.toString();
    }

    // ==== util archivos con fsync (tolerante a apagones) ====
    private static void appendLineFsync(Path p, String line) throws IOException {
        Files.createDirectories(p.getParent());
        try (FileChannel ch = FileChannel.open(p, CREATE, WRITE, APPEND)) {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(line + System.lineSeparator());
            while (bb.hasRemaining()) ch.write(bb);
            ch.force(true);
        }
    }

    private static void writeTextFsync(Path p, String text) throws IOException {
        Files.createDirectories(p.getParent());
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        try (FileChannel ch = FileChannel.open(tmp, CREATE, WRITE, TRUNCATE_EXISTING)) {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(text);
            while (bb.hasRemaining()) ch.write(bb);
            ch.force(true);
        }
        try { Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING); }
    }

    // ==== util varias ====
    private static String toJsonLine(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
            else sb.append('"').append(escape(v.toString())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static Map<String, Object> parseJsonLine(String json) {
        try {
            Map<String, Object> res = new LinkedHashMap<>();
            String s = json.trim();
            if (!s.startsWith("{") || !s.endsWith("}")) return null;
            s = s.substring(1, s.length() - 1).trim();
            if (s.isEmpty()) return res;
            String[] parts = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            for (String part : parts) {
                String[] kv = part.split(":", 2);
                String key = unquote(kv[0].trim());
                String val = kv[1].trim();
                if (val.equals("null")) res.put(key, null);
                else if (val.startsWith("\"")) res.put(key, unquote(val));
                else if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) res.put(key, Boolean.parseBoolean(val));
                else {
                    try { res.put(key, Long.parseLong(val)); }
                    catch (NumberFormatException e) { res.put(key, val); }
                }
            }
            return res;
        } catch (Exception e) { return null; }
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String center(String s, int width) {
        if (s.length() >= width) return s;
        int left = (width - s.length()) / 2;
        char[] pad = new char[left];
        Arrays.fill(pad, ' ');
        return new String(pad) + s;
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
