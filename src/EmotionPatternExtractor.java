import edu.stanford.nlp.util.ArrayMap;

import java.io.*;
import java.util.*;
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
        emotionMap.put(Enums.Emotions.joy.toString(), joyPatterns);
        emotionMap.put(Enums.Emotions.trust.toString(), trustPatterns);
        emotionMap.put(Enums.Emotions.fear.toString(), fearPatterns);
        emotionMap.put(Enums.Emotions.surprise.toString(), surprisePatterns);
        emotionMap.put(Enums.Emotions.sadness.toString(), sadnessPatterns);
        emotionMap.put(Enums.Emotions.disgust.toString(), disgustPatterns);
        emotionMap.put(Enums.Emotions.anger.toString(), angerPatterns);
        emotionMap.put(Enums.Emotions.anticipation.toString(), anticipationPatterns);
    }

    /**
     * Takes file containing emotion trigger expressions as input and returns an emotion map
     * storing these expressions as patterns in lists pertaining to the respective emotion.
     *
     * @param emotionTriggersFile: file path string of file containing emotion trigger expressions
     * @param outPath: output path of the files
     * @param random: if a random pattern files should be written to test inter-annotator agreement
     * @return map (key: emotion word, value: map of emotion patterns and their respective right constituent)
     * @throws IOException
     */
    public Map<String, Map<Pattern, Map<String, Boolean>>> extractEmotions(File emotionTriggersFile, String outPath, boolean random)
            throws IOException {

        InputStream inputStream = new FileInputStream(emotionTriggersFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        // writer to write the created patterns
        PrintWriter writer = new PrintWriter(outPath + "patterns.txt", "UTF-8");

        String line = reader.readLine();
        while (line != null) {
            // skip comments
            if (line.startsWith("#") || line.equals("")) {
                line = reader.readLine();
                continue;
            }
            String[] lineList = line.split("\t");
            String emotionWord = lineList[0];

            String[] patternWords = lineList[1].split(" ");
            StringBuilder patternBuilder = new StringBuilder();

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

            // look-behind for whitespace, so that words are only matched separately, e.g. not "grate" in "integrate"
            Pattern emotionPattern = Pattern.compile("(?<= )" + patternBuilder.toString().trim());
            // extract feature if cause is an NP or an S
            Boolean isNP = lineList[2].equals("NP");

            writer.printf("%s\t%s\t%s\n", emotionWord, patternBuilder.toString().trim(), isNP ? "NP" : "S");

            Map<String, Boolean> booleanMap = new ArrayMap<String, Boolean>();
            // puts boolean indicating if cause is an NP or not
            booleanMap.put(Enums.Features.isNP.toString(), isNP);

            Boolean passiveExists = Boolean.valueOf(lineList[3]);

            // puts boolean indicating if pattern is passive; always false; passive patterns with true are created
            booleanMap.put(Enums.Features.orderIsReversed.toString(), passiveExists);
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

        return emotionMap;
    }

    /**
     * Creates the passive form if a passive form exists. Can be created with different prepositions, though 'by'
     * takes an NP as cause, while 'that' takes an S.
     * @param writer: a writer
     * @param prep: the preposition for the pattern, at the moment either 'by' or 'that'
     * @param passiveLemmaForm: the lemma + pos tag, i.e. "frighten/VBN"
     * @param emotionWord: the emotion word
     * @param emotionMap: the emotion map
     */
    private static void createPassive(String prep, String passiveLemmaForm, String emotionWord, PrintWriter writer,
                                      Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap) {
        String pattern = String.format("(?<= )be/VB[PDGZ]/([0-9]+)(?! not)(?! never)( [a-z]+/RB/[0-9]+)? %s/[0-9]+ %s/IN/[0-9]+",
                passiveLemmaForm, prep);
        Pattern emotionPattern = Pattern.compile(pattern);
        Map<String, Boolean> booleanMap = new ArrayMap<String, Boolean>();
        // if preposition is by, cause of emotion is an NP; if preposition is that, cause is a clause, i.e. S
        booleanMap.put(Enums.Features.isNP.toString(), prep.equals("by"));
        booleanMap.put(Enums.Features.orderIsReversed.toString(), false);

        emotionMap.get(emotionWord).put(emotionPattern, booleanMap);
        writer.println(String.format("%s\t%s\t%s", emotionWord, pattern, prep.equals("by") ? "NP" : "S"));
    }
}
