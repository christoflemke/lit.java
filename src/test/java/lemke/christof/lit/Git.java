package lemke.christof.lit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

public class Git {
    private final Path root;

    public Git(Path root) {
        this.root = root;
    }

    public void delete(String file) {
        new GitCommand("git", "rm", file).run();
    }
    public GitCommand statusLong() {
        return new GitCommand("git", "status").run();
    }
    public GitCommand statusPorcelain() {
        return new GitCommand("git", "status", "--porcelain").run();
    }

    public GitCommand add(String... files) {
        return new GitCommand(concat(new String[] {"git", "add"}, files)).run();
    }

    public GitCommand commit() {
        Map<String, String> environment = Map.of(
            "GIT_COMMITTER_DATE", "1678008251 +0100",
            "GIT_AUTHOR_DATE", "1678008252 +0100",
            "GIT_AUTHOR_NAME", "Christof Lemke",
            "GIT_COMMITTER_NAME", "Christof Lemke",
            "GIT_AUTHOR_EMAIL", "doesnotexist@gmail.com"
        );
        return new GitCommand("git", "commit", "-m", "'test'")
            .environment(environment)
            .run();
    }

    private String[] concat(String[] args1, String... args2) {
        String[] result = Arrays.copyOf(args1, args1.length + args2.length);
        System.arraycopy(args2, 0, result, args1.length, args2.length);
        return result;
    }

    public GitCommand init() {
        return new GitCommand("git", "init").run();
    }

    public GitCommand diffCached() {
        return new GitCommand("git", "diff", "--cached").run();
    }

    public GitCommand rm(String file) {
        return new GitCommand("git", "rm", file).run();
    }

    public GitCommand diff() {
        return new GitCommand("git", "diff").run();
    }

    public GitCommand diffColor() {
        return new GitCommand("git", "-c", "color.ui=always", "diff").run();
    }

    public GitCommand branch(String branchName) {
        return new GitCommand("git", "branch", branchName).run();
    }

    public class GitCommand {
        private final String[] args;
        private int exitCode;
        private String output;
        private Map<String, String> environment;

        public GitCommand(String... args) {
            this.args = args;
            this.environment = Map.of();
        }
        public GitCommand(Map<String,String> environment, String... args) {
            this.args = args;
            this.environment = environment;
        }

        public GitCommand environment(Map<String, String> environment) {
            return new GitCommand(environment, args);
        }

        public int exitCode() {
            return exitCode;
        }

        private GitCommand run() {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(args);
                processBuilder.directory(root.toFile());
                processBuilder.environment().putAll(environment);
                Process process = processBuilder.start();
                exitCode = process.waitFor();
                if(exitCode != 0) {
                    String err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                    throw new RuntimeException("git failed with exit code: "+exitCode+" err:"+err);
                }
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
}
