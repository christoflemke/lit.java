package lemke.christof.lit;

import lemke.christof.lit.database.DbObject;
import lemke.christof.lit.database.Oid;
import lemke.christof.lit.refs.RefAst;
import lemke.christof.lit.refs.Revision;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

public record Refs(Path root, Database db) {
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

    /**
     * Read HEAD
     * No Recursion
     */
    public Optional<Ref> head() {
        return readRef(headPath());
    }

    public Optional<Oid> resolveHead() {
        Optional<Ref> headRef = head();
        return headRef.flatMap(this::resolveRef);
    }

    /**
     * Read ref.
     * No Recursion
     */
    private Optional<Ref> readRef(Path path) {
        final String s;
        try {
            s = Files.readString(gitPath().resolve(path));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (s.startsWith("ref: ")) {
            String[] split = s.split(" ");
            return Optional.of(new SymRef(Path.of(split[1].trim())));
        } else {
            return Optional.of(new OidRef(Oid.of(s.trim())));
        }
    }

    /**
     * Set HEAD to ref.
     * No recursion
     */
    public void setHead(Ref ref) {
        try {
            Files.writeString(headPath(), ref.toString()+"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update HEAD.
     * Set oid directly if HEAD currently points at an Oid,
     * or resolve head to a branch, and then update the branch with the oid.
     */
    public void updateHead(Oid oid) {
        Optional<Ref> head = head();
        if(head.isEmpty()) {
            setHead(new OidRef(oid));
            return;
        }
        if (head.get() instanceof Ref) {
            setHead(new OidRef(oid));
        } else if (head.get() instanceof SymRef symRef) {
            SymRef branchRef = resolveToSymRef(symRef);
            try {
                Files.writeString(gitPath().resolve(branchRef.path()), oid.toString()+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Optional<Oid> resolveBranchName(String name) {
        Optional<SymRef> ref = nameToRef(name);
        return ref.flatMap(r -> resolveRef(r));
    }

    /**
     * Resolve ref recursively to an oid
     */
    public Optional<Oid> resolveRef(Ref ref) {
        if (ref instanceof SymRef symRef) {
            Optional<Ref> next = readRef(symRef.path);
            if(next.isEmpty()) {
                return Optional.empty();
            }
            return resolveRef(next.get());
        } else if (ref instanceof OidRef oidRef) {
            return Optional.ofNullable(oidRef.oid());
        } else {
            throw new RuntimeException("Invalid ref: " + ref);
        }
    }

    /**
     * Find the last SymRef in the chain
     */
    private SymRef resolveToSymRef(SymRef ref) {
        Optional<Ref> next = readRef(ref.path);
        if (next.isPresent() && next.get() instanceof SymRef symRef) {
            return resolveToSymRef(symRef);
        } else {
            return ref;
        }
    }

    public Optional<SymRef> nameToRef(String name) {
        Optional<SymRef> symRef = pathForName(name).map(SymRef::new);
        return symRef;
    }

    private Optional<Path> pathForName(String name) {
        return List.of(gitPath(), refsPath(), headsPath()).stream()
            .map(prefix -> prefix.resolve(name))
            .filter(path -> path.toFile().exists())
            .map(path -> gitPath().relativize(path))
            .findFirst();
    }

    public void createBranch(String branchName, Oid startOid) {
        validateBranchName(branchName);
        try {
            Files.writeString(headsPath().resolve(branchName), startOid.toString()+"\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void validateBranchName(String branchName) {
        if (!RefAst.isBranchNameValid(branchName)) {
            throw new RuntimeException("Invalid branch name: " + branchName);
        }
    }

    /**
     * Target is anything you can check out
     */
    public Optional<Ref> lookupTarget(String target) {
        Optional<SymRef> symRef = nameToRef(target);
        if(symRef.isPresent()) {
            return Optional.of(symRef.get());
        }
        Oid oid = new Revision(db, this, target).resolve(Optional.empty());
        return Optional.of(new OidRef(oid));
    }

    public sealed interface Ref permits OidRef, SymRef {
        String shortName();
    }

    public record OidRef(Oid oid) implements Ref {
        @Override public String toString() {
            return oid.toString();
        }

        @Override public String shortName() {
            return oid.shortOid();
        }
    }

    public static final class SymRef implements Ref {
        private final Path path;

        SymRef(Path path) {
            if(path.isAbsolute()) {
                throw new RuntimeException("Expected relative path, got: "+path);
            }
            this.path = path;
        }

        public Path path() {
            return path;
        }

        public boolean isHead() {
            return path.equals(Path.of("HEAD"));
        }

        @Override public String toString() {
            return "ref: "+path.toString();
        }

        @Override public String shortName() {
            return path.getFileName().toString();
        }
    }
}
