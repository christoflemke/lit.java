package lemke.christof.lit;

import lemke.christof.lit.model.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public record Database(Path root) {
    Path objectsPath() {
        return root.resolve(".git").resolve("objects");
    }

    public void write(DbObject o) {
        try {
            Oid oid = o.oid();
            Path filePath = oid.objectPath(objectsPath());
            if(filePath.toFile().exists()) {
                return;
            }
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, o.compressedData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DbObject read(Oid oid) {
        Path path = oid.objectPath(objectsPath());
        try {
            InflaterInputStream in = new InflaterInputStream(new FileInputStream(path.toFile()), new Inflater());
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            String type = "";
            char c = 0;
            while ((c = (char) buffer.get()) != ' ') {
                type += c;
            }
            String length = "";
            while ((c = (char) buffer.get()) != 0) {
                length += c;
            }
            byte[] data = Arrays.copyOfRange(bytes, buffer.position(), buffer.limit());
            return switch (type) {
                case "blob" -> new Blob(data);
                case "commit" -> Commit.fromBytes(data);
                case "tree" -> Tree.fromBytes(data);
                default -> throw new RuntimeException("Unknown type: "+type);
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Oid> prefixMatch(String name) {
        if(name.length() <2) {
            return List.of();
        }
        try {
            return Files.list(objectsPath().resolve(name.substring(0, 2)))
                .filter(p -> p.getFileName().toString().startsWith(name.substring(2)))
                .map(p -> p.getParent().getFileName().toString() +p.getFileName())
                .map(Oid::of)
                .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record TreeEntry(Path path, Oid oid, String mode) {}

    public Map<Path, TreeEntry> readTree(DbObject parent, Path path, String mode) {
        if (parent instanceof Commit c) {
            Oid treeOid = c.treeOid();
            Tree tree = (Tree) read(treeOid);
            return readTree(tree, path, mode);
        }
        if (parent instanceof Blob) {
            return Map.of(path, new TreeEntry(path, parent.oid(), mode));
        }
        Tree tree = (Tree) parent;
        Map<Path, TreeEntry> result = new HashMap<>();
        for (Entry e : tree.entries()) {
            Path entryPath = path.resolve(e.relativePath());
            result.putAll(readTree(read(e.oid()), entryPath, e.mode()));
        }
        return result;
    }
}
