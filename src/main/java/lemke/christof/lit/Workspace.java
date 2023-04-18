package lemke.christof.lit;

import lemke.christof.lit.database.*;
import lemke.christof.lit.repository.Migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public record Workspace(Path root) {

    public boolean isDirectory(Path path) {
        return root.resolve(path).toFile().isDirectory();
    }

    public Stream<Path> list(Path start) {
        try {
            return Files.list(root.resolve(start));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void applyMigration(Migration migration) {
        new Migrator(migration).applyMigration();
    }

    public boolean isFile(Path path) {
        return resolve(path).toFile().isFile();
    }

    private class Migrator {
        private final Migration migration;

        private Migrator(Migration migration) {
            this.migration = migration;
        }

        public void applyMigration() {
            applyChangeList(TreeDiff.ChangeType.Delete);
            deleteDirectories();
            createDirectories();
            applyChangeList(TreeDiff.ChangeType.Update);
            applyChangeList(TreeDiff.ChangeType.Create);
        }

        private void createDirectories() {
            migration.mkdirs().stream().sorted().forEach(p -> {
                try {
                    if (Files.exists(p)) {
                        Files.delete(p);
                    }
                    if (!Files.isDirectory(p)) {
                        Files.createDirectories(p);
                    }
                } catch (DirectoryNotEmptyException e) {
                    // skip
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        private void deleteDirectories() {
            migration.rmdirs().stream()
                .sorted(reverseOrder())
                .map(p -> root.resolve(p))
                .map(Path::toFile)
                .forEach(File::delete);
        }

        private void applyChangeList(TreeDiff.ChangeType action) {
            for (var change : migration.changes().get(action)) {
                var path = root.resolve(change.path());

                deleteRecursive(path);
                if (action == TreeDiff.ChangeType.Delete) {
                    continue;
                }

                Blob blob = migration.blobData(change.oid());

                try {
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, blob.stringData(), CREATE, TRUNCATE_EXISTING);
                    if (change.newItem().get().mode() == FileMode.EXECUTABLE) {
                        path.toFile().setExecutable(true);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void deleteRecursive(Path path) {
            try {
                Files.walk(path)
                    .sorted(reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch(NoSuchFileException e) {
                // ignore
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

    public Optional<Blob> read(Path f) {
        try {
            byte[] bytes = Files.readAllBytes(root.resolve(f));
            return Optional.of(new Blob(bytes));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> readString(Path f) {
        return read(f).map(Blob::stringData);
    }

    static int REGULAR_MODE = 0100644;
    static int EXECUTABLE_MODE = 0100755;

    public Optional<FileStat> stat(Path path) {
        if (path.isAbsolute()) {
            throw new RuntimeException("Expected relative path");
        }
        Path absolutePath = resolve(path);
        if(!absolutePath.toFile().exists()) {
            return Optional.empty();
        }
        PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(absolutePath, PosixFileAttributeView.class);
        final PosixFileAttributes attributes;
        final Long inode;
        try {
            attributes = fileAttributeView.readAttributes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            return Optional.of(new FileStat(
                path,
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
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
