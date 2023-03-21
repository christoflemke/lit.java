package lemke.christof.lit.diff;

public enum EditSymbol {
    EQL(" "),
    INS("+"),
    DEL("-");

    final String text;

    EditSymbol(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
