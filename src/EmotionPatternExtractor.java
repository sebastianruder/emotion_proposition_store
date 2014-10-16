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

    Map<Pattern, Map<String, Boolean>> joyPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> trustPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> fearPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> surprisePatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> sadnessPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> disgustPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> angerPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<Pattern, Map<String, Boolean>> anticipationPatterns = new ArrayMap<Pattern, Map<String, Boolean>>();
    Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap = new ArrayMap<String, Map<Pattern, Map<String, Boolean>>>();

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
    public Map<String, Map<Pattern, Map<String, Boolean>>> extractEmotions(String emotionTriggersFile) throws IOException {

        InputStream inputStream = new FileInputStream(emotionTriggersFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        String line = reader.readLine();
        while (line != null) {
            // skip comments
            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }
            String[] lineList = line.split("\t");
            String emotionWord = lineList[0];
            String[] patternWords = lineList[1].split(" ");
            StringBuilder patternBuilder = new StringBuilder();

            Boolean passiveExists = Boolean.valueOf(lineList[3]);

            // pattern remembers the head, i.e. the verb so it can be later retrieved more easily
            int count = 0;
            for (String word : patternWords) {
                // make 'that' optional TODO: for some necessary, check generalisability
                if (word.startsWith("that")) {
                    patternBuilder.append(String.format("( %s/[0-9]+)?", word));
                    continue;
                }
                if (word.startsWith("(")) {
                    patternBuilder.append(word);
                    continue;
                }
                if (count > 0) {
                    patternBuilder.append(" ");
                }
                patternBuilder.append(word);
                // for (?!to)
                if (word.endsWith("VBP")) {
                    patternBuilder.append("/([0-9]+)");
                }
                else {
                    patternBuilder.append("/[0-9]+");
                }
                count++;
            }

            Pattern emotionPattern = Pattern.compile(patternBuilder.toString());
            Boolean isNP = lineList[2].equals("NP") ? true : false;

            Map<String, Boolean> booleanMap = new ArrayMap<String, Boolean>();
            // puts boolean indicating if cause is an NP or not
            booleanMap.put("isNP", isNP);
            // puts boolean indicating if pattern is passive or not
            booleanMap.put("passiveExists", passiveExists);

            emotionMap.get(emotionWord).put(emotionPattern, booleanMap);

            // creates passive pattern if a passive form exists
            if (passiveExists) {
                // creates pattern with 'by'
                String passiveLemmaForm = patternWords[0].replace("/VBP", "/VBN");
                emotionPattern = Pattern.compile(String.format("be/VBP/([0-9]+) %s/[0-9]+ by/IN/[0-9]+",
                        passiveLemmaForm));
                booleanMap = new ArrayMap<String, Boolean>();
                booleanMap.put("isNP", true);
                booleanMap.put("passiveExists", false);
                emotionMap.get(emotionWord).put(emotionPattern, booleanMap);
                // creates pattern with 'that'; cause is 'S'
                booleanMap.put("isNP", false);
                emotionPattern = Pattern.compile(String.format("be/VBP/([0-9]+) %s/[0-9]+( that/IN/[0-9]+)?",
                        passiveLemmaForm));
                emotionMap.get(emotionWord).put(emotionPattern, booleanMap);
            }
            // read next line
            line = reader.readLine();
        }
        return emotionMap;
    }
}
