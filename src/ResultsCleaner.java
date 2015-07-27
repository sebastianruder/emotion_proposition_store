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

        String dir = "/home/sebastian/git/sentiment_analysis/out/";

        args = new String[] { Utils.combine(dir, "results_final/collocations.txt") };
        // removeDuplicates(args[0]);
        //List<String> idList = removeDuplicatesWithCollocations(args[0]);
        //writeWithoutDuplicates(Utils.combine(dir, "results.txt"), Utils.combine(dir, "results_cleaned.txt"), idList);
        removePatterns(Utils.combine(dir, "results_cleaned.txt"), Utils.combine(dir, "results_cleaned_removed.txt"));
    }

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

    private static List<String> removeDuplicatesWithCollocations(String filePath) throws IOException {

        Map<String, String> sentenceIdMap = new HashMap<String, String>();

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        while (line != null && !line.equals("")) {

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
