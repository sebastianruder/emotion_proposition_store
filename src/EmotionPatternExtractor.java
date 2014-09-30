import edu.stanford.nlp.util.ArrayMap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EmotionPatternExtractor extracts emotion triggering expressions from a file and stores these as patterns in a list
 * pertaining to the respective emotions which are stored in a map for better retrieval
 */
public class EmotionPatternExtractor {

    List<Pattern> joyPatterns = new ArrayList<Pattern>();
    List<Pattern> trustPatterns = new ArrayList<Pattern>();
    List<Pattern> fearPatterns = new ArrayList<Pattern>();
    List<Pattern> surprisePatterns = new ArrayList<Pattern>();
    List<Pattern> sadnessPatterns = new ArrayList<Pattern>();
    List<Pattern> disgustPatterns = new ArrayList<Pattern>();
    List<Pattern> angerPatterns = new ArrayList<Pattern>();
    List<Pattern> anticipationPatterns = new ArrayList<Pattern>();
    // TODO: look up how to initialize list
    List<List<Pattern>> patternList = new ArrayList<List<Pattern>>();
    Map<String, List<Pattern>> emotionMap = new ArrayMap<String, List<Pattern>>();

    /**
     * Initializes emotion map storing patterns pertaining to Plutchik's eight basic emotions
     */
    public EmotionPatternExtractor() {
        emotionMap.put("joy", joyPatterns);
        emotionMap.put("trust", trustPatterns);
        emotionMap.put("fear", fearPatterns);
        emotionMap.put("surprise", surprisePatterns);
        emotionMap.put("sadness", sadnessPatterns);
        emotionMap.put("disgust", disgustPatterns);
        emotionMap.put("anger", angerPatterns);
        emotionMap.put("anticipation", anticipationPatterns);
    }

    /**
     * Takes file containing emotion trigger expressions as input and returns an emotion map
     * storing these expressions as patterns in lists pertaining to the respective emotion.
     *
     * @param emotionTriggersFile: file path string of file containing emotion trigger expressions
     * @return map (key: emotion word, value: list of patterns pertaining to this emotion)
     * @throws IOException
     */
    public Map<String, List<Pattern>> extractEmotions(String emotionTriggersFile) throws IOException {

        InputStream inputStream = new FileInputStream(emotionTriggersFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line = reader.readLine();
        while (line != null || line.equals("")) {
            String emotionWord = line.split("\t")[0];
            Pattern emotionPattern = Pattern.compile(line.split("\t")[1]);
            emotionMap.get(emotionWord).add(emotionPattern);
            System.out.println(line);

            // read next line
            line = reader.readLine();
        }
        return emotionMap;
    }

    public static void main(String[] args) throws IOException {

        // TODO: put this workflow in main of AgigaReader
        String filePath = "/home/sebastian/git/sentiment_analysis/emotion_trigger_patterns.txt";
        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, List<Pattern>> map = emotionExtractor.extractEmotions(filePath);
        Matcher m;
        for (List<Pattern> patterns : map.values()) {
            for (Pattern pattern : patterns) {
                m = pattern.matcher("sentence");
                while (m.find()) {
                    // TODO: do something
                    System.out.println(String.format("Group 1: %s, group 2: %s", m.group(1), m.group(2)));
                    // note: m.group(0) is whole string
                }
            }
        }
    }
}
