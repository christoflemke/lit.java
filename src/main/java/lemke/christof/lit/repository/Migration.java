package lemke.christof.lit.repository;

import lemke.christof.lit.Repository;
import lemke.christof.lit.database.Blob;
import lemke.christof.lit.database.DbObject;
import lemke.christof.lit.database.Oid;
import lemke.christof.lit.database.TreeDiff;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

public class Migration {
    private final TreeDiff diff;
    private final Repository repo;
    private final Map<TreeDiff.ChangeType, Set<TreeDiff.Change>> changes;
    private final Set<Path> mkdirs;
    private final Set<Path> rmdirs;

    public Migration(Repository repository, TreeDiff diff) {
        this.repo = repository;
        this.diff = diff;

        Comparator<TreeDiff.Change> comparing = Comparator.comparing(TreeDiff.Change::path);
        this.changes = Map.of(
            TreeDiff.ChangeType.Create, new TreeSet<>(comparing),
            TreeDiff.ChangeType.Delete, new TreeSet<>(comparing),
            TreeDiff.ChangeType.Update, new TreeSet<>(comparing)
        );
        this.mkdirs = new TreeSet<>();
        this.rmdirs = new TreeSet<>();
    }

    public Map<TreeDiff.ChangeType, Set<TreeDiff.Change>> changes() {
        return changes;
    }

    public Set<Path> rmdirs() {
        return rmdirs;
    }
    public Set<Path> mkdirs() {
        return mkdirs;
    }

    public void applyChanges() {
        planChange();
        updateWorkspace();
    }

    private void updateWorkspace() {
        repo.ws().applyMigration(this);
    }

    private void planChange() {
        for(var change : diff.changes().entrySet()) {
            recordChange(change.getKey(), change.getValue());
        }
    }

    private void recordChange(Path path, TreeDiff.Change change) {
        final TreeDiff.ChangeType action;
        Stream<Path> parents = parents(path.getParent());
        if(change.type() == TreeDiff.ChangeType.Delete) {
            parents.collect(toCollection(() -> rmdirs));
        } else {
            parents.collect(toCollection(() -> mkdirs));
        }
        changes.get(change.type()).add(change);
    }

    private Stream<Path> parents(Path path) {
        if(path.getParent() == null) {
            return Stream.of();
        }
        Stream<Path> stream = Stream.of(path);
        return Stream.concat(stream, parents(path.getParent()));
    }

    public Blob blobData(Optional<Oid> oid) {
        if(oid.isEmpty()) {
            throw new RuntimeException("Missing oid");
        }
        DbObject object = this.repo.db().read(oid.get());
        if(object instanceof Blob b) {
            return b;
        }
        throw new RuntimeException("Oid referenced a "+object.type()+" instead of a blob");
    }
}
