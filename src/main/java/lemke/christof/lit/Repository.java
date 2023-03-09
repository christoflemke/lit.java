package lemke.christof.lit;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public record Repository (Workspace ws, Database db, Refs refs, Environment env, IO io) {
    public static Repository create(Path root) {
        Workspace ws = new Workspace(root);
        return new Repository(ws, new Database(root), new Refs(root), Environment.createDefault(), IO.createDefault());
    }

    public Index createIndex() {
        return new Index(ws);
    }
}