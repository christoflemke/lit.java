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

public record CommitCommand(Workspace workspace, Database db, Environment env, Refs refs) implements Runnable {
    @Override
    public void run() {
        Index idx = new Index(workspace);
        idx.load();
        Workspace.BuildResult result = workspace.buildTree(idx);
        for (Tree t : result.trees()) {
            db.write(t);
        }
        String message = readMessage();
        String parent = refs.readHead();
        Commit commit = new Commit(parent, result.root().oid(), Author.createAuthor(env), Author.createCommitter(env), message);
        db.write(commit);
        refs.updateHead(commit.oid());
    }

    private String readMessage() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        return bufferedReader.lines().collect(Collectors.joining(""));
    }

    Blob createBlob(Path f) {
        return new Blob(workspace.read(f));
    }
}
