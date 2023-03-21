package lemke.christof.lit.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Meyers {
    private final List<Line> left;
    private final List<Line> right;
    private final int leftSize;
    private final int rightSize;
    private final int maxSize;

    public Meyers(List<Line> left, List<Line> right) {
        this.left = Collections.unmodifiableList(left);
        this.right = Collections.unmodifiableList(right);
        leftSize = left.size();
        rightSize = right.size();
        maxSize = Math.max(leftSize, rightSize);
    }

    public List<Edit> diff() {
        if (leftSize == 0 && rightSize == 0) {
            return List.of();
        }
        List<Move> backtrack = backtrack();
        List<Edit> diff = new ArrayList<>();
        for (var move : backtrack) {
            System.out.println(move);
            Line aLine = move.prevX >= leftSize ? null : left.get(move.prevX);
            Line bLine = move.prevY >= rightSize ? null : right.get(move.prevY);
            if (move.x == move.prevX) {
                diff.add(new Edit(EditSymbol.INS, null, bLine));
            } else if (move.y == move.prevY) {
                diff.add(new Edit(EditSymbol.DEL, aLine, null));
            } else {
                diff.add(new Edit(EditSymbol.EQL, aLine, bLine));
            }
        }
        Collections.reverse(diff);
        return diff;
    }

    private void assignArray(Integer[] a, int i, int v) {
        if (i < 0) {
            a[a.length + i] = v;
        } else {
            a[i] = v;
        }
    }

    private Integer accessArray(Integer[] a, int i) {
        if (i < 0) {
            return a[a.length + i];
        } else {
            return a[i];
        }
    }

    public List<Integer[]> shortestEdit() {
        Integer[] v = new Integer[maxSize * 2 + 1];
        v[1] = 0;
        List<Integer[]> trace = new ArrayList<>();
        /*
            The depth is the depth in the search tree
         */
        for (int depth = 0; depth <= maxSize; depth++) {
            trace.add(v.clone());
            /*
             * k = x - y
             * where x is the position on the horizontal axis
             * and y is the position on the vertical axis
             */
            for (int k = -depth; k <= depth; k += 2) {
                Integer vkMinus = accessArray(v, k - 1);
                Integer vkPlus = accessArray(v, k + 1);
                int x;
                if (k == -depth || (k != depth && vkMinus < vkPlus)) {
                    x = vkPlus;
                } else {
                    x = vkMinus + 1;
                }
                int y = x - k;
                while (x < leftSize && y < rightSize && left.get(x).text().equals(right.get(y).text())) {
                    x = x + 1;
                    y = y + 1;
                }
                assignArray(v, k, x);
                //System.out.println("depth: "+depth+", k: "+k+", x: "+x+", y: "+y);
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
            return "(" + prevX + "," + prevY + ") -> (" + x + "," + y + ")";
        }
    }

    public List<Move> backtrack() {
        int x = leftSize;
        int y = rightSize;
        List<Integer[]> shortestEdit = shortestEdit();
        List<Move> moves = new ArrayList<>();
        for (int d = shortestEdit.size() - 1; d >= 0; d--) {
            Integer[] v = shortestEdit.get(d);
            int k = x - y;
            //System.out.println("d: "+d+", k: "+k);
            int prevK = 0;
            if ((k == -d) || (k != d && accessArray(v, k - 1) < accessArray(v, k + 1))) {
                prevK = k + 1;
            } else {
                prevK = k - 1;
            }
            int prevX = accessArray(v,prevK);
            int prevY = prevX - prevK;
            while (x > prevX && y > prevY) {
                moves.add(new Move(x - 1, y - 1, x, y));
                x--;
                y--;
            }
            if (d > 0) {
                moves.add(new Move(prevX, prevY, x, y));
            }
            x = prevX;
            y = prevY;
        }
        return moves;
    }
}
