package lemke.christof.lit.commands;

import lemke.christof.lit.Repository;
import lemke.christof.lit.model.Oid;
import lemke.christof.lit.model.Revision;

import java.util.Optional;

public class BranchCommand implements Command {

    private final Repository repo;

    public BranchCommand(Repository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String[] args) {
        createBranch(args);
    }

    private void createBranch(String[] args) {
        if(args.length == 0) {
            throw new RuntimeException("Please provide a branch name");
        }
        String branchName = args[0];
        Optional<Oid> head = repo.refs().readHead();
        String startPoint;
        if (args.length > 1) {
            startPoint = args[1];
        } else {
            if(head.isEmpty()) {
                throw new RuntimeException("No revision specified and HEAD does not point to anything");
            }
            startPoint = head.get().value();
        }
        Oid startOid = this.repo.refs().resolveCommit(startPoint);
        repo.refs().createBranch(branchName, startOid);
    }
}
