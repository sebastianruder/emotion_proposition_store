import java.io.*;
import java.util.*;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Class to reade in a results.txt file and store the contents for further analysis.
 *
 * Created by sebastian on 17/05/15.
 */
public class ResultsReader {

    /**
     * List to store extractions.
     */
    private static List<Extraction> extractions;

    /**
     * Directory where scores are saved
     */
    private static String dir = "/home/sebastian/git/sentiment_analysis/out/scores/";

    private static int topN = 50;

    /**
     * Key: Expression in NRC Emotion Lexicon. Value: Array of booleans in order of Enums.Emotions indicating if the
     * expression is associated with that emotion.
     */
    private static Map<String, Boolean[]> emotionLexicon = new HashMap<String, Boolean[]>();

    /**
     * If information about whether the expression occurred with the same emotion in the NRC Emotion Lexicon (FALSE,
     * TRUE, NA) should be added to the unigram files.
     */
    private static boolean writeNRCOverlap = true;

    /**
     * Main method to read in an extraction file and calculate association metrics and write files per emotion
     * sorted by association metric.
     * @param args the extraction file
     * @throws IOException if the file couldn't be found
     */
    public static void main(String[] args) throws IOException {

        args = new String[] { "/home/sebastian/git/sentiment_analysis/out/results_cleaned_removed.txt" };
        extractions = readResults(args[0], false);
        if (writeNRCOverlap) {
            emotionLexicon = readNRCEmotionLexicon("/home/sebastian/git/sentiment_analysis/NRC-Emotion-Lexicon-v0.92/NRC_emotion_lexicon_list.txt");
        }

        Analyzer analyzer = new Analyzer("/home/sebastian/git/sentiment_analysis/out/stop_words.txt");
        analyzer.countFrequencies(extractions);

        for (Enums.Metric metricEnum : Enums.Metric.values()) {
            String metric = metricEnum.toString();

            // check if metric directory exists
            File metricDir = new File(Utils.combine(dir, metric));
            if (!metricDir.isDirectory()) {
                throw new IOException(metricDir + " is not a directory.");
            }

            Utils.cleanDirectory(metricDir.getPath(), "*");

            // if (metricEnum.equals(Enums.Metric.chi_square)) continue;

            for (Enums.Ngram ngramEnum : Enums.Ngram.values()) {

                // if (ngramEnum.equals(Enums.Ngram.unigram)) continue;
                String ngram = ngramEnum.toString();
                for (Enums.NgramSource ngramSource : Enums.NgramSource.values()) {

                    // no unigram expressions for combinations of S cause predicate and subject/direct object
                    if (ngramEnum.equals(Enums.Ngram.unigram) && (ngramSource.equals(Enums.NgramSource.s_cause_subj_pred) ||
                            ngramSource.equals(Enums.NgramSource.s_cause_pred_dobj))) {
                        continue;
                    }

                    Map<String, Double> map;
                    switch (metricEnum) {
                        case pmi:
                            map = analyzer.calculatePMI(ngramSource, ngramEnum);
                            break;
                        case chi_square:
                            map = analyzer.calculateChiSquare(ngramSource, ngramEnum);
                            break;
                        default:
                            throw new NotImplementedException();
                    }

                    map = Extensions.sortByValue(map, true);
                    System.out.printf("metric: %s, ngram: %s, ngram type: %s\n", metric, ngram, ngramSource.toString());

                    if (ngramSource.equals(Enums.NgramSource.np_cause) || ngramSource.equals(Enums.NgramSource.s_cause_pred_dobj)
                            || ngramSource.equals(Enums.NgramSource.emotion_holder) || ngramSource.equals(Enums.NgramSource.s_cause_subj_pred)) {
                        writeScoreFiles(map, metricEnum, ngramEnum, ngramSource, writeNRCOverlap, metricDir.getPath(), emotionLexicon);
                    }

                    Map<String, Map<String, Double>> overlapMap =
                            analyzer.calculcateEmotionOverlap(ngramSource, ngramEnum, map);
                    overlapMap = Extensions.sortByAggregatedValue(overlapMap, true);

                    String overlapFileName = String.format("%s_%s_%s.overlap", metric, ngram, ngramSource.toString());

                    if (ngramSource.equals(Enums.NgramSource.np_cause)) {
                        writeEmotionOverlapMap(overlapMap, Utils.combine(metricDir.getPath(), overlapFileName));
                    }

                    String sentimentFile = String.format("%s_%s_%s", metricEnum.toString(), ngramEnum.toString(), ngramSource.toString());
                    writeSentiment(overlapMap, Utils.combine(metricDir.getPath(), sentimentFile));
                }
            }
        }
    }

