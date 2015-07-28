import java.io.*;
import java.util.*;

/**
 * Class to remove duplicates from results, i.e. extractions that are equal except for their id.
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

        String dir = "/home/sebastian/git/sentiment_analysis/out/";

        args = new String[] { Utils.combine(dir, "results_final/collocations.txt") };
        //List<String> idList = removeDuplicatesWithCollocations(args[0]);
        //writeWithoutDuplicates(Utils.combine(dir, "results.txt"), Utils.combine(dir, "results_cleaned.txt"), idList);
        removePatterns(Utils.combine(dir, "results_cleaned.txt"), Utils.combine(dir, "results_cleaned_removed.txt"));
    }

    /**
     * Remove patterns that have been identified as erroneous.
     * @param oldFilePath the path to the old results file
     * @param newFilePath the path to the new results file
     * @throws IOException
     */
    private static void removePatterns(String oldFilePath, String newFilePath) throws IOException {

        List<String> patterns = new ArrayList<String> (Arrays.asList("depress", "aggravate", "rattle", "afflict", "inflame"));

        // clean-up file before adding to it
        File file = new File(newFilePath);
        file.delete();
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(newFilePath, true)));

        BufferedReader reader = new BufferedReader(new FileReader(oldFilePath));
        String line = reader.readLine();
        int i = 0;
        while (line != null && !line.equals("")) {

            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

            String pattern = line.split("\t")[2];
            boolean containsPattern = false;
            for (String removePattern : patterns) {
                if (Arrays.asList(pattern.split(" ")).contains(removePattern)) {
                    containsPattern = true;
                }
            }

            if (!containsPattern) {
                writer.println(line);
                if (++i % 1000 == 0) {
                    System.out.println("Line #" + i);
                }
            }

            line = reader.readLine();
        }

        writer.close();
    }

    /**
     * Writes the propositions corresponding to a list of ids with unique propositions to a file.
     * @param oldFilePath the path to the old results file
     * @param newFilePath the path to the new results file
     * @param idList the list of ids with unique propositions
     * @throws IOException
     */
    private static void writeWithoutDuplicates(String oldFilePath, String newFilePath, List<String> idList) throws IOException {

        // clean-up file before adding to it
        File file = new File(newFilePath);
        file.delete();
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(newFilePath, true)));

        BufferedReader reader = new BufferedReader(new FileReader(oldFilePath));
        String line = reader.readLine();
        int i = 0;
        while (line != null && !line.equals("")) {

            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

            String id = line.split("\t")[0];
            if (idList.contains(id)) {
                writer.println(line);
                if (++i % 1000 == 0) {
                    System.out.println("Line #" + ++i);
                }
            }

            line = reader.readLine();
        }

        writer.close();
    }

    /**
     * Uses the collocations to identify unique propositions. Stores their ids in a list of strings.
     * @param filePath the path to the collocations file
     * @return a list of ids of unique propositions
     * @throws IOException
     */
    private static List<String> removeDuplicatesWithCollocations(String filePath) throws IOException {

        Map<String, String> sentenceIdMap = new HashMap<String, String>();

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        while (line != null && !line.equals("")) {

            // skip lines that don't list the id and the sentence, e.g. #1 Emotion: 'trust' ...
            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

            String id = line.split("\t")[0];
            try {
                String sentence = line.split("\t")[1];
                sentenceIdMap.put(sentence, id);
            }
            catch (ArrayIndexOutOfBoundsException ex){
                // there is one erroneous line without an id; skip that one
            }

            line = reader.readLine();
        }

        List<String> idList = new ArrayList<String>();
        for (String sentence : sentenceIdMap.keySet()) {
            idList.add(sentenceIdMap.get(sentence));
        }

        return idList;
    }
}
