package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.model.Blob;

import java.io.IOException;
import java.nio.file.Path;

public record AddCommand (Workspace ws, Database db, Index idx, String[] args) implements Runnable {
    @Override
    public void run() {
        Path path = Path.of(args[1]);
        byte[] bytes = ws.read(path);
        Blob blob = new Blob(bytes);
        Object stat = null;
        db.write(blob);
        idx.add(path, blob.oid());
        idx.commit();
    }
}
