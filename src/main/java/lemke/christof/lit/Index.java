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
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Supplier;

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

    Path indexPath() {
        return indexPath;
    }

    public Set<Entry> entries() {
        return entries;
    }

    private FileLock tryLock() throws IOException {
        return new RandomAccessFile(lockPath.toFile(), "rw").getChannel().tryLock();
    }

    private void unlock(FileLock lock) throws IOException {
        if (lock != null) {
            lock.release();
            Files.delete(lockPath);
        }
    }

    public <T> T withLock(Supplier<T> fn) {
        try (FileLock lock = tryLock()) {
            if (lock == null) {
                throw new RuntimeException("Failed to acquire index.lock");
            }
            try {
                return fn.get();
            } finally {
                unlock(lock);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream openStream() throws FileNotFoundException {
        return new FileInputStream(indexPath.toFile());
    }

    public void load() {

        try (InputStream in = openStream()) {
            byte[] indexBytes = in.readAllBytes();
            ByteBuffer indexBuffer = ByteBuffer.wrap(indexBytes);
            int entryCount = readHeader(indexBuffer);

            readEntries(indexBuffer, entryCount);

            skipExtensions(indexBuffer);

            String indexSum = readSum(indexBuffer);

            String sum = calculateSum(indexBuffer);


            if (!sum.equals(indexSum)) {
                throw new RuntimeException("Index checksum mismatch. Computed: " + sum + " read: " + indexSum);
            }
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String calculateSum(ByteBuffer indexBuffer) {
        indexBuffer.position(0);
        byte[] data = new byte[indexBuffer.limit() - 20];
        indexBuffer.get(data);
        MessageDigest digest = createSha1();
        digest.update(data);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String readSum(ByteBuffer indexBuffer) {
        byte[] sumBytes = new byte[20];
        indexBuffer.get(sumBytes);
        String indexSum = HexFormat.of().formatHex(sumBytes);
        return indexSum;
    }

    private void skipExtensions(ByteBuffer buffer) {
        while (true) {
            byte[] extHeader = new byte[4];
            buffer.get(extHeader);
            String extHeaderString = new String(extHeader);
            if (extHeaderString.matches("\\p{Upper}{4}")) {
                int size = buffer.getInt();
                buffer.position(buffer.position() + size);
            } else {
                buffer.position(buffer.position() - 4);
                break;
            }
        }
    }

    private void readEntries(ByteBuffer in, int entryCount) throws IOException {
        for (int i = 0; i < entryCount; i++) {
            int ctime_sec = in.getInt();
            int ctime_nano = in.getInt();
            int mtime_sec = in.getInt();
            int mtime_nano = in.getInt();
            int dev = in.getInt();
            int ino = in.getInt();
            int mode = in.getInt();
            int uid = in.getInt();
            int gid = in.getInt();
            int fileSize = in.getInt();
            byte[] oidBytes = new byte[20];
            in.get(oidBytes);
            short flags = in.getShort();
            byte[] pathBytes = new byte[flags];
            in.get(pathBytes);

            int paddingLength = calculatePadding(pathBytes.length);

            for (int p = 0; p < paddingLength; p++) {
                if (in.get() != (byte)0x00) {
                    throw new RuntimeException("padding should be all 0s ");
                }
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

    private int readHeader(ByteBuffer buffer) throws IOException {
        byte[] sigBytes = new byte[4];
        buffer.get(sigBytes);
        String sig = new String(sigBytes);
        if (!sig.equals("DIRC")) {
            throw new RuntimeException("Invalid signature: " + sig);
        }
        int version = buffer.getInt();
        if (version != 2) {
            throw new RuntimeException("Unsupported index version: " + version);
        }
        return buffer.getInt();
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

    public String hash(Path relativePath) {
        return new Blob(ws.read(relativePath)).oid();
    }
    public static int calculatePadding(int pathLength) {
        return 8 - ((62 + pathLength) % 8);
    }

    public void update(Path path, String oid, FileStat currentStat) {
        Entry e = new Entry(path, oid, currentStat);
        boolean removed = entries.remove(e);
        boolean added = entries.add(e);
        if(!removed || !added) {
            System.err.println("Index did not contain entry for: "+path);
        }
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
            int pathBytes = pathBytes().length;
            int length = 62 + pathBytes + calculatePadding(pathBytes);
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
            return bytes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Entry) {
                return ((Entry) obj).path.equals(this.path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }
    }
}
