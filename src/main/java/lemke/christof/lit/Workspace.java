package lemke.christof.lit;

import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.Entry;
import lemke.christof.lit.model.Tree;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
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

    public List<Path> listFiles() {
        try {
            return Files.list(root)
                    .filter(p -> !Files.isDirectory(p))
                    .map(f -> root.relativize(f))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record BuildResult (Tree root, List<Tree> trees) {}

    public BuildResult buildTree(Index idx) {
        try {
            List<Tree> trees = new ArrayList<>();
            AtomicReference<Tree> rootTree = new AtomicReference<>();
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                Stack<List<Entry>> children = new Stack<>();
                Path currentParent = null;
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.endsWith(".git")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    children.push(new ArrayList<>());
                    currentParent = dir;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Tree tree = new Tree(children.pop());
                    currentParent = currentParent.getParent();
                    trees.add(tree);
                    if(children.empty()) {
                        rootTree.set(tree);
                    } else {
                        children.peek().add(new Entry(root.relativize(dir), tree.oid()));
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
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
}
