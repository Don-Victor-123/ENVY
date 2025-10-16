import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorteServiceTest {

    private static Path baseDir;
    private CorteService service;
    private final ZoneId zone = DateOps.DEFAULT_ZONE;

    @BeforeAll
    static void setUpBaseDir() throws IOException {
        baseDir = Files.createTempDirectory("cortes-test");
        System.setProperty("envy.cortes.dir", baseDir.toString());
    }

    @AfterAll
    static void cleanBaseDir() throws IOException {
        if (baseDir != null) {
            try (Stream<Path> stream = Files.walk(baseDir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                    if (!path.equals(baseDir)) {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    }
                });
            }
            Files.deleteIfExists(baseDir);
        }
        System.clearProperty("envy.cortes.dir");
    }

    @BeforeEach
    void clean() throws IOException {
        if (baseDir != null) {
            try (Stream<Path> stream = Files.walk(baseDir)) {
                stream.filter(path -> !path.equals(baseDir))
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
        service = new CorteService(DateOps.DEFAULT_ZONE, DateOps.DEFAULT_SHIFT_START, DateOps.DEFAULT_SHIFT_END);
    }

    @Test
    void aggregatesTicketsAcrossMidnight() throws IOException {
        LocalDate opDate = LocalDate.of(2025, 10, 10);
        service.registrarTicket(1001, "Noche", ZonedDateTime.of(2025, 10, 10, 21, 0, 0, 0, zone));
        service.registrarTicket(1002, "Madrugada", ZonedDateTime.of(2025, 10, 11, 1, 0, 0, 0, zone));

        List<CorteService.TicketRecord> tickets = service.loadTickets(opDate);
        assertEquals(2, tickets.size());
        CorteService.CorteResumen resumen = service.calcularResumen(opDate);
        assertEquals(2, resumen.totalTickets());
        assertEquals(1001, resumen.folioInicial());
        assertEquals(1002, resumen.folioFinal());
        assertEquals(List.of(1001L, 1002L),
                resumen.tickets().stream().map(CorteService.TicketRecord::folio).collect(Collectors.toList()));
        assertTrue(service.hayTickets(opDate));
    }

    @Test
    void readsLegacySplitTicketFiles() throws IOException {
        LocalDate opDate = LocalDate.of(2025, 10, 10);
        Path dayDir = PathResolver.cortesDir(LocalDate.of(2025, 10, 10));
        Path nextDir = PathResolver.cortesDir(LocalDate.of(2025, 10, 11));
        Files.createDirectories(dayDir);
        Files.createDirectories(nextDir);

        Files.writeString(dayDir.resolve("tickets.jsonl"),
                "{\"folio\":2001,\"fecha\":\"2025-10-10\",\"hora\":\"23:50:00\",\"mensaje\":\"A\"}" + System.lineSeparator(),
                StandardCharsets.UTF_8);
        Files.writeString(nextDir.resolve("tickets.jsonl"),
                "{\"folio\":2002,\"fecha\":\"2025-10-11\",\"hora\":\"00:10:00\",\"mensaje\":\"B\"}" + System.lineSeparator(),
                StandardCharsets.UTF_8);

        CorteService.CorteResumen resumen = service.calcularResumen(opDate);
        assertEquals(2, resumen.totalTickets());
        assertEquals(2001, resumen.folioInicial());
        assertEquals(2002, resumen.folioFinal());
        assertTrue(service.hayTickets(opDate));
        assertFalse(service.hayTickets(LocalDate.of(2025, 10, 12)));
    }

    @Test
    void incluyeFolioPorTicketEnFormato() throws IOException {
        LocalDate opDate = LocalDate.of(2025, 10, 10);
        service.registrarTicket(3001, "Primero", ZonedDateTime.of(2025, 10, 10, 21, 15, 0, 0, zone));
        service.registrarTicket(3002, "Segundo", ZonedDateTime.of(2025, 10, 11, 1, 45, 0, 0, zone));

        CorteService.CorteResumen resumen = service.calcularResumen(opDate);
        String corte = service.formatearCorte(opDate, resumen);

        assertTrue(corte.contains("Folio: 3001"));
        assertTrue(corte.contains("Folio: 3002"));
    }
}
