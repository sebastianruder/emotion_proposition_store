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

    Map<Pattern, String> joyPatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> trustPatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> fearPatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> surprisePatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> sadnessPatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> disgustPatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> angerPatterns = new ArrayMap<Pattern, String>();
    Map<Pattern, String> anticipationPatterns = new ArrayMap<Pattern, String>();
    // TODO: look up how to initialize list
    List<List<Pattern>> patternList = new ArrayList<List<Pattern>>();
    Map<String, Map<Pattern, String>> emotionMap = new ArrayMap<String, Map<Pattern, String>>();

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
     * @return map (key: emotion word, value: map of emotion patterns and their respective right constituent)
     * @throws IOException
     */
    public Map<String, Map<Pattern, String>> extractEmotions(String emotionTriggersFile) throws IOException {

        InputStream inputStream = new FileInputStream(emotionTriggersFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line = reader.readLine();
        while (line != null) {
            String emotionWord = line.split("\t")[0];
            Pattern emotionPattern = Pattern.compile(line.split("\t")[1]);
            String rightConstituent = line.split("\t")[2];
            emotionMap.get(emotionWord).put(emotionPattern, rightConstituent);

            // read next line
            line = reader.readLine();
        }
        return emotionMap;
    }

    public static void main(String[] args) throws IOException {

        // TODO: put this workflow in main of AgigaReader
        String filePath = "/home/sebastian/git/sentiment_analysis/emotion_trigger_patterns.txt";
        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, Map<Pattern, String>> map = emotionExtractor.extractEmotions(filePath);
        Matcher m;
        for (Map<Pattern, String> emotionMap: map.values()) {
            for (Pattern pattern : emotionMap.keySet()) {
                String rightConstituent = emotionMap.get(pattern);
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
