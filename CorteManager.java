import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

public class CorteManager {

    /** 0 = imprimir corte al cerrar; 1 = NO imprimir corte al cerrar */
    public static int BANDERA_IMPRESION_AL_CERRAR = 0;

    public static final ZoneId ZONE = DateOps.DEFAULT_ZONE;
    public static final LocalTime SHIFT_START = DateOps.DEFAULT_SHIFT_START;
    public static final LocalTime SHIFT_END = DateOps.DEFAULT_SHIFT_END;

    private static final CorteService SERVICE = new CorteService(ZONE, SHIFT_START, SHIFT_END);
    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String PASSWORD_POR_DEFECTO = "842867";
    private static final Path PASSWORD_FILE = Paths.get("corte_password.txt");

    public static void init(LocalDate opDate) {
        try {
            SERVICE.ensureOperationalDirectory(opDate);
        } catch (IOException e) {
            System.err.println("No se pudo crear carpeta de cortes: " + e.getMessage());
        }
    }

    public static void registrarTicket(long folio, String mensaje, ZonedDateTime timestamp) {
        try {
            SERVICE.registrarTicket(folio, mensaje, timestamp);
        } catch (IOException e) {
            System.err.println("No se pudo registrar el ticket: " + e.getMessage());
        }
    }

    public static Long recuperarSiguienteFolio(LocalDate opDate) {
        Optional<Long> siguiente = SERVICE.recuperarSiguienteFolio(opDate);
        return siguiente.orElse(null);
    }

    public static boolean estaCorteHecho(LocalDate opDate) {
        return SERVICE.estaCorteHecho(opDate);
    }

    public static boolean hayTickets(LocalDate opDate) {
        return SERVICE.hayTickets(opDate);
    }

    public static boolean validarContrasena(Scanner scanner) {
        String esperada = leerPassword();
        System.out.print("Ingrese la contraseña del corte: ");
        String ingresada = scanner.nextLine();
        return esperada.equals(ingresada);
    }

    public static boolean hacerCorteConImpresion(LocalDate opDate, String printerHint) {
        try {
            CorteService.CorteResumen resumen = SERVICE.calcularResumen(opDate);
            if (!resumen.hasTickets()) {
                System.out.println("No hay tickets para cortar.");
                return false;
            }

            String cuerpo = SERVICE.formatearCorte(opDate, resumen);
            SERVICE.guardarCorte(opDate, cuerpo);
            imprimirTexto(cuerpo, printerHint);
            SERVICE.marcarCorte(opDate);
            logRango(opDate, resumen);
            return true;
        } catch (Exception e) {
            System.err.println("Error al hacer el corte: " + e.getMessage());
            return false;
        }
    }

    public static boolean reimprimirCorte(LocalDate opDate, String printerHint) {
        try {
            CorteService.CorteResumen resumen = SERVICE.calcularResumen(opDate);
            if (!resumen.hasTickets()) {
                Optional<String> existente = SERVICE.leerCorte(opDate);
                if (existente.isPresent()) {
                    imprimirTexto(existente.get(), printerHint);
                    logRango(opDate, null);
                    return true;
                }
                System.out.println("No hay tickets registrados para ese día operativo.");
                return false;
            }

            String cuerpo = SERVICE.formatearCorte(opDate, resumen);
            SERVICE.guardarCorte(opDate, cuerpo);
            imprimirTexto(cuerpo, printerHint);
            SERVICE.marcarCorte(opDate);
            logRango(opDate, resumen);
            return true;
        } catch (Exception e) {
            System.err.println("No se pudo reimprimir el corte: " + e.getMessage());
            return false;
        }
    }

