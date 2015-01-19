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

    public static void extractWordTypes(String pathName) throws IOException {

        InputStream inputStream = new FileInputStream(pathName + "word_types.predicted");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        // writer to write extracted emotions
        PrintWriter writer = new PrintWriter(pathName + "word_types.feeling", "UTF-8");

        String line = reader.readLine();
        while (line != null) {
            String[] list = line.split("\t"); // format is adjective tab emotion
            if (list[1].equals("FEELING")) {
                System.out.println(list[0]);
                writer.println(list[0]);
            }

            // read next line
            line = reader.readLine();
            writer.flush();
        }
        writer.close();
    }

    public static void main(String[] args) throws IOException {
        extractWordTypes("/home/sebastian/git/sentiment_analysis/emotion_word_sources/tsvetkov_et_al./");
    }
}
