package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Repository;
import lemke.christof.lit.model.Commit;
import lemke.christof.lit.model.DbObject;
import lemke.christof.lit.model.Entry;
import lemke.christof.lit.model.Tree;

public record ShowHeadCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Database db = repo.db();
        Commit commit = (Commit) db.read(repo.refs().readHead());
        repo.io().out().println("commit " + commit.oid());
        Tree tree = (Tree) db.read(commit.treeOid());
        printTree(db, tree, 2);
    }

    private static String ident(int pad) {
        String result = "";
        for (int i = 0; i < pad; i++) {
            result += " ";
        }
        return result;
    }

    private void printTree(Database db, DbObject object, int pad) {
        repo.io().out().println(ident(pad) + object.toString());
        if (object instanceof Tree) {
            Tree tree = (Tree) object;
            for (Entry e : tree.entries()) {
                DbObject child = db.read(e.oid());
                printTree(db, child, pad + 2);
            }
        }
    }
}
