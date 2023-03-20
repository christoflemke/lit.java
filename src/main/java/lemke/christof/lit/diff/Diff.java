package lemke.christof.lit.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Diff {
    public static List<Edit> diff(String a, String b) {
        return new Meyers(lines(a), lines(b)).diff();
    }

    /*
    This is a bit complicated since I have to mimic ruby's String:lines exactly (include \n in string)
     */
    private static List<Line> lines(String document) {
        String line = "";
        int lineNumber = 1;
        List<Line> result = new ArrayList<>();
        for(int i = 0; i < document.length(); i++) {
            String next = document.substring(i,i+1);
            line += next;
            if(next.equals("\n")) {
                result.add(new Line(lineNumber, line));
                lineNumber++;
                line = "";
            }
        }
        if(!"".equals(line)) {
            result.add(new Line(lineNumber, line));
        }
        return result;
    }

//    private static List<Line> lines(String document) {
//        String[] split = document.split("\n");
//        List<Line> result = new ArrayList<>();
//        for(int i = 0; i < split.length; i++) {
//            result.add(new Line(i + 1, split[i]));
//        }
//        return result;
//    }

    public static List<Hunk> diffHunks(String a, String b) {
        return Hunk.filter(diff(a, b));
    }

}
