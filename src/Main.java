import edu.jhu.agiga.*;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.ModCollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;
import org.apache.xpath.SourceTree;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static Logger log = Logger.getLogger(StreamingSentenceReader.class.getName());

    public static void main(String[] args) throws IOException {

        Writer writer = new StringWriter();

        Pattern multiWordTerminalPat = Pattern.compile("\\([A-Z]+\\s[^\\(]+\\s[^\\(]+?\\)"); // taken from BasicAgigaSentence

        // instantiate pattern lists
        List<Pattern> joy_patterns = new ArrayList<Pattern>();
        List<Pattern> trust_patterns = new ArrayList<Pattern>();
        List<Pattern> fear_patterns = new ArrayList<Pattern>();
        List<Pattern> surprise_patterns = new ArrayList<Pattern>();
        List<Pattern> sadness_patterns = new ArrayList<Pattern>();
        List<Pattern> disgust_patterns = new ArrayList<Pattern>();
        List<Pattern> anger_patterns = new ArrayList<Pattern>();
        List<Pattern> anticipation_patterns = new ArrayList<Pattern>();

        //Pattern pattern = Pattern.compile("(trust)/VBP (that)/IN");
        //joy_patterns.add(pattern);

        // Parse each file provided on the command line.
        for (int i = 0; i < args.length; i++) {
            StreamingSentenceReader reader = new StreamingSentenceReader(args[i], new AgigaPrefs());
            log.info("Parsing XML");

            HeadFinder hf = new ModCollinsHeadFinder();

            for (AgigaSentence sent : reader) {

                Tree tree = sent.getStanfordContituencyTree();
                List<AgigaToken> tokens = sent.getTokens();

                Pattern pattern = Pattern.compile("(Germany)/NNP (x2)/NNP");

                // TODO: calculate average String size to prevent unnecessary reallocation
                StringBuilder sb = new StringBuilder(16);

                for (int j=0; j<tokens.size(); j++) {
                    AgigaToken tok = tokens.get(j);
                    String lemma = tok.getLemma();
                    String pos = tok.getPosTag();
                    sb.append(lemma);
                    sb.append("/");
                    sb.append(pos);
                    sb.append(" ");
                }
                String sentence = sb.toString();
                Matcher m = pattern.matcher(sentence);
                while (m.find()) {
                    System.out.println(String.format("Group 1: %s, group 2: %s", m.group(1), m.group(2)));
                    // note: m.group(0) is whole string
                }
                System.out.println(sb);

                /*
                for (AgigaToken token : sent.getTokens()) {
                    System.out.println(token.getWord());
                    System.out.println(token.getTokIdx());
                }
                for (AgigaTypedDependency dp : sent.getColCcprocDeps()) {
                    System.out.println(String.format("dp idx: %d, gov idx: %d, type: %s", dp.getDepIdx(), dp.getGovIdx(), dp.getType()));
                    sent.getTokens().get(dp.getDepIdx());
                }
                */

                /*
                for (AgigaConstants.DependencyForm df : AgigaConstants.DependencyForm.values()) {
                    System.out.println(sent.getStanfordTreeGraphNodes(df).toString());
                    // Do nothing
                }
                */

                                /*
                System.out.println(tree);
                System.out.println(sent.getParseText());

                */
                /*
                for (Tree leaf : tree.getLeaves()) {
                    System.out.println(leaf);

                }
                for (Label label : tree.preTerminalYield()) {
                    System.out.println(label);
                }
                System.out.println(String.format("Head terminal: %s, label: %s", tree.headTerminal(hf)));
                */
                /*

                for (Constituent cons : tree.constituents()) {
                    System.out.println(String.format("%s, to String: %s, label: %s",
                            cons, cons.toString(), cons.label()));
                }

                */
            }
            log.info("Number of sentences: " + reader.getNumSents());
        }
    }
}

// "X trust that S"
