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
 * Class to store and write emotion and pattern statistics.
 * Created by sebastian on 28/03/15.
 */
public class Stats {

    /**
     * Creates a map storing the number of matches of a pattern and its features from an emotion map.
     * @param emotionMap
     * @return
     */
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
     * Write stats files.
     * @param resultMap the hashmap in which the results have been saved
     * @param outDir the output directory to which the files should be written
     * @throws IOException
     */
    public static void writeStats(Map<Pattern, Map<String, String>> resultMap, String outDir) throws IOException {

        PrintWriter patternStatWriter = new PrintWriter(new BufferedWriter(new FileWriter(outDir + "pattern_stats.txt")));
        PrintWriter emotionStatWriter = new PrintWriter(new BufferedWriter(new FileWriter(outDir + "emotion_stats.txt")));
        PrintWriter emotionSummaryWriter = new PrintWriter(new BufferedWriter(new FileWriter(outDir + "emotion_summary.txt")));

        // create maps for keeping track of patterns, their counts, and for emotions, patterns, and counts
        Map<Pattern, Integer> statMap = new HashMap<Pattern, Integer>();
        Map<String, Map<Pattern, Integer>> emotionStatMap = new HashMap<String, Map<Pattern, Integer>>();
        for (Enum emotionEnum : Enums.Emotions.values()) {
            emotionStatMap.put(emotionEnum.toString(), new HashMap<Pattern, Integer>());
        }

        // add the patterns to the maps
        for (Pattern pattern : resultMap.keySet()) {
            int matches = Integer.parseInt(resultMap.get(pattern).get(Enums.Stats.matches.toString()));
            statMap.put(pattern, matches);
            emotionStatMap.get(resultMap.get(pattern).get(Enums.Stats.emotion.toString())).put(pattern, matches);
        }

        // sort the pattern, count map and write the patterns to a file
        statMap = Extensions.sortByValue(statMap);
        for (Pattern pattern : statMap.keySet()) {
            if (pattern != null) {
                String emotion = resultMap.get(pattern).get(Enums.Stats.emotion.toString());
                patternStatWriter.printf("%s\t%s\t%s\t%d\n", emotion, cleanPattern(pattern.toString()),
                        resultMap.get(pattern).get(Enums.Features.isNP.toString()),
                        statMap.get(pattern));
            }
        }

        emotionSummaryWriter.printf("Emotion\t#patterns\ttotal_freq\t#patterns(>10)\ttotal_freq(>10)\n");

        // iterate through the emotions, sorting each respective map, and add the patterns to a file
        for (String emotion : emotionStatMap.keySet()) {
            int totalFreq = 0;
            int totalFreq10 = 0;
            int patterns10 = 0;
            Map<Pattern, Integer> sortedEmotionMap = Extensions.sortByValue(emotionStatMap.get(emotion));
            for (Pattern pattern : sortedEmotionMap.keySet()) {
                if (pattern != null) {
                    int freq = sortedEmotionMap.get(pattern);
                    totalFreq += freq;
                    if (freq > 10) {
                        totalFreq10 += freq;
                        patterns10++;
                    }

                    emotionStatWriter.printf("%s\t%s\t%s\t%d\n", emotion, cleanPattern(pattern.toString()),
                            resultMap.get(pattern).get(Enums.Features.isNP.toString()), freq);
                }
            }

            emotionSummaryWriter.printf("%s\t%d\t%d\t%d\t%d\n", emotion, emotionStatMap.get(emotion).keySet().size(), totalFreq,
                    patterns10, totalFreq10);
        }

        patternStatWriter.close();
        emotionStatWriter.close();
        emotionSummaryWriter.close();
    }

    /**
     * Clean a pattern string, i.e. remove the POS tags and the indexes.
     * @param pattern the pattern string to be cleaned
     * @return the cleaned pattern
     */
    public static String cleanPattern(String pattern) {
        Pattern patternCleaner = Pattern.compile("[a-z]+?(?=/)");
        StringBuilder sb = new StringBuilder();
        Matcher m = patternCleaner.matcher(pattern.toString());
        while (m.find()) {
            sb.append(m.group().toString());
            sb.append(" ");
        }

        return sb.toString().trim();
    }
}
