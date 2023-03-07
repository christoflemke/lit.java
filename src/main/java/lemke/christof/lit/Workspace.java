package lemke.christof.lit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public record Workspace (Path root){
    public List<Path> listFiles() {
        try {
            return Files.list(root)
                    .filter(p -> !Files.isDirectory(p))
                    .map(f -> root.relativize(f))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] read(Path f) {
        try {
            return Files.readAllBytes(root.resolve(f));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
