import edu.jhu.agiga.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.ArrayMap;
import edu.stanford.nlp.util.IntPair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // note: file name must end with /
        String agigaPath = "/home/sebastian/git/sentiment_analysis/anno_gigaword/";
        String[] fileNames = new File(agigaPath).list();
        // works if in the directory there are only agiga gz-compressed files (otherwise, take a FilenameFilter)

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
        String filePath = "/home/sebastian/git/sentiment_analysis/pattern_templates.txt";
        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, Map<Pattern, Map<String, Boolean>>> map = emotionExtractor.extractEmotions(filePath);

        // map listing each pattern with the number of times it has found a successful match (experiencer + cause)
        Map<Pattern, Map<String, Integer>> resultMap = new ArrayMap<Pattern, Map<String, Integer>>();
        for (String emotion: map.keySet()) {
            for (Pattern pattern : map.get(emotion).keySet()) {
                Map<String, Integer> integerMap = new ArrayMap<String, Integer>();
                integerMap.put("occurences", 0);
                integerMap.put("matches", 0);
                resultMap.put(pattern, integerMap);
            }
        }

        // count number of successful matches (experiencer & cause have been found)
        int matches = 0;

        PrintWriter resultWriter = new PrintWriter("results.txt", "UTF-8");
        PrintWriter collWriter = new PrintWriter("collocations.txt", "UTF-8");

        for (String fileName : fileNames) {
            agigaReader = new StreamingDocumentReader(agigaPath + fileName, readingPrefs);
            // Iterate over the documents
            for (AgigaDocument doc : agigaReader) {
                List<AgigaSentence> sentences = doc.getSents();

                // Iterate over the sentences
                for (AgigaSentence sent : sentences) {
                    // only retrieve one emotion trigger per sentence; if pattern is found, continue
                    boolean patternFound = false;

                    StringBuilder sb = new StringBuilder();
                    List<AgigaToken> tokens = sent.getTokens();
                    // create a lemma string with pos and indices
                    for (int j = 0; j < tokens.size(); j++) {
                        AgigaToken tok = tokens.get(j);
                        String lemma = tok.getLemma();
                        String pos = tok.getPosTag();
                        int idx = tok.getTokIdx();
                        sb.append(lemma);
                        sb.append("/");
                        sb.append(pos);
                        sb.append("/");
                        sb.append(idx);
                        sb.append(" ");
                    }
                    String sentence = sb.toString();
                    // extract sentence root
                    Tree root = sent.getStanfordContituencyTree();

                    for (String emotion : map.keySet()) {
                        // iterate over all the patterns
                        for (Pattern pattern : map.get(emotion).keySet()) {
                            if (patternFound) {
                                break;
                            }
                            Matcher m = pattern.matcher(sentence);
                            if (m.find()) {
                                // counts occurences
                                resultMap.get(pattern).put("occurences", resultMap.get(pattern).get("occurences") + 1);
                                //System.out.println(String.format("#%d: %s", agigaReader.getNumSents(), sentence));

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

                                // Penn string shows phrase structur tree
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
                                Boolean passiveExists = map.get(emotion).get(pattern).get("passiveExists");
                                // cause of emotion is either an NP or S
                                Boolean isNP = map.get(emotion).get(pattern).get("isNP");

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
                                    resultMap.get(pattern).put("matches", resultMap.get(pattern).get("matches") + 1);
                                    matches++;

                                    // build clean string for collocations file
                                    StringBuilder cleanBuilder = new StringBuilder();
                                    for (int j = 0; j < tokens.size(); j++) {
                                        cleanBuilder.append(tokens.get(j).getWord());
                                        cleanBuilder.append(" ");
                                    }
                                    String cleanString = cleanBuilder.toString().trim();
                                    collWriter.println(String.format("%d\t%s", agigaReader.getNumSents(), cleanString));
                                    collWriter.flush();

                                    System.out.println(String.format("%d\t%s", agigaReader.getNumSents(), cleanString));
                                    System.out.println(String.format("#%d.%d/%d Emotion: '%s', " +
                                                    "experiencer: '%s', cause: '%s'",
                                            matches, resultMap.get(pattern).get("matches"),
                                            resultMap.get(pattern).get("occurences"), emotion, experiencer, cause));

                                        /* write output to file; output is of the form:
                                        sentence number tab pattern tab pattern matched tab number of matches tab
                                        number of occurences tab emotion tab experiencer tab cause
                                        (number of total matches is line number)*/
                                    resultWriter.println(String.format("%d\t%s\t%d\t%d\t%s\t%s\t%s",
                                            agigaReader.getNumSents(), m.group(0),
                                            resultMap.get(pattern).get("matches"),
                                            resultMap.get(pattern).get("occurences"), emotion, experiencer, cause));
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

                        List<Integer> startIdxList = new ArrayList<Integer>();
                        List<Integer> endIdxList = new ArrayList<Integer>();
                        if (withoutPP) {
                            preorderTraverse(child, root, startIdxList, endIdxList);
                        }
                        // building the experiencer or cause string, as lemma, without PPs
                        StringBuilder sb = new StringBuilder();
                        boolean inPP = false;
                        for (int j = span.getSource(); j <= span.getTarget(); j++) {
                            if (startIdxList.contains(j)) {
                                inPP = true;
                            }
                            else if (endIdxList.contains(j)) {
                                inPP = false;
                                continue;
                            }
                            if (inPP) {
                                continue;
                            }
                            AgigaToken token = tokens.get(j);
                            String s = token.getWord();
                            if (asLemma) {
                                s = token.getLemma();
                            }
                            sb.append(" ");
                            sb.append(s);
                        }
                        String s = sb.toString();
                        return s.trim();
                    }
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                log.info(String.format("Node %s has no child.", leaf.ancestor(height, root)));
            }
        return null;
    }

    /**
     * Performs preorder-traversal, i.e. depth-first search from a node. Extracts PPs and SBARs that are dominated
     * by the node. Nodes dominated by PPs or SBARs are skipped, so that PPs and SBARs don't overlap.
     * @param node
     * @param root
     * @param startIdxList
     * @param endIdxList
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
}
