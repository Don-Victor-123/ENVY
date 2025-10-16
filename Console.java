
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class Console {

    // ===== Configuración compartida (leída por PruebaImpresion) =====
    public static String MENSAJE = "ACCESO";
    public static String FECHA_ISO = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    public static String LOGO_PATH = "C:\\SWAP\\Console app betaenvy_logo.jpg";
    public static String PRINTER_HINT = ""; // "" = predeterminada, o "epson", "tm-t20", etc.

    // === FOLIO ===
    public static long FOLIO_ACTUAL = 1010L; // se sobrescribe al inicio pidiéndolo al usuario
    public static int FOLIO_WIDTH = 7; // cantidad de dígitos con ceros a la izquierda

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {

        CorteManager.init(FECHA_ISO);
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
                case 2 -> {

                    app.impresion(); // imprime con fecha de hoy
                    /*
                     * System.out.print("Ingrese fecha (yyyy-MM-dd): ");
                     * String fechaStr = scanner.nextLine().trim();
                     * try {
                     * LocalDate.parse(fechaStr, FMT); // valida formato
                     * app.impresion(fechaStr);
                     * } catch (DateTimeParseException ex) {
                     * System.out.println("Formato inválido. Use yyyy-MM-dd (ej. 2025-09-02)");
                     * }
                     */
                }
                case 3 -> // Recuperar corte (ajusta el número a tu menú)
                {
                    Long sug = CorteManager.recuperarSiguienteFolio(FECHA_ISO);
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
                    if (CorteManager.validarContrasena(scanner)) {
                        CorteManager.hacerCorteConImpresion(FECHA_ISO, PRINTER_HINT);
                    } else {
                        System.out.println("Contraseña incorrecta.");
                    }
                    break;
                }
                case 0 -> {
                    boolean permitirSalir = CorteManager.manejarCierreConCorte(scanner, FECHA_ISO, PRINTER_HINT);
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
        LocalDate fecha = LocalDate.now();
        FECHA_ISO = fecha.format(FMT); // actualiza la variable compartida
        System.out.println("Logo Envy");
        System.out.println(FECHA_ISO);

        try {
            PruebaImpresion.imprimirDesdeConsole(); // lanza impresión

            // registra el ticket para el corte (folio, mensaje, fecha)
            CorteManager.registrarTicket(FOLIO_ACTUAL-1, MENSAJE, FECHA_ISO);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
