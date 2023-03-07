package lemke.christof.lit.commands;

import lemke.christof.lit.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record InitCommand(String[] args) implements Runnable {
    @Override
    public void run() {
        Path repoPath = Path.of("");
        if (args.length >= 2) {
            repoPath = Path.of(args[1]);
        }
        Path gitPath = repoPath.resolve(".git");
        try {
            Files.createDirectories(gitPath);
            Files.createDirectories(gitPath.resolve("objects"));
            Files.createDirectories(gitPath.resolve("refs"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
