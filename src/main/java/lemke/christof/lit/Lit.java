package lemke.christof.lit;

import lemke.christof.lit.commands.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Lit {
    public static final String[] NO_ARGS = {};
    private final Repository repo;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    Map<String, String> envMap = new HashMap<>();
    {
        envMap.put("GIT_AUTHOR_NAME", "Christof Lemke");
        envMap.put("GIT_COMMITTER_NAME", "Christof Lemke");
        envMap.put("GIT_AUTHOR_EMAIL", "doesnotexist@gmail.com");
    }

    public Lit(Path root) {
        byte[] inputBytes = "commit message".getBytes();
        IO io = IO.createDefault()
            .withIn(new ByteArrayInputStream(inputBytes))
            .withOut(new PrintStream(out))
            .withErr(new PrintStream(err));
        repo = Repository.create(root)
            .withEnv(key -> envMap.get(key))
            .withIO(io);
    }

    public void add(String... files) {
        new AddCommand(repo).run(files);
    }

    public void commit() {
        new CommitCommand(repo).run(NO_ARGS);
    }

    public void init(String... args) {
        new InitCommand().run(args);
    }

    public void statusPorcelain() {
        new StatusCommand(repo, false).run(new String[] {"--porcelain"});
    }

    public void statusLong() {
        new StatusCommand(repo, false).run(new String[] {});
    }
    public void statusLongColor() {
        new StatusCommand(repo, true).run(new String[] {});
    }

    public Repository repo() {
        return this.repo;
    }

    public String output() {
        return this.out.toString(StandardCharsets.UTF_8);
    }

    public void diff() {
        new DiffCommand(repo).run(new String[] {});
    }
}
