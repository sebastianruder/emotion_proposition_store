import edu.stanford.nlp.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class to reade in a results.txt file and store the contents for further analysis.
 *
 * Created by sebastian on 17/05/15.
 */
public class ResultsReader {

    public static void main(String[] args) throws IOException {

        args = new String[] { "/home/sebastian/git/sentiment_analysis/out/results.txt" };
        readResults(args[0]);
    }

    private static void readResults(String filePath) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        int i = 0; // row count
        while (line != null && !line.equals("")) {

            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

            //# ID	Emotion	Pattern	Emotion Holder	(NP-Cause)	(Subj S-Cause)	(Pred S-Cause)	(Dobj S-Cause)	[Pobjs S-Cause]	[BoW Cause]

            String[] lineSplit = line.split("\t");
            String id = lineSplit[0];
            String emotion = lineSplit[1];
            String pattern = lineSplit[2];
            String emotionHolder = lineSplit[3];
            String NPCause = lineSplit[4];
            String subjSCause = lineSplit[5];
            String predSCause = lineSplit[6];
            String dobjSCause = lineSplit[7];
            String[] pobjs = lineSplit[8].replaceAll("^\\[|\\]$", "").split(", ");
            String[] causeBoW = lineSplit[9].replaceAll("^\\[|\\]$", "").split(", ");
            line = reader.readLine();
        }
    }
}
