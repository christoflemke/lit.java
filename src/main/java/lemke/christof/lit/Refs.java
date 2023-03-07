package lemke.christof.lit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public record Refs (Path root) {
    Path headPath() {
        return root.resolve(".git").resolve("HEAD");
    }
    public void updateHead(String ref) {
        try {
            Files.writeString(headPath(), ref);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readHead() {
        try {
            return Files.readString(headPath());
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
