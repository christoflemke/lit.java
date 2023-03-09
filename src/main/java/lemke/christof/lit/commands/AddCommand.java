package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.model.Blob;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public record AddCommand(Workspace ws, Database db, Index idx, String[] args) implements Runnable {
    @Override
    public void run() {
        try(FileLock lock = idx.tryLock()){
            if (lock == null) {
                throw new RuntimeException("Failed to acquire index.lock");
            }
            try {
                idx.load();
                files().forEach((path) -> {
                    byte[] bytes = ws.read(path);
                    Blob blob = idx.add(path);
                    db.write(blob);
                });
                idx.commit();
            } finally {
                idx.unlock(lock);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Stream<Path> files() {
        return Stream.of(args).skip(1).flatMap(s -> files(Path.of(s))).map(p -> ws.toRelativePath(p));
    }

    Stream<Path> files(Path start) {
        try {
            return ws.isDirectory(start) ? ws.list(start) : Stream.of(start);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
