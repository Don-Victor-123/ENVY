import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centraliza los formatos de impresión para tickets y cortes.
 * Modifica estos valores para ajustar textos, fuentes y tamaños.
 */
public final class PrintLayoutConfig {

    private PrintLayoutConfig() {
    }

    public static final class Corte {
        private Corte() {
        }

        public static final String TITULO = "CORTE YO MERENGUES";
        public static final String SUBTITULO = "CORTE DEL DÍA";
        public static final String ENCABEZADO_TICKETS = "Tickets:";
        public static final String LABEL_DESDE_FOLIO = "Desde folio: ";
        public static final String LABEL_HASTA_FOLIO = "Hasta folio: ";
        public static final String LABEL_GENERADO = "Generado: ";
        public static final int ANCHO_LINEA = 32;
        public static final Font FUENTE_TITULO = new Font("Monospaced", Font.BOLD, 14);
        public static final Font FUENTE_SUBTITULO = new Font("Monospaced", Font.BOLD, 14);
        public static final Font FUENTE_FECHA = new Font("Monospaced", Font.BOLD, 14);
        public static final Font FUENTE_RANGO = new Font("Monospaced", Font.PLAIN, 14);
        public static final Font FUENTE_SEPARADOR = new Font("Monospaced", Font.PLAIN, 14);
        public static final Font FUENTE_RESUMEN = new Font("Monospaced", Font.PLAIN, 14);
        public static final Font FUENTE_TICKET = new Font("Monospaced", Font.PLAIN, 14);
        public static final Font FUENTE_FOOTER = new Font("Monospaced", Font.PLAIN, 14);
        public static final Font FUENTE_POR_DEFECTO = new Font("Monospaced", Font.PLAIN, 14);
        public static final int ESPACIADO_RENGLON = 12;
        public static final int MARGEN_IZQUIERDO = 10;
        public static final int MARGEN_SUPERIOR = 14;
        public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
        public static final DateTimeFormatter FORMATO_RANGO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        public static final DateTimeFormatter FORMATO_HORA_TICKET = DateTimeFormatter.ofPattern("HH:mm:ss");

        public static FormattedCorte formatear(LocalDate opDate, CorteService.CorteResumen resumen, ZoneId zone,
                LocalTime shiftStart, LocalTime shiftEnd) {
            java.util.List<Line> lineas = new java.util.ArrayList<>();
            lineas.add(new Line(centrar(TITULO, ANCHO_LINEA), FUENTE_TITULO));
            lineas.add(new Line(centrar(SUBTITULO, ANCHO_LINEA), FUENTE_SUBTITULO));
            lineas.add(new Line(centrar(FORMATO_FECHA.format(opDate), ANCHO_LINEA), FUENTE_FECHA));
            lineas.add(new Line(centrar(rangoOperativo(opDate, zone, shiftStart, shiftEnd), ANCHO_LINEA), FUENTE_RANGO));
            lineas.add(new Line(repetir("-", ANCHO_LINEA), FUENTE_SEPARADOR));
            lineas.add(new Line("Tickets: " + resumen.totalTickets(), FUENTE_RESUMEN));
            if (resumen.totalTickets() > 0) {
                lineas.add(new Line(LABEL_DESDE_FOLIO + resumen.folioInicial(), FUENTE_RESUMEN));
                lineas.add(new Line(LABEL_HASTA_FOLIO + resumen.folioFinal(), FUENTE_RESUMEN));
                lineas.add(new Line(ENCABEZADO_TICKETS, FUENTE_RESUMEN));
                for (CorteService.TicketRecord ticket : resumen.tickets()) {
                    StringBuilder linea = new StringBuilder();
                    linea.append("Folio: ").append(ticket.folio());
                    ZonedDateTime ts = ticket.timestamp();
                    if (ts != null) {
                        linea.append(" Hora: ")
                                .append(FORMATO_HORA_TICKET.format(ts.withZoneSameInstant(zone).toLocalTime()));
                    }
                    String mensaje = ticket.mensaje();
                    if (mensaje != null && !mensaje.isBlank()) {
                        linea.append(" - ").append(mensaje);
                    }
                    lineas.add(new Line(linea.toString(), FUENTE_TICKET));
                }
            }
            lineas.add(new Line(repetir("-", ANCHO_LINEA), FUENTE_SEPARADOR));
            lineas.add(new Line(LABEL_GENERADO + FORMATO_RANGO.format(ZonedDateTime.now(zone)), FUENTE_FOOTER));
            return new FormattedCorte(lineas);
        }

        public static FormattedCorte wrapPlainText(String texto) {
            java.util.List<Line> lineas = new java.util.ArrayList<>();
            if (texto == null || texto.isEmpty()) {
                return new FormattedCorte(lineas);
            }
            String[] split = texto.split("\\R");
            for (String line : split) {
                lineas.add(new Line(line, FUENTE_POR_DEFECTO));
            }
            return new FormattedCorte(lineas);
        }

        private static String rangoOperativo(LocalDate opDate, ZoneId zone, LocalTime shiftStart, LocalTime shiftEnd) {
            ZonedDateTime start = DateOps.windowStart(opDate, zone, shiftStart);
            ZonedDateTime end = DateOps.windowEnd(opDate, zone, shiftStart, shiftEnd);
            return FORMATO_RANGO.format(start) + " → " + FORMATO_RANGO.format(end);
        }

        private static String centrar(String texto, int ancho) {
            if (texto.length() >= ancho) {
                return texto;
            }
            int espaciosIzquierda = (ancho - texto.length()) / 2;
            char[] pad = new char[espaciosIzquierda];
            java.util.Arrays.fill(pad, ' ');
            return new String(pad) + texto;
        }

        private static String repetir(String texto, int repeticiones) {
            StringBuilder sb = new StringBuilder(texto.length() * repeticiones);
            for (int i = 0; i < repeticiones; i++) {
                sb.append(texto);
            }
            return sb.toString();
        }

        public record Line(String text, Font font) {
            public Line {
                if (text == null) {
                    text = "";
                }
            }

            public Font fontOrDefault(Font fallback) {
                return font != null ? font : fallback;
            }
        }

        public static final class FormattedCorte {
            private final java.util.List<Line> lineas;

            private FormattedCorte(java.util.List<Line> lineas) {
                this.lineas = java.util.List.copyOf(lineas);
            }

            public java.util.List<Line> lineas() {
                return lineas;
            }

            public String texto() {
                if (lineas.isEmpty()) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lineas.size(); i++) {
                    if (i > 0) {
                        sb.append('\n');
                    }
                    sb.append(lineas.get(i).text());
                }
                sb.append('\n');
                return sb.toString();
            }
        }
    }

    public static final class Ticket {
        private Ticket() {
        }

        public static final Font FUENTE_FOLIO = new Font("SansSerif", Font.BOLD, 9);
        public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 55);
        public static final Font FUENTE_FECHA = new Font("SansSerif", Font.PLAIN, 15);
        public static final Font FUENTE_MENSAJE = new Font("SansSerif", Font.BOLD, 16);
        public static final String PREFIJO_FOLIO = "FOLIO: LP";
        public static final String TITULO = "ENVY";
        public static final String PREFIJO_FECHA = "FECHA: ";
        public static final int ESPACIADO_LINEA = 4;
        public static final int DESFASE_FOLIO_SUPERIOR = 60;
        public static final int DESFASE_FOLIO_DERECHO = 10;
    }
}
