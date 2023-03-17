package lemke.christof.lit.status;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.model.Commit;
import lemke.christof.lit.model.DbObject;
import lemke.christof.lit.model.FileStat;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class StatusBuilder {
    public SortedMap<String, ModifiedStatus> indexChanges() {
        return indexChanges;
    }

    public SortedMap<String, ModifiedStatus> workspaceChanges() {
        return workspaceChanges;
    }

    public SortedSet<String> untrackedFiles() {
        return untrackedFiles;
    }

    public SortedSet<String> changed() {
        return changed;
    }

    public enum ModifiedStatus {
        STAGED("S", " "),
        UNTRACKED("?", " "),
        WORKSPACE_MODIFIED("M", "modified"),
        WORKSPACE_DELETED("D", "deleted"),
        INDEX_ADDED("A", "new file"),
        INDEX_MODIFIED("M", "modified"),
        MIXED("_", "_"),
        NO_STATUS(" ", " ");

        public final String shortStatus;
        public final String longStatus;

        ModifiedStatus(String shortStatus, String longStatus) {
            this.shortStatus = shortStatus;
            this.longStatus = longStatus;
        }
    }

    private EnumSet<StatusBuilder.ModifiedStatus> workspaceStatuses = EnumSet.of(
        StatusBuilder.ModifiedStatus.WORKSPACE_MODIFIED,
        StatusBuilder.ModifiedStatus.WORKSPACE_DELETED
    );
    private EnumSet<StatusBuilder.ModifiedStatus> indexStatuses = EnumSet.of(
        StatusBuilder.ModifiedStatus.INDEX_ADDED,
        StatusBuilder.ModifiedStatus.INDEX_MODIFIED
    );
    private final SortedSet<String> changed = new TreeSet<>();
    private final SortedMap<String, StatusBuilder.ModifiedStatus> indexChanges = new TreeMap<>();
    private final SortedMap<String, StatusBuilder.ModifiedStatus> workspaceChanges = new TreeMap<>();
    private final SortedSet<String> untrackedFiles = new TreeSet<>();
    private final Repository repo;
    private final Index idx;

    public StatusBuilder(Repository repo, Index idx) {
        this.repo = repo;
        this.idx = idx;
    }

    public void computeChanges() {
        repo.ws().walkFileTree(new StatusBuilder.ModificationVisitor());
        computeChangesFromIndex();
    }

    private void computeChangesFromIndex() {
        Map<Path, DbObject> headTree = loadHeadTree();

        for (Index.Entry entry : idx.entries()) {
            Path path = entry.path();
            if (!repo.ws().resolve(path).toFile().exists()) {
                addStatus(path.toString(), StatusBuilder.ModifiedStatus.WORKSPACE_DELETED);
            }
            if (headTree.containsKey(path)) {
                DbObject dbObject = headTree.get(path);
                System.out.println(dbObject);
            } else {
                addStatus(path.toString(), StatusBuilder.ModifiedStatus.INDEX_ADDED);
            }
        }
    }

    private void addStatus(String path, StatusBuilder.ModifiedStatus status) {
        changed.add(path);
        if (workspaceStatuses.contains(status)) {
            workspaceChanges.put(path, status);
        } else if (indexStatuses.contains(status)) {
            indexChanges.put(path, status);
        }

        if (StatusBuilder.ModifiedStatus.UNTRACKED == status) {
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
        Stack<Map<Path, StatusBuilder.ModifiedStatus>> stack = new Stack<>();

        private void put(Path path, StatusBuilder.ModifiedStatus status) {
            stack.peek().put(path, status);
        }

        private StatusBuilder.ModifiedStatus checkForModification(Path relativePath) {
            Optional<Index.Entry> idxEntry = idx.get(relativePath);
            if (!idxEntry.isPresent()) {
                return StatusBuilder.ModifiedStatus.UNTRACKED;
            }

            FileStat currentStat = repo.ws().stat(relativePath);
            if (currentStat.equals(idxEntry.get().stat())) {
                return StatusBuilder.ModifiedStatus.STAGED;
            }

            if (idxEntry.get().stat().mode() != currentStat.mode()) {
                return StatusBuilder.ModifiedStatus.WORKSPACE_MODIFIED;
            }

            String fileOid = idx.hash(relativePath);
            if (idxEntry.get().oid().equals(fileOid)) {
                idx.update(relativePath, fileOid, currentStat);
                return StatusBuilder.ModifiedStatus.STAGED;
            }

            return StatusBuilder.ModifiedStatus.WORKSPACE_MODIFIED;
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

        private void addChanged(Map<Path, StatusBuilder.ModifiedStatus> children) {
            EnumSet<StatusBuilder.ModifiedStatus> reported = EnumSet.of(StatusBuilder.ModifiedStatus.UNTRACKED, StatusBuilder.ModifiedStatus.WORKSPACE_MODIFIED);
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
            Map<Path, StatusBuilder.ModifiedStatus> children = stack.pop();
            if (stack.isEmpty()) {
                addChanged(children);
                return FileVisitResult.CONTINUE;
            }
            if (children.values().stream().allMatch(s -> s == StatusBuilder.ModifiedStatus.STAGED)) {
                stack.peek().put(relativePath, StatusBuilder.ModifiedStatus.STAGED);
            } else if (children.values().stream().allMatch(s -> s == StatusBuilder.ModifiedStatus.UNTRACKED)) {
                stack.peek().put(relativePath, StatusBuilder.ModifiedStatus.UNTRACKED);
            } else {
                addChanged(children);
                stack.peek().put(relativePath, StatusBuilder.ModifiedStatus.MIXED);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
