package lemke.christof.lit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Git {
    private final Path root;

    public Git(Path root) {
        this.root = root;
    }

    public class GitCommand {
        private final String[] args;
        private int exitCode;
        private String output;

        public GitCommand(String... args) {
            this.args = args;
        }

        public int exitCode() {
            return exitCode;
        }

        private GitCommand run() {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(args);
                Path repoPath = TestUtil.projectRoot().resolve("testdata").resolve("status-repo");
                processBuilder.directory(repoPath.toFile());
                Process process = processBuilder.start();
                exitCode = process.waitFor();
                output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public String output() {
            return output;
        }
    }

    public GitCommand statusLong() {
        return new GitCommand("git", "status").run();
    }
    public GitCommand statusPorcelain() {
        return new GitCommand("git", "status", "--porcelain").run();
    }

}
