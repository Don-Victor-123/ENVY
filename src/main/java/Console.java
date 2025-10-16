import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Scanner;

public final class Console {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final LocalTime SHIFT_START = LocalTime.of(20, 0);
    private static final LocalTime SHIFT_END = LocalTime.of(8, 0);
    private static final boolean IMPRIMIR_CORTE_AL_CERRAR = true;

    private final Clock clock;
    private final ShiftConfiguration shiftConfiguration;
    private final TicketRepository ticketRepository;
    private final CorteService corteService;
    private final ReprintService reprintService;

    public Console() {
        this.clock = Clock.system(ZONE);
        this.shiftConfiguration = new ShiftConfiguration(ZONE, SHIFT_START, SHIFT_END);
        this.ticketRepository = new TicketRepository(clock, shiftConfiguration);
        Printer printer = new ConsolePrinter();
        this.corteService = new CorteService(shiftConfiguration, ticketRepository, printer);
        this.reprintService = new ReprintService(corteService);
    }

    public static void main(String[] args) {
        new Console().run();
    }

    private void run() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== MENÚ ===");
            System.out.println("1) Registrar venta");
            System.out.println("2) Generar corte actual");
            System.out.println("3) Reimprimir corte por fecha (YYYY-MM-DD)");
            System.out.println("0) Salir");
            System.out.print("> ");
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> registrarVenta(scanner);
                case "2" -> generarCorteActual();
                case "3" -> reimprimir(scanner);
                case "0" -> running = !salir();
                default -> System.out.println("Opción no válida");
            }
        }
    }

    private void registrarVenta(Scanner scanner) {
        System.out.print("ID del ticket: ");
        String id = scanner.nextLine().trim();
        System.out.print("Total: ");
        BigDecimal total = new BigDecimal(scanner.nextLine().trim());
        System.out.println("Cuerpo del ticket (finaliza con una línea vacía):");
        StringBuilder body = new StringBuilder();
        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                break;
            }
            body.append(line).append('\n');
        }
        ticketRepository.save(id, total, body.toString());
        System.out.println("Ticket registrado bajo DO " + currentOperationalDate());
    }

    private void generarCorteActual() {
        corteService.generarCorte(currentOperationalDate());
    }

    private void reimprimir(Scanner scanner) {
        System.out.print("Fecha del día operativo (YYYY-MM-DD): ");
        String input = scanner.nextLine().trim();
        try {
            reprintService.reprint(input);
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private boolean salir() {
        if (IMPRIMIR_CORTE_AL_CERRAR) {
            corteService.generarCorte(currentOperationalDate());
        }
        System.out.println("Hasta luego");
        return true;
    }

    private LocalDate currentOperationalDate() {
        LocalDateTime now = LocalDateTime.now(clock);
        return DateOps.operationalDate(now, ZONE, SHIFT_START);
    }
}
