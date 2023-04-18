package lemke.christof.lit.repository;

import com.google.common.collect.Streams;
import lemke.christof.lit.Index;
import lemke.christof.lit.Inspector;
import lemke.christof.lit.Repository;
import lemke.christof.lit.database.*;
import lemke.christof.lit.status.ModifiedStatus;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
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
    private final Map<Conflicts, Set<Path>> conflicts;
    private final Inspector inspector;
    private static final Map<Conflicts, String> MESSAGE_HEADERS = Map.of(
        Conflicts.STALE_FILE, "Your local changes to the following files would be overwritten by checkout:",
        Conflicts.STALE_DIRECTORY, "Updating the following directories would lose untracked files in them:",
        Conflicts.UNTRACKED_OVERWRITTEN, "The following untracked working tree files would be overwritten by checkout:",
        Conflicts.UNTRACKED_REMOVED, "The following untracked working tree files would be removed by checkout:"
    );
    private static final Map<Conflicts, String> MESSAGE_FOOTERS = Map.of(
        Conflicts.STALE_FILE, "Please commit your changes or stash them before you switch branches.",
        Conflicts.STALE_DIRECTORY, "\n",
        Conflicts.UNTRACKED_OVERWRITTEN, "Please move or remove them before you switch branches.",
        Conflicts.UNTRACKED_REMOVED, "Please move or remove them before you switch branches."
    );

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
        this.conflicts = Map.of(
            Conflicts.STALE_DIRECTORY, new TreeSet<>(),
            Conflicts.STALE_FILE, new TreeSet<>(),
            Conflicts.UNTRACKED_REMOVED, new TreeSet<>(),
            Conflicts.UNTRACKED_OVERWRITTEN, new TreeSet<>()
        );
        this.inspector = new Inspector(repo, index);
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
        for(var e : diff.changes().entrySet()) {
            Path path = e.getKey();
            TreeDiff.Change entry = e.getValue();
            checkForConflict(path, entry);
            recordChange(path, entry);
        }

        collectErrors();
    }

    private void collectErrors() {
        List<String> errors = new ArrayList<>();
        for (var conflict : this.conflicts.entrySet()) {
            var type = conflict.getKey();
            var paths = conflict.getValue();
            if(paths.isEmpty()) {
                continue;
            }
            String error = Streams.concat(
                Stream.of("error: " + MESSAGE_HEADERS.get(type)),
                paths.stream().map(p -> "\t" + p),
                Stream.of(MESSAGE_FOOTERS.get(type))
            ).collect(Collectors.joining("\n"));
            errors.add(error);
        }
        if(!errors.isEmpty()) {
            throw new RuntimeException(errors.stream().collect(Collectors.joining("\n")) + "\nAborting");
        }
    }

    private void checkForConflict(Path path, TreeDiff.Change change) {
        Optional<Index.Entry> entry = this.index.get(path);

        if(indexDiffersFromTrees(entry, change)) {
            conflicts.get(Conflicts.STALE_FILE).add(path);
            return;
        }
        Optional<FileStat> stat = repo.ws().stat(path);
        Conflicts type = getErrorType(stat, entry, change.newItem());
        if(stat.isEmpty()) {
            Optional<Path> parent = untrackedParent(path);
            if(parent.isPresent()) {
                conflicts.get(type).add(entry.isPresent() ? path : parent.get());
            }
        } else if(repo.ws().isFile(stat.get().path())) {
            Optional<ModifiedStatus> modifiedStatus = inspector.compareIndexToWorkspace(path);
            if(modifiedStatus.isPresent()) {
                conflicts.get(type).add(path);
            }
        } else if(repo.ws().isDirectory(stat.get().path())) {
            boolean trackable = inspector.trackableFile(path);
            if(trackable) {
                conflicts.get(type).add(path);
            }
        }
    }

    private Optional<Path> untrackedParent(Path path) {
        if(path == null) {
            return Optional.empty();
        }
        Optional<FileStat> stat = repo.ws().stat(path);
        if(stat.isPresent() && repo.ws().isFile(stat.get().path())) {
            if(inspector.trackableFile(path)) {
                return Optional.of(path);
            }
        }
        return untrackedParent(path.getParent());
    }

    private Conflicts getErrorType(Optional<FileStat> stat, Optional<Index.Entry> entry, Optional<Entry> item) {
        if(entry.isPresent()) {
            return Conflicts.STALE_FILE;
        }
        if(stat.map(s -> repo.ws().isDirectory(s.path())).orElse(false)) {
            return Conflicts.STALE_DIRECTORY;
        }
        if(item.isPresent()) {
            return Conflicts.UNTRACKED_OVERWRITTEN;
        }
        return Conflicts.UNTRACKED_REMOVED;
    }

    private boolean indexDiffersFromTrees(Optional<Index.Entry> entry, TreeDiff.Change change) {
        return
            inspector.compareTreeToIndex(change.oldItem(), entry).isPresent() &&
            inspector.compareTreeToIndex(change.newItem(), entry).isPresent();
    }

    private void recordChange(Path path, TreeDiff.Change change) {
        final TreeDiff.ChangeType action;
        Stream<Path> parents = parents(path);
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

    private enum Conflicts {
        STALE_FILE, STALE_DIRECTORY, UNTRACKED_OVERWRITTEN, UNTRACKED_REMOVED
    }
}
