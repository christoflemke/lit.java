package lemke.christof.lit.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record Commit (
        String parent,
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
        List<String> items = new ArrayList<>();
        items.add("tree " + treeOid);
        if (parent != null) {
            items.add("parent " + parent);
        }
        items.add("author " + author);
        items.add("committer " + committer);
        items.add("");
        items.add(message + "\n");

        return items.stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8);
    }
}
