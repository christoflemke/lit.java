package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Refs;
import lemke.christof.lit.Repository;
import lemke.christof.lit.database.Commit;
import lemke.christof.lit.database.DbObject;
import lemke.christof.lit.database.Oid;
import lemke.christof.lit.database.TreeDiff;
import lemke.christof.lit.repository.Migration;

import java.util.Optional;

public class CheckoutCommand implements Command {

    private final Repository repo;

    public CheckoutCommand(Repository repo) {
        this.repo = repo;
    }

    @Override public void run(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Missing argument");
        }
        String target = args[0];
        Optional<Refs.Ref> targetRef = repo.refs().lookupTarget(target);
        if(targetRef.isEmpty()) {
            throw new RuntimeException("Failed to turn target into ref: "+ target);
        }

        Optional<Oid> currentOid = repo.refs().resolveHead();
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
        repo.refs().setHead(targetRef.get());
    }

    private void printPreviousHead(Refs.SymRef oldRef, Refs.SymRef newRef, Oid currentOid, Oid targetOid) {
        if(oldRef.isHead() && !currentOid.equals(targetOid)) {
            printHeadPosition("Previous HEAD position was", currentOid);
        }
    }

    private void printHeadPosition(String message, Oid oid) {
        DbObject object = repo.db().read(oid);
        if(object instanceof Commit commit) {
            repo.io().out().println(message + " "+oid.shortOid()+" "+commit.titleLine());
        } else {
            throw new RuntimeException("Oid does not point at commit: "+ oid);
        }
    }

    private void printNewHead(Refs.SymRef oldRef, Refs.SymRef newRef, String target, Oid targetOid) {
        if(newRef.isHead()) {
            printHeadPosition("HEAD is now at", targetOid);
        } else if(newRef.equals(oldRef)) {
            repo.io().out().println("Already on '"+target+"'");
        } else {
            repo.io().out().println("Switched to branch '"+target+"'");
        }
    }

    private void printDetachmentNotice(Refs.SymRef oldRef, Refs.SymRef newRef, String target) {
        if (!newRef.isHead() || oldRef.isHead()) {
            return;
        }
        repo.io().out().println("Note: checking out '"+target+"'");
        repo.io().out().println("");
        repo.io().out().println(DETACHED_HEAD_MESSAGE);
        repo.io().out().println("");
    }

    private static final String DETACHED_HEAD_MESSAGE = """
You are in 'detached HEAD' state. You can look around, make experimental
changes and commit them, and you can discard any commits you make in this
state without impacting any branches by performing another checkout.
If you want to create a new branch to retain commits you create, you may
do so (now or later) by using the branch command. Example:
  jit branch <new-branch-name>
""";
}
