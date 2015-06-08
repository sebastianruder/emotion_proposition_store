import com.sun.javaws.exceptions.InvalidArgumentException;
import edu.jhu.agiga.*;
import edu.stanford.nlp.trees.Tree;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Utility functions for extracting tokens or trees.
 *
 * Created by sebastian on 12/04/15.
 */
public class Utils {

    // pronouns
    public static List<String> pronouns = Arrays.asList("I", "you", "he", "she", "it", "him", "her", "we", "they", "me", "us", "them");

    // possessive pronouns
    private static List<String> possessivePronouns = Arrays.asList("my", "your", "his", "her", "its", "our", "their");

    // named entity tags
    private static List<String> NEtags = Arrays.asList("NUMBER", "PERSON", "LOCATION", "ORGANIZATION");

    /**
     * Method to create a map mapping sentence indexes to a list of mentions and their representatives.
     * @param corefs a list of AgigaCoref
     * @return the mention map
     */
    public static Map<Integer, List<Map.Entry<AgigaMention, AgigaMention>>> createMentionMap(List<AgigaCoref> corefs) {

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
     * Get the bag-of-words of the NP or S(BAR) that encompasses the cause, tagged with parts-of-speech.
     * @param root the root of the tree
     * @param leaf the leaf node (cause)
     * @param label the label of the cause (either NP or S)
     * @param tokens a list of Agiga tokens of the sentence
     * @return the bag-of-words of the sentence
     */
    public static String getBagOfWords(Tree root, Tree leaf, String label, List<AgigaToken> tokens) {

        // at height 1 from leaf node is preterminal, at height 2 is the first ancestor
        for (int height = 2; height <= 6; height++) {
            try {
                Tree ancestor_1 = leaf.ancestor(height, root);
                Tree ancestor_2 = leaf.ancestor(height + 1, root);
                String ancestor_1_label = ancestor_1.label().toString();
                String ancestor_2_label = ancestor_2.label().toString();

                if ((ancestor_1_label.equals(label) || ancestor_1_label.equals(label + "BAR")) &&
                        !(ancestor_2_label.equals(label) || ancestor_2_label.equals(label + "BAR"))) {
                    int start = ancestor_1.getSpan().getSource();
                    int end = ancestor_1.getSpan().getTarget();

                    return createStringFromTokens(tokens.subList(start, end + 1), true, true, false, true);
                }
            } catch (ArrayIndexOutOfBoundsException exception) {
                // is thrown when node doesn't have a child
            } catch (NullPointerException exception) {
                // is thrown when ancestor is null as tree ends
            }
        }

        return "";
    }

    /**
     * Create a string from a list of tokens. Tokens are separated by whitespace; part-of-speech and index are
     * separated by a slash from tokens.
     * @param tokens a list of Agiga tokens
     * @param lemma a boolean indicating if tokens should be outputted as lemmas
     * @param pos a boolean indicating if part-of-speech should be added
     * @param idx a boolean indicating if indexes should be added
     * @param excludePunctuation a boolean indicating if punctuation tokens should be excluded
     * @return the concatenated token string
     */
    public static String createStringFromTokens(List<AgigaToken> tokens, boolean lemma, boolean pos, boolean idx,
                                                boolean excludePunctuation) {

        StringBuilder sb = new StringBuilder();

        for (AgigaToken tok : tokens) {
            if (excludePunctuation && (tok.getWord().equals(":") || tok.getWord().equals(",") ||
                    tok.getWord().equals("''") || tok.getWord().equals("_") || tok.getWord().equals("``"))) {
                continue;
            }

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
     * Transforms a comp to a string made up of its subject, predicate, object, and optionally prepositional objects.
     * The string can optionally be in lemma form and coreferents and named entities can be replaced.
     * @param gov the index of the comp predicate (dependent of the comp, governor of nsubj and dobj relationships in comp)
     * @param colDeps the list of collapsed AgigaTypedDependencies of the sentence
     * @param tokens the list of tokens of the sentence
     * @param sentences the list of sentences in the document
     * @param mentionPairs a list of pairs of mentions and their representative mention
     * @param NEtokens the tokens of the sentence tagged with named entities
     * @param addPObj if prepositional objects should be added
     * @param replaceCoref if coreferents should be replaced
     * @param asLemma if the string should be returned in lemma form
     * @param addNER if named entities should be replaced with their named entity tags
     * @return the string of the comp
     */
    public static String compToString(int gov, List<AgigaTypedDependency> colDeps, List<AgigaToken> tokens,
                                     List<AgigaSentence> sentences, List<Map.Entry<AgigaMention,
                                     AgigaMention>> mentionPairs, String[] NEtokens,
                                     boolean addPObj, boolean replaceCoref, boolean asLemma, boolean addNER) {

        // retrieve subject and direct object
        String nsubj = "";
        String dobj = "";
        for (AgigaTypedDependency dep : colDeps) {
            String type = dep.getType();
            if ((type.equals("nsubj") || type.equals("nsubjpass"))&& dep.getGovIdx() == gov) {
                nsubj = depToString(dep.getDepIdx(), colDeps, tokens, sentences, mentionPairs, NEtokens, addPObj, replaceCoref, asLemma, addNER);
            }
            else if (type.equals("dobj") && dep.getGovIdx() == gov) {
                dobj = depToString(dep.getDepIdx(), colDeps, tokens, sentences, mentionPairs, NEtokens, addPObj, replaceCoref, asLemma, addNER);
            }
        }

        // append subject, predicate, object to string builder
        StringBuilder sb = new StringBuilder();
        sb.append(nsubj);
        sb.append("\t");
        sb.append(tokens.get(gov).getLemma());
        sb.append("\t");
        sb.append(dobj);
        sb.append("\t");

        // append list of prepositional objects
        if (addPObj) {
            Map<String, List<Integer>> pObjs = extractPObjs(gov, colDeps);
            for (int i = 0; i < pObjs.keySet().size(); i++) {
                if (i == 0) {
                    sb.append("[");
                }
                else {
                    sb.append(", ");
                }
                String prep = (String)pObjs.keySet().toArray()[i];
                sb.append(prep);
                sb.append(":");
                sb.append(buildString(pObjs.get(prep), tokens, sentences, mentionPairs, NEtokens, replaceCoref, asLemma, addNER, "_"));
                if (i + 1 == pObjs.keySet().size()) {
                    sb.append("]");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Transform a subject or object dependency into a string, adding modifiers, and optionally prepositional objects,
     * in lemma form, replacing corefs and named entities.
     * @param gov the index of the subject / object (dependent of the nsubj / dobj, etc. dependency)
     * @param colDeps a list of collapsed AgigaTypedDependencies
     * @param tokens the tokens in the sentence
     * @param sentences the list of sentences in the document
     * @param mentionPairs a list of pairs of mentions and their representative mention
     * @param NEtokens the list of tokens and their named entity tags
     * @param addPObj if prepositional objects should be added
     * @param replaceCoref if coreferents should be replaced
     * @param asLemma if the string should be returned in lemma form
     * @param addNER if named entities should be replaced with their named entity tags
     * @return the subject or object in string form
     */
    public static String depToString(int gov, List<AgigaTypedDependency> colDeps, List<AgigaToken> tokens,
                                     List<AgigaSentence> sentences, List<Map.Entry<AgigaMention,
                                     AgigaMention>> mentionPairs, String[] NEtokens,
                                     boolean addPObj, boolean replaceCoref, boolean asLemma, boolean addNER) {

        StringBuilder sb = new StringBuilder();
        sb.append(buildString(addModifiers(gov, colDeps), tokens, sentences, mentionPairs, NEtokens, replaceCoref, asLemma, addNER, " "));

        if (addPObj) {
            Map<String, List<Integer>> pObjs = extractPObjs(gov, colDeps);
            for (String prep : pObjs.keySet()) {
                sb.append(" ");
                sb.append(prep);
                sb.append(":");
                sb.append(buildString(pObjs.get(prep), tokens, sentences, mentionPairs, NEtokens, replaceCoref, asLemma, addNER, "_"));
            }
        }

        return sb.toString().trim();
    }

    /**
     * Build a string from token indexes. Optionally in lemma form, replacing coreferents and named entities.
     * @param idxs the indexes of the tokens that the string should be built from
     * @param tokens the tokens in the sentence
     * @param sentences the list of sentences in the document
     * @param mentionPairs a list of pairs of mentions and their representative mention
     * @param NEtokens the list of tokens and their named entity tags
     * @param replaceCoref if coreferents should be replaced
     * @param asLemma if the string should be returned in lemma form
     * @param addNER if named entities should be replaced with their named entity tags
     * @param sep separator (whitespace for regular strings, underscore for prepositional objects)
     * @return the string built from the specified tokens
     */
    private static String buildString(List<Integer> idxs, List<AgigaToken> tokens, List<AgigaSentence> sentences,
                                      List<Map.Entry<AgigaMention, AgigaMention>> mentionPairs, String[] NEtokens,
                                      boolean replaceCoref, boolean asLemma, boolean addNER, String sep) {

        StringBuilder sb = new StringBuilder();
        for (int i : idxs) {

            AgigaToken token = tokens.get(i);
            String str = token.getWord();
            String NEtoken = NEtokens[i].split("/")[1];

            // replace numbers
            if (addNER && NEtoken.equals("NUMBER")) {
                str = "NUMBER";
            }
            // if mention is pronoun, replace with representative mention if exists
            else if (replaceCoref && pronouns.contains(token.getWord())) {
                try {
                    str = replaceCoref(str, i, sentences, mentionPairs, asLemma, addNER);
                }
                catch (IOException ex) {
                    System.out.println("NE tags couldn't be assigned.");
                }
            }
            else if (asLemma && !possessivePronouns.contains(token.getWord())) {
                str = token.getLemma();
            }

            sb.append(sep);
            sb.append(str);

            if (addNER && !NEtoken.equals("NUMBER") && NEtags.contains(NEtoken)) {
                sb.append("/");
                sb.append(NEtokens[i].split("/")[1]);
            }
        }

        return sb.toString().substring(1).trim();
    }

    /**
     * Return a list containing the index of a noun and its modifiers (compound nouns, adjectives, numbers).
     * @param gov the index of the noun
     * @param colDeps the list of collapsed AgigaTypedDependencies of the sentence
     * @return a sorted list of indexes
     */
    private static List<Integer> addModifiers(int gov, List<AgigaTypedDependency> colDeps) {
        List<Integer> mods = new ArrayList<Integer>();
        mods.add(gov);
        for (AgigaTypedDependency dep : colDeps) {
            String type = dep.getType();
            if ((type.equals("nn") || type.equals("amod") || type.equals("num")) && dep.getGovIdx() == gov) {
                mods.add(dep.getDepIdx());
            }
        }

        Collections.sort(mods);
        return mods;
    }

    /**
     * Return the indexes of the prepositional objects of a noun mapped to their prepositions.
     * @param gov the index of the noun
     * @param colDeps the list of collapsed AgigaTypedDependencies of the sentence
     * @return a map containing the preposition type and a list of indexes of the prepositional object
     */
    private static Map<String, List<Integer>> extractPObjs(int gov, List<AgigaTypedDependency> colDeps) {
        Map<String, List<Integer>> pObjs = new HashMap<String, List<Integer>>();
        for (AgigaTypedDependency dep : colDeps) {
            if (dep.getType().startsWith("prep_") && dep.getGovIdx() == gov) {
                pObjs.put(dep.getType().substring(5), addModifiers(dep.getDepIdx(), colDeps));
            }
        }

        return pObjs;
    }

    /**
     * Replace a mention with its most representative mention if it exists and is not too long.
     * @param input the mention that should be replaced
     * @param idx the index of the mention
     * @param sentences a list of sentences of the document
     * @param mentionPairs a list of pairs of mentions and their representative mention
     * @return the most representative mention; if it doesn't exist or is too long, the mention
     */
    private static String replaceCoref(String input, int idx, List<AgigaSentence> sentences,
                                       List<Map.Entry<AgigaMention, AgigaMention>> mentionPairs,
                                       boolean asLemma, boolean addNER) throws IOException {

        for (Map.Entry<AgigaMention, AgigaMention> pair : mentionPairs) {
            AgigaMention mention = pair.getKey();
            AgigaMention rep = pair.getValue();

            if (mention.getStartTokenIdx() == idx) {
                List<AgigaToken> repSentTokens = sentences.get(rep.getSentenceIdx()).getTokens();
                AgigaSentence repSent = sentences.get(rep.getSentenceIdx());
                List<AgigaTypedDependency> colDeps = repSent.getColDeps();
                // get NE tags
                StringWriter stringWriter = new StringWriter();
                repSent.writeNerTags(stringWriter);
                String[] repNEtokens = stringWriter.toString().split(" ");

                String representative = depToString(rep.getHeadTokenIdx(), colDeps, repSentTokens, sentences, null, repNEtokens,
                        true, false, asLemma, addNER);
                return representative;
            };
        }

        return input;
    }

    /**
     * Combines two paths into a single valid path.
     * @param path1 the first path
     * @param path2 the second path
     * @return the combined path
     */
    public static String combine(String path1, String path2)
    {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }

    /**
     * Deletes all files in a directory with a specified extension.
     * @param dir the directory
     * @param extension the file extension that should be deleted
     * @throws IOException if the directory is not found
     */
    public static void cleanDirectory(String dir, final String extension) throws IOException {
        File fileDir = new File(dir);
        if (!fileDir.isDirectory()) {
            throw new IOException(String.format("%s is not a valid directory.", dir));
        }

        String[] fileNames = fileDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(extension);
            }
        });

        for (String fileName : fileNames) {
            File file = new File(dir + fileName);
            file.delete();
        }
    }
}
