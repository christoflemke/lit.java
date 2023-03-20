package lemke.christof.lit.diff;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public record Hunk(int aStart, int bStart, List<Edit> edits) {
    private static int HUNK_CONTEXT = 3;

    public static List<Hunk> filter(List<Edit> edits) {
        RangeSet<Integer> editPositions = TreeRangeSet.create();
        Range<Integer> current = null;
        // group by edit positions
        for(int i = 0; i < edits.size(); i++) {
            Edit edit = edits.get(i);
            if(edit.sym() == Meyers.EditSymbol.EQL) {
                if(current != null) {
                    editPositions.add(range(current.lowerEndpoint(), i, edits.size()));
                    current = null;
                }
                continue;
            }
            if (current == null) {
                current = Range.atLeast(i);
            }
        }
        if (current != null) {
            editPositions.add(range(current.lowerEndpoint(), edits.size(), edits.size()));
        }

        List<Hunk> hunks = new ArrayList<>();
        for(var positions : editPositions.asRanges()) {
            hunks.add(toHunk(edits, positions.lowerEndpoint(), positions.upperEndpoint()));
        }

        return hunks;
    }

    private static Range<Integer> range(int editStart, int editEnd, int max) {
        editStart = Math.max(editStart - HUNK_CONTEXT, 0);
        editEnd = Math.min(editEnd + HUNK_CONTEXT, max);
        return Range.closed(editStart, editEnd);
    }


    private static Hunk toHunk(List<Edit> edits, int start, int end) {
        List<Edit> hunkEdits = edits.subList(start, end);
        Line aLine = hunkEdits.get(0).aLine();
        Line bLine = hunkEdits.get(0).bLine();
        return new Hunk(
            aLine == null ? 0 : aLine.number(),
            bLine == null ? 0 : bLine.number(),
            hunkEdits
        );
    }

    public String header() {
        String aOffset = offsetFor(Edit::aLine, aStart);
        String bOffset = offsetFor(Edit::bLine, bStart);

        return "@@ -" + aOffset + " +" + bOffset + " @@";
    }

    private String offsetFor(Function<Edit, Line> lineSelector, int def) {
        List<Line> lines = edits.stream()
            .map(lineSelector)
            .filter(Objects::nonNull)
            .toList();
        if(lines.isEmpty()) {
            return "0,0";
        }
        int start = lines.stream().findFirst().map(Line::number).orElse(def);
        int end = lines.size();
        if (start == end) {
            return Integer.toString(start);
        }
        return start + "," + end;
    }
}
