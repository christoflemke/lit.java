package lemke.christof.lit.commands;

import lemke.christof.lit.IO;
import lemke.christof.lit.Lit;
import lemke.christof.lit.Repository;
import org.junit.jupiter.api.BeforeEach;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BaseTest {
    Repository repo;
    Lit lit;
    StringWriter out;
    StringWriter err;

    @BeforeEach
    public void setup() throws IOException {
        Path root = Files.createTempDirectory("test-");
        Repository r = Repository.create(root);
        byte[] inputBytes = new byte[] {};
        out = new StringWriter();
        err = new StringWriter();
        repo = new Repository(r.ws(), r.db(), r.refs(), r.env(), new IO() {
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
        });
        lit = new Lit(repo);
        lit.init(root.toString());
    }

    void create(String path, String data) throws IOException {
        Path fooPath = repo.ws().resolve(path);
        Files.createDirectories(fooPath.getParent());
        Files.writeString(fooPath, data);
    }

    String output() {
        return out.toString();
    }
}
