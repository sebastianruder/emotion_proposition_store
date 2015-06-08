import java.io.*;
import java.util.*;

/**
 * Class to remove duplicates from results, i.e. extractions that are equal except for their id.
 * Erroneously removes a lot of entries; should be refined or omitted.
 *
 * Created by sebastian on 21/05/15.
 */
public class ResultsCleaner {

    /**
     * Main method for removing duplicates.
     * @param args the file whose duplicates should be removed
     * @throws IOException if file is not found
     */
    public static void main(String[] args) throws IOException {

        args = new String[] { "/home/sebastian/git/sentiment_analysis/out/results_all.txt" };
        removeDuplicates(args[0]);
    }

    /**
     * Removes duplicate extractions from a file and overwrites the file. Stores all extractions in a hashmap so that
     * extractions which are equal in everything but their id are removed.
     * @param filePath the path to the extraction file
     * @throws IOException if the file is not found
     */
    private static void removeDuplicates(String filePath) throws IOException {

        Map<Extraction, Integer> extractionMap = new HashMap<Extraction, Integer>();

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        int i = 0; // row count
        while (line != null && !line.equals("")) {

            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

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
                extractionMap.put(extraction, i++);
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException(String.format("Format error in line %d: %s", i - 1, line));
            }

            line = reader.readLine();
        }

        reader.close();
        extractionMap = Extensions.sortByValue(extractionMap, false);
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));

        for (Extraction ex: extractionMap.keySet()) {
            writer.print(ex.toString());
        }

        writer.close();
    }
}
