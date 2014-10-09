import edu.jhu.agiga.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.ArrayMap;

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
                Tree tree = sent.getStanfordContituencyTree();

                for (String emotion: map.keySet()) {
                    for (Pattern pattern : map.get(emotion).keySet()) {
                        // S or NP
                        String rightConstituent = map.get(emotion).get(pattern);
                        m = pattern.matcher(sentence);

                        if (m.find()) {
                            // System.out.println(sent.getParseText());
                            System.out.println(String.format("#%d: %s", agigaReader.getNumSents(), sentence));

                            // get indexes
                            int leftIdx = Integer.parseInt(m.group(1));
                            int rightIdx = Integer.parseInt(m.group(0).split("/")[m.group(0).split("/").length - 1]);
                            System.out.println(String.format("Pattern found: %s, constituent: %s, ids: %d, %d",
                                    m.group(0), rightConstituent, leftIdx, rightIdx));

                            AgigaToken leftToken = tokens.get(leftIdx);
                            AgigaToken rightToken = tokens.get(rightIdx);

                            // TODO: matching with node numbers
                            for (Tree leaf : tree.getLeaves()) {
                                if (leaf.toString().equals(leftToken.getWord())) {
                                    System.out.println(checkLabelOfAncestorChild(tree, leaf, "NP", true));
                                    System.out.println(checkLabelOfAncestorChild(tree, leaf, rightConstituent, false));
                                }
                            }

                            Map<String, String> outputMap;
                            // TODO: check if leftIdx is always correct idx
                            if (rightConstituent.equals("NP")) {
                                outputMap = extractVerbArguments(leftIdx, sent, false);
                            }
                            else {
                                outputMap = extractVerbArguments(leftIdx, sent, true);
                            }

                            System.out.println(String.format("%s\t%s\t%s",
                                    outputMap.get("subject"), emotion, outputMap.get("cause")));

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
                System.out.println(String.format("Left child: %s, right child: %s", leftChild, rightChild));
                if (left) {
                    child = leaf.ancestor(height, root).getChild(0);
                } else {
                    child = leaf.ancestor(height, root).getChild(1);
                }

                // SBAR label is also frequent
                if (child.label().toString().equals(label) || child.label().toString().equals(label + "BAR")) {

                    StringBuilder sb = new StringBuilder();
                    for (Tree node : child.getLeaves()) {
                        sb.append(node.toString());
                        sb.append(" ");
                    }
                    // System.out.println("Correct child: " + sb.toString());
                    return sb.toString();
                }

            } catch (ArrayIndexOutOfBoundsException exception) {
                log.info(String.format("Node %s has no child.", leaf.ancestor(height, root)));
            }

        }
        return null;
    }

    /**
     *
     * @param verbIdx
     * @param sent
     * @param ccomp
     * @return
     */
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
            /*
            System.out.println(String.format("Relation: %s(%s, %s)", type, tokens.get(dependency.getGovIdx()).getLemma(),
                    tokens.get(dependency.getDepIdx()).getLemma()));

            */
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
}
