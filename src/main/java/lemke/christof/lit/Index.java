package lemke.christof.lit;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoField;
import java.util.*;

public class Index {

    private final Path tmpFile;
    private final Path indexPath;
    private final Set<Entry> entries = new TreeSet<>(Comparator.comparing(o -> o.path));
    private final Path root;

    public Index(Path root) {
        this.root = root;
        this.indexPath = root.resolve(".git").resolve("index");
        try {
            this.tmpFile = Files.createTempFile("index-", null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(Path path, String oid) {
        Entry entry = createEntry(path, oid);
        entries.add(entry);
    }

    public void commit() {
        final MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try (DigestOutputStream stream = new DigestOutputStream(new FileOutputStream(tmpFile.toFile()), sha1)) {

            byte[] headerBytes = new byte[12];
            ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
            headerBuffer.put("DIRC".getBytes(StandardCharsets.UTF_8));
            headerBuffer.putInt(2);
            headerBuffer.putInt(entries.size());
            stream.write(headerBytes);

            for( Entry e: entries) {
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

    Entry createEntry(Path path, String oid) {
        PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(root.resolve(path), PosixFileAttributeView.class);
        final PosixFileAttributes attributes;
        final Long inode;
        try {
            attributes = fileAttributeView.readAttributes();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(attributes);
        return new Entry(root, path, oid, attributes);
    }

    public record Entry(Path root, Path path, String oid, PosixFileAttributes attributes) {
        static int REGULAR_MODE = 0100644;
        static int EXECUTABLE_MODE = 0100755;

        public Path absolutePath() {
            return root.resolve(path);
        }
        public short flags() {
            return (short) Math.min(path.toString().length(), 0xfff);
        }

        public int mode() {
            return attributes.permissions().contains(PosixFilePermission.OWNER_EXECUTE) ? EXECUTABLE_MODE : REGULAR_MODE;
        }

        public Integer ino() throws IOException {
            return Math.toIntExact((Long) Files.getAttribute(absolutePath(), "unix:ino"));
        }

        public Integer dev() throws IOException {
            return Math.toIntExact((Long) Files.getAttribute(absolutePath(), "unix:dev"));
        }

        public Integer ctime_sec() throws IOException {
            return Math.toIntExact(attributes.creationTime().toMillis() / 1000);
        }

        public Integer ctime_nano() throws IOException {
            return attributes.creationTime().toInstant().get(ChronoField.NANO_OF_SECOND);
        }

        public Integer mtime_sec() {
            return Math.toIntExact(attributes.lastModifiedTime().toMillis() / 1000);
        }

        public Integer mtime_nano() {
            return attributes.lastModifiedTime().toInstant().get(ChronoField.NANO_OF_SECOND);
        }

        public Integer uid() throws IOException {
            return (Integer) Files.getAttribute(absolutePath(), "unix:uid");
        }

        public Integer gid() throws IOException {
            return (Integer) Files.getAttribute(absolutePath(), "unix:gid");
        }

        public Integer size() throws IOException {
            return Math.toIntExact(Math.min(attributes.size(), Integer.MAX_VALUE));
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
            try {
                buffer.putInt(ctime_sec());
                buffer.putInt(ctime_nano());
                buffer.putInt(mtime_sec());
                buffer.putInt(mtime_nano());
                buffer.putInt(dev());
                buffer.putInt(ino());
                buffer.putInt(mode());
                buffer.putInt(uid());
                buffer.putInt(gid());
                buffer.putInt(size());
                buffer.put(oidBytes());
                buffer.putShort(flags());
                buffer.put(pathBytes());
                buffer.put((byte) 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return bytes;
        }
    }
}
