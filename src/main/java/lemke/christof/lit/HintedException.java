package lemke.christof.lit;

import lemke.christof.lit.model.Commit;
import lemke.christof.lit.model.DbObject;
import lemke.christof.lit.model.Oid;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;

public class HintedException extends RuntimeException {
    private final List<String> hints;
    private final List<String> errors;
    private HintedException(String message, List<String> hints, List<String> errors) {
        super(message);
        this.hints = hints;
        this.errors = errors;
    }

    public static RuntimeException fromShaCandidates(Database db, List<Oid> candidates, String name) {
        var candidateMessages = candidates.stream().sorted(Oid.byName).map(oid -> {
            DbObject object = db.read(oid);
            String info = "  " + oid.shortOid() + " " + object.type();
            if (object instanceof Commit commit) {
                return info + " " + commit.author().shortDate() + " - " + commit.titleLine();
            } else {
                return info;
            }
        });
        String error = "short SHA1 " + name + " is ambiguous";
        String fatal = "Not a valid object name: '"+name+"'";
        var hint = concat(Stream.of("The candidates are:"), candidateMessages).toList();
        return new HintedException(fatal, hint, List.of(error));
    }

    public static RuntimeException fromErrors(String message, List<String> errors) {
        return new HintedException(message, List.of(), errors);
    }

    @Override public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for(var error : errors) {
            sb.append("error: ");
            sb.append(error);
            sb.append("\n");
        }
        for(var hint : hints) {
            sb.append("hint: ");
            sb.append(hint);
            sb.append("\n");
        }
        sb.append("fatal: ");
        sb.append(super.getMessage());
        return sb.toString();
    }
}
