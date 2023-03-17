package lemke.christof.lit.status;

public enum ModifiedStatus {
    STAGED("S", " "),
    UNTRACKED("?", " "),
    WORKSPACE_MODIFIED("M", "modified"),
    WORKSPACE_DELETED("D", "deleted"),
    INDEX_ADDED("A", "new file"),
    INDEX_MODIFIED("M", "modified"),
    MIXED("_", "_"),
    NO_STATUS(" ", " ");

    public final String shortStatus;
    public final String longStatus;

    ModifiedStatus(String shortStatus, String longStatus) {
        this.shortStatus = shortStatus;
        this.longStatus = longStatus;
    }
}
