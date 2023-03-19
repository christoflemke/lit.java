package lemke.christof.lit.diff;

import java.util.List;

public record Edit(Meyers.EditSymbol sym, Line aLine, Line bLine) {
    @Override
    public String toString() {
        Line line = aLine == null ? bLine : aLine;
        return sym + line.text();
    }
}
