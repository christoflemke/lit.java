package lemke.christof.lit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BaseTest {
    protected Repository repo;
    protected Lit lit;

    {
        final Path root;
        try {
            root = Files.createTempDirectory("test-");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        lit = new Lit(root);
        this.repo = lit.repo();
        lit.init(root.toString());
    }

    protected void write(String path) {
        write(path, "");
    }

    protected void write(String path, String data) {
        try {
            Path fooPath = repo.ws().resolve(path);
            Files.createDirectories(fooPath.getParent());
            Files.writeString(fooPath, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File resolveFile(String relativePath) {
        Path path = Path.of(relativePath);
        if (path.isAbsolute()) {
            throw new RuntimeException("Expected relative path: " + relativePath);
        }
        return repo.ws().resolve(path).toFile();
    }

    protected void makeExecutable(String path) {
        if (!resolveFile(path).setExecutable(true)) {
            throw new RuntimeException("Failed to make file executable: " + path);
        }
    }

    protected void touch(String path) {
        if (!resolveFile(path).setLastModified(System.currentTimeMillis())) {
            throw new RuntimeException("Failed to set last modified");
        }
    }

    protected void delete(String path) {
        delete(resolveFile(path));
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                delete(c);
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete file: " + file);
        }
    }
}
