import java.io.*;
import java.util.*;

/**
 * Class to compare emotion annotations of the three annotators, calculate stats and inter-annotator agreeement.
 *
 * Created by sebastian on 16/05/15.
 */
public class AnnotationComparer {

    /**
     * The directory of the files from which the bigrams should be retrieved.
     */
    private static String pmiDir = "/home/sebastian/git/sentiment_analysis/out/scores/pmi/";

    /**
     * Main method to compare annotations.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // args = new String[] { "/home/sebastian/git/sentiment_analysis/annotation/annotated/" };

        // int noOfPatterns = 180;
        // compareAnnotations(args[0], 180);

        String bigramPath = "/home/sebastian/git/sentiment_analysis/annotation/bigrams_annotated/";
        int noOfBigrams = 320;
        compareBigramAnnotations(bigramPath, noOfBigrams);
    }

    private static void compareBigramAnnotations(String dirPath, int noOfBigrams) throws IOException {

        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory.", dirPath));
        }

        // get only .annotated files
        String[] fileNames = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".annotated");
            }
        });

        // the array of emotions, i.e. categories
        String[] emotions = new String[] { "joy", "trust", "fear", "surprise", "sadness", "disgust", "anger", "anticipation", "none" };

        String[] sentiments = new String[] { "positive", "negative", "neutral", "none" };

        // matrix to store bigram emotion counts
        int[][] matrix = new int[noOfBigrams][9];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                matrix[i][j] = 0;
            }
        }

        // matrix to store bigram sentiment counts
        int[][] sentimentMatrix = new int[noOfBigrams][4];
        for (int i = 0; i < sentimentMatrix.length; i++) {
            for (int j = 0; j < sentimentMatrix[0].length; j++) {
                sentimentMatrix[i][j] = 0;
            }
        }


        // array to store all annotated bigrams
        String[] bigrams = new String[matrix.length];

        for (String fileName : fileNames) {
            String filePath = dirPath + fileName;
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            int i = -2; // row count
            while (line != null && !line.equals("")) {
                if (++i == -1) {
                    line = reader.readLine();
                    continue;
                }

                String[] lineSplit = line.split("\t");
                bigrams[i] = lineSplit[0];
                boolean hasEmotion  = lineSplit[1].toLowerCase().equals("yes") ? true : false;
                if (hasEmotion) {
                    for (int j = 0; j < emotions.length; j++) {
                        String emotion = lineSplit[2].toLowerCase();
                        if (emotion.equals(emotions[j])) {
                            matrix[i][j]++;
                            sentimentMatrix[i][Enums.emotionToSentiment(Enums.Emotions.valueOf(emotion)).ordinal()]++;
                        }
                    }
                }
                else {
                    // increase none count
                    matrix[i][8]++;
                    sentimentMatrix[i][3]++;
                }

                line = reader.readLine();
            }

            reader.close();
        }

        int unanimousCount = 0; // count of unanimously labeled expressions
        int majorityCount = 0; // count of expressions where the majority agreed
        int unanimousSentimentCount = 0;
        int majoritySentimentCount = 0;
        int[] majEmoCount = new int[emotions.length];
        int[] majSentimentCount = new int[sentiments.length];
        Map<String, String> goldBigramEmotionMap = new HashMap<String, String>();
        Map<String, String> goldBigramSentimentMap = new HashMap<String, String>();

        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("%s", bigrams[i]);
            // resultWriter.printf("%s", bigrams[i]);

            for (int j = 0; j < matrix[0].length; j++ ) {

                if (matrix[i][j] == 3) {
                    unanimousCount++;
                }

                if (matrix[i][j] >= 2) {
                    majorityCount++;
                    majEmoCount[j] += 1;
                    goldBigramEmotionMap.put(bigrams[i], emotions[j]);
                }

                if (matrix[i][j] > 0) {
                    System.out.printf("; %s: %d", emotions[j], matrix[i][j]);
                }
            }

            for (int j = 0; j < sentimentMatrix[0].length; j++) {

                if (sentimentMatrix[i][j] == 3) {
                    unanimousSentimentCount++;
                }

                if (sentimentMatrix[i][j] >= 2) {
                    majoritySentimentCount++;
                    majSentimentCount[j] += 1;
                    goldBigramSentimentMap.put(bigrams[i], sentiments[j]);
                }

                if (sentimentMatrix[i][j] > 0) {
                    System.out.printf("; %s: %d", sentiments[j], sentimentMatrix[i][j]);
                }
            }

            System.out.println();
            // resultWriter.println();
        }

        double k = calculateFleissKappa(matrix, 3); // Fleiss' kappa for all expressions

        Map<String, String> bigramEmotionMap = AnnotationTaskGenerator.getBigramsForAnnotation(pmiDir, 20);
        Map<String, String> bigramSentimentMap = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : bigramEmotionMap.entrySet()) {
            String emotion = entry.getValue();
            String bigramNgramType = entry.getKey();
            if (emotion.equals("none")) {
                bigramSentimentMap.put(bigramNgramType, emotion);
            }
            else {
                bigramSentimentMap.put(bigramNgramType, Enums.emotionToSentiment(Enums.Emotions.valueOf(emotion)).toString());
            }
        }

        List<String> ngramTypes = Arrays.asList(new String[] { "np_cause", "s_cause_pred_dobj" });

        System.out.print("\t");
        for (String ngramType : ngramTypes) {
            System.out.printf("%s\t\t\t", ngramType);
        }

        System.out.print("\nEmotion/sentiment\tPrecision\tRecall\tF1\tPrecision\tRecall\tF1");

        System.out.println();
        for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {
            System.out.print(emotionEnum.toString() + "\t");
            for (String ngramType : ngramTypes) {
                compareAgainstGoldStandard(goldBigramEmotionMap, bigramEmotionMap, Arrays.asList(new String[]{emotionEnum.toString()}), Arrays.asList(new String[]{ngramType}));
            }

            System.out.println();
        }

        System.out.print("Total\t");
        for (String ngramType : ngramTypes) {
            compareAgainstGoldStandard(goldBigramEmotionMap, bigramEmotionMap, Arrays.asList(emotions), Arrays.asList(new String[]{ ngramType }));
        }

        System.out.println();

        for (Enums.Sentiment sentimentEnum : Enums.Sentiment.values()) {
            System.out.print(sentimentEnum.toString() + "\t");
            for (String ngramType : ngramTypes) {
                compareAgainstGoldStandard(goldBigramSentimentMap, bigramSentimentMap, Arrays.asList(new String[] {sentimentEnum.toString()}), Arrays.asList(new String[]{ngramType}));
            }

            System.out.println();
        }

        System.out.print("total\t");
        for (String ngramType : ngramTypes) {
            compareAgainstGoldStandard(goldBigramSentimentMap, bigramSentimentMap, Arrays.asList(sentiments), Arrays.asList(new String[]{ ngramType }));
        }

        System.out.printf(
                "\nExpressions with unanimous emotions: %d\n" +
                        "Expressions with majority emotions: %d\n" +
                        "\nExpressions with unanimous sentiment: %d\n" +
                        "Expressions with majority sentiment: %d\n" +
                        "Fleiss' kappa: %f\n",
                unanimousCount, majorityCount, unanimousSentimentCount, majoritySentimentCount, k
        );

        System.out.printf(
                "\nMajority emotions:\n" +
                        "Joy:\t%d\n" +
                        "Trust:\t%d\n" +
                        "Fear:\t%d\n" +
                        "Surprise:\t%d\n" +
                        "Sadness:\t%d\n" +
                        "Disgust:\t%d\n" +
                        "Anger:\t%d\n" +
                        "Anticipation:\t%d\n",
                majEmoCount[0], majEmoCount[1], majEmoCount[2], majEmoCount[3], majEmoCount[4], majEmoCount[5],
                majEmoCount[6], majEmoCount[7]
        );

        System.out.printf(
                "\nMajority sentiments:\n" +
                        "Positive:\t%d\n" +
                        "Negative:\t%d\n" +
                        "Neutral:\t%d\n" +
                        "None:\t%d\n",
                majSentimentCount[0], majSentimentCount[1], majSentimentCount[2], majSentimentCount[3]
        );
    }

    /**
     * Compare a bigram map against a gold standard map. Gold standard contains less bigrams, as only majority bigrams
     * have been included.
     * @param goldStandardMap key: bigram, value: gold emotion or sentiment
     * @param map key: bigram, value: emotion or sentiment
     */
    private static void compareAgainstGoldStandard(Map<String, String> goldStandardMap, Map<String, String> map,
                                                   List<String> emotionsToInclude, List<String> ngramTypesToInclude) {

        int truePositives = 0;
        int falsePositives = 0;
        int trueNegatives = 0;
        int falseNegatives = 0;
        List<String> truePositiveList = new ArrayList<String>();
        List<String> falsePositiveList = new ArrayList<String>();
        List<String> falseNegativeList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String emotion = entry.getValue();
            String bigram = entry.getKey().split("\t")[0];
            String ngramType = entry.getKey().split("\t")[1];

            if (!emotionsToInclude.contains(emotion) || !ngramTypesToInclude.contains(ngramType)) {
                if (ngramTypesToInclude.contains(ngramType)) {
                    if (goldStandardMap.containsKey(bigram)) {
                        String goldEmotion = goldStandardMap.get(bigram);
                        if (emotionsToInclude.contains(goldEmotion)) {
                            falseNegatives++;
                            falseNegativeList.add(bigram);
                        }
                    }
                }

                continue;
            }

            if (goldStandardMap.containsKey(bigram)) {
                String goldEmotion = goldStandardMap.get(bigram);
                if (emotion.equals(goldEmotion)) {
                    truePositives++;
                    truePositiveList.add(bigram);
                    // System.out.printf("Correct: %s\t%s\n", bigram, emotion);
                }
                else {
                    falsePositives++;
                    falsePositiveList.add(bigram + ":" + goldEmotion);
                }
            }
        }

