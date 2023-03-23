package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Repository;
import lemke.christof.lit.Util;
import lemke.christof.lit.model.*;

import java.util.Optional;

public record ShowHeadCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Database db = repo.db();
        Optional<Oid> oid = repo.refs().readHead();
        if(oid.isEmpty()) {
            throw new RuntimeException("HEAD does not point to anything");
        }
        Commit commit = (Commit) db.read(oid.get());
        repo.io().out().println("commit " + commit.oid());
        Tree tree = (Tree) db.read(commit.treeOid());
        printTree(db, tree, 2);
    }

    private void printTree(Database db, DbObject object, int pad) {
        repo.io().out().println(Util.pad(pad) + object.toString());
        if (object instanceof Tree tree) {
            for (Entry e : tree.entries()) {
                DbObject child = db.read(e.oid());
                printTree(db, child, pad + 2);
            }
        }
    }
}
