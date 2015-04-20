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
 * A class to read annotated gigaword documents and extract emotion-triggering expressions from them
 */
public class AgigaReader {

    // initialize logger for logging, duh
    private static Logger log = Logger.getLogger(StreamingSentenceReader.class.getName());

    private static boolean asLemma = true;

    private static boolean replaceCoref = true;

    private static boolean addNER = true;

    /**
     * Main method iterating over the annotated gigaword documents
     *
     * @param args input parameters
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        args = new String[3];
        args[0] = "/media/sebastian/Data";
        args[1] = "/home/sebastian/git/sentiment_analysis/pattern_templates.txt";
        args[2] = "/home/sebastian/git/sentiment_analysis/out";
        // java -jar sentiment.jar /media/sebastian/Data /home/sebastian/git/sentiment_analysis/pattern_templates.txt /home/sebastian/git/sentiment_analysis/out

        if (args.length != 3) {
            System.out.println("Too few or too many arguments. sentiment.jar takes exactly 3 arguments.\n" +
                    "Usage: java -jar sentiment.jar gigawordDirPath patternTemplatesFilePath outDirPath");
            System.exit(1);
        }

        File gigaDir = new File(args[0]);
        if (!gigaDir.isDirectory()) {
            System.out.printf("%s no directory or directory doesn't exist.", args[0]);
            System.exit(1);
        }

        File templatesFile = new File(args[1]);
        if (!templatesFile.exists()) {
            System.out.printf("%s doesn't exist.", args[1]);
            System.exit(1);
        }

        File outDir = new File(args[2]);
        if (!outDir.isDirectory()) {
            System.out.printf("%s is no directory or directory doesn't exist.", args[2]);
            System.exit(1);
        }

        // append / to file names if missing
        String agigaPath = args[0].endsWith("/") ? args[0] : args[0] + "/";
        String outPath = args[2].endsWith("/") ? args[2] : args[2] + "/";

        // filters .gz files
        String[] fileNames = gigaDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".gz");
            }
        });

        // Preferences of what should be read
        AgigaPrefs readingPrefs = new AgigaPrefs(); // all extraction is set to true per default
        // modify preferences individually if needed
        readingPrefs.setColDeps(true);
        readingPrefs.setBasicDeps(true);
        readingPrefs.setColCcprocDeps(true);
        readingPrefs.setNer(true);
        readingPrefs.setCoref(true);

        // Get the document reader - this "entails" all the documents within the gz-compressed file
        StreamingDocumentReader agigaReader;
        log.info("Parsing XML");

        // store the emotion-triggering patterns in a map
        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap =
                emotionExtractor.extractEmotions(templatesFile, outPath, true);

        // map listing each pattern with the number of times it has found a successful match (experiencer + cause)
        Map<Pattern, Map<String, String>> resultMap = Stats.createResultMap(emotionMap);

        int matches = 0; // count number of successful matches (experiencer & cause have been found)
        int count = 0; // count number of sentences spanning all documents

        // check if writers need to be closed or if wrapping in bufferedwriter is sufficient
        PrintWriter resultWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "results.txt")));
        PrintWriter collWriter = new PrintWriter(new BufferedWriter(new FileWriter(outPath + "collocations.txt")));

        for (String fileName : fileNames) {
            agigaReader = new StreamingDocumentReader(agigaPath + fileName, readingPrefs);
            // Iterate over the documents
            for (AgigaDocument doc : agigaReader) {
                List<AgigaSentence> sentences = doc.getSents();
                List<AgigaCoref> corefs = doc.getCorefs();

                // maps sentence indexes to a list of mentions and their representatives
                Map<Integer, List<Map.Entry<AgigaMention, AgigaMention>>> mentionMap = TreeTokenUtils.createMentionMap(corefs);

                // Iterate over the sentences
                for (AgigaSentence sent : sentences) {
                    if (count >= 2000000) {

                        // write the stats to a file
                        Stats.writeStats(resultMap, outPath);

                        resultWriter.close();
                        collWriter.close();
                        System.exit(0);
                    }

                    // TODO use JWI to interface in WorNet for majority sense / hyponym identification

                    count++;
                    // only retrieve one emotion trigger per sentence; if pattern is found, continue
                    boolean patternFound = false;
                    List<AgigaToken> tokens = sent.getTokens();

                    // create a lemma string with pos and indices
                    String sentence = TreeTokenUtils.createStringFromTokens(tokens, true, true, true);

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

                                // index the leaves to retrieve indices
                                root.indexLeaves();
                                // set the spans to retrieve spans
                                root.setSpans();

                                // words look like this: ["fear/VBD/15", ...]
                                String[] patternWords = m.group(0).split(" ");

                                // get leftmost and rightmost indices of pattern
                                int leftIdx = Integer.parseInt(patternWords[0].split("/")[2]);
                                int rightIdx = Integer.parseInt(patternWords[patternWords.length - 1].split("/")[2]);

                                // Penn string shows phrase structure tree
//                                String pennString = root.pennString();
//                                System.out.println(pennString);

                                List<Tree> leaves = root.getLeaves();
                                Tree leftNode = leaves.get(leftIdx);
                                Tree rightNode = leaves.get(rightIdx);

                                // pattern can have a passive form; if so, experiencer and cause are reversed
                                Boolean orderIsReversed = emotionMap.get(emotion).get(pattern).get(
                                        Enums.Features.orderIsReversed.toString());
                                // cause of emotion is either an NP or S
                                Boolean isNP = emotionMap.get(emotion).get(pattern).get(Enums.Features.isNP.toString());

                                Tree subjectNode = null;
                                Tree objectNode = null;
                                // search first in collapsed dependencies
                                List<AgigaTypedDependency> colDeps = sent.getColDeps();
                                for (AgigaTypedDependency dep : colDeps) {
//                                    if (!dep.getType().equals("root")) {
//                                        System.out.printf("Dep type: %s, gov: %s, dep: %s\n",
//                                                dep.getType(), tokens.get(dep.getGovIdx()).getWord(), tokens.get(dep.getDepIdx()).getWord());
//                                    }

                                    if ((dep.getType().equals("nsubj") || dep.getType().equals("nsubjpass"))&& dep.getGovIdx() == leftIdx) {
                                        Tree nsubjNode = leaves.get(dep.getDepIdx());
                                        if (nsubjNode.ancestor(3, root).label().toString().equals("NP")) {
                                            subjectNode = nsubjNode.ancestor(3, root);
                                        }
                                        else {
                                            subjectNode = nsubjNode.ancestor(2, root);
                                        }
                                        System.out.println("Nsubj: " + subjectNode);
                                    }
                                    else if (isNP && (dep.getType().equals("dobj") || dep.getType().equals("pobj")) && dep.getGovIdx() == rightIdx) {
                                        Tree dobjNode = leaves.get(dep.getDepIdx());
                                        if (dobjNode.ancestor(3, root).label().toString().equals("NP")) {
                                            objectNode = dobjNode.ancestor(3, root);
                                        }
                                        else {
                                            objectNode = dobjNode.ancestor(2, root);
                                        }

                                        System.out.println("Dobj: " + objectNode);
                                    }
                                    else if (!isNP && (dep.getType().equals("ccomp") || dep.getType().equals("xcomp")) && dep.getGovIdx() == rightIdx) {
                                        Tree compNode = leaves.get(dep.getDepIdx());
                                        try {
                                            for (int i = 2; i < 6; i++) {
                                                Tree ancestor = compNode.ancestor(i, root);
                                                if (ancestor.label().toString().equals("SBAR") || ancestor.label().toString().equals("S")) {
                                                    objectNode = compNode.ancestor(i, root);
                                                    System.out.println("Comp: " + objectNode);
                                                    break;
                                                }

                                            }
                                        }
                                        catch (NullPointerException ex) {
                                        }
                                    }
                                }

                                if (subjectNode == null) {
                                    subjectNode = TreeTokenUtils.findHolderOrCause(root, leftNode, "NP", true, null);
                                }

                                if (objectNode == null) {
                                    objectNode = TreeTokenUtils.findHolderOrCause(root, rightNode, isNP ? "NP" : "S", false, subjectNode);
                                }

                                Tree holderNode, causeNode;

                                if (orderIsReversed) {
                                    holderNode = objectNode;
                                    causeNode = subjectNode;
                                }
                                else {
                                    holderNode = subjectNode;
                                    causeNode = objectNode;
                                }

//                                // order is reversed, i.e. cause of emotion is subject (i.e. fascinate, anger, rile, etc.)
//                                if (orderIsReversed) {
//                                    causeNode = TreeTokenUtils.findHolderOrCause(root, leftNode, "NP", true, null);
//                                    holderNode = TreeTokenUtils.findHolderOrCause(root, rightNode, isNP ? "NP" : "S", false, causeNode);
//                                }
//                                // if order is normal, experiencer is subject, cause is dependent
//                                else {
//                                    holderNode = TreeTokenUtils.findHolderOrCause(root, leftNode, "NP", true, null);
//                                    causeNode = TreeTokenUtils.findHolderOrCause(root, rightNode, isNP ? "NP" : "S", false, holderNode);
//                                }

                                if (holderNode != null && causeNode != null) {

                                    StringWriter stringWriter = new StringWriter();
                                    sent.writeNerTags(stringWriter);
                                    String[] NEtokens = stringWriter.toString().split(" ");

                                    int sentIdx = sent.getSentIdx();
                                    List<Map.Entry<AgigaMention, AgigaMention>> mentionPairs = new ArrayList<Map.Entry<AgigaMention, AgigaMention>>();
                                    if (mentionMap.containsKey(sentIdx)) {
                                        mentionPairs = mentionMap.get(sentIdx);
                                    }

                                    String holder = TreeTokenUtils.getStringFromSpan(holderNode, tokens, sentences, mentionPairs, NEtokens, asLemma, replaceCoref, addNER);
                                    String cause = TreeTokenUtils.getStringFromSpan(causeNode, tokens, sentences, mentionPairs, NEtokens, asLemma, replaceCoref, addNER);

                                    // clean pattern
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
                                    String cleanSent = TreeTokenUtils.createStringFromTokens(tokens, false, false, false);
                                    String sentInfo = String.format("%s/%d\t%s", doc.getDocId(), sentIdx, cleanSent);
                                    String patternInfo = String.format("#%d Emotion: '%s', pattern: '%s', emotion holder: '%s', cause: '%s'",
                                            matches, emotion, cleanPattern, holder, cause);

                                    // write and print collocations
                                    collWriter.println(sentInfo);
                                    collWriter.println(patternInfo);
                                    collWriter.flush();

                                    System.out.println(sentInfo);
                                    System.out.println(patternInfo);

                                    // write output to file
                                    resultWriter.printf("%s/%d\t%s\t%s\t%s\t%s\n", doc.getDocId(), sentIdx, emotion,
                                            cleanPattern, holder, cause);
                                    resultWriter.flush();
                                }
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