package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.model.Blob;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record AddCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Index idx = repo.createIndex();
        try(FileLock lock = idx.tryLock()){
            if (lock == null) {
                throw new RuntimeException("Failed to acquire index.lock");
            }
            try {
                idx.load();
                Set<Path> files = files(args).collect(Collectors.toSet());
                for(Path path : files) {
                    Blob blob = idx.add(path);
                    repo.db().write(blob);
                }
                idx.commit();
            } finally {
                idx.unlock(lock);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Stream<Path> files(String[] args) {
        return Stream.of(args)
                .map(Path::of)
                .flatMap(this::files)
                .map(p -> repo.ws().toRelativePath(p));
    }

    Stream<Path> files(Path start) {
        try {
            return repo.ws().isDirectory(start) ? repo.ws().list(start).flatMap(this::files) : Stream.of(start);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
