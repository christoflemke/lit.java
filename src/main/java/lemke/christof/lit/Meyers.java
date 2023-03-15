package lemke.christof.lit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Meyers {
    private final String[] left;
    private final String[] right;
    private final int leftSize;
    private final int rightSize;
    private final int maxSize;

    public Meyers(String[] left, String[] right) {
        this.left = left;
        this.right = right;
        leftSize = left.length;
        rightSize = right.length;
        maxSize = Math.max(leftSize, rightSize);
    }

    public List<Edit> diff() {
        List<Move> backtrack = backtrack();
        List<Edit> diff = new ArrayList<>();
        for(var move : backtrack) {
            System.out.println(move);
            String aLine = move.prevX >= leftSize ? null : left[move.prevX];
            String bLine = move.prevY >= rightSize ? null : right[move.prevY];
            if (move.x == move.prevX) {
                diff.add(new Edit(EditSymbol.INS, bLine));
            } else if (move.y == move.prevY) {
                diff.add(new Edit(EditSymbol.DEL, aLine));
            } else {
                diff.add(new Edit(EditSymbol.EQL, aLine));
            }
        }
        Collections.reverse(diff);
        return diff;
    }

    private void assignArray(Integer[] a, int i, int v) {
        if(i < 0) {
            a[a.length + i] = v;
        } else {
            a[i] = v;
        }
    }

    private Integer accessArray(Integer[] a, int i) {
        if(i < 0) {
            return a[a.length + i];
        } else {
            return a[i];
        }
    }

    public List<Integer[]> shortestEdit() {
        Integer[] v = new Integer[maxSize * 2 + 1];
        v[1] = 0;
        List<Integer[]> trace = new ArrayList<>();
        for(int d  = 0; d <= maxSize; d++) {
            trace.add(v.clone());
            for (int k = -d; k <= d; k += 2) {
                int x;
                Integer vkMinus = accessArray(v, k - 1);
                Integer vkPlus = accessArray(v, k + 1);
                if (k == -d || (k != d && vkMinus < vkPlus))
                {
                    x = vkPlus;
                } else {
                    x = vkMinus + 1;
                }
                int y = x - k;
                while (x < leftSize && y < rightSize && left[x].equals(right[y])) {
                    x = x + 1;
                    y = y + 1;
                }
                assignArray(v, k, x);
                //System.out.println("d: "+d+", k: "+k+", x: "+x+", y: "+y);
                if (x >= leftSize && y >= rightSize) {
                    return trace;
                }
            }
        }
        throw new RuntimeException("No solution found");
    }

    record Move(int prevX, int prevY, int x, int y) {
        @Override
        public String toString() {
            return "("+prevX+","+prevY+") -> ("+x+","+y+")";
        }
    }

    enum EditSymbol {
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

    record Edit(EditSymbol sym, String text) {
        @Override
        public String toString() {
            return sym + text;
        }
    }


    public List<Move> backtrack() {
        int x = leftSize;
        int y = rightSize;
        List<Integer[]> shortestEdit = shortestEdit();
        List<Move> moves = new ArrayList<>();
        for (int d = shortestEdit.size() - 1; d >= 0 ; d--) {
            Integer[] v = shortestEdit.get(d);
            int k = x - y;
            //System.out.println("d: "+d+", k: "+k);
            int prevK = 0;
            if ((k == -d) || (k != d && v[k - 1] < v[k + 1])) {
                prevK = k + 1;
            } else {
                prevK = k -1;
            }
            int prevX = v[prevK];
            int prevY = prevX - prevK;
            while (x > prevX && y > prevY) {
                moves.add(new Move(x - 1, y - 1, x, y));
                x--;y--;
            }
            if (d > 0) {
                moves.add(new Move(prevX, prevY, x, y));
            }
            x = prevX; y = prevY;
        }
        return moves;
    }
}
