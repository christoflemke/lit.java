package lemke.christof.lit.commands;

import lemke.christof.lit.Color;
import lemke.christof.lit.Refs;
import lemke.christof.lit.Repository;
import lemke.christof.lit.status.ModifiedStatus;
import lemke.christof.lit.status.Status;
import lemke.christof.lit.Util;

import java.io.PrintStream;
import java.util.*;

public class StatusCommand implements Command {
    private final Repository repo;
    private final boolean useColors;

    public StatusCommand(Repository repo, boolean useColors) {
        this.repo = repo;
        this.useColors = useColors;
    }

    @Override
    public void run(String[] args) {
        Status status = repo.status();

        if (args.length > 0 && args[0].equals("--porcelain")) {
            printPorcelain(status);
        } else {
            new LongStatus(status).printLong();
        }
    }

    class LongStatus {

        private final Status status;

        public LongStatus(Status status) {
            this.status = status;
        }

        private void printLong() {
            String branchName = repo.refs().head().map(Refs.Ref::shortName).orElse("");
            repo.io().out().println("On branch " + branchName);

            String stagedMessage = """
                Changes to be committed:
                  (use "git restore --staged <file>..." to unstage)""";
            print_changes(stagedMessage, status.indexChanges(), Color.GREEN);


            String unstagedMessage = """
                Changes not staged for commit:
                  (use "git add/rm <file>..." to update what will be committed)
                  (use "git restore <file>..." to discard changes in working directory)""";
            print_changes(unstagedMessage, status.workspaceChanges(), Color.RED);
            String untrackedMessage = """
                Untracked files:
                  (use "git add <file>..." to include in what will be committed)""";
            print_changes(untrackedMessage, status.untrackedFiles(), Color.RED);
            print_commit_status();
            repo.io().out().println("");
        }

        private void print_commit_status() {
            if (!status.indexChanges().isEmpty()) {
                return;
            }

            PrintStream out = repo.io().out();
            if (!status.workspaceChanges().isEmpty()) {
                out.println("no changes added to commit");
            } else if (!status.untrackedFiles().isEmpty()) {
                out.println("nothing added to commit but untracked files present");
            } else {
                out.println("nothing to commit, working tree clean");
            }
        }

        private void print_changes(String message, SortedMap<String, ModifiedStatus> changes, Color color) {
            if (changes.isEmpty()) {
                return;
            }
            PrintStream out = repo.io().out();
            out.println(message);
            int labelWidth = 12;
            for (var change : changes.entrySet()) {
                String status = Util.rightPad(change.getValue().longStatus + ":", labelWidth);
                out.println("\t" + color.format(status + change.getKey(), useColors));
            }
            out.println("");
        }

        private void print_changes(String message, SortedSet<String> changes, Color color) {
            if (changes.isEmpty()) {
                return;
            }
            PrintStream out = repo.io().out();
            out.println(message);
            for (var change : changes) {
                out.println("\t" + color.format(change, useColors));
            }
        }
    }

    private void printPorcelain(Status status) {
        for (var path : status.changed()) {
            if (status.untrackedFiles().contains(path)) {
                continue;
            }
            String left = status.indexChanges().getOrDefault(path, ModifiedStatus.NO_STATUS).shortStatus;
            String right = status.workspaceChanges().getOrDefault(path, ModifiedStatus.NO_STATUS).shortStatus;
            repo.io().out().println(left + right + " " + path);
        }
        for (var path : status.untrackedFiles()) {
            repo.io().out().println("?? " + path);
        }
    }
}
