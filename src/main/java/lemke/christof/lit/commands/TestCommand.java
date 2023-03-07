package lemke.christof.lit.commands;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class TestCommand implements Runnable {
    @Override
    public void run() {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant().toFormatter();
        formatter.format(LocalDateTime.now());
    }
}
