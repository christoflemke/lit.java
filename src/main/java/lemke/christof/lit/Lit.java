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
    private final Path root;

    public Lit(Path root) {
        this.root = root;
    }

    public LitCommand add(String... files) {
        return new LitCommand(repo -> new AddCommand(repo)).run(files);
    }

    public LitCommand commit() {
        return new LitCommand(repo -> new CommitCommand(repo)).run();
    }

    public LitCommand init(String... args) {
        return new LitCommand(repo -> new InitCommand()).run(args);
    }

    public LitCommand statusPorcelain() {
        return new LitCommand(repo -> new StatusCommand(repo, false)).run("--porcelain");
    }

    public LitCommand statusLong() {
        return new LitCommand(repo -> new StatusCommand(repo, false)).run();
    }
    public LitCommand statusLongColor() {
        return new LitCommand(repo -> new StatusCommand(repo, true)).run();
    }

    public LitCommand diff() {
        return new LitCommand(repo -> new DiffCommand(repo, false)).run();
    }

    public LitCommand diffColor() {
        return new LitCommand(repo -> new DiffCommand(repo, true)).run();
    }

    public LitCommand diffCached() {
        return new LitCommand((repository) -> new DiffCommand(repository, false)).run("--cached");
    }

    public LitCommand showHead() {
        return new LitCommand(repo -> new ShowHeadCommand(repo)).run();
    }

    public LitCommand listHead() {
        return new LitCommand(repo -> new ListHeadCommand(repo)).run();
    }

    public LitCommand branch(String... args) {
        return new LitCommand(repo -> new BranchCommand(repo)).run(args);
    }

    private interface CreateFn {
        Command create(Repository repo);
    }

    public class LitCommand {
        private final Command command;
        private final Repository repo;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Map<String, String> envMap = new HashMap<>();
        {
            envMap.put("GIT_AUTHOR_NAME", "Christof Lemke");
            envMap.put("GIT_COMMITTER_NAME", "Christof Lemke");
            envMap.put("GIT_AUTHOR_EMAIL", "doesnotexist@gmail.com");
            envMap.put("GIT_AUTHOR_DATE", "1678008252 +0100");
            envMap.put("GIT_COMMITTER_DATE", "1678008251 +0100");
        }

        public LitCommand(CreateFn create) {
            byte[] inputBytes = "commit message".getBytes();
            IO io = IO.createDefault()
                .withIn(new ByteArrayInputStream(inputBytes))
                .withOut(new PrintStream(out))
                .withErr(new PrintStream(err));
            repo = Repository.create(root)
                .withEnv(key -> envMap.get(key))
                .withIO(io);
            this.command = create.create(repo);
        }

        public LitCommand run(String... args) {
            command.run(args);
            return this;
        }

        public String output() {
            return this.out.toString(StandardCharsets.UTF_8);
        }
    }
}