        double precision = (double)truePositives / ((double)truePositives + falsePositives);
        double recall = (double)truePositives / ((double)truePositives + falseNegatives);
        double fScore = 2 * (precision * recall / (precision + recall));
        System.out.printf(Locale.US, "%.2f\t%.2f\t%.2f\t", precision, recall, fScore);
    }

    /**
     * Compares annotations, prints out stats, and calculates Fleiss' kappa for inter-annotator agreement.
     * @param dirPath the directory of the .annotated files
     * @throws IOException if the directory is not found
     */
    private static void compareAnnotations(String dirPath, int noOfPatterns) throws IOException {

        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory.", dirPath));
        }

        // get only .annotated files
        String[] fileNames = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".annotated");
            }
        });

        // the array of emotions, i.e. categories
        String[] emotions = new String[] { "joy", "trust", "fear", "surprise", "sadness", "disgust", "anger", "anticipation", "none" };

        // matrix to store first choice emotion counts
        int[][] matrix = new int[noOfPatterns][9];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                matrix[i][j] = 0;
            }
        }

        // matrix to store first and second choice emotion counts
        int[][] matrix2ndChoice = new int[noOfPatterns][emotions.length];
        for (int i = 0; i < matrix2ndChoice.length; i++) {
            for (int j = 0; j < matrix2ndChoice[0].length; j++) {
                matrix[i][j] = 0;
            }
        }

        // matrix to store degrees of emotions; 25 = 8 emotions * 3 degrees + 1 (none)
        int[][] degreeMatrix = new int[noOfPatterns][25];
        for (int i = 0; i < degreeMatrix.length; i++) {
            for (int j = 0; j < degreeMatrix[0].length; j++) {
                degreeMatrix[i][j] = 0;
            }
        }

        // array to store all annotated expressions
        String[] expressions = new String[matrix.length];

        for (String fileName : fileNames) {
            String filePath = dirPath + fileName;
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            int i = 0; // row count
            while (line != null && !line.equals("")) {
                String[] lineSplit = line.split("\t");
                expressions[i] = lineSplit[0];
                for (int j = 0; j < emotions.length; j++) {
                    String emotion = lineSplit[1].split("_")[0];
                    if (emotion.equals(emotions[j])) {
                        System.out.println(emotion);
                        matrix[i][j]++;
                        matrix2ndChoice[i][j]++;

                        // add degree
                        if (emotion.equals("none")) {
                            degreeMatrix[i][degreeMatrix[0].length - 1]++;
                        }
                        else {
                            String degree = lineSplit[1].split("_")[1];
                            degreeMatrix[i][j * 3 + degree.length() - 1]++;
                        }
                    }

                    // add second choice to matrix if it exists
                    if (lineSplit.length > 2 && lineSplit[2].split("_")[0].equals(emotions[j])) {
                        matrix2ndChoice[i][j]++;
                    }
                }

                i++;
                line = reader.readLine();
            }

            reader.close();
        }

        int unanimousCount = 0; // count of unanimously labeled expressions
        int majorityCount = 0; // count of expressions where the majority agreed
        int unanimous2ndChoiceCount = 0; // count of unanimously labeled expressions when including second choice
        int degreeUnanimousCount = 0; // count if degree as well as emotion is unanimous
        int degreeMajorityCount = 0; // count if majority agreed on emotion and degree

        PrintWriter resultWriter = new PrintWriter(dirPath + "annotation_results"); // writer for annotation results
        Map<String, String> patternEmotionMap = new HashMap<String, String>();

        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("%s", expressions[i]);
            resultWriter.printf("%s", expressions[i]);
            boolean added = false;

            for (int j = 0; j < matrix[0].length; j++ ) {

                // increment degree counts
                for (int k = 0; k < 3; k++) {
                    int degreeCount;
                    if (j ==  matrix[0].length - 1) {
                        degreeCount = degreeMatrix[i][j * 3];
                    }
                    else {
                        degreeCount = degreeMatrix[i][j * 3 + k];
                    }

                    if (degreeCount == 3) {
                        degreeUnanimousCount++;
                    }

                    if (degreeCount >= 2) {
                        degreeMajorityCount++;
                        patternEmotionMap.put(expressions[i], String.format("%s_%d", emotions[j], k + 1));
                        added = true;
                    }
                }

                if (matrix[i][j] == 3) {
                    unanimousCount++;
                }

                if (matrix[i][j] >= 2) {
                    majorityCount++;
                    if (!added) {
                        patternEmotionMap.put(expressions[i], String.format("%s", emotions[j]));
                    }
                }

                if (matrix2ndChoice[i][j] != 0) {
                    if (matrix2ndChoice[i][j] == 3) {
                        unanimous2ndChoiceCount++;
                    }

                    String matrix2ndChoiceString = String.format("; %s: %d (%d)", emotions[j], matrix[i][j], matrix2ndChoice[i][j]);
                    System.out.print(matrix2ndChoiceString);
                    resultWriter.print(matrix2ndChoiceString);
                }


            }

            System.out.println();
            resultWriter.println();
        }

        PrintWriter majorityWriter = new PrintWriter(dirPath + "majority_expressions"); // writer for majority expressions
        patternEmotionMap = Extensions.sortByValue(patternEmotionMap, false);
        for (Map.Entry<String, String> entry : patternEmotionMap.entrySet()) {
            majorityWriter.printf("%s\t%s\n", entry.getValue(), entry.getKey());
        }

        majorityWriter.close();

        // create matrix only with expressions where majority agreed
        int[][] majorityMatrix = new int[majorityCount][emotions.length];
        int majorityIndex = 0;
        int[] majEmoCount = new int[emotions.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < emotions.length; j++) {
                if (matrix[i][j] >= 2) {
                    majorityMatrix[majorityIndex][j] = matrix[i][j];
                    majorityIndex++;
                    majEmoCount[j] += 1;
                }
            }
        }

        double k = calculateFleissKappa(matrix, 3); // Fleiss' kappa for all expressions
        double majorityK = calculateFleissKappa(majorityMatrix, 3); // Fleiss' kappa for expressions where majority agreed

        String statsString = String.format(
                "\nExpressions with unanimous emotions: %d\n" +
                        "Expressions with unanimous emotions (including 2nd choice): %d\n" +
                        "Expressions with majority emotions: %d\n" +
                        "Expressions with unanimous emotions + degree: %d\n" +
                        "Expressions with majority emotions + degree: %d\n" +
                        "Fleiss' kappa: %f\n" +
                        "Fleiss' kappa for expressions with majority emotions: %f\n",
                unanimousCount, unanimous2ndChoiceCount, majorityCount, degreeUnanimousCount, degreeMajorityCount, k, majorityK
                );

        String emotionString = String.format(
                "\nMajority emotions:\n" +
                        "Joy:\t%d\n" +
                        "Trust:\t%d\n" +
                        "Fear:\t%d\n" +
                        "Surprise:\t%d\n" +
                        "Sadness:\t%d\n" +
                        "Disgust:\t%d\n" +
                        "Anger:\t%d\n" +
                        "Anticipation:\t%d\n",
                majEmoCount[0], majEmoCount[1], majEmoCount[2], majEmoCount[3], majEmoCount[4], majEmoCount[5],
                majEmoCount[6], majEmoCount[7]
        );

        System.out.print(statsString);
        System.out.print(emotionString);
        resultWriter.print(statsString);
        resultWriter.print(emotionString);
        resultWriter.close();

        // printMatrix(matrix);
    }

    /**
     * Print the rows and columns of a matrix.
     * @param matrix a two-dimensional array
     */
    private static void printMatrix(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.printf("%1d ", matrix[i][j]);
            }

            System.out.println();
        }
    }

    /**
     * Calculate Fleiss' kappa given a two-dimensional array and the number of annotators.
     * @param matrix int[i][j] where i is the number of annotated instances and j is the number of categories
     * @param rater the number of annotators
     * @return Fleiss' kappa
     */
    private static double calculateFleissKappa(int[][] matrix, int rater) {

        // array for row scores; P_i is sum over squares of row elements / (rater * (rater - 1))
        double[] P_i = new double[matrix.length];

        // array for column scores; p_j is sum over column elements / (column length * rater)
        double[] p_j = new double[matrix[0].length]; // array for column scores

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                P_i[i] += Math.pow(matrix[i][j], 2);
                p_j[j] += matrix[i][j];
            }

            P_i[i] -= rater;
            P_i[i] /= (rater * (rater - 1));
        }

        for (int j = 0; j < p_j.length; j++) {
            p_j[j] /= matrix.length * 3;
        }

        // calculate P as sum over P_is normalized over all instances
        double P = 0;
        for (int i = 0; i < P_i.length; i++) {
            P += P_i[i];
        }

        P /= P_i.length;

        // calculate P_e as sum over the squares of p_j
        double P_e = 0;
        for (int j = 0; j < p_j.length; j++) {
            P_e += Math.pow(p_j[j], 2);
        }

        double k = (P - P_e) / (1 - P_e);
        return k;
    }

}
