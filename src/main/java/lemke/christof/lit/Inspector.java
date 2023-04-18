package lemke.christof.lit;

import lemke.christof.lit.database.Blob;
import lemke.christof.lit.database.Entry;
import lemke.christof.lit.database.FileStat;
import lemke.christof.lit.status.ModifiedStatus;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class Inspector {
    private final Repository repo;
    private final Index index;

    public Inspector(Repository repo, Index index) {
        this.repo = repo;
        this.index = index;
    }

    public boolean trackableFile(Path path) {
        Optional<FileStat> stat = repo.ws().stat(path);
        if (stat.isEmpty()) {
            return false;
        }
        FileStat fileStat = stat.get();
        if(repo.ws().isFile(fileStat.path())) {
            return index.isTracked(path);
        }
        if(!repo.ws().isDirectory(fileStat.path())) {
            return false;
        }
        Stream<Path> list = repo.ws().list(path);
        return list.anyMatch(p -> trackableFile(p));
    }

    public Optional<ModifiedStatus> compareIndexToWorkspace(Path path) {
        Optional<Index.Entry> entry = index.get(path);
        Optional<FileStat> stat = repo.ws().stat(path);
        if(entry.isEmpty()) {
            return Optional.of(ModifiedStatus.UNTRACKED);
        }
        if(stat.isEmpty()) {
            return Optional.of(ModifiedStatus.WORKSPACE_DELETED);
        }
        FileStat wsStat = stat.get();
        Index.Entry idxEntry = entry.get();
        if(wsStat.isModified(idxEntry.stat())) {
            return Optional.of(ModifiedStatus.INDEX_MODIFIED);
        }
        if(wsStat.timesMatch(idxEntry.stat())) {
            return Optional.empty();
        }
        Blob blob = repo.ws().read(path).get();
        if(blob.oid().equals(idxEntry.oid())) {
            return Optional.empty();
        }
        return Optional.of(ModifiedStatus.INDEX_MODIFIED);
    }

    public Optional<ModifiedStatus> compareTreeToIndex(Optional<Entry> item, Optional<Index.Entry> entry) {
        if(item.isEmpty() && entry.isEmpty()) {
            return Optional.empty();
        }
        if(item.isEmpty()) {
            return Optional.of(ModifiedStatus.INDEX_ADDED);
        }
        if(entry.isEmpty()) {
            return Optional.of(ModifiedStatus.INDEX_DELETED);
        }
        Index.Entry idxEntry = entry.get();
        Entry treeItem = item.get();
        if(!treeItem.mode().equals(idxEntry.stat().modeString())) {
            return Optional.of(ModifiedStatus.INDEX_MODIFIED);
        }
        if(!treeItem.oid().equals(idxEntry.oid())) {
            return Optional.of(ModifiedStatus.INDEX_MODIFIED);
        }
        return Optional.empty();
    }
}
