package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.model.Blob;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.stream.Stream;

public record AddCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        try(FileLock lock = repo.idx().tryLock()){
            if (lock == null) {
                throw new RuntimeException("Failed to acquire index.lock");
            }
            try {
                repo.idx().load();
                files(args).forEach((path) -> {
                    Blob blob = repo.idx().add(path);
                    repo.db().write(blob);
                });
                repo.idx().commit();
            } finally {
                repo.idx().unlock(lock);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Stream<Path> files(String[] args) {
        return Stream.of(args).flatMap(s -> files(Path.of(s))).map(p -> repo.ws().toRelativePath(p));
    }

    Stream<Path> files(Path start) {
        try {
            return repo.ws().isDirectory(start) ? repo.ws().list(start) : Stream.of(start);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
