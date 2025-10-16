
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Console {

    // ===== Configuración compartida (leída por PruebaImpresion) =====
    public static String MENSAJE = "ACCESO";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = CorteManager.ZONE;
    private static final LocalTime SHIFT_START = CorteManager.SHIFT_START;
    public static String FECHA_ISO =
            DateOps.operationalDate(LocalDateTime.now(ZONE), ZONE, SHIFT_START).format(FMT);
    public static String LOGO_PATH = "C:\\SWAP\\Console app betaenvy_logo.jpg";
    public static String PRINTER_HINT = ""; // "" = predeterminada, o "epson", "tm-t20", etc.

    // === FOLIO ===
    public static long FOLIO_ACTUAL = 1010L; // se sobrescribe al inicio pidiéndolo al usuario
    public static int FOLIO_WIDTH = 7; // cantidad de dígitos con ceros a la izquierda

    public static void main(String[] args) {
        LocalDate opDate = currentOperationalDate();
        FECHA_ISO = opDate.format(FMT);
        CorteManager.init(opDate);
        // 0 = imprime siempre el corte al cerrar; 1 = NO imprimir al cerrar
        CorteManager.BANDERA_IMPRESION_AL_CERRAR = 0;

        Scanner scanner = new Scanner(System.in);
        Console app = new Console();

        // ---- Preguntar el inicio del folio ----
        System.out.print("Ingrese inicio de folio (ej. 1010 para ver 0001010): ");
        String folioStr = scanner.nextLine().trim();
        Long folioInicial = tryParseLong(folioStr);
        if (folioInicial != null && folioInicial >= 0) {
            FOLIO_ACTUAL = folioInicial;
        } else {
            System.out.println("Entrada inválida. Se usará el valor por defecto: " + FOLIO_ACTUAL);
        }

        // (Opcional) si quieres permitir cambiar el ancho:
        // System.out.print("Ancho de folio (dígitos, ENTER para " + FOLIO_WIDTH + "):
        // ");
        // String wStr = scanner.nextLine().trim();
        // Integer w = tryParseInt(wStr);
        // if (w != null && w > 0) FOLIO_WIDTH = w;
        int opcion = -1;
        System.out.println("Presione 0 para salir.");

        do {
            System.out.println("""
                    Seleccione una opción:
                      1) Imprimir cover
                      3) Recuperar folio
                      4) Hacer corte
                      5) Reimprimir corte por fecha (YYYY-MM-DD)
                      0) Salir
                    """);

            String linea = scanner.nextLine().trim();
            try {
                opcion = Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                System.out.println("Opción inválida. Intente de nuevo.");
                opcion = -1;
                continue;
            }

            switch (opcion) {
                case 1 ->
                    app.impresion(); // imprime con fecha de hoy
                case 3 -> // Recuperar folio sugerido
                {
                    LocalDate vigente = currentOperationalDate();
                    Long sug = CorteManager.recuperarSiguienteFolio(vigente);
                    if (sug != null) {
                        FOLIO_ACTUAL = sug;
                        System.out.println("Folio recuperado: " + (FOLIO_ACTUAL));
                    } else {
                        System.out.println("No hay corte/tickets previos hoy para recuperar.");
                    }
                    break;
                }
                case 4 -> // Imprimir corte (ajusta el número a tu menú)
                {
                    LocalDate vigente = currentOperationalDate();
                    if (CorteManager.validarContrasena(scanner)) {
                        CorteManager.hacerCorteConImpresion(vigente, PRINTER_HINT);
                    } else {
                        System.out.println("Contraseña incorrecta.");
                    }
                    break;
                }
                case 5 -> {
                    System.out.print("Ingrese fecha operativa (yyyy-MM-dd): ");
                    String fechaStr = scanner.nextLine().trim();
                    try {
                        LocalDate fecha = LocalDate.parse(fechaStr, FMT);
                        boolean ok = CorteManager.reimprimirCorte(fecha, PRINTER_HINT);
                        if (!ok) {
                            System.out.println("No se generó el corte para esa fecha.");
                        }
                    } catch (DateTimeParseException ex) {
                        System.out.println("Formato inválido. Use yyyy-MM-dd (ej. 2025-09-02)");
                    }
                    break;
                }
                case 0 -> {
                    LocalDate vigente = currentOperationalDate();
                    boolean permitirSalir = CorteManager.manejarCierreConCorte(scanner, vigente, PRINTER_HINT);
                    if (!permitirSalir) {
                        // Regresa al menú sin salir
                        break;
                    }
                    System.out.println("Saliendo...");
                    return; // o el break/salida que ya uses
                }

                default ->
                    System.out.println("Opción no reconocida. Intente de nuevo.");
            }
        } while (opcion != 0);

        // scanner.close(); // ciérralo si no vas a reutilizar la consola
    }

    public void impresion() {
        ZonedDateTime timestamp = ZonedDateTime.now(ZONE);
        LocalDate opDate = DateOps.operationalDate(timestamp.toLocalDateTime(), ZONE, SHIFT_START);
        FECHA_ISO = opDate.format(FMT);
        CorteManager.init(opDate);
        System.out.println("Logo Envy");
        System.out.println(FECHA_ISO);

        try {
            PruebaImpresion.imprimirDesdeConsole(); // lanza impresión

            // registra el ticket para el corte (folio, mensaje, fecha)
            CorteManager.registrarTicket(FOLIO_ACTUAL - 1, MENSAJE, timestamp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static LocalDate currentOperationalDate() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return DateOps.operationalDate(now.toLocalDateTime(), ZONE, SHIFT_START);
    }

    public void impresion(String fecha) {
        FECHA_ISO = fecha; // actualiza la variable compartida
        System.out.println("Logo Envy");
        System.out.println(fecha);

        try {
            PruebaImpresion.imprimirDesdeConsole(); // lanza impresión
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Helpers =====
    private static Long tryParseLong(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer tryParseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
