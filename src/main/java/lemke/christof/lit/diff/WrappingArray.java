package lemke.christof.lit.diff;

class WrappingArray implements Cloneable {
    private final int[] a;

    WrappingArray(int size) {
        this.a = new int[size];
    }

    WrappingArray(int[] a) {
        this.a = a;
    }

    int get(int i) {
        return i < 0 ? a[a.length + i] : a[i];
    }

    void set(int i, int v) {
        if (i < 0) {
            a[a.length + i] = v;
        } else {
            a[i] = v;
        }
    }

    @Override
    protected WrappingArray clone() {
        return new WrappingArray(this.a.clone());
    }
}
