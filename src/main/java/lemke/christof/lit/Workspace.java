package lemke.christof.lit;

import lemke.christof.lit.commands.StatusCommand;
import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.Entry;
import lemke.christof.lit.model.FileStat;
import lemke.christof.lit.model.Tree;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Workspace (Path root){

    public boolean isDirectory(Path path) {
        return root.resolve(path).toFile().isDirectory();
    }

    public Stream<Path> list(Path start) throws IOException {
        return Files.list(root.resolve(start));
    }

    public Path toRelativePath(Path path) {
        if (path.isAbsolute()) {
            return root.relativize(path);
        } else {
            return path;
        }
    }

    public Path resolve(String relativePath) {
        return root.resolve(relativePath);
    }

    public Path resolve(Path relativePath) {
        return root.resolve(relativePath);
    }

    record FileTree (Path path, List<FileTree> children) {
        public String print(int indent) {
            String result = "";
            for(int i = 0; i<indent; i++) {
                result += " ";
            }
            result += path + "\n";
            for(FileTree c: children) {
                result += c.print(indent + 2);
            }
            return result;
        }

        public boolean isDir() {
            return !children.isEmpty();
        }
    }

    public Stream<Path> listFiles() {
        try {
            return Files.list(root)
                    .filter(p -> !Files.isDirectory(p))
                    .map(f -> root.relativize(f))
                    .sorted();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record BuildResult (Tree root, List<Tree> trees) {}

    public void walkFileTree(FileVisitor<? super java.nio.file.Path> visitor) throws IOException {
        Files.walkFileTree(root, visitor);
    }

    public BuildResult buildTree(Index idx) {
        try {
            List<Tree> trees = new ArrayList<>();
            AtomicReference<Tree> rootTree = new AtomicReference<>();
            walkFileTree(new SimpleFileVisitor<>() {
                Stack<List<Entry>> children = new Stack<>();
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.endsWith(".git")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    children.push(new ArrayList<>());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    Tree tree = new Tree(children.pop());
                    trees.add(tree);
                    if(children.empty()) {
                        rootTree.set(tree);
                    } else {
                        children.peek().add(new Entry(root.relativize(dir), tree.oid()));
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relativePath = root.relativize(file);
                    Optional<Index.Entry> entry = idx.get(relativePath);
                    if (entry.isPresent()) {
                        children.peek().add(new Entry(relativePath, entry.get().oid()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return new BuildResult(rootTree.get(), trees);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] read(Path f) {
        try {
            return Files.readAllBytes(root.resolve(f));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    static int REGULAR_MODE = 0100644;
    static int EXECUTABLE_MODE = 0100755;
    public FileStat stat(Path path) {
        if(path.isAbsolute()) {
            throw new RuntimeException("Expected relative path");
        }
        Path absolutePath = resolve(path);
        PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(absolutePath, PosixFileAttributeView.class);
        final PosixFileAttributes attributes;
        final Long inode;
        try {
            attributes = fileAttributeView.readAttributes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            return new FileStat(
                    Math.toIntExact(attributes.creationTime().toMillis() / 1000),
                    attributes.creationTime().toInstant().get(ChronoField.NANO_OF_SECOND),
                    Math.toIntExact(attributes.lastModifiedTime().toMillis() / 1000),
                    attributes.lastModifiedTime().toInstant().get(ChronoField.NANO_OF_SECOND),
                    Math.toIntExact((Long) Files.getAttribute(absolutePath, "unix:dev")),
                    Math.toIntExact((Long) Files.getAttribute(absolutePath, "unix:ino")),
                    attributes.permissions().contains(PosixFilePermission.OWNER_EXECUTE) ? EXECUTABLE_MODE : REGULAR_MODE,
                    (Integer) Files.getAttribute(absolutePath, "unix:uid"),
                    (Integer) Files.getAttribute(absolutePath, "unix:gid"),
                    Math.toIntExact(Math.min(attributes.size(), Integer.MAX_VALUE))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
