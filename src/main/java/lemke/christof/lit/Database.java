package lemke.christof.lit;

import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.Commit;
import lemke.christof.lit.model.DbObject;
import lemke.christof.lit.model.Tree;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
            switch (type) {
                case "blob":
                    return new Blob(data);
                case "commit":
                    return Commit.fromBytes(data);
                case "tree":
                    return Tree.fromBytes(data);
                default:
                    throw new RuntimeException("Unknown type: "+type);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path objectPath(String oid) {
        Path dirPath = objectsPath().resolve(oid.substring(0, 2));
        return dirPath.resolve(oid.substring(2));
    }
}
