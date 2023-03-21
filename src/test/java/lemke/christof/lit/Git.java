package lemke.christof.lit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

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
        return new GitCommand("git", "commit", "-m", "'test'").run();
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
                processBuilder.directory(root.toFile());
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
