import edu.stanford.nlp.util.ArrayMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by sebastian on 28/03/15.
 */
public class Stats {

    public static Map<Pattern, Map<String, Integer>> createResultMap(
            Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap) {

        Map<Pattern, Map<String, Integer>> resultMap = new ArrayMap<Pattern, Map<String, Integer>>();
        for (String emotion: emotionMap.keySet()) {
            for (Pattern pattern : emotionMap.get(emotion).keySet()) {
                Map<String, Integer> integerMap = new ArrayMap<String, Integer>();
                integerMap.put(Enums.Stats.occurrences.toString(), 0);
                integerMap.put(Enums.Stats.matches.toString(), 0);
                integerMap.put(Enums.Features.isNP.toString(),
                        emotionMap.get(emotion).get(pattern).get(Enums.Features.isNP.toString()) ? 1 : 0);
                resultMap.put(pattern, integerMap);
            }
        }

        return resultMap;
    }

    /**
     * Write stats to a file.
     * @param resultMap the hashmap in which the results have been saved
     * @param fileName the name of the file to which should be written
     * @throws IOException
     */
    public static void writeStats(Map<Pattern, Map<String, Integer>> resultMap, String fileName) throws IOException {

        PrintWriter statWriter = new PrintWriter(new BufferedWriter(new FileWriter("stats.txt")));

        Map<Pattern, Integer> statMap = new HashMap<Pattern, Integer>();
        for (Pattern pattern : resultMap.keySet()) {
            statMap.put(pattern, resultMap.get(pattern).get(Enums.Stats.matches.toString()));
        }
        statMap = MapUtil.sortByValue(statMap);

        for (Pattern pattern : statMap.keySet()) {
            if (pattern != null) {
                statWriter.println(String.format("%s\t%s\t%d\t%d", pattern,
                        resultMap.get(pattern).get(Enums.Features.isNP.toString()) == 1 ? "NP" : "S",
                        resultMap.get(pattern).get(Enums.Stats.matches.toString()),
                        resultMap.get(pattern).get(Enums.Stats.occurrences.toString())));
            }
        }

        statWriter.close();
    }
}
