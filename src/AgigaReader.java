import edu.jhu.agiga.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.IntPair;

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

    /**
     * Main method iterating over the annotated gigaword documents
     * @param args input parameters
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {


        args = new String[3];
        args[0] = "/media/sebastian/Data";
        args[1] = "/home/sebastian/git/sentiment_analysis/pattern_templates.txt";
        args[2] = "/home/sebastian/git/sentiment_analysis";
        // java -jar sentiment.jar /media/sebastian/Data /home/sebastian/git/sentiment_analysis/pattern_templates.txt /home/sebastian/git/sentiment_analysis


        if (args.length != 3) {
            System.out.println("Too few or too many arguments. sentiment.jar takes exactly 3 arguments.\n" +
                    "Usage: java -jar sentiment.jar gigawordDirPath patternTemplatesFilePath outDirPath");
            System.exit(1);
        }

        File gigaDir = new File(args[0]);
        if (!gigaDir.isDirectory()) {
            System.out.println(String.format("{0} is no directory or directory doesn't exist.", args[0]));
            System.exit(1);
        }

        File templatesFile = new File(args[1]);
        if (!templatesFile.exists()) {
            System.out.println(String.format("{0} doesn't exist.", args[1]));
            System.exit(1);
        }

        File outDir = new File(args[2]);
        if (!outDir.isDirectory()) {
            System.out.println(String.format("{0} is no directory or directory doesn't exist.", args[2]));
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
        readingPrefs.setBasicDeps(false);
        readingPrefs.setColCcprocDeps(false);

        // Get the document reader - this "entails" all the documents within the gz-compressed file
        StreamingDocumentReader agigaReader;
        log.info("Parsing XML");

        // store the emotion-triggering patterns in a map

        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, Map<Pattern, Map<String, Boolean>>> emotionMap =
                emotionExtractor.extractEmotions(templatesFile, false);

        // map listing each pattern with the number of times it has found a successful match (experiencer + cause)
        Map<Pattern, Map<String, Integer>> resultMap = Stats.createResultMap(emotionMap);

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

                // Iterate over the sentences
                for (AgigaSentence sent : sentences) {
                    if (count >= 2000000) {

                        // write the stats to a file
                        Stats.writeStats(resultMap, outPath + "stats.txt");

                        resultWriter.close();
                        collWriter.close();
                        System.exit(0);
                    }

                    count++;
                    // only retrieve one emotion trigger per sentence; if pattern is found, continue
                    boolean patternFound = false;

                    List<AgigaToken> tokens = sent.getTokens();

                    // create a lemma string with pos and indices
                    String sentence = createStringFromTokens(tokens, true, true, true);

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
                                // counts occurrences
                                resultMap.get(pattern).put(Enums.Stats.occurrences.toString(),
                                        resultMap.get(pattern).get(Enums.Stats.occurrences.toString()) + 1);
                                //System.out.println(String.format("#%d: %s", count, sentence));

                                // index the leaves to retrieve indices
                                root.indexLeaves();
                                // set the spans to retrieve spans
                                root.setSpans();

                                // words look like this: ["fear/VBD/15", ...]
                                String[] patternWords = m.group(0).split(" ");

                                // get leftmost and rightmost indices of pattern
                                int leftIdx = Integer.parseInt(patternWords[0].split("/")[2]);
                                int rightIdx = Integer.parseInt(patternWords[patternWords.length - 1].split("/")[2]);
                            /*System.out.println(String.format("Pattern found: %s, constituent: %s",
                                    m.group(0), map.get(emotion).get(pattern).get("isNP") ? "NP" : "S"));*/

                                // Penn string shows phrase structure tree
                                // String pennString = root.pennString();
                                // System.out.println(pennString);

                                List<Tree> leaves = root.getLeaves();
                                Tree leftNode = leaves.get(leftIdx);
                                Tree rightNode = leaves.get(rightIdx);
                            /*
                            System.out.println(String.format("Left node (%s): %s; right node (%s): %s",
                                    leftNode.ancestor(1, root).getSpan(), leftNode.toString(),
                                    rightNode.ancestor(1, root).getSpan(), rightNode.toString()));
                            */
                                // pattern can have a passive form; if so, experiencer and cause are reversed
                                Boolean passiveExists = emotionMap.get(emotion).get(pattern).get(
                                        Enums.Features.passiveExists.toString());
                                // cause of emotion is either an NP or S
                                Boolean isNP = emotionMap.get(emotion).get(pattern).get(Enums.Features.isNP.toString());

                                String experiencer, cause;
                                // if a passive form exists, experiencer is dependent, cause is subject
                                if (passiveExists) {
                                    experiencer = findExperiencerOrCause(root, rightNode, isNP ? "NP" : "S", tokens,
                                            false, true, true);
                                    cause = findExperiencerOrCause(root, leftNode, "NP", tokens, true, true, true);
                                }
                                // if no passive form exists, experiencer is subject, cause is dependent
                                else {
                                    experiencer = findExperiencerOrCause(root, leftNode, "NP", tokens, true, true, true);
                                    cause = findExperiencerOrCause(root, rightNode, isNP ? "NP" : "S", tokens, false,
                                            true, true);
                                }

                                if (experiencer != null && cause != null) {
                                    patternFound = true;
                                    resultMap.get(pattern).put(Enums.Stats.matches.toString(),
                                            resultMap.get(pattern).get(Enums.Stats.matches.toString()) + 1);
                                    matches++;

                                    // write clean sentence to collocations file
                                    String cleanSent = createStringFromTokens(tokens, false, false, false);
                                    collWriter.println(String.format("%d\t%s", count, cleanSent));
                                    collWriter.flush();

                                    System.out.println(String.format("%d\t%s", count, cleanSent));
                                    System.out.println(String.format("#%d.%d/%d Emotion: '%s', " +
                                                    "experiencer: '%s', cause: '%s'",
                                            matches, resultMap.get(pattern).get(Enums.Stats.matches.toString()),
                                            resultMap.get(pattern).get(
                                                    Enums.Stats.occurrences.toString()), emotion, experiencer, cause));

                                        /* write output to file; output is of the form:
                                        sentence number tab pattern tab pattern matched tab number of matches tab
                                        number of occurrences tab emotion tab experiencer tab cause
                                        (number of total matches is line number)*/
                                    resultWriter.println(String.format("%d\t%s\t%d\t%d\t%s\t%s\t%s",
                                            count, m.group(0), resultMap.get(pattern).get(Enums.Stats.matches.toString()),
                                            resultMap.get(pattern).get(Enums.Stats.matches.toString()), emotion, experiencer, cause));
                                    resultWriter.flush();
                                }
                                // System.out.println();
                            }
                        }
                    }
                }
            }
        }
        resultWriter.close();
        collWriter.close();
    }

    /**
     * Create a string from a list of tokens. Tokens are separated by whitespace; part-of-speech and index are
     * separated by a slash from tokens.
     * @param tokens a list of Agiga tokens
     * @param lemma a boolean indicating if tokens should be outputted as lemmas
     * @param pos a boolean indicating if part-of-speech should be added
     * @param idx a boolean indicating if indexes should be added
     * @return the concatenated token string
     */
    public static String createStringFromTokens(List<AgigaToken> tokens, boolean lemma, boolean pos, boolean idx) {

        StringBuilder sb = new StringBuilder();

        for (AgigaToken tok : tokens) {
            String lemmaString = tok.getLemma();
            String posString = tok.getPosTag();
            int idxInt = tok.getTokIdx();

            if (lemma) { sb.append(lemmaString); }
            else { sb.append(tok.getWord()); }
            if (pos) { sb.append("/"); sb.append(posString); }
            if (idx) { sb.append("/"); sb.append(idxInt); }
            sb.append(" ");
        }

        return sb.toString().trim();
    }

    /**
     * Retrieves span of first sibling/cousin node having a given label on either side of a leaf node.
     * Performs a cut at a height of 4 (height of leaf is 1).
     * @param root: root node of tree
     * @param leaf: leaf node whose sibling should be retrieved
     * @param label: label of wanted node
     * @param tokens: a list of the tokens of the sentence
     * @param left: if node should be looked for on the left side of the sibling node
     * @param asLemma: if the experiencer or cause should be returned as a lemma string
     * @param withoutPP: if PPs and SBARs should be removed
     * @return a list containing the start index and the end index of the span of the wanted node
     */
    public static String findExperiencerOrCause(Tree root, Tree leaf, String label, List<AgigaToken> tokens,
                                                          boolean left, boolean asLemma, boolean withoutPP) {
        Tree child;
        // at height 1 from leaf node is preterminal, at height 2 is the first ancestor, set maximum height to 4
        for (int height = 2; height <= 4; height++)
            try {
                // iterate over the children, start with the second child if looking on the right
                for (int i = left ? 0 : 1; i < leaf.ancestor(height, root).getChildrenAsList().size(); i++) {
                    child = leaf.ancestor(height, root).getChild(i);
                    // break so as not to look to the right side of the leaf when searching left
                    if (child.getLeaves().contains((leaf))) {
                        break;
                    }
                    /*System.out.println(String.format("%s child: %s, ancestor: %s" +
                            "",left ? "left" : "right", child, leaf.ancestor(height, root)));*/
                    // label of child should equal label; SBAR label is also frequent
                    if (child.label().toString().equals(label) || child.label().toString().equals(label + "BAR")) {
                        IntPair span = child.getSpan();

                        // lists of start indixes and end index of PPs and SBARs which should
                        List<Integer> startIdxExcludeList = new ArrayList<Integer>();
                        List<Integer> endIdxExcludeList = new ArrayList<Integer>();
                        if (withoutPP) {
                            preorderTraverse(child, root, startIdxExcludeList, endIdxExcludeList);
                        }
                        // building the experiencer or cause string, as lemma
                        return buildStringFromSpan(tokens, span.getSource(), span.getTarget(), asLemma,
                                startIdxExcludeList, endIdxExcludeList);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                log.info(String.format("Node %s has no child.", leaf.ancestor(height, root)));
            } catch (NullPointerException exception) {
                log.info(String.format("Ancestor is null."));
            }
        return null;
    }

    // TODO: cut off subordinate sentences based on length

    /**
     * Performs preorder-traversal, i.e. depth-first search from a node. Extracts PPs and SBARs that are dominated
     * by the node. Nodes dominated by PPs or SBARs are skipped, so that PPs and SBARs don't overlap.
     * @param node the start node from which traversal shoud be performed
     * @param root the root node of the tree
     * @param startIdxList list which stores start indexes of PPs and SBARs
     * @param endIdxList list which stores end indexes of PPs and SBARs
     */
    public static void preorderTraverse(Tree node, Tree root, List<Integer> startIdxList, List<Integer> endIdxList) {
        if (!node.isPreTerminal()) {
            for (Tree child : node.getChildrenAsList()) {
                // extracts PPs and SBARs
                String label = child.label().value();
                if (label.equals("PP") || label.equals("SBAR")) {
                    // System.out.println(child.nodeNumber(root) + ": " + child.getSpan() + ": " + child.toString());
                    startIdxList.add(child.getSpan().getSource());
                    endIdxList.add(child.getSpan().getTarget());
                }
                else {
                    preorderTraverse(child, root, startIdxList, endIdxList);
                }
            }
        }
    }


    /**
     * Builds the experiencer or cause string. Excludes tokens that are specified in the spans of start and end indexes
     * in the exclude lists.
     * @param tokens a list of Agiga tokens
     * @param start the start index of the span from which the string should be extracted
     * @param end the end index of the span from which the string should be extracted
     * @param asLemma if the string should be returned in lemma form
     * @param startIdxExcludeList a list of indices that start spans that should be excluded
     * @param endIdxExcludeList a list of indices that end spans that should be excluded
     * @return a whitespace-delimited string in the specified span from which the specified spans have been removed
     */
    public static String buildStringFromSpan(List<AgigaToken> tokens, int start, int end, boolean asLemma,
                                             List<Integer> startIdxExcludeList, List<Integer> endIdxExcludeList) {

        StringBuilder sb = new StringBuilder();
        boolean inPP = false;
        for (int i = start; i <= end; i++) {
            if (startIdxExcludeList.contains(i)) {
                inPP = true;
            }
            else if (endIdxExcludeList.contains(i)) {
                inPP = false;
                continue;
            }
            if (inPP) {
                continue;
            }
            AgigaToken token = tokens.get(i);
            String s = token.getWord();
            if (asLemma) {
                s = token.getLemma();
            }
            sb.append(" ");
            sb.append(s);
        }

        return sb.toString().trim();
    }
}
