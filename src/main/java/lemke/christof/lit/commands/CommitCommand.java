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

public record CommitCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Index idx = repo.createIndex();
        idx.load();
        Workspace.BuildResult result = repo.ws().buildTree(idx);
        for (Tree t : result.trees()) {
            repo.db().write(t);
        }
        String message = readMessage();
        String parent = repo.refs().readHead();
        Commit commit = new Commit(
                parent,
                result.root().oid(),
                Author.createAuthor(repo.env()),
                Author.createCommitter(repo.env()),
                message
        );
        repo.db().write(commit);
        repo.refs().updateHead(commit.oid());
    }

    private String readMessage() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(repo.io().in()));
        return bufferedReader.lines().collect(Collectors.joining(""));
    }

    Blob createBlob(Path f) {
        return new Blob(repo.ws().read(f));
    }
}
