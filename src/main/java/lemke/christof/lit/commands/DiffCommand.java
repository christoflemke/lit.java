package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.Util;
import lemke.christof.lit.model.Blob;
import lemke.christof.lit.status.Status;

import java.nio.file.Path;

public class DiffCommand implements Command {
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
            switch (change.getValue()) {
                case WORKSPACE_MODIFIED -> diffFileModified(change.getKey());
                case WORKSPACE_DELETED -> diffFileDeleted(change.getKey());
            }
        }
    }

    private void diffFileDeleted(String key) {
        Path path = Path.of(key);

        Index.Entry entry = idx.get(path).get();
        String aOid = entry.oid();
        String aMode = entry.stat().modeString();
        Path aPath = Path.of("a").resolve(key);

        String bOid = NULL_OID;
        Path bPath = Path.of("b").resolve(path);

        println("diff --git "+aPath+" "+bPath);
        println("deleted file mode "+aMode);
        println("index "+shorten(aOid)+".."+shorten(bOid));
        println("--- "+aPath);
        println("+++ "+NULL_PATH);
    }

    private void diffFileModified(String key) {
        Path path = Path.of(key);

        Index.Entry entry = idx.get(path).get();
        String aOid = entry.oid();
        String aMode = entry.stat().modeString();
        Path aPath = Path.of("a").resolve(key);

        Blob blob = Blob.fromString(repo.ws().readString(path));
        String bOid = blob.oid();
        String bMode = repo.ws().stat(path).modeString();
        Path bPath = Path.of("b").resolve(key);

        println("diff --git "+aPath+" "+bPath);
        if (!aMode.equals(bMode)) {
            println("old mode "+aMode);
            println("new mode "+bMode);
        }
        if(aOid.equals(bOid)) {
            return;
        }
        String oidRange = "index "+shorten(aOid) + ".." + shorten(bOid);
        if (aMode.equals(bMode)) {
            oidRange += " " + aMode;
        }
        println(oidRange);
        println("--- "+aPath);
        println("--- "+bPath);
    }

    private String shorten(String aOid) {
        return aOid.substring(0, 6);
    }

    private void println(Object o) {
        repo.io().out().println(o);
    }
}
