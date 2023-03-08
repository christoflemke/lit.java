package lemke.christof.lit;

import lemke.christof.lit.model.DbObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record Database(Path root) {
    Path objectsPath() {
        return root.resolve(".git").resolve("objects");
    }

    public void write(DbObject o) {
        try {
            String oid = o.oid();
            Path dirPath = objectsPath().resolve(oid.substring(0, 2));
            Path filePath = dirPath.resolve(oid.substring(2));
            Files.createDirectories(dirPath);
            Files.write(filePath, o.compressedData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
