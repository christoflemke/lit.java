package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import lemke.christof.lit.diff.Diff;
import lemke.christof.lit.diff.Edit;
import lemke.christof.lit.diff.Hunk;
import lemke.christof.lit.database.Blob;
import lemke.christof.lit.database.Oid;
import lemke.christof.lit.status.Status;

import java.nio.file.Path;
import java.util.List;

public class DiffCommand implements Command {

    private record Target(Path path, Oid oid, FileMode mode, String data) {
        String diffPath(String a) {
            return mode == null ? NULL_PATH : Path.of(a).resolve(path).toString();
        }
    }

    private static final Oid NULL_OID = Oid.of(Util.repeat("0", 40));
    private static final String NULL_PATH = "/dev/null";
    private final Repository repo;
    private final Status status;
    private final Index idx;
    private final boolean useColor;

    public DiffCommand(Repository repo, boolean useColor) {
        this.repo = repo;
        status = repo.status();
        idx = repo.createIndex();
        idx.load();
        this.useColor = useColor;
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
        Oid oid = blob.oid();
        FileMode mode = repo.ws().stat(path).modeString();
        return new Target(path, oid, mode, data);
    }

    private Target fromIndex(Path path) {
        Index.Entry entry = idx.get(path).get();
        Oid oid = entry.oid();
        FileMode mode = entry.stat().modeString();
        Blob blob = (Blob) repo.db().read(oid);
        return new Target(path, oid, mode, blob.stringData());
    }

    private Target fromNothing(Path path) {
        return new Target(path, NULL_OID, null, "");
    }

    private void printDiff(Target a, Target b) {
        Path aPath = Path.of("a").resolve(a.path);
        Path bPath = Path.of("b").resolve(b.path);
        printHeader("diff --git " + aPath + " " + bPath);
        printDiffMode(a, b);
        printDiffContent(a, b);
    }

    private void printDiffMode(Target a, Target b) {
        if (a.mode == null) {
            printHeader("new file mode " + b.mode);
        } else if (b.mode == null) {
            printHeader("deleted file mode " + a.mode);
        } else if (!a.mode.equals(b.mode)) {
            printHeader("old mode " + a.mode);
            printHeader("new mode " + b.mode);
        }
    }

    private void printDiffContent(Target a, Target b) {
        if (a.oid.equals(b.oid)) {
            return;
        }
        String oidRange = "index " + a.oid.shortOid() + ".." + b.oid.shortOid();
        if (a.mode != null && a.mode.equals(b.mode)) {
            oidRange += " " + a.mode;
        }
        printHeader(oidRange);
        printHeader("--- " + a.diffPath("a"));
        printHeader("+++ " + b.diffPath("b"));

        List<Hunk> hunks = Diff.diffHunks(a.data, b.data);
        hunks.forEach(this::printDiffHunk);
    }

    private void printDiffHunk(Hunk hunk) {
        printColor(hunk.header(), Color.CYAN);
        hunk.edits().forEach(this::printDiffEdit);
    }

    private void printDiffEdit(Edit e) {
        String text = e.toString().stripTrailing();
        switch (e.sym()) {
            case EQL -> println(Color.justReset(text, useColor));
            case INS -> {
                // Gits output is a bit strange here, since it individually wraps the sign and the text in color codes.
                if (useColor) {
                    print(Color.GREEN.format(e.sym().toString(), true));
                    String line = e.aLine() == null ? e.bLine().text() : e.aLine().text();
                    println(Color.GREEN.format(line.stripTrailing(), true));
                } else {
                    println(text);
                }
            }
            case DEL -> println(Color.RED.format(text, useColor));
        }
    }

    private void printColor(String in, Color c) {
        println(c.format(in, useColor));
    }

    private void printHeader(String in) {
        println(Color.BOLD.format(in, useColor));
    }

    private void println(Object o) {
        repo.io().out().println(o);
    }

    private void print(Object o) {
        repo.io().out().print(o);
    }
}
