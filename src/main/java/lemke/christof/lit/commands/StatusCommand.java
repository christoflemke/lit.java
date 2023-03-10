package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.model.FileStat;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public record StatusCommand(Repository repo) implements Command {

    private enum ModifiedStatus {
        STAGED, UNSTAGED, MODIFIED, DELETED, MIXED
    }

    @Override
    public void run(String[] args) {
        PrintStream out = repo.io().out();
        try {
            Index idx = repo.createIndex();
            idx.load();

            Map<String, ModifiedStatus> files = new TreeMap<>();
            repo.ws().walkFileTree(new ModificationVisitor(idx, files));

            for(Index.Entry entry : idx.entries()) {
                Path path = entry.path();
                if(!repo.ws().resolve(path).toFile().exists()) {
                    files.put(path.toString(), ModifiedStatus.DELETED);
                }
            }

            for (Map.Entry<String, ModifiedStatus> e : files.entrySet()) {
                if (e.getValue() == ModifiedStatus.DELETED) {
                    out.println(" D " + e.getKey());
                } else if (e.getValue() == ModifiedStatus.UNSTAGED) {
                    out.println("?? " + e.getKey());
                } else if (e.getValue() == ModifiedStatus.MODIFIED) {
                    out.println(" M " + e.getKey());
                }
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class ModificationVisitor extends SimpleFileVisitor<Path> {
        private final Index idx;
        private final Map<String, ModifiedStatus> files;
        Stack<Map<Path, ModifiedStatus>> stack;

        public ModificationVisitor(Index idx, Map<String, ModifiedStatus> files) {
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
                        return ModifiedStatus.MODIFIED;
                    } else if(idxEntry.get().oid().equals(idx.hash(relativePath))) {
                        return ModifiedStatus.STAGED;
                    } else {
                        return ModifiedStatus.MODIFIED;
                    }
                }
            } else {
                return ModifiedStatus.UNSTAGED;
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
            EnumSet<ModifiedStatus> reported = EnumSet.of(ModifiedStatus.UNSTAGED, ModifiedStatus.MODIFIED);
            children.entrySet().stream()
                .filter(e -> reported.contains(e.getValue()))
                .forEach(e -> {
                    String path = repo.ws().isDirectory(e.getKey()) ? e.getKey() + "/" : e.getKey().toString();
                    files.put(path, e.getValue());
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
            } else if (children.values().stream().allMatch(s -> s == ModifiedStatus.UNSTAGED)) {
                stack.peek().put(relativePath, ModifiedStatus.UNSTAGED);
            } else {
                addUnstaged(children);
                stack.peek().put(relativePath, ModifiedStatus.MIXED);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
