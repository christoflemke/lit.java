package lemke.christof.lit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public record Refs (Path root) {
    Path headPath() {
        return gitPath().resolve("HEAD");
    }

    private Path gitPath() {
        return root.resolve(".git");
    }

    public void updateHead(String ref) {

        try {
            Path tempFile = Files.createTempFile(gitPath(), "head-", null);
            Files.writeString(tempFile, ref);
            Files.move(tempFile, headPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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
