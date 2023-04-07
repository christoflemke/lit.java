package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Repository;
import lemke.christof.lit.database.*;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public record ListHeadCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Database db = repo.db();
        Optional<Oid> oid = repo.refs().readHead();
        if(oid.isEmpty()) {
            throw new RuntimeException("HEAD does not point to anything");
        }
        Commit commit = (Commit) db.read(oid.get());

        Tree tree = (Tree) db.read(commit.treeOid());
        listRecursive(db, tree, Path.of(""));
    }

    private void listRecursive(Database db, Tree o, Path path) {
        for (Entry e : o.entries().values()) {
            Path entryPath = path.resolve(e.relativePath());

            DbObject child = db.read(e.oid());
            if (child instanceof Tree t) {
                listRecursive(db, t, entryPath);
            } else if (child instanceof Blob) {
                repo.io().out().println("" + e.mode() + " " + e.oid() + " " + entryPath);
            }
        }
    }
}
