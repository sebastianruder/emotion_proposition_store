import edu.jhu.agiga.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sebastian on 29/09/14.
 * Adapted from Eva. TODO: include note/reference
 */
public class AgigaReader {

    private static Logger log = Logger.getLogger(StreamingSentenceReader.class.getName());

    public static void main(String[] args) throws IOException {
        // note: file name must end with /
        String agigaPath = "/home/sebastian/git/sentiment_analysis/anno_gigaword/";
        String[] fileNames = new File(agigaPath).list();
        //ok, if in the directory there are only agiga gz-compressed files (otherwise, take a FilenameFilter)
        for (String fileName : fileNames) {
            readAgiga(agigaPath + fileName);
        }
    }

    private static void readAgiga(String agigaFileNameWithPath) throws IOException {

        // Preferences of what should be read
        AgigaPrefs readingPrefs = new AgigaPrefs(); // all extraction is set to true per default
        // modify preferences individually if needed
        readingPrefs.setColDeps(true);
        readingPrefs.setBasicDeps(false);
        readingPrefs.setColCcprocDeps(false);

        //Get the document reader - this "entails" all the documents within the gz-compressed file
        StreamingDocumentReader agigaReader;
        agigaReader = new StreamingDocumentReader(agigaFileNameWithPath, readingPrefs);

        log.info("Parsing XML");

        Pattern simplePattern = Pattern.compile("China/NNP/([0-9]+) has/AUXZ/([0-9]+)");

        // extract emotions
        String filePath = "/home/sebastian/git/sentiment_analysis/emotion_trigger_patterns.txt";
        EmotionPatternExtractor emotionExtractor = new EmotionPatternExtractor();
        Map<String, Map<Pattern, String>> map = emotionExtractor.extractEmotions(filePath);
        Matcher m;

        // Iterate over the documents
        for (AgigaDocument doc : agigaReader) {
            List<AgigaSentence> sentences = doc.getSents();

            // Iterate over the sentences
            for (AgigaSentence sent : sentences) {

                // TODO: calculate average String size to prevent unnecessary reallocation
                StringBuilder sb = new StringBuilder(16);
                List<AgigaToken> tokens = sent.getTokens();
                for (int j=0; j<tokens.size(); j++) {
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
                // System.out.println(sentence);
                m = simplePattern.matcher(sentence);

                Tree tree = sent.getStanfordContituencyTree();

                for (Map<Pattern, String> emotionMap: map.values()) {
                    for (Pattern pattern : emotionMap.keySet()) {
                        String rightConstituent = emotionMap.get(pattern);
                        m = pattern.matcher(sentence);

                        while (m.find()) {

                            //System.out.println(sentence);
                            System.out.println(sent.getParseText());
                            // get indexes
                            int leftIdx = Integer.parseInt(m.group(1));
                            int rightIdx = Integer.parseInt(m.group(0).split("/")[m.group(0).split("/").length - 1]);
                            System.out.println(String.format("Pattern found: %s, constituent: %s, ids: %d, %d",
                                    m.group(0), rightConstituent, leftIdx, rightIdx));

                            AgigaToken leftToken = tokens.get(leftIdx);
                            AgigaToken rightToken = tokens.get(rightIdx);

                            // TODO: matching with node numbers

                            for (Tree leaf : tree.getLeaves()) {

                                // System.out.println(String.format("Leaf: %s, token: %s", leaf, token.getWord()));


                                if (leaf.toString().equals(leftToken.getWord())) {

                                    //System.out.println("Token found! " + leftToken.getWord());
                                    // for (Tree child : leaf.ancestor(2, tree).getChildrenAsList()) {

                                    // left child of first ancestor
                                    checkLabelOfAncestorChild(tree, leaf, "NP", true);
                                }
                                if (leaf.toString().equals(rightToken.getWord())) {

                                    //System.out.println("Token found! " + leftToken.getWord());
                                    //for (Tree child : leaf.ancestor(2, tree).getChildrenAsList()) {

                                    checkLabelOfAncestorChild(tree, leaf, rightConstituent, false);
                                }
                            }
                            System.out.println();
                        }
                    }
                }

            }

            // log.info("Number of sentences: " + agigaReader.getNumSents());
        }
    }

    public static String checkLabelOfAncestorChild(Tree root, Tree leaf, String label, boolean left) {

        Tree child;
        if (left) {
            System.out.println("Left:");
        }
        else {
            System.out.println("Right:");
        }

        // at height 2 is the first ancestor, limited height experimentally to 4
        for (int height = 2; height <= 4; height++) {

            try {
                Tree leftChild = leaf.ancestor(height, root).getChild(0);
                Tree rightChild = leaf.ancestor(height, root).getChild(1);
                if (left) {
                    child = leaf.ancestor(height, root).getChild(0);
                } else {
                    child = leaf.ancestor(height, root).getChild(1);
                }

                // System.out.println(String.format("Left child: %s, right child: %s", leftChild, rightChild));

                if (child.label().toString().equals(label)) {

                    StringBuilder sb = new StringBuilder();
                    for (Tree node : child.getLeaves()) {
                        sb.append(node.toString());
                        sb.append(" ");
                    }
                    System.out.println("Correct child: " + sb.toString());
                    return sb.toString();
                }

            } catch (ArrayIndexOutOfBoundsException exception) {
                log.info(String.format("Node %s has no child.", leaf.ancestor(height, root)));
            }

        }
        return null;
    }
}
