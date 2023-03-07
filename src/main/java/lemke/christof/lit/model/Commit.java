package lemke.christof.lit.model;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public record Commit (
        String treeOid,
        Author author,
        Author committer,
        String message
) implements DbObject {
    @Override
    public String type() {
        return "commit";
    }

    @Override
    public byte[] data() {
        String stringData = List.of(
                "tree " + treeOid,
                "author " + author,
                "committer " + committer,
                "",
                message + "\n"
        ).stream().collect(Collectors.joining("\n"));
        return stringData.getBytes(StandardCharsets.UTF_8);
    }
}
