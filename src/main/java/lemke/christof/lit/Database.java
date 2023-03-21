package lemke.christof.lit;

import lemke.christof.lit.model.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public record Database(Path root) {
    Path objectsPath() {
        return root.resolve(".git").resolve("objects");
    }

    public void write(DbObject o) {
        try {
            String oid = o.oid();
            Path filePath = objectPath(oid);
            if(filePath.toFile().exists()) {
                return;
            }
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, o.compressedData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DbObject read(String oid) {
        Path path = objectPath(oid);
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

    private Path objectPath(String oid) {
        Path dirPath = objectsPath().resolve(oid.substring(0, 2));
        return dirPath.resolve(oid.substring(2));
    }

    public record TreeEntry(Path path, String oid, String mode) {}

    public Map<Path, TreeEntry> readTree(DbObject parent, Path path, String mode) {
        if (parent instanceof Commit c) {
            String treeOid = c.treeOid();
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
