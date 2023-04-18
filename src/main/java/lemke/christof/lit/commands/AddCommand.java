package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.database.Blob;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record AddCommand(Repository repo) implements Command {
    @Override
    public void run(String[] args) {
        Index idx = repo.createIndex();
        idx.withLock(() -> {
            idx.load();
            Set<Path> files = files(args).collect(Collectors.toSet());
            for (Path path : files) {
                Blob blob = idx.add(path);
                repo.db().write(blob);
            }
            idx.commit();
            return null;
        });
    }

    Stream<Path> files(String[] args) {
        return Stream.of(args)
            .map(Path::of)
            .flatMap(this::files)
            .map(p -> repo.ws().toRelativePath(p));
    }

    static Set<Path> IGNORED = Set.of(Path.of(".git"));

    Stream<Path> files(Path start) {
        if (IGNORED.contains(repo.ws().toRelativePath(start))) {
            return Stream.of();
        } else if (repo.ws().isDirectory(start)) {
            return repo.ws().list(start).flatMap(this::files);
        } else {
            return Stream.of(start);
        }
    }
}
