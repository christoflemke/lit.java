package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.model.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;

public class StatusCommand implements Command {

    private enum ModifiedStatus {
        STAGED, WORKSPACE_UNTRACKED, WORKSPACE_MODIFIED, WORKSPACE_DELETED, INDEX_ADDED, INDEX_MODIFIED, MIXED
    }

    private final Repository repo;
    private final Index idx;
    private final Map<String, EnumSet<ModifiedStatus>> changes = new TreeMap<>();

    public StatusCommand(Repository repo) {
        this.repo = repo;
        idx = repo.createIndex();
        idx.load();
    }

    @Override
    public void run(String[] args) {
        try (PrintStream out = repo.io().out()) {
            repo.ws().walkFileTree(new ModificationVisitor());

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

            Function<String, String> statusFor = (String path) -> {
                EnumSet<ModifiedStatus> statuses = changes.get(path);
                String left = " ";
                if (statuses.contains(ModifiedStatus.INDEX_ADDED)) left = "A";
                if (statuses.contains(ModifiedStatus.INDEX_MODIFIED)) left = "M";

                String right = " ";
                if (statuses.contains(ModifiedStatus.WORKSPACE_DELETED)) right = "D";
                if (statuses.contains(ModifiedStatus.WORKSPACE_MODIFIED)) right = "M";
                if (statuses.contains(ModifiedStatus.WORKSPACE_UNTRACKED)) {
                    right = "?";
                    left = "?";
                }
                return left + right;
            };
            for (Map.Entry<String, EnumSet<ModifiedStatus>> e : changes.entrySet()) {
                final String key = e.getKey();
                if (statusFor.apply(key).isBlank()) continue;
                out.println(statusFor.apply(key) + " " + key);
            }

            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addStatus(String path, ModifiedStatus status) {
        EnumSet<ModifiedStatus> statuses = changes.computeIfAbsent(
            path,
            k -> EnumSet.noneOf(ModifiedStatus.class)
        );
        statuses.add(status);
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
                return ModifiedStatus.WORKSPACE_UNTRACKED;
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
            EnumSet<ModifiedStatus> reported = EnumSet.of(ModifiedStatus.WORKSPACE_UNTRACKED, ModifiedStatus.WORKSPACE_MODIFIED);
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
            } else if (children.values().stream().allMatch(s -> s == ModifiedStatus.WORKSPACE_UNTRACKED)) {
                stack.peek().put(relativePath, ModifiedStatus.WORKSPACE_UNTRACKED);
            } else {
                addChanged(children);
                stack.peek().put(relativePath, ModifiedStatus.MIXED);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
