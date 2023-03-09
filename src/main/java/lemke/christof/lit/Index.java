package lemke.christof.lit;

import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.FileStat;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoField;
import java.util.*;

public class Index {

    private final Path tmpFile;
    private final Path indexPath;
    private final Set<Entry> entries = new TreeSet<>(Comparator.comparing(o -> o.path));
    private final Path lockPath;
    private final Workspace ws;

    Index(Workspace ws) {
        this.ws = ws;
        this.indexPath = ws.resolve(".git").resolve("index");
        this.lockPath = ws.resolve(".git").resolve("index.lock");
        try {
            this.tmpFile = Files.createTempFile("index-", null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Entry> entries() {
        return entries;
    }

    public FileLock tryLock() throws IOException {
        return new RandomAccessFile(lockPath.toFile(), "rw").getChannel().tryLock();
    }

    public void unlock(FileLock lock) throws IOException {
        if (lock != null) {
            lock.release();
            Files.delete(lockPath);
        }
    }


    private DataInputStream openStream(MessageDigest digest) throws FileNotFoundException {
        InputStream in = new FileInputStream(indexPath.toFile());
        DigestInputStream digestInputStream = new DigestInputStream(in, digest);
        return new DataInputStream(digestInputStream);
    }

    public void load() {
        MessageDigest digest = createSha1();
        try (DataInputStream dataInputStream = openStream(digest)) {
            int entryCount = readHeader(dataInputStream);

            readEntries(dataInputStream, entryCount);

            String sum = HexFormat.of().formatHex(digest.digest());
            byte[] sumBytes = dataInputStream.readNBytes(20);
            String indexSum = HexFormat.of().formatHex(sumBytes);
            if (!sum.equals(indexSum)) {
                throw new RuntimeException("Index checksum mismatch. Computed: " + sum + " read: " + indexSum);
            }
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readEntries(DataInputStream in, int entryCount) throws IOException {
        for (int i = 0; i < entryCount; i++) {
            int ctime_sec = in.readInt();
            int ctime_nano = in.readInt();
            int mtime_sec = in.readInt();
            int mtime_nano = in.readInt();
            int dev = in.readInt();
            int ino = in.readInt();
            int mode = in.readInt();
            int uid = in.readInt();
            int gid = in.readInt();
            int fileSize = in.readInt();
            byte[] oidBytes = in.readNBytes(20);
            short flags = in.readShort();
            byte[] pathBytes = in.readNBytes(flags);

            int paddingLength = 9 - ((7 + pathBytes.length) % 8);

            byte[] padding = in.readNBytes(paddingLength);
            Arrays.sort(padding);
            if (padding[padding.length - 1] != 0) {
                throw new RuntimeException("padding should be all 0s");
            }

            FileStat stat = new FileStat(ctime_sec,
                    ctime_nano,
                    mtime_sec,
                    mtime_nano,
                    dev,
                    ino,
                    mode,
                    uid,
                    gid,
                    fileSize);

            Entry e = new Entry(
                    Path.of(new String(pathBytes, StandardCharsets.UTF_8)),
                    HexFormat.of().formatHex(oidBytes),
                    stat

            );
            entries.add(e);
        }
    }

    private int readHeader(DataInputStream in) throws IOException {
        String sig = new String(in.readNBytes(4));
        if (!sig.equals("DIRC")) {
            throw new RuntimeException("Invalid signature: " + sig);
        }
        int version = in.readInt();
        if (version != 2) {
            throw new RuntimeException("Unsupported index version: " + version);
        }
        return in.readInt();
    }

    public Blob add(Path path) {
        Blob blob = new Blob(ws.read(path));
        Entry entry = createEntry(path, blob.oid());
        entries.add(entry);
        return blob;
    }

    public Optional<Entry> get(Path path) {
        if (path.isAbsolute()) {
            throw new RuntimeException("Path is absolute: " + path);
        }
        return entries.stream().filter(e -> e.path.equals(path)).findFirst();
    }

    public boolean contains(Path path) {
        return get(path).isPresent();
    }

    public void commit() {
        final MessageDigest sha1 = createSha1();
        try (DigestOutputStream stream = new DigestOutputStream(new FileOutputStream(tmpFile.toFile()), sha1)) {

            byte[] headerBytes = new byte[12];
            ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
            headerBuffer.put("DIRC".getBytes(StandardCharsets.UTF_8));
            headerBuffer.putInt(2);
            headerBuffer.putInt(entries.size());
            stream.write(headerBytes);

            for (Entry e : entries) {
                stream.write(e.data());
            }
            stream.flush();
            byte[] digest = stream.getMessageDigest().digest();
            stream.write(digest);
            Files.move(tmpFile, indexPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageDigest createSha1() {
        final MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return sha1;
    }

    Entry createEntry(Path path, String oid) {
        FileStat stat = ws.stat(path);
        return new Entry(
            path,
            oid,
            stat
        );
    }

    public record Entry(Path path, String oid, FileStat stat) {

        public short flags() {
            return (short) Math.min(path.toString().length(), 0xfff);
        }

        public byte[] oidBytes() {
            return HexFormat.of().parseHex(oid());
        }

        public byte[] pathBytes() {
            return path.toString().getBytes(StandardCharsets.UTF_8);
        }

        public byte[] data() {
            int length = 63 + pathBytes().length;
            length += 8 - (length % 8);
            byte[] bytes = new byte[length];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.putInt(stat.ctime_sec());
            buffer.putInt(stat.ctime_nano());
            buffer.putInt(stat.mtime_sec());
            buffer.putInt(stat.mtime_nano());
            buffer.putInt(stat.dev());
            buffer.putInt(stat.ino());
            buffer.putInt(stat.mode());
            buffer.putInt(stat.uid());
            buffer.putInt(stat.gid());
            buffer.putInt(stat.size());
            buffer.put(oidBytes());
            buffer.putShort(flags());
            buffer.put(pathBytes());
            buffer.put((byte) 0);
            return bytes;
        }
    }
}
