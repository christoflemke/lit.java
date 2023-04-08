package lemke.christof.lit.repository;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.database.Blob;
import lemke.christof.lit.database.DbObject;
import lemke.christof.lit.database.Oid;
import lemke.christof.lit.database.TreeDiff;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static lemke.christof.lit.database.TreeDiff.ChangeType.*;

public class Migration {
    private final TreeDiff diff;
    private final Repository repo;
    private final Map<TreeDiff.ChangeType, Set<TreeDiff.Change>> changes;
    private final Set<Path> mkdirs;
    private final Set<Path> rmdirs;
    private final Index index;

    public Migration(Repository repository, Index index, TreeDiff diff) {
        this.repo = repository;
        this.diff = diff;
        this.index = index;

        Comparator<TreeDiff.Change> comparing = Comparator.comparing(TreeDiff.Change::path);
        this.changes = Map.of(
            Create, new TreeSet<>(comparing),
            Delete, new TreeSet<>(comparing),
            Update, new TreeSet<>(comparing)
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
        updateIndex();
    }

    private void updateIndex() {
        for(var change: changes.get(Delete)) {
            index.remove(change.path());
        }
        for(var action : List.of(Create, Update)) {
            for(var change: changes.get(action)) {
                var path = change.path();
                index.add(path, change.oid().get());
            }
        }
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
        if(change.type() == Delete) {
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
