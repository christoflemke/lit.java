package lemke.christof.lit;

import lemke.christof.lit.database.DbObject;
import lemke.christof.lit.database.Oid;
import lemke.christof.lit.refs.Revision;
import lemke.christof.lit.refs.RefAst;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

public record Refs (Path root, Database db) {
    Path headPath() {
        return gitPath().resolve("HEAD");
    }

    private Path gitPath() {
        return root.resolve(".git");
    }

    private Path refsPath() {
        return gitPath().resolve("refs");
    }

    private Path headsPath() {
        return refsPath().resolve("heads");
    }

    public Oid resolveCommit(String name) {
        return new Revision(db, this, name).resolve(Optional.of(DbObject.ObjectType.COMMIT));
    }

    public void updateHead(Oid ref) {
        try {
            Path tempFile = Files.createTempFile(gitPath(), "head-", null);
            Files.writeString(tempFile, ref.value());
            Files.move(tempFile, headPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setHead(String target, Oid oid) {
        Path path = headsPath().resolve(target);

        if(path.toFile().isFile()) {
            Path relative = gitPath().relativize(path);
            updateRefFile(headPath(), "ref: "+relative);
        } else {
            updateRefFile(headPath(), oid.toString());
        }
    }

    private void updateRefFile(Path path, String refOrOid) {
        try {
            Files.writeString(path, refOrOid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Just returns the branch name HEAD is pointing to
     * @return A branch name or and Oid
     */
    public String readHeadBranch() {
        String ref;
        try {
            ref = Files.readString(headPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (ref.startsWith("ref:")) {
            String[] split = ref.split(" ");
            String[] path = split[1].split("/");
            return path[path.length - 1].trim();
        } else {
            return ref.trim();
        }
    }

    /**
     * Resolves HEAD to an Oid
     * @return An Oid if HEAD is valid
     */
    public Optional<Oid> readHead() {
        return readSymRef(headPath());
    }

    public void createBranch(String branchName, Oid startOid) {
        writeRef(branchName, startOid);
    }

    private void writeRef(String branchName, Oid ref) {
        try {
            validateBranchName(branchName);
            Path branchPath = gitPath().resolve("refs").resolve("heads").resolve(branchName);
            Files.createDirectories(branchPath.getParent());
            Path tmpPath = branchPath.getParent().resolve(branchName + ".tmp");
            Files.writeString(tmpPath, ref + "\n");
            try {
                Files.move(tmpPath, branchPath);
            }
            catch (FileAlreadyExistsException e) {
                throw new RuntimeException("Branch already exists: " + branchName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void validateBranchName(String branchName) {
        if (!RefAst.isBranchNameValid(branchName)) {
            throw new RuntimeException("Invalid branch name: " + branchName);
        }
    }

    public Optional<Oid> readRef(String name) {
        Optional<Path> path = pathForName(name);
        if (path.isPresent()) {
            return path.flatMap(this::readSymRef);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Oid> readSymRef(Path path) {
        try {
            String fromFile = Files.readString(path);
            if(fromFile.startsWith("ref: ")) {
                String[] split = fromFile.split(" ");
                Path relPath = Path.of(split[1].trim());
                return readSymRef(gitPath().resolve(relPath));
            }
            return Optional.of(Oid.of(fromFile));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<Path> pathForName(String name) {
        return List.of(gitPath(), refsPath(), headsPath()).stream()
            .map(prefix -> prefix.resolve(name))
            .filter(path -> path.toFile().exists())
            .findFirst();
    }
}
