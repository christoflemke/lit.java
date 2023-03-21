package lemke.christof.lit.status;

import com.google.common.collect.Sets;
import lemke.christof.lit.Database;
import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.model.Commit;
import lemke.christof.lit.model.FileStat;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class StatusBuilder {
    private EnumSet<ModifiedStatus> workspaceStatuses = EnumSet.of(
        ModifiedStatus.WORKSPACE_MODIFIED,
        ModifiedStatus.WORKSPACE_DELETED
    );
    private EnumSet<ModifiedStatus> indexStatuses = EnumSet.of(
        ModifiedStatus.INDEX_ADDED,
        ModifiedStatus.INDEX_MODIFIED,
        ModifiedStatus.INDEX_DELETED
    );
    private final SortedSet<String> changed = new TreeSet<>();
    private final SortedMap<String, ModifiedStatus> indexChanges = new TreeMap<>();
    private final SortedMap<String, ModifiedStatus> workspaceChanges = new TreeMap<>();
    private final SortedSet<String> untrackedFiles = new TreeSet<>();
    private final Repository repo;
    private final Index idx;

    public StatusBuilder(Repository repo, Index idx) {
        this.repo = repo;
        this.idx = idx;
    }

    public Status computeChanges() {
        repo.ws().walkFileTree(new StatusBuilder.ModificationVisitor());
        Map<Path, Database.TreeEntry> headTree = compareIndexHead();
        compareIndexWS();
        return new Status(changed, indexChanges, workspaceChanges, untrackedFiles, headTree);
    }

    private void compareIndexWS() {
        Set<Path> indexPaths = idx.entries().stream().map(e -> e.path()).collect(Collectors.toSet());
        Set<Path> wsFiles = repo.ws().listFiles().collect(Collectors.toSet());

        Sets.SetView<Path> inIndexButNotInWS = Sets.difference(indexPaths, wsFiles);
        for(var p : inIndexButNotInWS) {
            addStatus(p.toString(), ModifiedStatus.WORKSPACE_DELETED);
        }
    }

    private Map<Path, Database.TreeEntry> compareIndexHead() {
        Map<Path, Database.TreeEntry> headTree = loadHeadTree();
        Set<Path> indexPaths = idx.entries().stream().map(e -> e.path()).collect(Collectors.toSet());
        Set<Path> headPaths = headTree.keySet();

        Sets.SetView<Path> inHeadButNotInIndex = Sets.difference(headPaths, indexPaths);
        for(var p : inHeadButNotInIndex) {
            addStatus(p.toString(), ModifiedStatus.INDEX_DELETED);
        }

        Sets.SetView<Path> inIndexButNotInHead = Sets.difference(indexPaths, headPaths);
        for(var p : inIndexButNotInHead) {
            addStatus(p.toString(), ModifiedStatus.INDEX_ADDED);
        }

        Sets.SetView<Path> intersection = Sets.intersection(indexPaths, headPaths);
        for (var p : intersection) {
            Database.TreeEntry dbObject = headTree.get(p);
            Index.Entry entry = idx.get(p).get();
            if (!dbObject.oid().equals(entry.oid())) {
                addStatus(p.toString(), ModifiedStatus.INDEX_MODIFIED);
            }
        }
        return headTree;
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

    private Map<Path, Database.TreeEntry> loadHeadTree() {
        String head = this.repo.refs().readHead();
        if (head == null) {
            return Map.of();
        }
        Commit commit = (Commit) repo.db().read(head);
        return repo.db().readTree(commit, Path.of(""), null);
    }

    private class ModificationVisitor extends SimpleFileVisitor<Path> {
        Stack<Map<Path, ModifiedStatus>> stack = new Stack<>();

        private void put(Path path, ModifiedStatus status) {
            stack.peek().put(path, status);
        }

        private ModifiedStatus checkForModification(Path relativePath) {
            Optional<Index.Entry> idxEntry = idx.get(relativePath);
            if (!idxEntry.isPresent()) {
                return ModifiedStatus.UNTRACKED;
            }

            FileStat currentStat = repo.ws().stat(relativePath);
            if (currentStat.equals(idxEntry.get().stat())) {
                return ModifiedStatus.STAGED;
            }

            if (idxEntry.get().stat().mode() != currentStat.mode()) {
                return ModifiedStatus.WORKSPACE_MODIFIED;
            }

            String fileOid = idx.hash(relativePath);
            if (idxEntry.get().oid().equals(fileOid)) {
                idx.update(relativePath, fileOid, currentStat);
                return ModifiedStatus.STAGED;
            }

            return ModifiedStatus.WORKSPACE_MODIFIED;
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
