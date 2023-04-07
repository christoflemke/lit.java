package lemke.christof.lit.database;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public record Commit (
        Optional<Oid> parent,
        Oid treeOid,
        Author author,
        Author committer,
        String message
) implements DbObject {
    @Override
    public ObjectType type() {
        return ObjectType.COMMIT;
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

        return items.stream().collect(joining("\n")).getBytes(StandardCharsets.UTF_8);
    }

    public static Commit fromBytes(byte[] data) {
        String dataString = new String(data);
        Scanner scanner = new Scanner(dataString);
        Map<String, String> fields = new HashMap<>();
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if("".equals(line)) {
                break;
            }
            String[] split = line.split(" ");
            String rest = stream(split).skip(1).collect(joining(" "));
            fields.put(split[0], rest);
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

    public String titleLine() {
        return message.lines().findFirst().orElse("");
    }
}
