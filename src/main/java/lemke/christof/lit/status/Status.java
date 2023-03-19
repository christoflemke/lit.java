package lemke.christof.lit.status;

import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

public record Status(
    SortedSet<String> changed,
    SortedMap<String, ModifiedStatus> indexChanges,
    SortedMap<String, ModifiedStatus> workspaceChanges,
    SortedSet<String> untrackedFiles, Map<Path, lemke.christof.lit.Database.TreeEntry> headTree) {
}
