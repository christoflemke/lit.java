package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.diff.Diff;
import lemke.christof.lit.diff.Edit;
import lemke.christof.lit.diff.Hunk;
import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.DbObject;
import lemke.christof.lit.status.Status;

import java.nio.file.Path;
import java.util.List;

public class DiffCommand implements Command {

    private record Target(Path path, String oid, String mode, String data) {
        String diffPath(String a) {
            return mode == null ? NULL_PATH : Path.of(a).resolve(path).toString();
        }
    }

    private static final String NULL_OID = Util.repeat("0", 40);
    private static final String NULL_PATH = "/dev/null";
    private final Repository repo;
    private final Status status;
    private final Index idx;

    public DiffCommand(Repository repo) {
        this.repo = repo;
        status = repo.status();
        idx = repo.createIndex();
        idx.load();
    }

    @Override
    public void run(String[] args) {
        if (args.length > 0 && args[0].equals("--cached")) {
            diffHeadIndex();
        } else {
            diffIndexWorkspace();
        }
    }

    private void diffHeadIndex() {
        for (var change : status.indexChanges().entrySet()) {
            Path path = Path.of(change.getKey());
            switch (change.getValue()) {
                case INDEX_ADDED -> printDiff(fromNothing(path), fromIndex(path));
                case INDEX_MODIFIED -> printDiff(fromHead(path), fromIndex(path));
                // TODO: INDEX_DELETED
            }
        }
    }

    private void diffIndexWorkspace() {
        for (var change : status.workspaceChanges().entrySet()) {
            Path path = Path.of(change.getKey());
            switch (change.getValue()) {
                case WORKSPACE_MODIFIED -> printDiff(fromIndex(path), fromFile(path));
                case WORKSPACE_DELETED -> printDiff(fromIndex(path), fromNothing(path));
            }
        }
    }

    private Target fromHead(Path path) {
        Database.TreeEntry object = status.headTree().get(path);
        Blob blob = (Blob) repo.db().read(object.oid());
        return new Target(path, object.oid(), object.mode(), blob.stringData());
    }

    private Target fromFile(Path path) {
        String data = repo.ws().readString(path);
        Blob blob = Blob.fromString(data);
        String oid = blob.oid();
        String mode = repo.ws().stat(path).modeString();
        return new Target(path, oid, mode, data);
    }

    private Target fromIndex(Path path) {
        Index.Entry entry = idx.get(path).get();
        String oid = entry.oid();
        String mode = entry.stat().modeString();
        Blob blob = (Blob) repo.db().read(oid);
        return new Target(path, oid, mode, blob.stringData());
    }

    private Target fromNothing(Path path) {
        return new Target(path, NULL_OID, null, "");
    }

    private void printDiff(Target a, Target b) {
        Path aPath = Path.of("a").resolve(a.path);
        Path bPath = Path.of("b").resolve(b.path);
        println("diff --git " + aPath + " " + bPath);
        printDiffMode(a, b);
        printDiffContent(a, b);
    }

    private void printDiffMode(Target a, Target b) {
        if (a.mode == null) {
            println("new file mode " + b.mode);
        } else if (b.mode == null) {
            println("deleted file mode " + a.mode);
        } else if (!a.mode.equals(b.mode)) {
            println("old mode " + a.mode);
            println("new mode " + b.mode);
        }
    }

    private void printDiffContent(Target a, Target b) {
        if (a.oid.equals(b.oid)) {
            return;
        }
        String oidRange = "index " + shorten(a.oid) + ".." + shorten(b.oid);
        if (a.mode != null && a.mode.equals(b.mode)) {
            oidRange += " " + a.mode;
        }
        println(oidRange);
        println("--- " + a.diffPath("a"));
        println("+++ " + b.diffPath("b"));

        List<Hunk> hunks = Diff.diffHunks(a.data, b.data);
        hunks.forEach(this::printDiffHunk);
    }

    private void printDiffHunk(Hunk hunk) {
        println(hunk.header());
        hunk.edits().forEach(this::print);
    }

    private String shorten(String aOid) {
        return aOid.substring(0, 7);
    }

    private void println(Object o) {
        repo.io().out().println(o);
    }

    private void print(Object o) {
        repo.io().out().print(o);
    }
}
