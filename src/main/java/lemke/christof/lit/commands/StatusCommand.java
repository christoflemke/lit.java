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

public record StatusCommand(Repository repo) implements Command {

    private enum ModifiedStatus {
        STAGED, UNTRACKED, WORKSPACE_MODIFIED, WORKSPACE_DELETED, INDEX_ADDED, INDEX_MODIFIED, MIXED
    }

    @Override
    public void run(String[] args) {
        PrintStream out = repo.io().out();
        try {
            Index idx = repo.createIndex();
            idx.load();

            Map<String, EnumSet<ModifiedStatus>> files = new TreeMap<>();

            repo.ws().walkFileTree(new ModificationVisitor(idx, files));

            Map<Path, DbObject> headTree = loadHeadTree();

            for(Index.Entry entry : idx.entries()) {
                Path path = entry.path();
                EnumSet<ModifiedStatus> statuses = files.computeIfAbsent(path.toString(), k -> EnumSet.noneOf(ModifiedStatus.class));
                if(!repo.ws().resolve(path).toFile().exists()) {
                    statuses.add(ModifiedStatus.WORKSPACE_DELETED);
                }
                if (headTree.containsKey(path)) {
                    DbObject dbObject = headTree.get(path);
                    System.out.println(dbObject);
                } else {
                    statuses.add(ModifiedStatus.INDEX_ADDED);
                }
            }

            Function<String, String> statusFor = (String path) -> {
                EnumSet<ModifiedStatus> statuses = files.get(path);
                String left = " ";
                if (statuses.contains(ModifiedStatus.INDEX_ADDED)) left = "A";
                if (statuses.contains(ModifiedStatus.INDEX_MODIFIED)) left = "M";

                String right = " ";
                if (statuses.contains(ModifiedStatus.WORKSPACE_DELETED)) right = "D";
                if (statuses.contains(ModifiedStatus.WORKSPACE_MODIFIED)) right = "M";
                if (statuses.contains(ModifiedStatus.UNTRACKED)) {right = "?"; left = "?";}
                return left + right;
            };
            for (Map.Entry<String, EnumSet<ModifiedStatus>> e : files.entrySet()) {
                final String key = e.getKey();
                if (statusFor.apply(key).isBlank()) continue;
                out.println(statusFor.apply(key)+" "+key);
            }

            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Path, DbObject> loadHeadTree() {
        String head = this.repo.refs().readHead();
        if (head == null) {
            return Map.of();
        }

        Commit commit = (Commit) repo.db().read(head);
        return readTree(commit.treeOid(), Path.of(""));
    }

    private Map<Path, DbObject> readTree(String oid, Path path) {
        DbObject o = repo.db().read(oid);
        if (o instanceof Blob) {
            return Map.of(path, o);
        }
        Tree tree = (Tree) o;
        Map<Path, DbObject> result = new HashMap<>();
        for(Entry e : tree.entries()) {
            Path entryPath = path.resolve(e.relativePath());
            result.putAll(readTree(e.oid(), entryPath));
        }
        return result;
    }

    private class ModificationVisitor extends SimpleFileVisitor<Path> {
        private final Index idx;
        private final Map<String, EnumSet<ModifiedStatus>> files;
        Stack<Map<Path, ModifiedStatus>> stack;

        public ModificationVisitor(Index idx, Map<String, EnumSet<ModifiedStatus>> files) {
            this.idx = idx;
            this.files = files;
            stack = new Stack<>();
        }

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
                    if(idxEntry.get().stat().mode() != currentStat.mode()) {
                        return ModifiedStatus.WORKSPACE_MODIFIED;
                    } else if(idxEntry.get().oid().equals(idx.hash(relativePath))) {
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

        private void addUnstaged(Map<Path, ModifiedStatus> children) {
            EnumSet<ModifiedStatus> reported = EnumSet.of(ModifiedStatus.UNTRACKED, ModifiedStatus.WORKSPACE_MODIFIED);
            children.entrySet().stream()
                .filter(e -> reported.contains(e.getValue()))
                .forEach(e -> {
                    String path = repo.ws().isDirectory(e.getKey()) ? e.getKey() + "/" : e.getKey().toString();
                    EnumSet<ModifiedStatus> statuses = files.computeIfAbsent(path, k -> EnumSet.noneOf(ModifiedStatus.class));
                    statuses.add(e.getValue());
                });
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            Path relativePath = repo.ws().toRelativePath(dir);
            Map<Path, ModifiedStatus> children = stack.pop();
            if (stack.isEmpty()) {
                addUnstaged(children);
                return FileVisitResult.CONTINUE;
            }
            if (children.values().stream().allMatch(s -> s == ModifiedStatus.STAGED)) {
                stack.peek().put(relativePath, ModifiedStatus.STAGED);
            } else if (children.values().stream().allMatch(s -> s == ModifiedStatus.UNTRACKED)) {
                stack.peek().put(relativePath, ModifiedStatus.UNTRACKED);
            } else {
                addUnstaged(children);
                stack.peek().put(relativePath, ModifiedStatus.MIXED);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
