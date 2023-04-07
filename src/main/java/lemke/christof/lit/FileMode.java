package lemke.christof.lit;

import java.util.Map;

public enum FileMode {
    EXECUTABLE("100755"),
    NORMAL("100644"),
    DIRECTORY("40000");

    private final String modeString;

    FileMode(String modeString) {
        this.modeString = modeString;
    }

    @Override public String toString() {
        return modeString;
    }

    private static Map<String, FileMode> mapping = Map.of(
        NORMAL.modeString, NORMAL,
        DIRECTORY.modeString, DIRECTORY,
        EXECUTABLE.modeString, EXECUTABLE
    );
    public static FileMode fromString(String s) {
        FileMode fileMode = mapping.get(s);
        if( fileMode == null) {
            throw new RuntimeException("Invalid file mode: "+s);
        }
        return fileMode;
    }
}
