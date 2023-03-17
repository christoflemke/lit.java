package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.Util;
import lemke.christof.lit.model.Blob;
import lemke.christof.lit.status.Status;

import java.nio.file.Path;

public class DiffCommand implements Command {

    private record Target(Path path, String oid, String mode) {
        String diffPath() {
            return mode == null ? NULL_PATH : path.toString();
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
        for(var change : status.workspaceChanges().entrySet()) {
            Path path = Path.of(change.getKey());
            switch (change.getValue()) {
                case WORKSPACE_MODIFIED -> print_diff(fromIndex(path), fromFile(path));
                case WORKSPACE_DELETED -> print_diff(fromIndex(path), fromNothing(path));
            }
        }
    }

    private Target fromFile(Path path) {
        Blob blob = Blob.fromString(repo.ws().readString(path));
        String oid = blob.oid();
        String mode = repo.ws().stat(path).modeString();
        return new Target(path, oid, mode);
    }

    private Target fromIndex(Path path) {
        Index.Entry entry = idx.get(path).get();
        String oid = entry.oid();
        String mode = entry.stat().modeString();
        return new Target(path, oid, mode);
    }

    private Target fromNothing(Path path) {
        return new Target(path, NULL_OID, null);
    }

    private void print_diff(Target a, Target b) {
        Path aPath = Path.of("a").resolve(a.path);
        Path bPath = Path.of("b").resolve(b.path);
        println("diff --git "+aPath+" "+bPath);
        print_diff_mode(a, b);
        print_diff_content(a, b);
    }

    private void print_diff_mode(Target a, Target b) {
        if (b.mode == null) {
            println("deleted file mode "+a.mode);
        } else if (!a.mode.equals(b.mode)){
            println("old mode "+a.mode);
            println("new mode "+b.mode);
        }
    }

    private void print_diff_content(Target a, Target b) {
        if(a.oid.equals(b.oid)) {
            return;
        }
        String oidRange = "index "+shorten(a.oid) + ".." + shorten(b.oid);
        if (a.mode.equals(b.mode)) {
            oidRange += " " + a.mode;
        }
        println(oidRange);
        println("--- "+a.diffPath());
        println("+++ "+b.diffPath());
    }

    private String shorten(String aOid) {
        return aOid.substring(0, 6);
    }

    private void println(Object o) {
        repo.io().out().println(o);
    }
}