    public static boolean manejarCierreConCorte(Scanner scanner, LocalDate opDate, String printerHint) {
        if (BANDERA_IMPRESION_AL_CERRAR != 0) {
            return true;
        }
        if (estaCorteHecho(opDate) || !hayTickets(opDate)) {
            return true;
        }

        if (validarContrasena(scanner)) {
            boolean ok = hacerCorteConImpresion(opDate, printerHint);
            if (!ok) {
                System.out.println("No se pudo completar el corte.");
                return false;
            }
            return true;
        }

        System.out.println("Contraseña incorrecta.");
        System.out.print("Estás a punto de salir SIN hacer el corte. ¿Continuar? (s/n): ");
        String r1 = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (!r1.equals("s") && !r1.equals("si") && !r1.equals("sí")) {
            return false;
        }

        System.out.print("¿Seguro que quieres salir sin hacer el corte? (s/n): ");
        String r2 = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
        if (!r2.equals("s") && !r2.equals("si") && !r2.equals("sí")) {
            return false;
        }

        return true;
    }

    private static void logRango(LocalDate opDate, CorteService.CorteResumen resumen) {
        ZonedDateTime start = DateOps.windowStart(opDate, ZONE, SHIFT_START);
        ZonedDateTime end = DateOps.windowEnd(opDate, ZONE, SHIFT_START, SHIFT_END);
        System.out.println("Corte DO " + opDate + " cubre " + LOG_FMT.format(start) + " → " + LOG_FMT.format(end));
        if (resumen != null && resumen.hasTickets()) {
            if (resumen.primerTicket() != null && resumen.ultimoTicket() != null) {
                System.out.println("Tickets desde " + LOG_FMT.format(resumen.primerTicket()) + " hasta "
                        + LOG_FMT.format(resumen.ultimoTicket()));
            }
        }
    }

    private static String leerPassword() {
        try {
            if (Files.exists(PASSWORD_FILE)) {
                return Files.readString(PASSWORD_FILE, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ignored) {
        }
        return PASSWORD_POR_DEFECTO;
    }

    private static void imprimirTexto(String texto, String printerHint) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();

        PrintService svc = elegirImpresora(printerHint);
        if (svc != null) {
            try {
                job.setPrintService(svc);
            } catch (PrinterException e) {
                System.err.println("No se pudo seleccionar la impresora: " + e.getMessage());
            }
        }

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        double width = mmToPoints(80);
        double height = mmToPoints(200);
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        pf.setPaper(paper);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setFont(PrintLayoutConfig.Corte.FUENTE_IMPRESION);
            int x = 10;
            int y = 14;
            for (String line : texto.split("\\R")) {
                g2.drawString(line, x, y);
                y += PrintLayoutConfig.Corte.ESPACIADO_RENGLON;
            }
            return Printable.PAGE_EXISTS;
        }, pf);

        job.print();
    }

    private static double mmToPoints(double mm) {
        return (mm / 25.4) * 72.0;
    }

    private static PrintService elegirImpresora(String hint) {
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services == null || services.length == 0) {
                return null;
            }
            if (hint == null || hint.isBlank()) {
                return PrintServiceLookup.lookupDefaultPrintService();
            }
            String h = hint.toLowerCase(Locale.ROOT);
            for (PrintService s : services) {
                String name = s.getName();
                if (name != null && name.toLowerCase(Locale.ROOT).contains(h)) {
                    return s;
                }
            }
            return PrintServiceLookup.lookupDefaultPrintService();
        } catch (Exception e) {
            return null;
        }
    }

    // Métodos de compatibilidad (evitan romper firmas públicas previas)
    public static void init(String fechaIso) {
        init(LocalDate.parse(fechaIso));
    }

    public static void registrarTicket(long folio, String mensaje, String fechaIso) {
        registrarTicket(folio, mensaje, ZonedDateTime.now(ZONE));
    }

    public static Long recuperarSiguienteFolio(String fechaIso) {
        return recuperarSiguienteFolio(LocalDate.parse(fechaIso));
    }

    public static boolean hacerCorteConImpresion(String fechaIso, String printerHint) {
        return hacerCorteConImpresion(LocalDate.parse(fechaIso), printerHint);
    }

    public static boolean manejarCierreConCorte(Scanner scanner, String fechaIso, String printerHint) {
        return manejarCierreConCorte(scanner, LocalDate.parse(fechaIso), printerHint);
    }
}
