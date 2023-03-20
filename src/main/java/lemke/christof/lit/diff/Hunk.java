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
        int editStart = -1;
        // group by edit positions
        for(int i = 0; i < edits.size(); i++) {
            Edit edit = edits.get(i);
            if(edit.sym() == Meyers.EditSymbol.EQL) {
                if(editStart >= 0) {
                    editPositions.add(range(editStart, i, edits.size()));
                    editStart = -1;
                }
                continue;
            }
            if (editStart == -1) {
                editStart = i;
            }
        }
        if (editStart != -1) {
            editPositions.add(range(editStart, edits.size(), edits.size()));
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
            aLine == null ? -1 : aLine.number(),
            bLine == null ? -1 : bLine.number(),
            hunkEdits
        );
    }

//    public static List<Hunk> filter(List<Edit> edits) {
//        List<Hunk> hunks = new ArrayList<>();
//        int offset = 0;
//
//        while (true) {
//            // Skip EQL edits
//            while (offset < edits.size() && edits.get(offset).sym() == Meyers.EditSymbol.EQL) {
//                offset++;
//            }
//            // return if at the end
//            if (offset >= edits.size()) {
//                return hunks;
//            }
//
//            // reverse a few steps to include the context
//            offset -= HUNK_CONTEXT + 1;
//
//            // get the line numbers
//            int aStart = offset < 0 ? 0 : edits.get(offset).aLine().number();
//            int bStart = offset < 0 ? 0 : edits.get(offset).bLine().number();
//
//            hunks.add(new Hunk(aStart, bStart, new ArrayList<>()));
//            offset = Hunk.build(new Hunk(aStart, bStart, new ArrayList<>()), edits, offset);
//        }
//    }
//
//    private static int build(Hunk hunk, List<Edit> edits, int offset) {
//        int counter = -1;
//
//        while (counter != 0) {
//            // add current edit
//            if (offset >= 0 && counter > 0 && offset < edits.size()) {
//                hunk.edits.add(edits.get(offset));
//            }
//            // scan
//            offset++;
//            // break if we reached the end
//            if (offset > edits.size()) {
//                break;
//            }
//
//            /*
//            Before we hit the first INS/DEL, counter will just count down.
//            While we are scanning through the INS/DELs, it will reset to 2*HUNK_CONTEXT+1.
//            Once we are past the INS/DELs, we will start to count down to 0
//             */
//            int editPosition = offset + HUNK_CONTEXT;
//            Meyers.EditSymbol sym = editPosition >= edits.size() ? null : edits.get(editPosition).sym();
//            if (sym == Meyers.EditSymbol.INS || sym == Meyers.EditSymbol.DEL) {
//                counter = 2 * HUNK_CONTEXT + 1;
//            } else {
//                counter--;
//            }
//        }
//
//        return offset;
//    }

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

        int start = lines.stream().findFirst().map(Line::number).orElse(def);
        return start + "," + lines.size();
    }
}
