package lemke.christof.lit;

import lemke.christof.lit.model.FileStat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class IndexEntryTest {
    private static Path path;
    private static Index.Entry entry;

    @BeforeAll
    public static void setup() throws IOException {
        Path root = Files.createTempDirectory("test-");
        path = Files.createTempFile(root, "test-", null);
        Workspace ws = new Workspace(root);
        entry = new Index(ws).createEntry(path, "1234");
    }

    public static Stream<String> intMethods() {
        return Stream.of(
                "ctime_sec",
                "ctime_nano",
                "mtime_sec",
                "mtime_nano",
                "dev",
                "ino",
                "mode",
                "uid",
                "gid",
                "size"
        );
    }

    @ParameterizedTest
    @MethodSource("intMethods")
    public void testIntValue(String methodName) throws Exception {
        Method method = FileStat.class.getDeclaredMethod(methodName);
        Object result = method.invoke(entry.stat());
        assertThat(result, isA(Integer.class));
    }


}
