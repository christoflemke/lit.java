package lemke.christof.lit.database;

import lemke.christof.lit.FileMode;

public record FileStat(int ctime_sec, int ctime_nano, int mtime_sec, int mtime_nano, int dev,
                       int ino, int mode, int uid, int gid, int size) {
    public boolean checkModified(FileStat stat) {
        return
            this.mode == stat.mode &&
            this.size == stat.size;
    }

    public FileMode modeString() {
        return FileMode.fromString(Integer.toOctalString(mode));
    }
}
