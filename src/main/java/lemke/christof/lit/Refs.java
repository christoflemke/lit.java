package lemke.christof.lit;

import lemke.christof.lit.model.Oid;
import lemke.christof.lit.refs.RefAst;

import java.io.FileNotFoundException;
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

    public void updateHead(Oid ref) {
        try {
            Path tempFile = Files.createTempFile(gitPath(), "head-", null);
            Files.writeString(tempFile, ref.value());
            Files.move(tempFile, headPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    public Optional<Oid> readHead() {
        try {
            String ref = Files.readString(headPath());
            if (ref.startsWith("ref:")) {
                String[] split = ref.split(" ");
                String sha = Files.readString(gitPath().resolve(Path.of(split[1].trim())));
                return Optional.of(Oid.of(sha));
            }
            return Optional.of(Oid.of(ref));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            return path.flatMap(this::readRefFile);
        }
        List<Oid> candidates = db.prefixMatch(name);
        if(candidates.size() == 1) {
            return Optional.of(candidates.get(0));
        }
        return Optional.empty();
    }

    private Optional<Oid> readRefFile(Path path) {
        try {
            return Optional.of(Oid.of(Files.readString(path)));
        } catch (FileNotFoundException e) {
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
