package lemke.christof.lit;

import lemke.christof.lit.commands.AddCommand;
import lemke.christof.lit.commands.CommitCommand;
import lemke.christof.lit.commands.InitCommand;
import lemke.christof.lit.commands.StatusCommand;

public class Lit {
    public static final String[] NO_ARGS = {};
    private final Repository repo;

    public Lit(Repository repo) {
        this.repo = repo;
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
}
