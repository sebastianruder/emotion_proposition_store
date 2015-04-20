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

    public static Map<Pattern, Map<String, String>> createResultMap(
            Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap) {

        Map<Pattern, Map<String, String>> resultMap = new ArrayMap<Pattern, Map<String, String>>();
        for (String emotion: emotionMap.keySet()) {
            for (Pattern pattern : emotionMap.get(emotion).keySet()) {
                Map<String, String> featureMap = new ArrayMap<String, String>();
                featureMap.put(Enums.Stats.matches.toString(), "0");
                featureMap.put(Enums.Features.isNP.toString(),
                        emotionMap.get(emotion).get(pattern).get(Enums.Features.isNP.toString()) ? "NP" : "S");
                featureMap.put(Enums.Stats.emotion.toString(), emotion);
                resultMap.put(pattern, featureMap);
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
    public static void writeStats(Map<Pattern, Map<String, String>> resultMap, String outDir) throws IOException {

        PrintWriter patternStatWriter = new PrintWriter(new BufferedWriter(new FileWriter(outDir + "pattern_stats.txt")));
        PrintWriter emotionStatWriter = new PrintWriter(new BufferedWriter(new FileWriter(outDir + "emotion_stats.txt")));

        // create map for keeping track of the stats concerning emotions
        Map<String, Map<Pattern, Integer>> emotionStatMap = new HashMap<String, Map<Pattern, Integer>>();
        for (Enum emotionEnum : Enums.Emotions.values()) {
            emotionStatMap.put(emotionEnum.toString(), new HashMap<Pattern, Integer>());
        }

        Map<Pattern, Integer> statMap = new HashMap<Pattern, Integer>();

        for (Pattern pattern : resultMap.keySet()) {
            int matches = Integer.parseInt(resultMap.get(pattern).get(Enums.Stats.matches.toString()));
            statMap.put(pattern, matches);
            emotionStatMap.get(resultMap.get(pattern).get(Enums.Stats.emotion.toString())).put(pattern, matches);
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

                String emotion = resultMap.get(pattern).get(Enums.Stats.emotion.toString());
                patternStatWriter.printf("%s\t%s\t%s\t%d\n", emotion, sb.toString().trim(),
                        resultMap.get(pattern).get(Enums.Features.isNP.toString()),
                        statMap.get(pattern));
            }
        }

        for (String emotion : emotionStatMap.keySet()) {
            Map<Pattern, Integer> sortedEmotionMap = ExtensionMethods.sortByValue(emotionStatMap.get(emotion));
            for (Pattern pattern : sortedEmotionMap.keySet()) {
                if (pattern != null) {
                    StringBuilder sb = new StringBuilder();
                    Matcher m = patternCleaner.matcher(pattern.toString());
                    while (m.find()) {
                        sb.append(m.group().toString());
                        sb.append(" ");
                    }

                    emotionStatWriter.printf("%s\t%s\t%s\t%d\n", emotion, sb.toString().trim(),
                            resultMap.get(pattern).get(Enums.Features.isNP.toString()),
                            sortedEmotionMap.get(pattern));
                }
            }
        }

        patternStatWriter.close();
        emotionStatWriter.close();
    }
}
