import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to create the annotation task for verifying the emotion of top bigrams and a sample sentence.
 *
 * Created by sebastian on 23/06/15.
 */
public class AnnotationTaskGenerator {

    /**
     * The directory of the files from which the bigrams should be retrieved.
     */
    private static String pmiDir = "/home/sebastian/git/sentiment_analysis/out/scores/pmi/";

    /**
     * The collocations file containing sentences.
     */
    private static String collocationsFilePath = "/home/sebastian/git/sentiment_analysis/out/results_final/collocations.txt";

    /**
     * The path to the file containing the annotation task.
     */
    private static String annotationFilePath = "/home/sebastian/git/sentiment_analysis/out/annotation.txt";

    /**
     * Number of top ngrams that should be retrieved.
     */
    private static int topN = 20;

    /**
     * Main method to create the annotation task.
     * @param args the input arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // the bigrams that should be annotated
        Map<String, String> bigramEmotionMap = getBigramsForAnnotation(pmiDir, topN);

        File file = new File(annotationFilePath);
        file.delete();
        String[] collocations = readCollocationsFile(collocationsFilePath);

        for (Map.Entry<String, String> entry : bigramEmotionMap.entrySet()) {

            String emotion = entry.getValue();
            String bigram = entry.getKey().split("\t")[0];

            List<String> matchedSentences = matchAgainstCollocations(collocations, emotion, bigram);
            String randomCollocation = getRandomMember(matchedSentences);

            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(annotationFilePath, true)));
            System.out.printf("%s\t\t\t%s\t\t\n", bigram, randomCollocation);
            writer.printf("%s\t\t\t%s\t\t\n", bigram, randomCollocation);
            writer.close();
        }
    }

    /**
     * Retrieves a random member from a list of string.
     * @param list the list of strings
     * @return a random member of said list
     */
    private static String getRandomMember(List<String> list) {
        int randomIdx = (int)(Math.random() * list.size());
        return list.get(randomIdx);
    }

    /**
     * Matches a bigram and an emotion against the string array of collocations using a predefined pattern and returns
     * the matched sentences.
     * @param collocations an array of collocations
     * @param emotion the emotion that should be matched
     * @param bigram the bigram that should be matched
     * @return the matched sentences
     */
    private static List<String> matchAgainstCollocations(String[] collocations, String emotion, String bigram) {

        List<String> matchedSentences = new ArrayList<String>();
        Pattern pattern = Pattern.compile("emotion" + ".*[^a-z]" + emotion + ".*cause.*[^a-z]" + Extensions.join(bigram.replace("$", "\\$").replace("NUM", "NUMBER").toLowerCase().split(" "), "([^a-z].*[^a-z]| )") + "[^a-z].*cause bow");

        for (int i = 1; i < collocations.length; i++) {

            Matcher m = pattern.matcher(collocations[i]);
            if (m.find()) {
                matchedSentences.add(collocations[i - 1].split("\t")[1]);
            }
        }

        return matchedSentences;
    }

    /**
     * Read in a collocations file and store it in a String array. Note: The standard collocations file that is 4,641,270
     * lines long is assumed.
     * @param filePath the file path to the collocations file
     * @return a string array containing the collocations file
     * @throws IOException
     */
    public static String[] readCollocationsFile(String filePath) throws IOException {

        String[] collocations = new String[4641271];
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        String line = reader.readLine();

        int i = 0;
        while (line != null && !line.equals("")) {

            collocations[i++] = line.toLowerCase();
            line = reader.readLine();
        }

        return collocations;
    }

    /**
     * Return the top n bigrams of NP cause and predicate direct object files in a directory as a map of bigrams + their
     * ngram source and their emotions.
     * @param dir the directory containing the files
     * @param topN the top n bigrams
     * @return a map with key: bigram tab ngram type (np_cause|s_cause_pred_dobj); value: emotion
     * @throws IOException
     */
    public static Map<String, String> getBigramsForAnnotation(String dir, int topN) throws IOException {

        Map<String, String> bigramEmotionMap = new HashMap<String, String>();

        List<String> fileNames = Utils.getFileNames(dir, "(np_cause|pred_dobj)", "bigram", "\\.txt");
        Collections.sort(fileNames);

        for (String fileName : fileNames) {

            String ngramSource = fileName.contains(Enums.NgramSource.np_cause.toString()) ? Enums.NgramSource.np_cause.toString() : Enums.NgramSource.s_cause_pred_dobj.toString();

            int count = 0;

            BufferedReader reader = new BufferedReader(new FileReader(Utils.combine(dir, fileName)));
            String line = reader.readLine();

            while (line != null && !line.equals("") && count < topN && count++ < topN + 1) {

                // add ngram source to bigram to be used in evaluation
                String bigram = line.split("\t")[0] + "\t" + ngramSource;
                String emotion = fileName.split("_")[0];

                // don't get bigrams for named entities
                if (bigram.contains("/")) {
                    line = reader.readLine();
                    continue;
                }

                bigramEmotionMap.put(bigram, emotion);
                line = reader.readLine();
            }
        }

        return bigramEmotionMap;
    }
}
