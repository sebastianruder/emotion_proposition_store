import edu.stanford.nlp.util.ArrayMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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

        PrintWriter statWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

        Map<Pattern, Integer> statMap = new HashMap<Pattern, Integer>();
        for (Pattern pattern : resultMap.keySet()) {
            statMap.put(pattern, resultMap.get(pattern).get(Enums.Stats.matches.toString()));
        }
        statMap = ExtensionMethods.sortByValue(statMap);
        Pattern patternCleaner = Pattern.compile("[a-z]+?(?=/)");

        for (Pattern pattern : statMap.keySet()) {
            if (pattern != null) {
                StringBuilder sb = new StringBuilder();
                Matcher m = patternCleaner.matcher(pattern.toString());
                while (m.find()) {
                    sb.append(m.group().toString());
                    sb.append(" ");
                }

                statWriter.printf("%s\t%s\t%d\n", sb.toString().trim(),
                        resultMap.get(pattern).get(Enums.Features.isNP.toString()) == 1 ? "NP" : "S",
                        resultMap.get(pattern).get(Enums.Stats.matches.toString()));
            }
        }

        statWriter.close();
    }
}
