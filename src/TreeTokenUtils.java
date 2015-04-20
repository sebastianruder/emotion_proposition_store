import edu.jhu.agiga.AgigaCoref;
import edu.jhu.agiga.AgigaMention;
import edu.jhu.agiga.AgigaSentence;
import edu.jhu.agiga.AgigaToken;
import edu.stanford.nlp.trees.Tree;

import java.util.*;

/**
 * Created by sebastian on 12/04/15.
 */
public class TreeTokenUtils {

    private static List<String> pronouns = Arrays.asList("I", "he", "she", "it", "him", "her", "we", "they", "me", "us", "them");

    private static List<String> possessivePronouns = Arrays.asList("my", "your", "his", "her", "its", "our", "their");

    private static List<String> NEtags = Arrays.asList("NUMBER", "PERSON", "LOCATION", "ORGANIZATION");


    public static Map<Integer, List<Map.Entry<AgigaMention, AgigaMention>>> createMentionMap(List<AgigaCoref> corefs) {

        // maps sentence indexes to a list of mentions and their representatives
        Map<Integer, List<Map.Entry<AgigaMention, AgigaMention>>> mentionMap = new HashMap<Integer, List<Map.Entry<AgigaMention, AgigaMention>>>();

        for (AgigaCoref coref : corefs) {
            List<AgigaMention> mentions = coref.getMentions();
            AgigaMention rep = null;

            for (AgigaMention mention : mentions) {
                if (mention.isRepresentative()) {
                    rep = mention;
                    break;
                }
            }

            for (AgigaMention mention : mentions) {
                int sentIdx = mention.getSentenceIdx();
                if (!mentionMap.containsKey(sentIdx)) {
                    mentionMap.put(sentIdx, new ArrayList<Map.Entry<AgigaMention, AgigaMention>>());
                }

                mentionMap.get(sentIdx).add(new AbstractMap.SimpleEntry<AgigaMention, AgigaMention>(mention, rep));
            }
        }

        return mentionMap;
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
    public static Tree findHolderOrCause(Tree root, Tree leaf, String label, boolean left, Tree node) {
        Tree child;
        // at height 1 from leaf node is preterminal, at height 2 is the first ancestor
        for (int height = 2; height <= 6; height++)
            try {
                Tree ancestor = leaf.ancestor(height, root);

                // node shouldn't be embedded in a PP
                if (leaf.ancestor(height + 1 , root).label().toString().equals("PP")) {
                    continue;
                }

                // iterate over the children of ancestor, start with the second child if looking on the right
                for (int i = 0; i < ancestor.getChildrenAsList().size(); i++) {
                    child = ancestor.getChild(i);

                    // if looking on the right, child should not lie left of the leaf
                    if (!left && child.getSpan().getTarget() < leaf.ancestor(2, root).getSpan().getSource()) {
                    }
                    // break so as not to look to the right side of the leaf when searching left (i.e. for experiencer)
                    else if (child.getLeaves().contains((leaf))) {
                        if (left) {
                            break;
                        }
                    }
                    else if (child.label().toString().equals(label) || child.label().toString().equals(label + "BAR")) {

                        // , after NP node indicates adjunct (temporal, e.g. "next morning", "this day", etc.)
                        if (i + 1 < ancestor.getChildrenAsList().size() && ancestor.getChild(i + 1).label().toString().equals(",")) {

                        }
                        // emotion holder and cause shouldn't be identical
                        else if (!child.equals(node))
                        {
                            return child;
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                // is thrown when node doesn't have a child
            } catch (NullPointerException exception) {
                // is thrown when ancestor is null as tree ends
            }
        return null;
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

    public static String getStringFromSpan(Tree node, List<AgigaToken> tokens, List<AgigaSentence> sentences,
                                           List<Map.Entry<AgigaMention, AgigaMention>> mentionPairs, String[] NEtokens,
                                           boolean asLemma, boolean replaceCoref, boolean replaceNE) {

        // lists of start indixes and end index of PPs and SBARs which should be excluded
        List<Integer> startIdxList = new ArrayList<Integer>();
        List<Integer> endIdxList = new ArrayList<Integer>();

        preorderTraverse(node, startIdxList, endIdxList);
        return buildStringFromSpan(tokens, sentences, node.getSpan().getSource(), node.getSpan().getTarget(), mentionPairs,
                NEtokens, startIdxList, endIdxList, asLemma, replaceCoref, replaceNE);
    }

    /**
     * Performs preorder-traversal, i.e. depth-first search from a node. Extracts PPs and SBARs that are dominated
     * by the node. Nodes dominated by PPs or SBARs are skipped, so that PPs and SBARs don't overlap.
     * @param node the start node from which traversal shoud be performed
     * @param startIdxList list which stores start indexes of PPs and SBARs
     * @param endIdxList list which stores end indexes of PPs and SBARs
     */
    public static void preorderTraverse(Tree node, List<Integer> startIdxList, List<Integer> endIdxList) {
        if (!node.isPreTerminal()) {
            for (Tree child : node.getChildrenAsList()) {
                // extracts PPs and SBARs
                String label = child.label().value();

                // don't remove PPs that are only three tokens long
                //if (label.equals("PP") && child.getSpan().getSource() + 3 > child.getSpan().getTarget()) {
                //}
                if (label.equals("PP")) {
                    for (Tree ppChild : child.getChildrenAsList()) {
                        if (!ppChild.label().toString().equals("IN") || ppChild.label().toString().equals("TO")) {
                            preorderTraverse(ppChild, startIdxList, endIdxList);
                        }
                    }
                }
                else if (label.equals("SBAR") || label.equals(":") || label.equals(",") ||
                        label.equals("''") || label.equals("_") || label.equals("``")) {
                    startIdxList.add(child.getSpan().getSource());
                    endIdxList.add(child.getSpan().getTarget());
                }
                else {
                    preorderTraverse(child, startIdxList, endIdxList);
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
    public static String buildStringFromSpan(List<AgigaToken> tokens, List<AgigaSentence> sentences, int start, int end,
                                             List<Map.Entry<AgigaMention, AgigaMention>> mentionPairs, String[] NEtokens,
                                             List<Integer> startIdxExcludeList, List<Integer> endIdxExcludeList,
                                             boolean asLemma, boolean replaceCoref, boolean addNER) {

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
            String str = token.getWord();
            String NEtoken = NEtokens[i].split("/")[1];

            // replace numbers
            if (NEtoken.equals("NUMBER")) {
                str = "NUMBER";
            }
            // if mention is pronoun, replace with representative mention if exists
            else if (replaceCoref && pronouns.contains(token.getWord())) {
                for (Map.Entry<AgigaMention, AgigaMention> pair : mentionPairs) {
                    AgigaMention mention = pair.getKey();
                    AgigaMention rep = pair.getValue();

                    if (mention.getStartTokenIdx() == i) {
                        List<AgigaToken> repSentTokens = sentences.get(rep.getSentenceIdx()).getTokens();
                        int repStart = rep.getStartTokenIdx();
                        int repEnd = rep.getEndTokenIdx();

                        // representative mention may not be longer than 4 tokens
                        if (repStart + 5 > repEnd) {
                            str = createStringFromTokens(repSentTokens.subList(rep.getStartTokenIdx(), rep.getEndTokenIdx()), false, false, false);
                            break;
                        }
                    };
                }
            }
            else if (asLemma && !possessivePronouns.contains(token.getWord())) {
                str = token.getLemma();
            }

            sb.append(" ");
            sb.append(str);

            if (addNER && !NEtoken.equals("NUMBER") && NEtags.contains(NEtoken)) {
                sb.append("/");
                sb.append(NEtokens[i].split("/")[1]);
            }
        }

        return sb.toString().trim();
    }
}
