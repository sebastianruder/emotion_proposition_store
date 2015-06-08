import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to compare emotion annotations of the three annotators, calculate stats and inter-annotator agreeement.
 *
 * Created by sebastian on 16/05/15.
 */
public class AnnotationComparer {

    /**
     * Main method to compare annotations.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        args = new String[] { "/home/sebastian/git/sentiment_analysis/annotation/annotated/" };

        compareAnnotations(args[0]);
    }

    /**
     * Compares annotations, prints out stats, and calculates Fleiss' kappa for inter-annotator agreement.
     * @param dirPath the directory of the .annotated files
     * @throws IOException if the directory is not found
     */
    private static void compareAnnotations(String dirPath) throws IOException {

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
        int[][] matrix = new int[190][9];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                matrix[i][j] = 0;
            }
        }

        // matrix to store first and second choice emotion counts
        int[][] matrix2ndChoice = new int[190][emotions.length];
        for (int i = 0; i < matrix2ndChoice.length; i++) {
            for (int j = 0; j < matrix2ndChoice[0].length; j++) {
                matrix[i][j] = 0;
            }
        }

        // matrix to store degrees of emotions; 25 = 8 emotions * 3 degrees + 1 (none)
        int[][] degreeMatrix = new int[190][25];
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
                            degreeMatrix[i][j * 3 + degree.length()]++;
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
                        "Joy: %d\n" +
                        "Trust: %d\n" +
                        "Fear: %d\n" +
                        "Surprise: %d\n" +
                        "Sadness: %d\n" +
                        "Disgust: %d\n" +
                        "Anger: %d\n" +
                        "Anticipation: %d\n",
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
