import edu.jhu.agiga.*;
import edu.stanford.nlp.trees.Tree;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by sebastian on 29/09/14.
 * Adapted from Eva. TODO: include note/reference
 */
public class AgigaReader {

    private static Logger log = Logger.getLogger(StreamingSentenceReader.class.getName());

    public static void main(String[] args) {
        // note: file name must end with /
        String agigaPath = "/home/sebastian/git/sentiment_analysis/anno_gigaword/";
        String[] fileNames = new File(agigaPath).list();
        //ok, if in the directory there are only agiga gz-compressed files (otherwise, take a FilenameFilter)
        for (String fileName : fileNames) {
            readAgiga(agigaPath + fileName);
        }
    }

    private static void readAgiga(String agigaFileNameWithPath) {

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

        //Iterate over the documents
        for (AgigaDocument doc : agigaReader) {

            //See more methods in AgigaDocument.java
            String docId = doc.getDocId();
            List<AgigaSentence> sentences = doc.getSents();

            for (AgigaSentence sent : sentences) {

                //See more methods in AgigaSentence.java
                int sentIdx = sent.getSentIdx();
                List<AgigaToken> tokens = sent.getTokens();

                for (AgigaToken token : tokens) {

                    //See more methods in AgigaToken.java
                    String lemma = token.getLemma();
                    System.out.println(lemma);
                }

                Tree constTree = sent.getStanfordContituencyTree();
                String pennString = constTree.pennString();
                System.out.println(pennString);
                //jede Phrase ist in einer neuen Zeile, sodass die Phrasennamen gut extrahierbar sind.

                String parseText = sent.getParseText();
                System.out.println(parseText); //das ist weniger schön, aber vllt. ok für dich

                Tree tree = sent.getStanfordContituencyTree();

                // TODO: calculate average String size to prevent unnecessary reallocation
                StringBuilder sb = new StringBuilder(16);

                /*
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
                */
            }

            log.info("Number of sentences: " + agigaReader.getNumSents());
        }
    }
}
