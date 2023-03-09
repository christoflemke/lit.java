package lemke.christof.lit.commands;

import lemke.christof.lit.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record InitCommand() implements Command {
    @Override
    public void run(String[] args) {
        Path repoPath = Path.of("");
        if (args.length >= 1) {
            repoPath = Path.of(args[0]);
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
