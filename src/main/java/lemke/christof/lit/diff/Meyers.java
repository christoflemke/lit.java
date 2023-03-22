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
        maxSize = leftSize + rightSize;
    }

    public List<Edit> diff() {
        if (leftSize == 0 && rightSize == 0) {
            return List.of();
        }
        List<Move> backtrack = backtrack();
        List<Edit> diff = new ArrayList<>();
        for (var move : backtrack) {
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

    public List<WrappingArray> shortestEdit() {
        WrappingArray v = new WrappingArray(maxSize * 2 + 1);
        v.set(1, 0);
        List<WrappingArray> trace = new ArrayList<>();
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
                int x;
                if (k == -depth || (k != depth && v.get(k -1) < v.get(k + 1))) {
                    x = v.get(k + 1);// down
                } else {
                    x = v.get(k - 1) + 1; // right
                }
                int y = x - k;
                while (x < leftSize && y < rightSize && left.get(x).text().equals(right.get(y).text())) {
                    x = x + 1;
                    y = y + 1;
                }
                v.set(k, x);
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
        List<WrappingArray> shortestEdit = shortestEdit();
        List<Move> moves = new ArrayList<>();
        for (int d = shortestEdit.size() - 1; d >= 0; d--) {
            WrappingArray v = shortestEdit.get(d);
            int k = x - y;
            int prevK = 0;
            if ((k == -d) || (k != d && v.get(k - 1) < v.get(k + 1))) {
                prevK = k + 1;
            } else {
                prevK = k - 1;
            }
            int prevX = v.get(prevK);
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
