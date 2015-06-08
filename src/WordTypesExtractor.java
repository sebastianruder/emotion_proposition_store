import edu.jhu.agiga.AgigaSentence;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class to extract predicted word types from word_types.predicted by Tsvetkov et al.
 *
 * Created by sebastian on 19/01/15.
 */
public class WordTypesExtractor {

    /**
     * Extracts words that have been labeled with FEELING.
     * @param pathName the directory of the word_types.predicted file.
     * @throws IOException
     */
    public static void extractWordTypes(String pathName) throws IOException {

        InputStream inputStream = new FileInputStream(pathName + "word_types.predicted");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        // writer to write extracted emotions
        PrintWriter writer = new PrintWriter(pathName + "word_types.feeling", "UTF-8");

        String line = reader.readLine();
        while (line != null) {
            String[] array = line.split("\t"); // format is adjective tab emotion
            if (array[1].equals("FEELING")) {
                System.out.println(array[0]);
                writer.println(array[0]);
            }

            // read next line
            line = reader.readLine();
            writer.flush();
        }
        writer.close();
    }

    /**
     * Main method to run the word extraction.
     * @param args the input arguments
     * @throws IOException if the file is not found
     */
    public static void main(String[] args) throws IOException {
        extractWordTypes("/home/sebastian/git/sentiment_analysis/emotion_word_sources/tsvetkov_et_al./");
    }
}
