package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record CommitCommand(Workspace workspace, Database db, Environment env) implements Runnable {
    @Override
    public void run() {
        try {
            List<Entry> entries = new ArrayList<>();
            for (Path f : workspace.listFiles()) {
                Blob blob = createBlob(f);
                db.write(blob);
                entries.add(new Entry(f, blob.oid()));
            }
            Tree tree = new Tree(entries);
            db.write(tree);
            String message = readMessage();
            Commit commit = new Commit(tree.oid(), Author.createAuthor(env), Author.createCommitter(env), message);
            db.write(commit);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readMessage() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        return bufferedReader.lines().collect(Collectors.joining(""));
    }

    Blob createBlob(Path f) {
        return new Blob(workspace.read(f));
    }
}
