package lemke.christof.lit.diff;

public record Edit(EditSymbol sym, Line aLine, Line bLine) {
    @Override
    public String toString() {
        Line line = aLine == null ? bLine : aLine;
        return sym + line.text();
    }
}
