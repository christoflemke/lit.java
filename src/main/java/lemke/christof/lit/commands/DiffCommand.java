import lemke.christof.lit.*;
import lemke.christof.lit.diff.Diff;
import lemke.christof.lit.diff.Edit;
import lemke.christof.lit.diff.Hunk;
import java.util.List;
    private record Target(Path path, String oid, String mode, String data) {
        String diffPath(String a) {
            return mode == null ? NULL_PATH : Path.of(a).resolve(path).toString();
        Blob blob = (Blob) repo.db().read(object.oid());
        return new Target(path, object.oid(), object.mode(), blob.stringData());
        String data = repo.ws().readString(path);
        Blob blob = Blob.fromString(data);
        return new Target(path, oid, mode, data);
        Blob blob = (Blob) repo.db().read(oid);
        return new Target(path, oid, mode, blob.stringData());
        return new Target(path, NULL_OID, null, "");
        println("--- " + a.diffPath("a"));
        println("+++ " + b.diffPath("b"));

        List<Hunk> hunks = Diff.diffHunks(a.data, b.data);
        hunks.forEach(this::printDiffHunk);
    }

    private void printDiffHunk(Hunk hunk) {
        println(hunk.header());
        hunk.edits().forEach(this::print);

    private void print(Object o) {
        repo.io().out().print(o);
    }