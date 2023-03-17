package lemke.christof.lit.status;

import java.util.SortedMap;
import java.util.SortedSet;

public record Status(
    SortedSet<String> changed,
    SortedMap<String, StatusBuilder.ModifiedStatus> indexChanges,
    SortedMap<String, StatusBuilder.ModifiedStatus> workspaceChanges,
    SortedSet<String> untrackedFiles) {
}
