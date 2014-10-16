import edu.jhu.agiga.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.ArrayMap;

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
 * Adapted from Eva. TODO: include note/reference
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
        for (String fileName : fileNames) {
            readAgiga(agigaPath + fileName);
        }
    }

    /**
     * Reads in a document using a StreamingDocumentReader and extracts expressions
     * @param agigaFileNameWithPath: absolute file path of the file that should be read in
     * @throws IOException
     */
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

        // store the emotion-triggering patterns in a map
        String filePath = "/home/sebastian/git/sentiment_analysis/v2_emotion_trigger_patterns.txt";
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

        // TODO: write collocations
        // create writer to write to output file with statistics/collocations
        PrintWriter writer = new PrintWriter("results.txt", "UTF-8");

        // Iterate over the documents
        for (AgigaDocument doc : agigaReader) {
            List<AgigaSentence> sentences = doc.getSents();

            // Iterate over the sentences
            for (AgigaSentence sent : sentences) {
                // only retrieve one emotion trigger per sentence; if pattern is found, continue
                boolean patternFound = false;

                // TODO: calculate average String size to prevent unnecessary reallocation
                StringBuilder sb = new StringBuilder(16);
                List<AgigaToken> tokens = sent.getTokens();
                // create a lemma string with pos and indices
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
                // extract sentence root
                Tree root = sent.getStanfordContituencyTree();

                for (String emotion: map.keySet()) {
                    // iterate over all the patterns
                    for (Pattern pattern : map.get(emotion).keySet()) {
                        if (patternFound) {
                            break;
                        }

                        Matcher m = pattern.matcher(sentence);

                        if (m.find()) {
                            // counts occurences
                            resultMap.get(pattern).put("occurences", resultMap.get(pattern).get("occurences") + 1);

                            System.out.println(String.format("#%d: %s", agigaReader.getNumSents(), sentence));
                            // get indices
                            int headIdx = Integer.parseInt(m.group(1));
                            // get rightmost index; TODO: is this necessary at all? maybe. optimize!
                            // int rightIdx = Integer.parseInt(m.group(0).split("/")[m.group(0).split("/").length - 1]);
                            System.out.println(String.format("Pattern found: %s, constituent: %s",
                                    m.group(0), map.get(emotion).get(pattern).get("isNP")));
                            AgigaToken headToken = tokens.get(headIdx);

                            // iterate over all leaves, i.e. terminals
                            for (Tree leaf : root.getLeaves()) {
                                if (leaf.toString().equals(headToken.getWord())) {
                                    // pattern can have a passive form; if so, experiencer and cause are reversed
                                    Boolean passiveExists = map.get(emotion).get(pattern).get("passiveExists");
                                    // cause of emotion is either an NP or S
                                    Boolean isNP = map.get(emotion).get(pattern).get("isNP");

                                    List<Integer> experiencerSpan, causeSpan;
                                    // if a passive form exists, experiencer is dependent, cause is subject
                                    if (passiveExists) {
                                        experiencerSpan = checkLabelOfAncestorChild(root, leaf, isNP ? "NP" : "S", false);
                                        causeSpan = checkLabelOfAncestorChild(root, leaf, "NP", true);
                                    }
                                    // if no passive form exists, experiencer is subject, cause is dependent
                                    else {
                                        experiencerSpan = checkLabelOfAncestorChild(root, leaf, "NP", true);
                                        causeSpan = checkLabelOfAncestorChild(root, leaf, isNP ? "NP" : "S", false);
                                    }
                                    System.out.println("Experiencer span: " + experiencerSpan + ", cause span: " +
                                            causeSpan);

                                    if (experiencerSpan != null && causeSpan != null) {
                                        patternFound = true;
                                        resultMap.get(pattern).put("matches", resultMap.get(pattern).get("matches") + 1);
                                        matches++;

                                        // TODO: remove PPs

                                        // lemmatize string
                                        String experiencer = getLeafString(experiencerSpan.get(0),
                                                experiencerSpan.get(1), tokens, true);
                                        String cause = getLeafString(causeSpan.get(0), causeSpan.get(1), tokens, true);

                                        System.out.println(String.format("#%d.%d/%d Emotion: '%s', " +
                                                        "experiencer: '%s', cause: '%s'",
                                                matches, resultMap.get(pattern).get("matches"),
                                                resultMap.get(pattern).get("occurences"), emotion, experiencer, cause));

                                        /* write output to file; output is of the form:
                                        sentence number tab pattern tab pattern matched tab number of matches tab
                                        number of occurences tab emotion tab experiencer tab cause
                                        (number of total matches is line number)*/
                                        writer.println(String.format("%d\t%s\t%s\t%d\t%d\t%s\t%s\t%s",
                                                agigaReader.getNumSents(), pattern, m.group(0),
                                                resultMap.get(pattern).get("matches"),
                                                resultMap.get(pattern).get("occurences"), emotion, experiencer, cause));
                                        writer.flush();
                                    }
                                }
                            }
                            /*
                            Map<String, String> outputMap;
                            // at the moment, headIdx is used as only index; pretty sure this is always correct,
                            // but should maybe be looked into
                            if (isNP) {
                                outputMap = extractVerbArguments(headIdx, sent, false);
                            }
                            else {
                                outputMap = extractVerbArguments(headIdx, sent, true);
                            }

                            System.out.println(String.format("%s\t%s\t%s",
                                    outputMap.get("subject"), emotion, outputMap.get("cause")));
                            */
                            System.out.println();
                        }
                    }
                }
            }
        }
        writer.close();
    }

    /**
     * Retrieves span of first sibling/cousin node having a given label on either side of a leaf node.
     * Performs a cut at a height of 4 (height of leaf is 1).
     * @param root: root node of tree
     * @param leaf: leaf node whose sibling should be retrieved
     * @param label: label of wanted node
     * @param left: if node should be looked for on the left side of the sibling node
     * @return a list containing the start index and the end index of the span of the wanted node
     */
    public static List<Integer> checkLabelOfAncestorChild(Tree root, Tree leaf, String label, boolean left) {

        // TODO: retrieve spans better, avoid spans intersect in higher function; see above using rightmost node

        Tree child;
        // at height 2 is the first ancestor, set maximum height experimentally to 4
        for (int height = 2; height <= 4; height++)
            try {
                if (left) { // left child
                    child = leaf.ancestor(height, root).getChild(0);
                } else { // right child
                    child = leaf.ancestor(height, root).getChild(1);
                }
                // System.out.println(String.format("%s child: %s", left ? "left" : "right", child));

                // label of child should equal label; SBAR label is also frequent
                if (child.label().toString().equals(label) || child.label().toString().equals(label + "BAR")) {
                    // index the leaves to retrieve indices
                    root.indexLeaves();

                    List<Integer> idxList = new ArrayList<Integer>();
                    int idx = 0;
                    boolean start = true;
                    for (Tree node : child.getLeaves()) {
                        if (!start) {
                            idx++;
                            }
                        else {
                            start = false;
                            CoreLabel coreLabel = (CoreLabel) node.label();
                            // set index to start index; label index starts at 1
                            idx = coreLabel.get(CoreAnnotations.IndexAnnotation.class) - 1;
                            // add start index to list
                            idxList.add(idx);
                        }
                    }
                    // add end index to list
                    idxList.add(idx);
                    // make sure that list only contains start and end index
                    assert(idxList.size() == 2);
                    // return list containing start index, end index
                    return idxList;
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                log.info(String.format("Node %s has no child.", leaf.ancestor(height, root)));
            }
        return null;
    }

    /**
     * Returns a string containing the word forms/lemmas of the leaves that are in the given span of the given node.
     * @param startIdx: start index of the span
     * @param endIdx: end index of the span
     * @param tokens: a list of AgigaTokens of the sentence of the node
     * @param lemma: if lemmas should be retrieved
     * @return a lemma or word form string
     */
    public static String getLeafString(int startIdx, int endIdx, List<AgigaToken> tokens, boolean lemma) {
        StringBuilder sb = new StringBuilder();
        boolean start = true;

        for (int i = startIdx; i <= endIdx; i++) {
            AgigaToken token = tokens.get(i);
            String s = token.getWord();
            if (lemma) {
                s = token.getLemma();
            }
            if (!start) {
                sb.append(" ");
            }
            sb.append(s);
            start = false;
        }
        return sb.toString();
    }

    /**
     * Extracts certain verb arguments of a given sentence. Not sure if necessary.
     * @param verbIdx
     * @param sent
     * @param ccomp
     * @return
     */
    /*
    public static Map<String, String> extractVerbArguments(int verbIdx, AgigaSentence sent, boolean ccomp) {

        Map<String, String> map = new ArrayMap<String, String>();
        List<AgigaToken> tokens = sent.getTokens();
        List<AgigaTypedDependency> dependencies = sent.getAgigaDeps(AgigaConstants.DependencyForm.COL_DEPS);
        int ccompVerbIdx = 0;
        String ccompVerb = "";

        // TODO: deal with case of finding subject if verb is in conjunction

        // if argument of verb is a ccomp, get index of ccomp dependent for later
        // argument can also be a dep, in case a more precise dependency couldn't be determined by the parser
        if (ccomp) {
            for (AgigaTypedDependency dependency : dependencies) {
                if ((dependency.getType().equals("ccomp") || dependency.getType().equals("dep"))
                        && dependency.getGovIdx() == verbIdx) {
                    ccompVerbIdx = dependency.getDepIdx();
                    // retrieve lemma rather than string
                    ccompVerb = tokens.get(ccompVerbIdx).getLemma();
                }
            }
        }

        int dobjIdx = 0;
        int ccompNsubjIdx = 0;
        int ccompDobjIdx = 0;
        int ccompIobjIdx = 0;
        int andVerbIdx = 0;
        String dobj = "";
        String ccompNsubj = "";
        String ccompDobj = "";
        String ccompIobj = "";

        // Relation: dep(angry, defame), Relation: nsubjpass(defame, city)
        // xcomp(happy, sit); prt(sit, down); prep_with(sit, Government)
        // Relation: dep(afraid, detach)
        // Relation: nsubjpass(design, order)
        // conj_but for nsubj
        // Relation: dep(happy, be); advmod(be, here)

        for (AgigaTypedDependency dependency : dependencies) {
            String type = dependency.getType();
            if (type.equals("root")) {
                continue;
            } else if (type.equals("nsubj") && dependency.getGovIdx() == verbIdx) {
                map.put("subject", tokens.get(dependency.getDepIdx()).getLemma());
            }

            //System.out.println(String.format("Relation: %s(%s, %s)", type, tokens.get(dependency.getGovIdx()).getLemma(),
            //        tokens.get(dependency.getDepIdx()).getLemma()));


            // if argument of verb is a ccomp
            if (ccomp) {
                // extract subject, direct and indirect objects of ccomp, if applicable
                if (dependency.getGovIdx() == ccompVerbIdx) {
                    if (type.equals("nsubj")) {
                        ccompNsubjIdx = dependency.getDepIdx();
                        ccompNsubj = tokens.get(ccompNsubjIdx).getLemma();
                    } else if (type.equals("dobj")) {
                        ccompDobjIdx = dependency.getDepIdx();
                        ccompDobj = ", " + tokens.get(ccompDobjIdx).getLemma();
                    } else if (type.equals("iobj")) {
                        ccompIobjIdx = dependency.getDepIdx();
                        ccompIobj = ", " + tokens.get(ccompIobjIdx).getLemma();
                    }
                }
            }
            // else argument must be a direct object; extract direct object
            else {
                if (type.equals("dobj") && dependency.getGovIdx() == verbIdx) {
                    dobjIdx = dependency.getDepIdx();
                    dobj = tokens.get(dobjIdx).getLemma();
                }
            }

            if (type.equals("conj_and")) {
                if (dependency.getGovIdx() == verbIdx) {
                    andVerbIdx = dependency.getDepIdx();
                }
                else if (dependency.getDepIdx() == verbIdx) {
                    andVerbIdx = dependency.getGovIdx();
                }
            }
        }

        // check for compound noun phrases
        for (AgigaTypedDependency dependency : dependencies) {
            if (dependency.getType().equals("nn")) {
                if (ccomp) {
                    if (dependency.getGovIdx() == ccompNsubjIdx) {
                        ccompNsubj = tokens.get(dependency.getDepIdx()).getLemma() + " " + ccompNsubj;
                    }
                    else if (dependency.getGovIdx() == ccompDobjIdx) {
                        ccompDobj = ", " + tokens.get(dependency.getDepIdx()).getLemma() + " " + ccompDobj.replace(", ", "");
                    }
                    else if (dependency.getGovIdx() == ccompIobjIdx) {
                        ccompIobj = ", " + tokens.get(dependency.getDepIdx()).getLemma() + " " + ccompIobj.replace(", ", "");
                    }
                }
                else if (dependency.getGovIdx() == dobjIdx) {
                    dobj = tokens.get(dependency.getDepIdx()).getLemma() + " " + dobj;
                }
            }
            // if subject hasn't been assigned yet, retrieve subject from relation of other verb in conjunction
            if (!map.containsKey("subject")) {
                if (dependency.getType().equals("nsubj") && dependency.getGovIdx() == andVerbIdx) {
                    map.put("subject", tokens.get(dependency.getDepIdx()).getLemma());
                }
            }
        }

        if (ccomp) {
            map.put("cause", String.format("%s(%s%s%s)", ccompVerb, ccompNsubj, ccompDobj, ccompIobj));
        }
        else {
            map.put("cause", dobj);
        }
        return map;
    }
    */
}
