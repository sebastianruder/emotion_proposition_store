import java.io.*;
import java.util.*;

/**
 * Class to reade in a results.txt file and store the contents for further analysis.
 *
 * Created by sebastian on 17/05/15.
 */
public class ResultsReader {

    /**
     * List to store extractions.
     */
    private static List<Extraction> extractions = new ArrayList<Extraction>();

    /**
     * Main method to read in an extraction file and calculate association metrics and write files per emotion
     * sorted by association metric.
     * @param args the extraction file
     * @throws IOException if the file couldn't be found
     */
    public static void main(String[] args) throws IOException {

        args = new String[] { "/home/sebastian/git/sentiment_analysis/out/results_all.txt" };
        String dir = "/home/sebastian/git/sentiment_analysis/out/scores/";
        readResults(args[0]);
        Analyzer analyzer = new Analyzer("/home/sebastian/git/sentiment_analysis/out/stop_words.txt");
        analyzer.countFrequencies(extractions);
        Map<String, Double> emotionUnigramNPPMIMap = analyzer.calculatePMI(true, true);
        Map<String, Double> emotionBigramNPPMIMap = analyzer.calculatePMI(true, false);
        Map<String, Double> emotionUnigramSPMIMap = analyzer.calculatePMI(false, true);
        Map<String, Double> emotionBigramSPMIMap = analyzer.calculatePMI(false, false);
        Map<String, Double> emotionUnigramNPChiSquareMap = analyzer.calculateChiSquare(true, true);
        Map<String, Double> emotionBigramNPChiSquareMap = analyzer.calculateChiSquare(true, false);
        Map<String, Double> emotionUnigramSChiSquareMap = analyzer.calculateChiSquare(false, true);
        Map<String, Double> emotionBigramSChiSquareMap = analyzer.calculateChiSquare(false, false);

        System.out.println("\nPMI Top 50 PMI unigrams in NP cause");
        printTopEntries(50, emotionUnigramNPPMIMap);
        System.out.println("\nPMI Top 50 PMI bigrams in NP cause");
        printTopEntries(50, emotionBigramNPPMIMap);
        System.out.println("\nPMI Top 50 PMI unigrams in S cause");
        printTopEntries(50, emotionUnigramSPMIMap);
        System.out.println("\nPMI Top 50 PMI bigrams in S cause");
        printTopEntries(50, emotionBigramSPMIMap);

        System.out.println("\nPMI Top 50 chi-square unigrams in NP cause");
        printTopEntries(50, emotionUnigramNPChiSquareMap);
        System.out.println("\nPMI Top 50 chi-square bigrams in NP cause");
        printTopEntries(50, emotionBigramNPChiSquareMap);
        System.out.println("\nPMI Top 50 chi-square unigrams in S cause");
        printTopEntries(50, emotionUnigramSChiSquareMap);
        System.out.println("\nPMI Top 50 chi-square bigrams in S cause");
        printTopEntries(50, emotionBigramSChiSquareMap);

        Utils.cleanDirectory(dir, ".txt");

        writeMap(emotionBigramNPChiSquareMap, "chi-square", dir);
        writeMap(emotionBigramNPPMIMap, "pmi", dir);
    }

    /**
     * Writes the ngrams of an association map to one file per emotion, sorted by association score.
     * @param map key: emotion tab ngram; value: association score
     * @param metric the association metric that was used
     * @param dir the directory that the files should be written to
     * @throws IOException if the directory was not found
     */
    private static void writeMap(Map<String, Double> map, String metric, String dir) throws IOException {
        map = Extensions.sortByValue(map, true);
        for (Map.Entry<String, Double> entry: map.entrySet()) {
            String emotion = entry.getKey().split("\t")[0];
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%s%s_%s.txt", dir, emotion, metric), true)));
            writer.println(entry.getKey().split("\t")[1] + "\t" + entry.getValue());
            writer.close();
        }
    }

    /**
     * Prints the top n entries of a map.
     * @param topN the top n entries that should be printed
     * @param map key: string; value: score
     */
    private static void printTopEntries(int topN, Map<String, Double> map) {
        map = Extensions.sortByValue(map, true);
        int count = 0;
        for (Map.Entry<String, Double> entry: map.entrySet()) {
            if (count++ > topN) {
                break;
            }

            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Reads extractions from a file and stores them as <code>Extraction</code> in a list.
     * @param filePath the path to the extraction file
     * @throws IOException if the file was not found
     */
    private static void readResults(String filePath) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        int i = 0; // row count
        while (line != null && !line.equals("")) {

            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

            //# ID	Emotion	Pattern	Emotion Holder	(NP-Cause)	(Subj S-Cause)	(Pred S-Cause)	(Dobj S-Cause)	[Pobjs S-Cause]	[BoW Cause]

            String[] lineSplit = line.split("\t");

            try {
                String id = lineSplit[0];
                String emotion = lineSplit[1];
                String pattern = lineSplit[2];
                String emotionHolder = lineSplit[3];
                String NPCause = lineSplit[4];
                String subjSCause = lineSplit[5];
                String predSCause = lineSplit[6];
                String dobjSCause = lineSplit[7];
                String pobjs = lineSplit[8];
                String causeBoW = lineSplit[9];
                Extraction extraction = new Extraction(id, emotion, pattern, emotionHolder, NPCause, subjSCause, predSCause,
                        dobjSCause, pobjs, causeBoW);
                extractions.add(extraction);
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException(String.format("Format error in line %d: %s", i - 1, line));
            }

            line = reader.readLine();
        }

        System.out.println("Done");
    }

}
