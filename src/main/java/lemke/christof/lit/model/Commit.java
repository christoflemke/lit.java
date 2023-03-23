package lemke.christof.lit.model;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public record Commit (
        Optional<Oid> parent,
        Oid treeOid,
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
        if (parent.isPresent()) {
            items.add("parent " + parent.get());
        }
        items.add("author " + author);
        items.add("committer " + committer);
        items.add("");
        items.add(message + "\n");

        return items.stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8);
    }

    public static Commit fromBytes(byte[] data) {
        Scanner scanner = new Scanner(new String(data));
        Map<String, String> fields = new HashMap<>();
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if("".equals(line)) {
                break;
            }
            String[] split = line.split(" ");
            fields.put(split[0], split[1]);
        }
        String message = scanner.nextLine();
        return new Commit(
            Oid.ofNullable(fields.get("parent")),
            Oid.of(fields.get("tree")),
            Author.fromString(fields.get("author")),
            Author.fromString(fields.get("committer")),
            message
        );
    }
}
