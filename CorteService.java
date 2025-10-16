import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Encapsula la persistencia y cálculo de cortes por día operativo.
 */
public class CorteService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ZoneId zone;
    private final LocalTime shiftStart;
    private final LocalTime shiftEnd;

    public CorteService(ZoneId zone, LocalTime shiftStart, LocalTime shiftEnd) {
        this.zone = Objects.requireNonNull(zone, "zone");
        this.shiftStart = Objects.requireNonNull(shiftStart, "shiftStart");
        this.shiftEnd = Objects.requireNonNull(shiftEnd, "shiftEnd");
    }

    public void ensureOperationalDirectory(LocalDate opDate) throws IOException {
        Files.createDirectories(PathResolver.cortesDir(opDate));
    }

    public void registrarTicket(long folio, String mensaje, ZonedDateTime timestamp) throws IOException {
        Objects.requireNonNull(timestamp, "timestamp");
        LocalDate opDate = DateOps.operationalDate(timestamp.toLocalDateTime(), zone, shiftStart);
        ensureOperationalDirectory(opDate);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("folio", folio);
        data.put("fecha", ISO_DATE.format(timestamp.toLocalDate()));
        data.put("hora", HORA.format(timestamp.toLocalTime()));
        data.put("mensaje", mensaje == null ? "" : mensaje);
        data.put("operationalDate", opDate.format(ISO_DATE));
        data.put("timestamp", timestamp.toOffsetDateTime().toString());

        Path ticketsFile = PathResolver.ticketsFile(opDate);
        appendLineFsync(ticketsFile, toJsonLine(data));
        writeTextFsync(PathResolver.ultimoFolioFile(opDate), Long.toString(folio));
    }

    public Optional<Long> recuperarSiguienteFolio(LocalDate opDate) {
        Path p = PathResolver.ultimoFolioFile(opDate);
        if (!Files.exists(p)) {
            return Optional.empty();
        }
        try {
            String s = Files.readString(p, StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) {
                return Optional.empty();
            }
            long ultimo = Long.parseLong(s);
            return Optional.of(ultimo + 1);
        } catch (Exception e) {
            System.err.println("No se pudo leer ultimo_folio.txt: " + e.getMessage());
            return Optional.empty();
        }
    }

    public boolean hayTickets(LocalDate opDate) {
        try {
            return !loadTickets(opDate).isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean estaCorteHecho(LocalDate opDate) {
        return Files.exists(PathResolver.corteFlag(opDate));
    }

    public CorteResumen calcularResumen(LocalDate opDate) throws IOException {
        List<TicketRecord> tickets = loadTickets(opDate);
        int total = tickets.size();
        long folioInicial = 0;
        long folioFinal = 0;
        ZonedDateTime firstTs = null;
        ZonedDateTime lastTs = null;

        for (TicketRecord t : tickets) {
            long folio = t.folio();
            if (folioInicial == 0 || folio < folioInicial) {
                folioInicial = folio;
            }
            if (folio > folioFinal) {
                folioFinal = folio;
            }
            if (firstTs == null || t.timestamp().isBefore(firstTs)) {
                firstTs = t.timestamp();
            }
            if (lastTs == null || t.timestamp().isAfter(lastTs)) {
                lastTs = t.timestamp();
            }
        }

        return new CorteResumen(opDate, total, folioInicial, folioFinal, firstTs, lastTs);
    }

    public void guardarCorte(LocalDate opDate, String cuerpo) throws IOException {
        writeTextFsync(PathResolver.corteTxt(opDate), cuerpo);
    }

    public Optional<String> leerCorte(LocalDate opDate) {
        Path p = PathResolver.corteTxt(opDate);
        if (!Files.exists(p)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(p, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("No se pudo leer corte.txt: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void marcarCorte(LocalDate opDate) throws IOException {
        Files.createDirectories(PathResolver.cortesDir(opDate));
        Files.writeString(PathResolver.corteFlag(opDate), "ok", StandardCharsets.UTF_8, CREATE, WRITE,
                TRUNCATE_EXISTING);
    }

    public String formatearCorte(LocalDate opDate, CorteResumen resumen) {
        StringBuilder sb = new StringBuilder();
        sb.append(center("CORTE YO MERENGUES", 25)).append('\n');
        sb.append(center("CORTE DEL DÍA", 25)).append('\n');
        sb.append(center(opDate.format(ISO_DATE), 25)).append('\n');
        sb.append(center(rangoOperativo(opDate), 25)).append('\n');
        sb.append(repeat("-", 32)).append('\n');
        sb.append("Tickets: ").append(resumen.totalTickets()).append('\n');
        if (resumen.totalTickets() > 0) {
            sb.append("Desde folio: ").append(resumen.folioInicial()).append('\n');
            sb.append("Hasta folio: ").append(resumen.folioFinal()).append('\n');
        }
        sb.append(repeat("-", 25)).append('\n');
        sb.append("Generado: ").append(RANGE_FMT.format(ZonedDateTime.now(zone))).append('\n');
        return sb.toString();
    }

    public List<TicketRecord> loadTickets(LocalDate opDate) throws IOException {
        List<TicketRecord> tickets = new ArrayList<>();
        Set<Path> visited = new LinkedHashSet<>();
        for (Path file : PathResolver.candidateTicketFiles(opDate)) {
            if (!visited.add(file) || !Files.exists(file)) {
                continue;
            }
            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    Map<String, Object> obj = parseJsonLine(line);
                    if (obj == null) {
                        continue;
                    }
                    TicketRecord ticket = toTicketRecord(obj);
                    if (ticket == null) {
                        continue;
                    }
                    LocalDate doTicket = DateOps.operationalDate(ticket.timestamp().toLocalDateTime(), zone,
                            shiftStart);
                    if (!doTicket.equals(opDate)) {
                        continue;
                    }
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }

    private TicketRecord toTicketRecord(Map<String, Object> obj) {
        Object folioObj = obj.get("folio");
        if (!(folioObj instanceof Number)) {
            return null;
        }
        long folio = ((Number) folioObj).longValue();

        ZonedDateTime timestamp = parseTimestamp(obj);
        if (timestamp == null) {
            return null;
        }
        String mensaje = Objects.toString(obj.getOrDefault("mensaje", ""), "");
        return new TicketRecord(folio, timestamp, mensaje);
    }

    private ZonedDateTime parseTimestamp(Map<String, Object> obj) {
        Object ts = obj.get("timestamp");
        if (ts instanceof String tsStr && !tsStr.isBlank()) {
            try {
                return ZonedDateTime.parse(tsStr);
            } catch (Exception ignored) {
            }
        }
        Object fecha = obj.get("fecha");
        Object hora = obj.get("hora");
        if (fecha instanceof String fechaStr && hora instanceof String horaStr) {
            try {
                LocalDate f = LocalDate.parse(fechaStr, ISO_DATE);
                LocalTime h = LocalTime.parse(horaStr, HORA);
                LocalDateTime ldt = LocalDateTime.of(f, h);
                return ldt.atZone(zone);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String rangoOperativo(LocalDate opDate) {
        ZonedDateTime start = DateOps.windowStart(opDate, zone, shiftStart);
        ZonedDateTime end = DateOps.windowEnd(opDate, zone, shiftStart, shiftEnd);
        return RANGE_FMT.format(start) + " → " + RANGE_FMT.format(end);
    }

    private static String toJsonLine(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else {
                sb.append('"').append(escape(v.toString())).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static Map<String, Object> parseJsonLine(String json) {
        try {
            Map<String, Object> res = new LinkedHashMap<>();
            String s = json.trim();
            if (!s.startsWith("{") || !s.endsWith("}")) {
                return null;
            }
            s = s.substring(1, s.length() - 1).trim();
            if (s.isEmpty()) {
                return res;
            }
            String[] parts = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            for (String part : parts) {
                String[] kv = part.split(":", 2);
                if (kv.length != 2) {
                    continue;
                }
                String key = unquote(kv[0].trim());
                String val = kv[1].trim();
                if (val.equals("null")) {
                    res.put(key, null);
                } else if (val.startsWith("\"")) {
                    res.put(key, unquote(val));
                } else if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                    res.put(key, Boolean.parseBoolean(val));
                } else {
                    try {
                        res.put(key, Long.parseLong(val));
                    } catch (NumberFormatException e) {
                        res.put(key, val);
                    }
                }
            }
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private static String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void appendLineFsync(Path p, String line) throws IOException {
        Files.createDirectories(p.getParent());
        try (FileChannel ch = FileChannel.open(p, CREATE, WRITE, APPEND)) {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(line + System.lineSeparator());
            while (bb.hasRemaining()) {
                ch.write(bb);
            }
            ch.force(true);
        }
    }

    private static void writeTextFsync(Path p, String text) throws IOException {
        Files.createDirectories(p.getParent());
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        try (FileChannel ch = FileChannel.open(tmp, CREATE, WRITE, TRUNCATE_EXISTING)) {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(text);
            while (bb.hasRemaining()) {
                ch.write(bb);
            }
            ch.force(true);
        }
        try {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String center(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        int left = (width - s.length()) / 2;
        char[] pad = new char[left];
        java.util.Arrays.fill(pad, ' ');
        return new String(pad) + s;
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public record TicketRecord(long folio, ZonedDateTime timestamp, String mensaje) {
    }

    public record CorteResumen(LocalDate opDate, int totalTickets, long folioInicial, long folioFinal,
            ZonedDateTime primerTicket, ZonedDateTime ultimoTicket) {
        public boolean hasTickets() {
            return totalTickets > 0;
        }
    }
}
