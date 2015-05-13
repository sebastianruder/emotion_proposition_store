import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to write patterns from a file and terms from the NRC emotion lexicon in a random order to a file including
 * instructions for annotation. The annotated file is used to check inter-annotator agreement.
 *
 * Created by sebastian on 28/04/15.
 */
public class RandomWriter {

    /**
     * Main method for reading in a pattern file and a file containing NRC terms and writing them in random order.
     * @param args the pattern file, the nrc terms file, the output file
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // hashmap for writing patterns in a random order to measure inter-annotator agreement; key: random, value: pattern
        Map<Double, String> randomPatternMap = new HashMap<Double, String>();

        String path = "/home/sebastian/git/sentiment_analysis/annotation/";
        InputStream inputStream = new FileInputStream(path + "random_patterns_gen.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        // read in the pattern file and add the patterns to the map; format: emotion tab expression tab isNP tab freq
        String line = reader.readLine();
        while (line != null) {
            String pattern = line.split("\t")[1];
            randomPatternMap.put(Math.random(), pattern);
            System.out.println(pattern);
            line = reader.readLine();
        }

        // read in the NRC terms file and add the patterns to the map; format: (emotion tab)+ expression
        inputStream = new FileInputStream(path + "nrc_terms.txt");
        reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        line = reader.readLine();
        while (line != null) {
            String[] lineSplit = line.split("\t");
            String term = lineSplit[lineSplit.length - 1];
            randomPatternMap.put(Math.random(), term);
            System.out.println(term);
            line = reader.readLine();
        }

        // write instructions
        PrintWriter randomWriter = new PrintWriter(path + "patterns_to_annotate.txt", "UTF-8");
        randomWriter.println("Annotation of emotions\n" +
                "Please annotate the following expressions with the most closely associated emotion and its degree among the following:\n" +
                "Joy, trust, fear, surprise, sadness, disgust, anger, and anticipation.\n" +
                "Please keep Plutchik's wheel of emotions (http://www.fractal.org/Bewustzijns-Besturings-Model/Plutchikfig6.gif)" +
                " visible while undertaking this task to assist you in differentiating the emotions.\n" +
                "Please also specify the degree of emotion on Plutchik's wheel like this: joy_I (inner wheel), joy_II (middle wheel), " +
                "joy_III (outer wheel).\n" +
                "If an expression clearly pertains to only emotion, assign just one. If no emotion can be clearly associated " +
                "with an expression, label it with 'none'. If you associate two emotions with an expression, " +
                "assign the most and the second-most closely associated emotion. Delimit expressions and emotions with tabs.\n" +
                "E.g. funfair   joy_II\n" +
                "pessimist  sadness_III fear_III\n" +
                "Please annotate only the first 30-40 patterns at the beginning. After these, we will meet briefly to " +
                "discuss problems and ambiguities. Thank you for your time!\n");

        // write the expressions in a random order
        randomWriter.println("expression\t1st emotion\t(2nd emotion)");
        for (double d : Extensions.asSortedList(randomPatternMap.keySet())) {
            randomWriter.println(randomPatternMap.get(d));
        }

        // write the controll questions
        randomWriter.println("\nFinal question: Do the following sentences correspond with the sentiments you marked? " +
                "Indicate yes/no.\n" +
                "rely on: Most big managed care plans rely on advice from panels of medical experts to recommend acceptance or rejection of new technology .\n" +
                "look forward to: I support a hefty tax cut , and I look forward to working with the members of the House and Senate .\n" +
                "overwhelm: Two years ago , he overwhelmed Augusta National , but that happens perhaps once in a lifetime because Augusta is too canny and cunning .\n" +
                "expect: He said he expects the subscriber levels to grow now that AT&T has assumed control of the cable company .\n" +
                "regret that: He also said he regrets that China has failed to open talks with the Dalai Lama , the exiled spiritual leader of Tibet .\n"
                );

        randomWriter.close();
    }
}
