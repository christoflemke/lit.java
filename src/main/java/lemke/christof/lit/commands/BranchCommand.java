package lemke.christof.lit.commands;

import lemke.christof.lit.Repository;

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
        repo.refs().createBranch(branchName);
    }
}
