import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by sebastian on 21/06/15.
 */
public class ResultsStatsWriter {

    public static void main(String[] args) throws IOException {
        String dir = "/home/sebastian/git/sentiment_analysis/out/";
        List<Extraction> extractions = ResultsReader.readResults(Utils.combine(dir, "results_cleaned_removed.txt"), true);
        writeResultsStats(extractions, Utils.combine(dir, "stats_all.txt"));
    }

    public static void writeResultsStats(List<Extraction> extractions, String filePath) throws IOException {
        File file = new File(filePath);
        file.delete();
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));

        Map<String, Double> patternCounts = new HashMap<String, Double>();
        Map<String, Double> emotionCounts = new HashMap<String, Double>();
        Map<String, Map<String, Double>> emotionPatternCounts = new HashMap<String, Map<String, Double>>();
        Map<String, Double> npCounts = new HashMap<String, Double>();
        for (Enums.Emotions emotion : Enums.Emotions.values()) {
            emotionPatternCounts.put(emotion.toString(), new HashMap<String, Double>());
        }

        for (Extraction extraction : extractions) {
            Extensions.updateMap(patternCounts, extraction.getPattern());
            Extensions.updateMap(emotionCounts, extraction.getEmotion());
            Extensions.updateMap(emotionPatternCounts.get(extraction.getEmotion()), extraction.getPattern());
            if (!extraction.getNPCause().equals("")) {
                Extensions.updateMap(npCounts, extraction.getEmotion());
            }
        }

        int total = extractions.size();
        emotionCounts = Extensions.sortByValue(emotionCounts, true);
        writeEntries(writer, emotionCounts, total);
        npCounts = Extensions.sortByValue(npCounts, true);
        compareCauseType(writer, npCounts, emotionCounts);
        patternCounts = Extensions.sortByValue(patternCounts, true);
        writeEntries(writer, patternCounts, total);
        for (String emotion : emotionPatternCounts.keySet()) {
            Map<String, Double> emotionPatternMap = Extensions.sortByValue(emotionPatternCounts.get(emotion), true);
            writer.printf("\n%s\n-----\n", emotion);
            writeEntries(writer, emotionPatternMap, total);
        }

        writer.close();
    }

    private static void compareCauseType(PrintWriter writer, Map<String, Double> npCounts, Map<String, Double> emotionCounts) {
        writer.println("Emotion\t# patterns with\t% patterns with");
        writer.println("\tNP cause\tS cause");

        // set format for comma separation
        DecimalFormat usFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        // usFormat.applyPattern("#.00");
        usFormat.setGroupingUsed(true);
        usFormat.setGroupingSize(3);

        int totalNP = 0;
        int total = 0;
        for (Map.Entry<String, Double> entry : npCounts.entrySet()) {
            String emotion = entry.getKey();
            writer.printf("%s\t%s\t%s\n", entry.getKey(), usFormat.format(entry.getValue()), usFormat.format(emotionCounts.get(emotion) - entry.getValue()));
            totalNP += entry.getValue();
            total += emotionCounts.get(emotion);
        }

        writer.printf("Total\t%s\t%s\n\n", usFormat.format(totalNP), usFormat.format(total - totalNP));
    }

    private static void writeEntries(PrintWriter writer, Map<String, Double> map, int totalExtractions) {
        writer.println("Emotion\tFrequency\t% of total extractions");
        int total = 0;
        int totalTop10 = 0;
        int noFreqHigher10 = 0;
        int i = 0;

        // set format for comma separation
        DecimalFormat usFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        // usFormat.applyPattern("#.00");
        usFormat.setGroupingUsed(true);
        usFormat.setGroupingSize(3);

        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String percent = String.format(Locale.US, "%.2f", entry.getValue() / (double) totalExtractions * 100);
            total += entry.getValue();
            if (i++ < 10) {
                writer.printf("%s\t%s\t%s\n", entry.getKey(), usFormat.format(entry.getValue()), percent);
                totalTop10 += entry.getValue();
            }

            if (entry.getValue() > 10) {
                noFreqHigher10++;
            }
        }

        writer.printf("Total top 10\t%s\t100.00\n", usFormat.format(totalTop10));
        writer.printf("Total\t%s\t100.00\n", usFormat.format(total));
        writer.printf("# of patterns with frequency > 10: %d\n\n", noFreqHigher10);
    }
}
