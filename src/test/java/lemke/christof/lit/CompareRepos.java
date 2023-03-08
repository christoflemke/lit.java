package lemke.christof.lit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompareRepos {

    @BeforeAll
    public static void compare() throws Exception {
        String path = CompareRepos.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        Path root = Path.of(path).getParent().getParent().getParent().getParent();
        ProcessBuilder processBuilder = new ProcessBuilder("./testdata/setup-test-data.sh");
        processBuilder.directory(root.toFile());
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode);
    }

    public static Stream<String> shas() {
        return Stream.of(
                "32aad8c35387213772370ce678e403db1ed28243",
                "42f46a330ec74552df5bc383abf72931e194360d",
                "678c5674a0e41cab69ddb8469ac499c4363d9ea3",
                "7bfce1cd9c31878b94002055025281617f403903",
                "88e38705fdbd3608cddbe904b67c731f3234c45b",
                "916966c40a819e1f5aafe9ea58a34892ecb82a9a",
                "cc628ccd10742baea8241c5924df992b5c019f71",
                "ce013625030ba8dba906f756967f9e9ca394464a"
        );
    }

    @ParameterizedTest
    @MethodSource("shas")
    public void compareBytes(String sha) throws IOException {
        Path actualPath = Path.of("testdata", "my-repo", ".git", "objects", sha.substring(0,2), sha.substring(2));
        Path referencePath = Path.of("testdata", "reference-repo", ".git", "objects", sha.substring(0,2), sha.substring(2));
        assertTrue(actualPath.toFile().exists(), "Actual path does not exist");
        assertTrue(referencePath.toFile().exists(), "Actual path does not exist");
        byte[] actualBytes = Files.readAllBytes(actualPath);
        byte[] referenceBytes = Files.readAllBytes(referencePath);
        assertEquals(HexFormat.of().formatHex(referenceBytes), HexFormat.of().formatHex(actualBytes));
    }

    @Test
    public void compareIndex() throws IOException {
        Path actualPath = Path.of("testdata", "lit-index.hex");
        Path referencePath = Path.of("testdata", "git-index.hex");
        byte[] actualBytes = Files.readAllBytes(actualPath);
        byte[] referenceBytes = Files.readAllBytes(referencePath);
        assertEquals(HexFormat.of().formatHex(referenceBytes), HexFormat.of().formatHex(actualBytes));
    }

}
