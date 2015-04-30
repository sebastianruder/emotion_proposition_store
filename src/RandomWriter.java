import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to write patterns from file in a random order and produce two output files: for annotation and checking.
 *
 * Created by sebastian on 28/04/15.
 */
public class RandomWriter {

    public static void main(String[] args) throws IOException {

        // hashmap for writing patterns in a random order to measure inter-annotator agreement; key: random, value: pattern
        Map<Double, String> randomPatternMap = new HashMap<Double, String>();

        String path = "/home/sebastian/git/sentiment_analysis/annotation/";
        InputStream inputStream = new FileInputStream(path + "random_patterns_gen.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line = reader.readLine();
        while (line != null) {
            String pattern = line.split("\t")[1];
            randomPatternMap.put(Math.random(), pattern);
            System.out.println(pattern);
            line = reader.readLine();
        }

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

        PrintWriter randomWriter = new PrintWriter(path + "patterns_to_annotate.txt", "UTF-8");
        randomWriter.println("Annotation of emotions\n" +
                "Please annotate the following expressions with the most closely associated emotion among the following:\n" +
                "Joy, trust, fear, surprise, sadness, disgust, anger, and anticipation.\n" +
                "If an expression clearly pertains to only emotion, assign just one. If you associate two emotions with an expression," +
                "assign the most and the second-most closely associated emotion. Delimit expressions and emotions with tabs.\n" +
                "Please keep Plutchik's wheel of emotions (http://www.fractal.org/Bewustzijns-Besturings-Model/Plutchikfig6.gif)" +
                " visible while undertaking this task to assist you in differentiating the emotions. Thank you for your time!\n");

        randomWriter.println("expression\t1st emotion\t(2nd emotion)");
        for (double d : ExtensionMethods.asSortedList(randomPatternMap.keySet())) {
            randomWriter.println(randomPatternMap.get(d));
        }

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
