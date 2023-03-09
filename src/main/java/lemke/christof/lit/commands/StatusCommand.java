package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public record StatusCommand(Repository repo) implements Command {

    private enum FileStatus {
        STAGED, UNSTAGED, MIXED
    }

    @Override
    public void run(String[] args) {
        BufferedWriter out = repo.io().out();
        try {
            Index idx = repo.createIndex();
            idx.load();

            Set<Path> files = new TreeSet<>();
            repo.ws().walkFileTree(new SimpleFileVisitor<>() {
                Stack<Map<Path, FileStatus>> stack = new Stack<>();
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relativePath = repo.ws().toRelativePath(file);
                    if (idx.contains(relativePath)) {
                        stack.peek().put(relativePath, FileStatus.STAGED);
                    } else {
                        stack.peek().put(relativePath, FileStatus.UNSTAGED);
                    }
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

                private void addUnstaged(Map<Path, FileStatus> children) {
                    children.entrySet().stream()
                            .filter(e -> e.getValue() == FileStatus.UNSTAGED)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toCollection(() -> files));
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    Path relativePath = repo.ws().toRelativePath(dir);
                    Map<Path, FileStatus> children = stack.pop();
                    if (stack.isEmpty()) {
                        addUnstaged(children);
                        return FileVisitResult.CONTINUE;
                    }
                    if (children.values().stream().allMatch(s -> s == FileStatus.STAGED)) {
                        stack.peek().put(relativePath, FileStatus.STAGED);
                    } else if(children.values().stream().allMatch(s -> s == FileStatus.UNSTAGED)) {
                        stack.peek().put(relativePath, FileStatus.UNSTAGED);
                    } else {
                        addUnstaged(children);
                        stack.peek().put(relativePath, FileStatus.MIXED);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path p : files) {
                if (repo.ws().isDirectory(p)) {
                    out.write("?? " + p + "/");
                } else {
                    out.write("?? " + p);
                }
                out.newLine();
            }
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
