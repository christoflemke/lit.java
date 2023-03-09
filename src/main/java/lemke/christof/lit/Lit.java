package lemke.christof.lit;

import lemke.christof.lit.commands.AddCommand;
import lemke.christof.lit.commands.CommitCommand;
import lemke.christof.lit.commands.InitCommand;

import java.nio.file.Path;

public class Lit {
    private final Repository repo;

    public Lit(Repository repo) {
        this.repo = repo;
    }

    public void add(String... files) {
        new AddCommand(repo).run(files);
    }

    public void commit() {
        new CommitCommand(repo).run(new String[] {});
    }

    public void init(String... args) {
        new InitCommand().run(args);
    }

}
