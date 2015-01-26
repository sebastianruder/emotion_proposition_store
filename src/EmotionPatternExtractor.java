import edu.jhu.agiga.Util;
import edu.stanford.nlp.util.ArrayMap;

import java.io.*;
import java.util.*;
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

        // writer to write the created patterns
        PrintWriter writer = new PrintWriter("patterns.txt", "UTF-8");

        // hashmap for writing patterns in a random order to measure annotator agreement
        Map<Double, String> randomPatternMap = new HashMap<Double, String>();

        String line = reader.readLine();
        while (line != null) {
            // skip comments
            if (line.startsWith("#") || line.equals("")) {
                line = reader.readLine();
                continue;
            }
            String[] lineList = line.split("\t");
            String emotionWord = lineList[0];

            // randomPatternMap.put(Math.random(), lineList[1]);

            String[] patternWords = lineList[1].split(" ");
            StringBuilder patternBuilder = new StringBuilder();

            Boolean passiveExists = Boolean.valueOf(lineList[3]);

            // pattern remembers the head, i.e. the verb or adjective so it can be later retrieved more easily
            for (String word : patternWords) {
                // make 'that' optional
                if (word.startsWith("that")) {
                    patternBuilder.append(String.format("( %s/[0-9]+)?", word));
                    continue;
                }
                // adds words without modifying that should be skipped in certain context, e.g. 'to' in 'be happy to'
                else if (word.startsWith("(?!")) {
                    patternBuilder.append(word);
                    continue;
                }
                // account for adverbs modifying adjectives; adjectives shouldn't be negated
                else if (word.equals("RB")) {
                    patternBuilder.append("(?! not)(?! never)( [a-z]+/RB/[0-9]+)?");
                    continue;
                }
                /* Stanford verb tags: VBD = past tense; VBG = present participle/gerund; VBN = past participle;
                 VBP = present tense, no 3rd person singular; VBZ = present tense, 3rd person singular
                 All tags (except past participle) are possible. */
                else if (word.endsWith("Verb")) {
                    word = word.replace("Verb", "VB[DGPZ]");
                }
                patternBuilder.append(" ");
                patternBuilder.append(word);
                patternBuilder.append("/[0-9]+");
            }

            Pattern emotionPattern = Pattern.compile(patternBuilder.toString().trim());
            // extract feature if cause is an NP or an S
            Boolean isNP = lineList[2].equals("NP") ? true : false;

            writer.println(String.format(
                    "%s\t%s\t%s", emotionWord, patternBuilder.toString().trim(), isNP ? "NP" : "S"));

            Map<String, Boolean> booleanMap = new ArrayMap<String, Boolean>();
            // puts boolean indicating if cause is an NP or not
            booleanMap.put("isNP", isNP);
            // puts boolean indicating if pattern is passive or not
            booleanMap.put("passiveExists", passiveExists);
            emotionMap.get(emotionWord).put(emotionPattern, booleanMap);

            // creates passive pattern if a passive form exists
            if (passiveExists) {
                // create pattern with 'that' and 'by'
                String passiveLemmaForm = patternWords[0].replace("Verb", "VBN");
                createPassive("that", passiveLemmaForm, emotionWord, writer, emotionMap);
                createPassive("by", passiveLemmaForm, emotionWord, writer, emotionMap);
            }
            // read next line
            line = reader.readLine();
            writer.flush();
        }
        writer.close();
        /*
        PrintWriter randomWriter = new PrintWriter("random_patterns.txt", "UTF-8");
        for (double d : asSortedList(randomPatternMap.keySet())) {
            randomWriter.println(randomPatternMap.get(d));
        }
        randomWriter.close();
        */
        return emotionMap;
    }

    // TODO: 'fear for'

    /**
     * Creates the passive form if a passive form exists. Can be created with different prepositions, though 'by'
     * takes an NP as cause, while 'that' takes an S.
     * @param writer: a writer
     * @param prep: the preposition for the pattern, at the moment either 'by' or 'that'
     * @param passiveLemmaForm: the lemma + pos tag, i.e. "frighten/VBN"
     * @param emotionWord: the emotion word
     * @param emotionMap: the emotion map
     */
    public static void createPassive(String prep, String passiveLemmaForm, String emotionWord, PrintWriter writer,
                                      Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap) {
        String pattern = String.format("be/VB[PDGZ]/([0-9]+)(?! not)(?! never)( [a-z]+/RB/[0-9]+)? %s/[0-9]+ %s/IN/[0-9]+",
                passiveLemmaForm, prep);
        Pattern emotionPattern = Pattern.compile(pattern);
        Map<String, Boolean> booleanMap = new ArrayMap<String, Boolean>();
        // if preposition is by, cause of emotion is an NP; if preposition is that, cause is a clause, i.e. S
        booleanMap.put("isNP", prep.equals("by") ? true : false);
        booleanMap.put("passiveExists", false);

        emotionMap.get(emotionWord).put(emotionPattern, booleanMap);
        writer.println(String.format("%s\t%s\t%s", emotionWord, pattern, prep.equals("by") ? "NP" : "S"));
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }
}
