import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TicketFormatter {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssVV");
    private static final DateTimeFormatter HUMAN_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private TicketFormatter() {
    }

    public static String fileName(TicketRecord ticket) {
        return ticket.id() + "_" + FILE_TS.format(ticket.timestamp());
    }

    public static String serialize(TicketRecord ticket) {
        return "TOTAL=" + ticket.total() + "\n" + ticket.body();
    }

    public static TicketRecord parse(String fileName, String content, ZoneId zone) {
        int underscore = fileName.indexOf('_');
        if (underscore < 0) {
            throw new IllegalArgumentException("Invalid ticket filename: " + fileName);
        }
        String id = fileName.substring(0, underscore);
        String tsPart = fileName.substring(underscore + 1);
        ZonedDateTime timestamp = ZonedDateTime.parse(tsPart, FILE_TS).withZoneSameInstant(zone);

        String[] lines = content.split("\n", 2);
        BigDecimal total = BigDecimal.ZERO;
        String body = "";
        if (lines.length > 0 && lines[0].startsWith("TOTAL=")) {
            total = new BigDecimal(lines[0].substring("TOTAL=".length()));
            body = lines.length > 1 ? lines[1] : "";
        } else {
            body = content;
        }
        return new TicketRecord(id, timestamp, total, body);
    }

    public static String toPrintableText(TicketRecord ticket) {
        return "Ticket " + ticket.id() + "\n" + HUMAN_TS.format(ticket.timestamp()) + "\nTOTAL: " + ticket.total() + "\n" + ticket.body() + "\n";
    }
}
