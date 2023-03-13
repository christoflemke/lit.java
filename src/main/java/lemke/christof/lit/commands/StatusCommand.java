package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.Util;
import lemke.christof.lit.model.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class StatusCommand implements Command {

    private enum ModifiedStatus {
        STAGED("S", " "),
        UNTRACKED("?", " "),
        WORKSPACE_MODIFIED("M", "modified"),
        WORKSPACE_DELETED("D", "deleted"),
        INDEX_ADDED("A", "new file"),
        INDEX_MODIFIED("M", "modified"),
        MIXED("_", "_"),
        NO_STATUS(" ", " ");

        final String shortStatus;
        final String longStatus;

        ModifiedStatus(String shortStatus, String longStatus) {
            this.shortStatus = shortStatus;
            this.longStatus = longStatus;
        }
    }

    private EnumSet<ModifiedStatus> workspaceStatuses = EnumSet.of(
        ModifiedStatus.WORKSPACE_MODIFIED,
        ModifiedStatus.WORKSPACE_DELETED
    );
    private EnumSet<ModifiedStatus> indexStatuses = EnumSet.of(
        ModifiedStatus.INDEX_ADDED,
        ModifiedStatus.INDEX_MODIFIED
    );
    private final Repository repo;
    private final Index idx;
    private final SortedSet<String> changed = new TreeSet<>();
    private final SortedMap<String, ModifiedStatus> indexChanges = new TreeMap<>();
    private final SortedMap<String, ModifiedStatus> workspaceChanges = new TreeMap<>();
    private final SortedSet<String> untrackedFiles = new TreeSet<>();
    private final boolean useColors;

    public StatusCommand(Repository repo, boolean useColors) {
        this.repo = repo;
        this.useColors = useColors;
        idx = repo.createIndex();
        idx.load();
    }

    @Override
    public void run(String[] args) {
        try {
            repo.ws().walkFileTree(new ModificationVisitor());
            computeChangesFromIndex();

            if (args.length > 0 && args[0].equals("--porcelain")) {
                printPorcelain();
            } else {
                new LongStatus().printLong();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class LongStatus {

        enum Color {
            GREEN(32), RED(31);

            final int code;

            Color(int code) {
                this.code = code;
            }

            String format(String in, boolean useColor) {
                if (useColor) {
                    return "\u001B[" + code + "m" + in + "\u001B[0m";
                } else {
                    return in;
                }
            }
        }

        private void printLong() {
            repo.io().out().println("On branch "+repo.refs().readHeadBranch());

            String stagedMessage = """
                                      Changes to be committed:
                                        (use "git restore --staged <file>..." to unstage)""";
            print_changes(stagedMessage, indexChanges, Color.GREEN);


            String unstagedMessage = """
                Changes not staged for commit:
                  (use "git add/rm <file>..." to update what will be committed)
                  (use "git restore <file>..." to discard changes in working directory)""";
            print_changes(unstagedMessage, workspaceChanges, Color.RED);
            String untrackedMessage = """
                Untracked files:
                  (use "git add <file>..." to include in what will be committed)""";
            print_changes(untrackedMessage, untrackedFiles, Color.RED);
            print_commit_status();
            repo.io().out().println("");
        }

        private void print_commit_status() {
            if (!indexChanges.isEmpty()) {
                return;
            }

            PrintStream out = repo.io().out();
            if (!workspaceChanges.isEmpty()) {
                out.println("no changes added to commit");
            } else if (!untrackedFiles.isEmpty()) {
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
                String status = Util.rightPad(change.getValue().longStatus+":", labelWidth);
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

    private void printPorcelain() {
        for (var path : changed) {
            if (untrackedFiles.contains(path)) {
                continue;
            }
            String left = indexChanges.getOrDefault(path, ModifiedStatus.NO_STATUS).shortStatus;
            String right = workspaceChanges.getOrDefault(path, ModifiedStatus.NO_STATUS).shortStatus;
            repo.io().out().println(left + right + " " + path);
        }
        for (var path : untrackedFiles) {
            repo.io().out().println("?? " + path);
        }
    }

    private void computeChangesFromIndex() {
        Map<Path, DbObject> headTree = loadHeadTree();

        for (Index.Entry entry : idx.entries()) {
            Path path = entry.path();
            if (!repo.ws().resolve(path).toFile().exists()) {
                addStatus(path.toString(), ModifiedStatus.WORKSPACE_DELETED);
            }
            if (headTree.containsKey(path)) {
                DbObject dbObject = headTree.get(path);
                System.out.println(dbObject);
            } else {
                addStatus(path.toString(), ModifiedStatus.INDEX_ADDED);
            }
        }
    }

    private void addStatus(String path, ModifiedStatus status) {
        changed.add(path);
        if (workspaceStatuses.contains(status)) {
            workspaceChanges.put(path, status);
        } else if (indexStatuses.contains(status)) {
            indexChanges.put(path, status);
        }

        if (ModifiedStatus.UNTRACKED == status) {
            untrackedFiles.add(path);
        }
    }

    private Map<Path, DbObject> loadHeadTree() {
        String head = this.repo.refs().readHead();
        if (head == null) {
            return Map.of();
        }
        Commit commit = (Commit) repo.db().read(head);
        return repo.db().readTree(commit.treeOid(), Path.of(""));
    }

    private class ModificationVisitor extends SimpleFileVisitor<Path> {
        Stack<Map<Path, ModifiedStatus>> stack = new Stack<>();

        private void put(Path path, ModifiedStatus status) {
            stack.peek().put(path, status);
        }

        private ModifiedStatus checkForModification(Path relativePath) {
            Optional<Index.Entry> idxEntry = idx.get(relativePath);
            if (idxEntry.isPresent()) {
                FileStat currentStat = repo.ws().stat(relativePath);
                if (currentStat.equals(idxEntry.get().stat())) {
                    return ModifiedStatus.STAGED;
                } else {
                    if (idxEntry.get().stat().mode() != currentStat.mode()) {
                        return ModifiedStatus.WORKSPACE_MODIFIED;
                    } else if (idxEntry.get().oid().equals(idx.hash(relativePath))) {
                        return ModifiedStatus.STAGED;
                    } else {
                        return ModifiedStatus.WORKSPACE_MODIFIED;
                    }
                }
            } else {
                return ModifiedStatus.UNTRACKED;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            Path relativePath = repo.ws().toRelativePath(file);
            put(relativePath, checkForModification(relativePath));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (repo.ws().toRelativePath(dir).equals(Path.of(".git"))) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            stack.push(new TreeMap<>());
            return FileVisitResult.CONTINUE;
        }

        private void addChanged(Map<Path, ModifiedStatus> children) {
            EnumSet<ModifiedStatus> reported = EnumSet.of(ModifiedStatus.UNTRACKED, ModifiedStatus.WORKSPACE_MODIFIED);
            children.entrySet().stream()
                .filter(e -> reported.contains(e.getValue()))
                .forEach(e -> {
                    String path = repo.ws().isDirectory(e.getKey()) ? e.getKey() + "/" : e.getKey().toString();
                    addStatus(path, e.getValue());
                });
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            Path relativePath = repo.ws().toRelativePath(dir);
            Map<Path, ModifiedStatus> children = stack.pop();
            if (stack.isEmpty()) {
                addChanged(children);
                return FileVisitResult.CONTINUE;
            }
            if (children.values().stream().allMatch(s -> s == ModifiedStatus.STAGED)) {
                stack.peek().put(relativePath, ModifiedStatus.STAGED);
            } else if (children.values().stream().allMatch(s -> s == ModifiedStatus.UNTRACKED)) {
                stack.peek().put(relativePath, ModifiedStatus.UNTRACKED);
            } else {
                addChanged(children);
                stack.peek().put(relativePath, ModifiedStatus.MIXED);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