    // writes aggregated score of expressions across positive and negative sentiment emotions
    // filename without extension
    private static void writeSentiment(Map<String, Map<String, Double>> overlapMap, String fileName) throws IOException {

        Map<String, Double> posMap = new HashMap<String, Double>();
        Map<String, Double> negMap = new HashMap<String, Double>();

        for (Map.Entry<String, Map<String, Double>> entry : overlapMap.entrySet()) {
            double posScore = 0;
            double negScore = 0;
            String expression = entry.getKey();
            Map<String, Double> ngramMap = entry.getValue();
            for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {

                if (ngramMap.containsKey(emotionEnum.toString())) {
                    if (Enums.emotionToSentiment(emotionEnum).equals(Enums.Sentiment.positive)) {
                        posScore += ngramMap.get(emotionEnum.toString());
                    } else if (Enums.emotionToSentiment(emotionEnum).equals(Enums.Sentiment.negative)) {
                        negScore += ngramMap.get(emotionEnum.toString());
                    }
                }
            }

            posMap.put(expression, posScore);
            negMap.put(expression, negScore);
        }

        posMap = Extensions.sortByValue(posMap, true);
        negMap = Extensions.sortByValue(negMap, true);
        PrintWriter posWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".pos")));
        PrintWriter negWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".neg")));

        for (Map.Entry<String, Double> entry : posMap.entrySet()) {
            posWriter.printf("%s\t%s\n", entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Double> entry : negMap.entrySet()) {
            negWriter.printf("%s\t%s\n", entry.getKey(), entry.getValue());
        }

        posWriter.close();
        negWriter.close();
    }

    /**
     * Writes ngrams that have high assocation metric scores across several emotions in csv format.
     * Note: At the moment cuts off after the top 1000 expressions.
     * @param overlapMap key: expression; value: map with key: emotion; value: score.
     * @param fileName the name of the file the output should be saved as
     * @throws IOException if an error occurred during writing
     */
    private static void writeEmotionOverlapMap(Map<String, Map<String, Double>> overlapMap, String fileName) throws IOException {

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

        // write overlap in positive/negative
        PrintWriter posNegWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName + "_pos_neg.overlap")));

        // writer to write percentages
        PrintWriter percentWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".percent")));

        // write column names
        for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {
            if (Enums.Emotions.valueOf(emotionEnum.toString()).ordinal() != 0) {
                writer.print(",");
                percentWriter.print(",");
            }
            writer.printf("%s", emotionEnum.toString());
            percentWriter.printf("%s", emotionEnum.toString());
        }

        // write positive / neutral / negative column names
        for (Enums.Sentiment sentimentEnum : Enums.Sentiment.values()) {
            if (Enums.Sentiment.valueOf(sentimentEnum.toString()).ordinal() != 0) posNegWriter.print(",");
            posNegWriter.printf("%s", sentimentEnum.toString());
        }

        Map<String, Double> ngramTotalMap = new HashMap<String, Double>();

        int count = 0;
        for (Map.Entry<String, Map<String, Double>> entry : overlapMap.entrySet()) {
            double posScore = 0;
            double negScore = 0;
            double neuScore = 0;
            String ngram = entry.getKey();
            ngramTotalMap.put(ngram, 0.0);
            writer.printf("\n%s,", ngram);
            posNegWriter.printf("\n%s,", ngram);
            // System.out.printf("\n%s: ", entry.getKey());
            Map<String, Double> ngramMap = entry.getValue();
            for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {
                String emotion = emotionEnum.toString();
                if (Enums.Emotions.valueOf(emotion).ordinal() != 0) writer.print(",");

                // checks if map contains emotion, i.e. if score of expression for that emotion > 0
                if (ngramMap.containsKey(emotion)) {
                    writer.printf(Locale.US, "%.2f", ngramMap.get(emotion));

                    // update ngram total count
                    ngramTotalMap.put(ngram, ngramTotalMap.get(ngram) + ngramMap.get(emotion));
                    // System.out.printf(emotionEnum.toString() + ": " + ngramMap.get(emotionEnum.toString()) + "; ");

                    // add up positive / negative / neutral scores
                    if (Enums.emotionToSentiment(emotionEnum).equals(Enums.Sentiment.positive)) {
                        posScore += ngramMap.get(emotion);
                    } else if (Enums.emotionToSentiment(emotionEnum).equals(Enums.Sentiment.negative)) {
                        negScore += ngramMap.get(emotion);
                    } else if (Enums.emotionToSentiment(emotionEnum).equals(Enums.Sentiment.neutral)) {
                        neuScore += ngramMap.get(emotion);
                    }
                }
                else {
                    writer.print("0.00");
                }
            }

            posNegWriter.printf(Locale.US, "%.2f,%.2f,%.2f", posScore, negScore, neuScore);

            if (count++ > 1000) break;
        }

        // write percent overlap as well
        count = 0;
        for (Map.Entry<String, Map<String, Double>> entry : overlapMap.entrySet()) {
            String ngram = entry.getKey();
            percentWriter.printf("\n%s,", ngram);
            // System.out.printf("\n%s: ", entry.getKey());
            Map<String, Double> ngramMap = entry.getValue();
            for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {
                String emotion = emotionEnum.toString();
                if (Enums.Emotions.valueOf(emotion).ordinal() != 0) percentWriter.print(",");

                // checks if map contains emotion, i.e. if score of expression for that emotion > 0
                if (ngramMap.containsKey(emotion)) {
                    percentWriter.printf(Locale.US, "%.2f", ((double)ngramMap.get(emotion)) / ngramTotalMap.get(ngram) * 100.0);
                    // System.out.printf(emotionEnum.toString() + ": " + ngramMap.get(emotionEnum.toString()) + "; ");
                }
                else {
                    percentWriter.print("0.00");
                }
            }

            if (count++ > 1000) break;
        }

        writer.close();
        posNegWriter.close();
        percentWriter.close();
    }

    /**
     * Writes the ngrams of an association map to one file per emotion, sorted by association score.
     * @param map key: emotion tab ngram; value: association score
     * @param metricEnum the association metric that was used (pmi, chi-square)
     * @param ngramEnum the ngram that was used (unigram, bigram)
     * @param ngramSourceEnum the ngram type that was used
     * @param writeNRCOverlap if overlap with the NRC Emotion Lexicon should be written
     * @param dir the directory that the files should be written to
     * @throws IOException if the directory was not found
     */
    private static void writeScoreFiles(Map<String, Double> map, Enums.Metric metricEnum, Enums.Ngram ngramEnum,
                                        Enums.NgramSource ngramSourceEnum, boolean writeNRCOverlap, String dir,
                                        Map<String, Boolean[]> emotionLexicon) throws IOException {

        for (Map.Entry<String, Double> entry: map.entrySet()) {
            String emotion = entry.getKey().split("\t")[0];
            String ngram = entry.getKey().split("\t")[1];
            String fileName =  String.format("%s_%s_%s_%s.txt", emotion, metricEnum.toString(), ngramEnum.toString(), ngramSourceEnum.toString());
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(Utils.combine(dir, fileName), true)));

            if (writeNRCOverlap) {
                String overlap = "NA";
                String overlapWithSentiment = "NA";
                boolean isAssociated = false;
                boolean isAssociatedWithSentiment = false;
                Enums.Sentiment sentiment = Enums.emotionToSentiment(Enums.Emotions.valueOf(emotion));
                if (ngramEnum.equals(Enums.Ngram.unigram)) {
                    if (emotionLexicon.containsKey(ngram)) {
                        isAssociated |= emotionLexicon.get(ngram)[Enums.Emotions.valueOf(emotion).ordinal()];
                        overlap = isAssociated ? "TRUE" : "FALSE";

                        // only check association with sentiment for clearly positive or negative emotions
                        if (sentiment.equals(Enums.Sentiment.positive) || sentiment.equals(Enums.Sentiment.negative)) {
                            isAssociatedWithSentiment |= emotionLexicon.get(ngram)[sentiment.ordinal() + 8];
                            overlapWithSentiment = isAssociatedWithSentiment ? "TRUE" : "FALSE";
                        }
                    }
                }
                else if (ngramEnum.equals(Enums.Ngram.bigram)) {

                    for (String unigram : ngram.split(" ")) {
                        if (unigram.contains(":")) {
                            unigram = unigram.split(":")[0];
                        }

                        if (emotionLexicon.containsKey(unigram)) {
                            isAssociated |= emotionLexicon.get(unigram)[Enums.Emotions.valueOf(emotion).ordinal()];
                            overlap = isAssociated ? "TRUE" : "FALSE";

                            // only check association with sentiment for clearly positive or negative emotions
                            if (sentiment.equals(Enums.Sentiment.positive) || sentiment.equals(Enums.Sentiment.negative)) {
                                isAssociatedWithSentiment |= emotionLexicon.get(unigram)[sentiment.ordinal() + 8];
                                overlapWithSentiment = isAssociatedWithSentiment ? "TRUE" : "FALSE";
                            }
                        }
                    }
                }
                writer.printf(Locale.ENGLISH, "%s\t%f\t%s\t%s\n", ngram, entry.getValue(), overlap, overlapWithSentiment);
            }
            else {
                writer.println(ngram + "\t" + entry.getValue());
            }

            writer.close();
        }
    }

    /**
     * Prints the top n entries of a sorted map.
     * @param topN the top n entries that should be printed
     * @param map key: string; value: score
     */
    public static void printTopEntries(int topN, Map<String, Double> map) {
        int count = 0;
        for (Map.Entry<String, Double> entry: map.entrySet()) {
            if (count++ > topN) {
                break;
            }

            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Reads in the NRC Word-Emotion Associaton Lexicon (EmoLex) and stores its entries in a map.
     * @param fileName the path to the file of the lexicon
     * @return a map with key: unigram; value: array of booleans indicating agreement with EmoLex; index of boolean
     * corresponds to index of emotion in <code>Enums.Emotions</code>. 9-th and 10th position are filled by positive and
     * negative sentiment respectively.
     * @throws IOException
     */
    public static Map<String, Boolean[]> readNRCEmotionLexicon(String fileName) throws IOException {
        Map<String, Boolean[]> emotionLexicon = new HashMap<String, Boolean[]>();
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String previousExpression = "";
        String line = reader.readLine();
        while (line != null && !line.equals("")) {
            String[] lineSplit = line.split("\t");
            String expression = lineSplit[0];
            String emotion = lineSplit[1];
            boolean isAssociated = lineSplit[2].equals("1") ? true : false;

            if (!expression.equals(previousExpression)) {

                // including positive, negative
                emotionLexicon.put(expression, new Boolean[10]);
            }

            // check if emotion or sentiment is associated with the expression in the NRC sentiment lexicon
            if (emotion.equals(Enums.Sentiment.positive.toString()) || emotion.equals(Enums.Sentiment.negative.toString())) {

                // positive and negative are at 9-th respectively 10th position in boolean array; neutral is not added
                emotionLexicon.get(expression)[Enums.Sentiment.valueOf(emotion).ordinal() + 8] = isAssociated;
            }
            else {
                emotionLexicon.get(expression)[Enums.Emotions.valueOf(emotion).ordinal()] = isAssociated;
            }

            previousExpression = expression;
            line = reader.readLine();
        }

        return emotionLexicon;
    }

    /**
     * Reads extractions from a file and stores them as <code>Extraction</code> in a list.
     * @param filePath the path to the extraction file
     * @throws IOException if the file was not found
     */
    public static List<Extraction> readResults(String filePath, boolean normalizePattern) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        List<Extraction> extractions = new ArrayList<Extraction>();
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

                if (normalizePattern) {

                    // as patterns include adverbs, modify patterns back to base form
                    List<String> prepositions = new ArrayList<String>(Arrays.asList("that", "about", "of", "by", "on", "for"));
                    if (pattern.startsWith("be ")) {
                        String[] patternSplit = pattern.split(" ");
                        int length = patternSplit.length;
                        boolean endsWithPreposition = false;
                        for (String preposition : prepositions) {
                            if (pattern.endsWith(preposition)) {
                                endsWithPreposition = true;
                                if (length != 3) {
                                    pattern = String.format("%s %s", patternSplit[0], patternSplit[length - 2]);
                                }
                            }
                        }

                        if (!endsWithPreposition && length != 2) {
                            pattern = String.format("%s %s", patternSplit[0], patternSplit[length - 1]);
                        }
                    }

                    // exclude prepositions
                    for (String preposition : prepositions) {
                        if (pattern.endsWith(preposition)) {
                            pattern = pattern.substring(0, pattern.length() - preposition.length() - 1);
                        }
                    }
                }

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
        return extractions;
    }
}
