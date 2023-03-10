package lemke.christof.lit.commands;

import lemke.christof.lit.IO;
import lemke.christof.lit.Lit;
import lemke.christof.lit.Repository;
import org.junit.jupiter.api.BeforeEach;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BaseTest {
    Repository repo;
    Lit lit;
    StringWriter out = new StringWriter();;
    StringWriter err = new StringWriter();;
    Map<String,String> envMap = new HashMap<>();

    {
        Path root = null;
        try {
            root = Files.createTempDirectory("test-");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Repository r = Repository.create(root);
        byte[] inputBytes = new byte[]{};
        IO io = new IO() {
            @Override
            public InputStream in() {
                return new ByteArrayInputStream(inputBytes);
            }

            @Override
            public BufferedWriter out() {
                return new BufferedWriter(out);
            }

            @Override
            public BufferedWriter err() {
                return new BufferedWriter(err);
            }
        };
        repo = new Repository(r.ws(), r.db(), r.refs(), key -> envMap.get(key), io);
        lit = new Lit(repo);
        lit.init(root.toString());
    }

    void write(String path) {
        write(path, "");
    }

    void write(String path, String data) {
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
        if(path.isAbsolute()) {
            throw new RuntimeException("Expected relative path: "+relativePath);
        }
        return repo.ws().resolve(path).toFile();
    }

    void makeExecutable(String path) {
        if(!resolveFile(path).setExecutable(true)) {
            throw new RuntimeException("Failed to make file executable: "+path);
        }
    }

    void touch(String path) {
        if (!resolveFile(path).setLastModified(System.currentTimeMillis())) {
            throw new RuntimeException("Failed to set last modified");
        }
    }

    void delete(String path) {
        delete(resolveFile(path));
    }

    private void delete(File file) {
        if(file.isDirectory()) {
            for(File c : file.listFiles()) {
                delete(c);
            }
        }
        if(!file.delete()) {
            throw new RuntimeException("Failed to delete file: "+file);
        }
    }

    String output() {
        return out.toString();
    }
}
