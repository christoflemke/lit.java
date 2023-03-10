package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Repository;
import lemke.christof.lit.model.*;

import java.nio.file.Path;
import java.util.List;

public record ListHeadCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Database db = repo.db();
        Commit commit = (Commit) db.read(repo.refs().readHead());

        Tree tree = (Tree) db.read(commit.treeOid());
        listRecursive(db, tree, Path.of(""));
    }

    private void listRecursive(Database db, Tree o, Path path) {
        List<Entry> entries = ((Tree) o).entries();
        for (Entry e : entries) {
            Path entryPath = path.resolve(e.relativePath());

            DbObject child = db.read(e.oid());
            if (child instanceof Tree) {
                listRecursive(db, (Tree) child, entryPath);
            } else if (child instanceof Blob) {
                repo.io().out().println("" + e.mode() + " " + e.oid() + " " + entryPath);
            }
        }
    }
}
