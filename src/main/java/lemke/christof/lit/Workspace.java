package lemke.christof.lit;

import lemke.christof.lit.database.Entry;
import lemke.christof.lit.database.FileStat;
import lemke.christof.lit.database.Tree;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public record Workspace(Path root) {

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

    public Stream<Path> listFiles() {
        return listFilesRecursive(root).map(f -> root.relativize(f)).sorted();
    }

    public Stream<Path> listFilesRecursive(Path root) {
        try {
            return Files.list(root)
                .flatMap(f -> {
                   if (f.toFile().isDirectory()) {
                       return listFilesRecursive(f);
                   } else {
                       return Stream.of(f);
                   }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record BuildResult(Tree root, List<Tree> trees) {
    }

    public void walkFileTree(FileVisitor<? super java.nio.file.Path> visitor) {
        try {
            Files.walkFileTree(root, visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileMode mode(Path name) {
        if (Files.isDirectory(name)) {
            return FileMode.DIRECTORY;
        } else if (Files.isExecutable(name)) {
            return FileMode.EXECUTABLE;
        } else {
            return FileMode.NORMAL;
        }
    }

    public BuildResult buildTree(Index idx) {
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
                Map<Path, Entry> cs = children.pop().stream()
                    .collect(toMap(Entry::name, identity()));
                trees.add(new Tree(cs));
                if (children.empty()) {
                    rootTree.set(new Tree(cs));
                } else {
                    Path path = root.relativize(dir);
                    children.peek().add(new Entry(path, new Tree(cs).oid(), mode(path)));
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = root.relativize(file);
                Optional<Index.Entry> entry = idx.get(relativePath);
                if (entry.isPresent()) {
                    children.peek().add(new Entry(relativePath, entry.get().oid(), mode(relativePath)));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return new BuildResult(rootTree.get(), trees);
    }

    public byte[] read(Path f) {
        try {
            return Files.readAllBytes(root.resolve(f));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readString(Path f) {
        return new String(read(f), StandardCharsets.UTF_8);
    }

    static int REGULAR_MODE = 0100644;
    static int EXECUTABLE_MODE = 0100755;

    public FileStat stat(Path path) {
        if (path.isAbsolute()) {
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
