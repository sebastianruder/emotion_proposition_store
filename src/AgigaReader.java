import edu.jhu.agiga.*;
import edu.stanford.nlp.trees.*;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sebastian on 29/09/14.
 * Adapted from Eva Mujdricza-Maydt (mujdricz@cl.uni-heidelberg.de).
 *
 * A class to read annotated Gigaword documents and extract emotion-triggering expressions from them.
 */
public class AgigaReader {

    /*
    The logger used for logging
     */
    private static Logger log = Logger.getLogger(StreamingSentenceReader.class.getName());

    /*
    A boolean indicating if emotion holders and causes should be output in lemma form.
     */
    private static boolean asLemma = true;

    /*
    A boolean indicating if pronouns in the emotion holders and causes should be replaced with the most representative
    coreferent.
     */
    private static boolean replaceCoref = true;

    /*
    A boolean indicating if named entity tags should be added to the emotion holders and causes.
     */
    private static boolean addNER = true;

    /**
     * A regex for matching pronouns, this, and what in the output string.
     */
    private static String pronounRegex = String.format("(?<=^)(%s|what|this|that)(?=$)", Extensions.join((String[]) Utils.pronouns.toArray(), "|"));

    private static List<String> prepositionsThat = Arrays.asList(new String[] {"that", "of", "to", "about", "on"});


