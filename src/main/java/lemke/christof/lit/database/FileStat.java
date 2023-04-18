package lemke.christof.lit.database;

import lemke.christof.lit.FileMode;

import java.nio.file.Path;

public record FileStat(Path path, int ctime_sec, int ctime_nano, int mtime_sec, int mtime_nano, int dev,
                       int ino, int mode, int uid, int gid, int size) {
    public boolean isModified(FileStat stat) {
        return
            this.mode != stat.mode ||
            this.size != stat.size;
    }

    public FileMode modeString() {
        return FileMode.fromString(Integer.toOctalString(mode));
    }

    public boolean timesMatch(FileStat stat) {
        return
            this.ctime_sec == stat.ctime_sec &&
            this.ctime_nano == stat.ctime_nano &&
            this.mtime_sec == stat.mtime_nano &&
            this.mtime_nano == stat.mtime_nano;
    }
}
