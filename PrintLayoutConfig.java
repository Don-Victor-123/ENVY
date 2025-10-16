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
        public static final Font FUENTE_IMPRESION = new Font("Monospaced", Font.BOLD, 14);
        public static final int ESPACIADO_RENGLON = 12;
        public static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
        public static final DateTimeFormatter FORMATO_RANGO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        public static final DateTimeFormatter FORMATO_HORA_TICKET = DateTimeFormatter.ofPattern("HH:mm:ss");

        public static String formatear(LocalDate opDate, CorteService.CorteResumen resumen, ZoneId zone,
                LocalTime shiftStart, LocalTime shiftEnd) {
            StringBuilder sb = new StringBuilder();
            sb.append(centrar(TITULO, ANCHO_LINEA)).append('\n');
            sb.append(centrar(SUBTITULO, ANCHO_LINEA)).append('\n');
            sb.append(centrar(FORMATO_FECHA.format(opDate), ANCHO_LINEA)).append('\n');
            sb.append(centrar(rangoOperativo(opDate, zone, shiftStart, shiftEnd), ANCHO_LINEA)).append('\n');
            sb.append(repetir("-", ANCHO_LINEA)).append('\n');
            sb.append("Tickets: ").append(resumen.totalTickets()).append('\n');
            if (resumen.totalTickets() > 0) {
                sb.append(LABEL_DESDE_FOLIO).append(resumen.folioInicial()).append('\n');
                sb.append(LABEL_HASTA_FOLIO).append(resumen.folioFinal()).append('\n');
                sb.append(ENCABEZADO_TICKETS).append('\n');
                for (CorteService.TicketRecord ticket : resumen.tickets()) {
                    sb.append("Folio: ").append(ticket.folio());
                    ZonedDateTime ts = ticket.timestamp();
                    if (ts != null) {
                        sb.append(" Hora: ").append(FORMATO_HORA_TICKET.format(ts.withZoneSameInstant(zone).toLocalTime()));
                    }
                    String mensaje = ticket.mensaje();
                    if (mensaje != null && !mensaje.isBlank()) {
                        sb.append(" - ").append(mensaje);
                    }
                    sb.append('\n');
                }
            }
            sb.append(repetir("-", ANCHO_LINEA)).append('\n');
            sb.append(LABEL_GENERADO)
                    .append(FORMATO_RANGO.format(ZonedDateTime.now(zone)))
                    .append('\n');
            return sb.toString();
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
