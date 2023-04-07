package lemke.christof.lit.database;

import lemke.christof.lit.Database;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

public class TreeDiff {
    private final Database db;
    private final Map<Path, Change> changes;

    public TreeDiff(Database db) {
        this.db = db;
        this.changes = new TreeMap<>();

    }

    public void compareOids(Oid a, Oid b) {
        compareOids(Optional.ofNullable(a), Optional.ofNullable(b), Path.of(""));
    }

    private void compareOids(Optional<Oid> a, Optional<Oid> b, Path prefix) {
        if (a.equals(b)) {
            return;
        }

        Map<Path, Entry> aEntries = oidToEntries(a);
        Map<Path, Entry> bEntries = oidToEntries(b);
        detectDeletions(aEntries, bEntries, prefix);
        detectAdditions(aEntries, bEntries, prefix);
    }

    private void detectAdditions(Map<Path, Entry> a, Map<Path, Entry> b, Path prefix) {
        for (var entry : b.values()) {
            var path = prefix.resolve(entry.name());
            var other = a.get(entry.name());

            if (other != null) {
                continue;
            }

            if (entry.isTree()) {
                compareOids(Optional.empty(), Optional.of(entry.oid()), path);
            } else {
                changes.put(path, new Change(Optional.empty(), Optional.of(entry)));
            }
        }
    }

    private void detectDeletions(Map<Path, Entry> a, Map<Path, Entry> b, Path prefix) {
        for (var entry : a.values()) {
            var path = prefix.resolve(entry.name());
            var other = Optional.of(b.get(path));

            if (entry.equals(other)) {
                continue;
            }

            Function<Entry, Oid> entryToOid = e -> e.isTree() ? e.oid() : null;
            Optional<Oid> treeAOid = Optional.of(entry).map(entryToOid);
            Optional<Oid> treeBOid = other.map(entryToOid);

            compareOids(treeAOid, treeBOid, path);
            Change change = new Change(
                Optional.of(entry).filter(Entry::isNotATree),
                other.filter(Entry::isNotATree)
            );
            if (!change.isEmpty()) {
                changes.put(path, change);
            }
        }
    }

    private Map<Path, Entry> oidToEntries(Optional<Oid> oid) {
        return oid
            .flatMap(this::oidToTree)
            .map(Tree::entries)
            .orElse(Map.of());
    }

    private Optional<Tree> oidToTree(Oid oid) {
        DbObject object = db.read(oid);
        if (object instanceof Commit c) {
            return oidToTree(c.treeOid());
        } else if (object instanceof Tree t) {
            return Optional.of(t);
        } else {
            return Optional.empty();
        }
    }

    private record Change(Optional<Entry> a, Optional<Entry> b) {
        public boolean isEmpty() {
            return a.isEmpty() && b.isEmpty();
        }
    }
}
