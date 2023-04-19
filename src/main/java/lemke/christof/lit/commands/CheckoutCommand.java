package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.database.TreeDiff;
import lemke.christof.lit.repository.Migration;

public class CheckoutCommand implements Command {

    private final Repository repo;

    public CheckoutCommand(Repository repo) {
        this.repo = repo;
    }

    @Override public void run(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Missing argument");
        }
        var target = args[0];

        var currentOid = repo.refs().readHead();
        if(currentOid.isEmpty()) {
            throw new RuntimeException("Head does not point to anything");
        }
        var targetOid = repo.refs().resolveCommit(target);
        Index index = repo.createIndex();
        index.withLock(() -> {
            index.load();
            TreeDiff diff = repo.db().treeDiff(currentOid.get(), targetOid);
            Migration migration = repo.migration(diff, index);
            migration.applyChanges();

            index.commit();
            return null;
        });
        repo.refs().setHead(target, targetOid);
    }
}