    /**
     * Main method iterating over the annotated Gigaword documents
     *
     * @param args the directory of the Gigaword files, the pattern templates file, the output directory
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        //args = new String[3];
        //args[0] = "/media/sebastian/Data";
        //args[1] = "/home/sebastian/git/sentiment_analysis/pattern_templates_2.0.txt";
        //args[2] = "/home/sebastian/git/sentiment_analysis/out";
        // java -jar sentiment.jar /media/sebastian/Data /home/sebastian/git/sentiment_analysis/pattern_templates.txt /home/sebastian/git/sentiment_analysis/out
        // nohup java -jar sentiment_java_1.6.jar /home/resources/corpora/monolingual/annotated/anno_eng_gigaword_5/data/xml/ pattern_templates_2.0.txt output/ &

        // validation of input parameters
        if (args.length != 3) {
            System.out.println("Too few or too many arguments. sentiment.jar takes exactly 3 arguments.\n" +
                    "Usage: java -jar sentiment.jar gigawordDirPath patternTemplatesFilePath outDirPath");
            System.exit(1);
        }

        File gigaDir = new File(args[0]);
        if (!gigaDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s no directory or directory doesn't exist.", args[0]));
        }

        File templatesFile = new File(args[1]);
        if (!templatesFile.exists()) {
            throw new FileNotFoundException(String.format("%s doesn't exist.", args[1]));
        }

        File outDir = new File(args[2]);
        if (!outDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is no directory or directory doesn't exist.", args[2]));
        }

        String agigaPath = gigaDir.toString();
        String outPath = outDir.toString();

        // filters .gz files
        String[] fileNames = gigaDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".gz");
            }
        });

        if (fileNames.length == 0) {
            throw new FileNotFoundException(String.format("%s doesn't contain any Agiga files.", args[0]));
        }

        // Preferences of what should be read
        AgigaPrefs readingPrefs = new AgigaPrefs(); // all extraction is set to true per default
        // modify preferences individually if needed
        readingPrefs.setColDeps(true); // collapsed dependencies
        readingPrefs.setBasicDeps(true);
        readingPrefs.setColCcprocDeps(true);
        readingPrefs.setNer(true); // adds NE annotation
        readingPrefs.setCoref(true); // adds coreference annotation

        // Get the document reader - this "entails" all the documents within the gz-compressed file
        StreamingDocumentReader agigaReader;
        log.info("Parsing XML");

        // store the emotion-triggering patterns in a map
        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap =
                emotionExtractor.extractEmotions(templatesFile, outPath);

        // map listing each pattern with the number of times it has found a successful match (experiencer + cause)
        Map<Pattern, Map<String, String>> resultMap = Stats.createResultMap(emotionMap);

        int matches = 0; // count number of successful matches (experiencer & cause have been found)
        int count = 0; // count number of sentences spanning all documents

        PrintWriter resultWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "results.txt")));
        PrintWriter collWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "collocations.txt")));

        // write headline for result writer
        resultWriter.printf("# ID\tEmotion\tPattern\tEmotion Holder\t(NP-Cause)\t(Subj S-Cause)\t(Pred S-Cause)\t(Dobj S-Cause)\t[Pobjs S-Cause]\t[BoW Cause]\n");

        for (String fileName : fileNames) {
            agigaReader = new StreamingDocumentReader(agigaPath + fileName, readingPrefs);
            // Iterate over the documents
            for (AgigaDocument doc : agigaReader) {
                List<AgigaSentence> sentences = doc.getSents();
                List<AgigaCoref> corefs = doc.getCorefs();

                // maps sentence indexes to a list of mentions and their representatives
                Map<Integer, List<Map.Entry<AgigaMention, AgigaMention>>> mentionMap = Utils.createMentionMap(corefs);

                // Iterate over the sentences
                for (AgigaSentence sent : sentences) {

                    // write stats in intervals to file
                    if (count++ % 5000000 == 0) {

                        // write the stats to a file
                        Stats.writeStats(resultMap, outPath);
                    }

                    // only retrieve one emotion trigger per sentence; if pattern is found, continue
                    boolean patternFound = false;
                    List<AgigaToken> tokens = sent.getTokens();

                    // create a lemma string with POS and indices
                    String sentence = Utils.createStringFromTokens(tokens, true, true, true, false);

                    // extract sentence root
                    Tree root = sent.getStanfordContituencyTree();

                    for (String emotion : emotionMap.keySet()) {

                        // iterate over all the patterns
                        for (Pattern pattern : emotionMap.get(emotion).keySet()) {
                            if (patternFound) {
                                break;
                            }

                            Matcher m = pattern.matcher(sentence);
                            if (m.find()) {
                                root.indexLeaves(); // index the leaves to retrieve indices
                                root.setSpans(); // set the spans to retrieve spans

                                // words look like this: ["fear/VBD/15", ...]
                                String[] patternWords = m.group(0).split(" ");

                                // get leftmost and rightmost indices of pattern; right-most in pattern can be 'that'
                                // or preposition; will dominate object and allow retrieval via constituents
                                int leftIdx = Integer.parseInt(patternWords[0].split("/")[2]);
                                int rightIdx = Integer.parseInt(patternWords[patternWords.length - 1].split("/")[2]);

                                // rightIdx for dependencies if that is present

                                int rightDepIdx = -2; // root has idx -1; no dep has idx -2
                                if (prepositionsThat.contains(tokens.get(rightIdx).getWord())) {
                                    rightDepIdx = rightIdx - 1;
                                }
                                else {
                                    rightDepIdx = rightIdx;
                                }

                                // Penn string shows phrase structure tree
//                                String pennString = root.pennString();
//                                System.out.println(pennString);

                                List<Tree> leaves = root.getLeaves();

                                // retrieve information from emotion map if order is reversed, if object is an NP
                                Boolean orderIsReversed = emotionMap.get(emotion).get(pattern).get(
                                        Enums.Features.orderIsReversed.toString());
                                // cause of emotion is either an NP or S
                                Boolean isNP = emotionMap.get(emotion).get(pattern).get(Enums.Features.isNP.toString());

                                // get NE tags
                                StringWriter stringWriter = new StringWriter();
                                sent.writeNerTags(stringWriter);
                                String[] NEtokens = stringWriter.toString().split(" ");

                                // get mention, coreferent pairs
                                int sentIdx = sent.getSentIdx();
                                List<Map.Entry<AgigaMention, AgigaMention>> mentionPairs = new ArrayList<Map.Entry<AgigaMention, AgigaMention>>();
                                if (mentionMap.containsKey(sentIdx)) {
                                    mentionPairs = mentionMap.get(sentIdx);
                                }

                                String subject = null;
                                String object = null;
                                int subjectIdx = -2;
                                int objectIdx = -2;

                                // search first in collapsed dependencies
                                // collapsed dependencies skips relative pronouns, "who", etc.
                                // basic dependencies have better information about prepositional comps
                                List<AgigaTypedDependency> basicDeps = sent.getBasicDeps();
                                List<AgigaTypedDependency> colDeps = sent.getColDeps();
                                for (AgigaTypedDependency dep : colDeps) {
                                    String type = dep.getType();
                                    if (subject == null && (dep.getGovIdx() == leftIdx || dep.getGovIdx() == rightDepIdx)
                                            && (type.equals("nsubj") || type.equals("nsubjpass"))) {
                                        subject = Utils.depToString(dep.getDepIdx(), colDeps, tokens, sentences,
                                                mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                        subjectIdx = dep.getDepIdx();
                                    }
                                    else if (object == null && isNP && dep.getGovIdx() == rightIdx &&
                                                (type.equals("dobj"))) {
                                        object = Utils.depToString(dep.getDepIdx(), colDeps, tokens, sentences,
                                                mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                        objectIdx = dep.getDepIdx();
                                    }
                                    // if dependent is a sentence part, it is either a ccomp, xcomp, or a dep in a VP
                                    else if (object == null && !isNP &&
                                            (dep.getGovIdx() == rightIdx || dep.getGovIdx() == rightDepIdx) &&
                                            (type.equals("ccomp") || type.equals("xcomp"))) {
                                        object = Utils.compToString(dep.getDepIdx(), colDeps, tokens, sentences,
                                                mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                        objectIdx = dep.getDepIdx();
                                    }
                                }

                                // if subject hasn't been found, check for conjunction or disjunction
                                if (subject == null) {
                                    for (AgigaTypedDependency dep : colDeps) {
                                        String type = dep.getType();
                                        // verb can modify subject; note: in newer version is remade as part of vmod
                                        if (type.equals("partmod") && dep.getDepIdx() == leftIdx) {
                                            subject = Utils.depToString(dep.getGovIdx(), colDeps, tokens, sentences,
                                                    mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                            subjectIdx = dep.getDepIdx();
                                        }

                                        if ((type.equals("conj_and") || type.equals("conj_or") || type.equals("conj_but"))
                                                && dep.getDepIdx() == leftIdx) {
                                            for (AgigaTypedDependency dep2 : colDeps) {
                                                if (dep2.getType().equals("nsubj") && dep.getGovIdx() == dep2.getGovIdx()) {
                                                    subject = Utils.depToString(dep2.getDepIdx(), colDeps, tokens, sentences,
                                                            mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                                    subjectIdx = dep.getDepIdx();
                                                }
                                            }
                                        }
                                    }
                                }

                                // if object hasn't been found, object can be in a dep in a VP
                                if (object == null && !isNP) {
                                    for (AgigaTypedDependency dep : colDeps) {
                                        if (dep.getType().equals("dep") &&
                                                leaves.get(dep.getDepIdx()).ancestor(2, root).label().toString().equals("VP") &&
                                                dep.getGovIdx() == rightIdx) {
                                            object = Utils.compToString(dep.getDepIdx(), colDeps, tokens, sentences,
                                                    mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                            objectIdx = dep.getDepIdx();
                                        }
                                    }
                                }

                                if (object == null) {
                                    for (AgigaTypedDependency dep : basicDeps) {
                                        String type = dep.getType();
                                        // "take pleasure in going home"; has problems with copula constructions, e.g. "take pleasure in being the brute on the floor"
                                        if (object == null && !isNP && leftIdx != rightIdx && type.equals("pcomp")) {
                                            object = Utils.compToString(dep.getDepIdx(), colDeps, tokens, sentences,
                                                    mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                            objectIdx = dep.getDepIdx();
                                        }
                                        // for prepositional objects, e.g. "proud of", "count on"
                                        else if (object == null && isNP && leftIdx != rightIdx && type.equals("pobj") &&
                                                dep.getGovIdx() == rightIdx) {
                                            object = Utils.depToString(dep.getDepIdx(), colDeps, tokens, sentences,
                                                    mentionPairs, NEtokens, true, replaceCoref, asLemma, addNER);
                                            objectIdx = dep.getDepIdx();
                                        }
                                    }
                                }

                                // skip if either subject or object weren't found
                                if (subject == null || object == null) {
                                    continue;
                                }

                                // reverse emotion holder and cause if order is reversed
                                String holder, cause;
                                Integer causeIdx;
                                if (orderIsReversed) {
                                    holder = object;
                                    cause = subject;
                                    causeIdx = subjectIdx;
                                }
                                else {
                                    holder = subject;
                                    cause = object;
                                    causeIdx = objectIdx;
                                }

                                if (cause.matches(pronounRegex)) {
                                    // exclude matches that still contain pronouns, this, or what
                                    continue;
                                }

                                StringBuilder causeBOWBuilder = new StringBuilder();
                                causeBOWBuilder.append("[");
                                causeBOWBuilder.append(Extensions.join(Utils.getBagOfWords(root, leaves.get(causeIdx), isNP ? "NP" : "S", tokens).split(" "), ", "));
                                causeBOWBuilder.append("]");
                                String causeBOW = causeBOWBuilder.toString();

                                // format cause
                                // NP-cause \ŧ Subject S-cause \t Predicate \t Object \t Pobjs
                                // NP-cause is empty if cause is S; Subject, Predicate, Object, Pobjs
                                // are empty if cause is NP
                                StringBuilder causeBuilder = new StringBuilder();
                                if (isNP) {
                                    causeBuilder.append(cause);
                                    causeBuilder.append("\t\t\t\t");
                                }
                                else {
                                    causeBuilder.append("\t");
                                    causeBuilder.append(cause);
                                }

                                String causeFormat = causeBuilder.toString();

                                // clean up pattern, remove part-of-speech and index
                                String[] cleanPatternTokens = m.group(0).split(" ");
                                StringBuilder cleanPatternBuilder = new StringBuilder();
                                for (String element : cleanPatternTokens) {
                                    cleanPatternBuilder.append(element.split("/")[0]);
                                    cleanPatternBuilder.append(" ");
                                }

                                String cleanPattern = cleanPatternBuilder.toString().trim();
                                patternFound = true;
                                resultMap.get(pattern).put(Enums.Stats.matches.toString(),
                                        String.valueOf(Integer.parseInt(resultMap.get(pattern).get(Enums.Stats.matches.toString())) + 1));
                                matches++;

                                // write clean sentence to collocations file
                                String cleanSent = Utils.createStringFromTokens(tokens, false, false, false, false);
                                String sentInfo = String.format("%s/%d\t%s", doc.getDocId(), sentIdx, cleanSent);
                                String patternInfo = String.format("#%d Emotion: '%s', pattern: '%s', emotion holder: '%s', cause: '%s', cause BoW: %s",
                                        matches, emotion, cleanPattern, holder, cause, causeBOW);

                                // write and print collocations
                                collWriter.println(sentInfo);
                                collWriter.println(patternInfo);
                                collWriter.flush();
                                System.out.println(sentInfo);
                                System.out.println(patternInfo);

                                // write output to file
                                resultWriter.printf("%s/%d\t%s\t%s\t%s\t%s\t%s\n", doc.getDocId(), sentIdx, emotion,
                                        cleanPattern, holder, causeFormat, causeBOW);
                                resultWriter.flush();
                            }
                        }
                    }
                }
            }
        }

        resultWriter.close();
        collWriter.close();
    }
}
