package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public record StatusCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        BufferedWriter out = repo.io().out();
        try {
            Index idx = repo.createIndex();
            idx.load();
            for (Path p : repo.ws().listFiles().filter(p -> idx.get(p).isEmpty()).toList()) {
                out.write("?? " + p);
                out.newLine();
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
