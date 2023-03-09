package lemke.christof.lit.commands;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class TestCommand implements Command {
    @Override
    public void run(String[] args) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendInstant().toFormatter();
        formatter.format(LocalDateTime.now());
    }
}
