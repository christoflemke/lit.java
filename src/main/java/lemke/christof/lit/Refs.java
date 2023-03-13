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
            String ref = Files.readString(headPath());
            if (ref.startsWith("ref:")) {
                String[] split = ref.split(" ");
                String sha = Files.readString(gitPath().resolve(Path.of(split[1].trim())));
                return sha.trim();
            }
            return ref.trim();
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
