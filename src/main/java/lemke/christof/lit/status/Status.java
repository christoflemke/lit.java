package lemke.christof.lit.status;

import java.util.SortedMap;
import java.util.SortedSet;

public record Status(
    SortedSet<String> changed,
    SortedMap<String, ModifiedStatus> indexChanges,
    SortedMap<String, ModifiedStatus> workspaceChanges,
    SortedSet<String> untrackedFiles) {
}
