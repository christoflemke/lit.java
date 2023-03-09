package lemke.christof.lit;

import java.nio.file.Path;

public record Repository (Workspace ws, Database db, Refs refs, Environment env) {
    public static Repository create(Path root) {
        Workspace ws = new Workspace(root);
        return new Repository(ws, new Database(root), new Refs(root), new Environment());
    }

    public Index createIndex() {
        return new Index(ws);
    }
}
