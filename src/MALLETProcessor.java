import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to process topic distributions created with MALLET.
 *
 * Created by sebastian on 13/07/15.
 */
public class MALLETProcessor {

    /**
     * The directory of the files from which the bigrams should be retrieved.
     */
    private static String pmiDir = "/home/sebastian/git/sentiment_analysis/out/scores/pmi/";

    /**
     * The results file containing the cleaned extractions.
     */
    private static String resultsFilePath = "/home/sebastian/git/sentiment_analysis/out/results_cleaned_removed.txt";

    /**
     * The directory containing the input files to MALLET.
     */
    private static String malletInputDir = "/home/sebastian/git/sentiment_analysis/mallet/input";

    /**
     * The directory containing the output files of MALLET.
     */
    private static String malletOutputDir = "/home/sebastian/git/sentiment_analysis/mallet/output";

    /**
     * The directory containing the SemEval 2007 Affective Text dataset.
     */
    private static String headlineDirectory = "/home/sebastian/git/sentiment_analysis/mallet/headlines";

    /**
     * The hash map created by the <code>Results.Reader.readNRCEmotionLexicon</code> method.
     */
    private static Map<String, Boolean[]> emotionLexicon;

    /**
     * The topic configuration for which the topic files should be processed.
     */
    private static int noOfTopics = 50;

    /**
     * The top n topic keys in the topic-keys file whose overlap with EmoLex should be measured. Note: Throws an error
     * if file contains fewer than the specified number of keys.
     */
    private static int topN = 30;

    /**
     * The main method to perform various forms of processing.
     * @param args the input arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // generateEmotionFiles();

        emotionLexicon = ResultsReader.readNRCEmotionLexicon("/home/sebastian/git/sentiment_analysis/NRC-Emotion-Lexicon-v0.92/NRC_emotion_lexicon_list.txt");

        String topicsFile = Utils.combine(malletOutputDir, String.format("topics-%d.txt", noOfTopics));
        String topicsKeyFile = Utils.combine(malletOutputDir, String.format("topic-keys-%d.txt", noOfTopics));

        getInputStats();

        processTopicDistributions(topicsFile, topicsKeyFile, noOfTopics, topN);

        // String xmlDoc = "/home/sebastian/git/sentiment_analysis/mallet/test/affectivetext_test.xml";
        // extractHeadlines(xmlDoc);
    }

    /**
     * Extracts the headlines from the SemEval 2007 Affective Text XML document and writes them to files in the headline
     * directory.
     * @param xml the XML document
     */
    private static void extractHeadlines(String xml) {

        Document dom;
        // Make an  instance of the DocumentBuilderFactory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // use the factory to take an instance of the document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            // parse using the builder to get the DOM mapping of the
            // XML file
            dom = db.parse(xml);

            Element doc = dom.getDocumentElement();
            NodeList childNodes = doc.getElementsByTagName("instance");
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                String headline = node.getTextContent();
                String id = node.getAttributes().getNamedItem("id").getNodeValue();
                PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(Utils.combine(headlineDirectory, id + ".txt"))));
                writer.print(headline);
                writer.close();
            }

        } catch (ParserConfigurationException pce) {
            System.out.println(pce.getMessage());
        } catch (SAXException se) {
            System.out.println(se.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }

    }

    /**
     * Processes the topic distributions.
     * @param topicsFile the path to the topics file, e.g. "path/to/topics-XX.txt"
     * @param topicKeysFile the path to the topic keys file, e.g. "path/to/topic-keys-XX.txt"
     * @param noOfTopics the number of topics that are used
     * @param topN the top n topic keys for which overlap should be calculated
     * @throws IOException
     */
    private static void processTopicDistributions(String topicsFile, String topicKeysFile, int noOfTopics, int topN) throws IOException {

        // retrieve an array of topic keys
        String[] topicKeysArray = processTopicKeysFile(topicKeysFile, noOfTopics);

        // initialize overlap map
        Map<Enums.NRCOverlap, Map<Enums.Emotions, Map<String, Double>>> overlapMap = new HashMap<Enums.NRCOverlap, Map<Enums.Emotions, Map<String, Double>>>();
        for (Enums.NRCOverlap overlap : Enums.NRCOverlap.values()) {
            overlapMap.put(overlap, new HashMap<Enums.Emotions, Map<String, Double>>());
        }

        BufferedReader reader = new BufferedReader(new FileReader(topicsFile));
        String line = reader.readLine();

        while (line != null && !line.equals("")) {

            if (line.startsWith("#")) {
                line = reader.readLine();
                continue;
            }

            String[] lineSplit = line.split("\t");
            String fileName = lineSplit[1]; // file name is "emotion.txt"
            String emotion = fileName.split("/")[fileName.split("/").length - 1].split("\\.")[0];
            System.out.println("\n" + fileName);
            Map<Double, Integer> topicValueMap = new TreeMap<Double, Integer>(Collections.reverseOrder());

            for (int i = 2; i < lineSplit.length; i++) {
                double topicValue = Double.parseDouble(lineSplit[i]);
                topicValueMap.put(topicValue, i - 2);
            }

            int count = 0;

            for (Map.Entry<Double, Integer> entry : topicValueMap.entrySet()) {
                if (count++ < 1) {
                    calculateNRCOverlapWithTopicKeys(topicKeysArray[entry.getValue()], emotion, topN, overlapMap);
                }

                if (count < 4) {
                    System.out.printf("Topic #%d, score: %f\n%s\n", entry.getValue(), entry.getKey(), topicKeysArray[entry.getValue()]);
                }
            }

            line = reader.readLine();
        }

        Visualizer.printEmotionOverlapValues(0, 4, overlapMap);
        Visualizer.printEmotionOverlapValues(4, 8, overlapMap);
    }

    private static void calculateNRCOverlapWithTopicKeys(String topicKeys, String emotion, int topN, Map<Enums.NRCOverlap, Map<Enums.Emotions, Map<String, Double>>> overlapMap) throws IOException {

        // count the occurrences of FALSE, TRUE, and NA per emotion and sentiment for each file (i.e. emotion)
        int[] NRCEmotionOverlapCounts = new int[3];
        int[] NRCSentimentOverlapCounts = new int[3];

        int count = 0;

        for (String topicKey : topicKeys.split(" ")) {
            if (count++ >= topN) {
                continue;
            }

            String overlap = "NA";
            String overlapWithSentiment = "NA";
            boolean isAssociated = false;
            boolean isAssociatedWithSentiment = false;
            Enums.Sentiment sentiment = Enums.emotionToSentiment(Enums.Emotions.valueOf(emotion));

            if (emotionLexicon.containsKey(topicKey)) {
                isAssociated |= emotionLexicon.get(topicKey)[Enums.Emotions.valueOf(emotion).ordinal()];
                overlap = isAssociated ? "TRUE" : "FALSE";

                // only check association with sentiment for clearly positive or negative emotions
                if (sentiment.equals(Enums.Sentiment.positive) || sentiment.equals(Enums.Sentiment.negative)) {
                    isAssociatedWithSentiment |= emotionLexicon.get(topicKey)[sentiment.ordinal() + 8];
                    overlapWithSentiment = isAssociatedWithSentiment ? "TRUE" : "FALSE";
                }
            }

            NRCEmotionOverlapCounts[Enums.NRCOverlap.valueOf(overlap).ordinal()]++;
            NRCSentimentOverlapCounts[Enums.NRCOverlap.valueOf(overlapWithSentiment).ordinal()]++;
        }

        for (Enums.NRCOverlap overlap : Enums.NRCOverlap.values()) {
            double emotionPercent = (double)NRCEmotionOverlapCounts[overlap.ordinal()] / (double)topN * 100;
            double sentimentPercent = (double)NRCSentimentOverlapCounts[overlap.ordinal()] / (double)topN * 100;

            overlapMap.get(overlap).put(Enums.Emotions.valueOf(emotion), new HashMap<String, Double>());
            overlapMap.get(overlap).get(Enums.Emotions.valueOf(emotion)).put("Emotion", emotionPercent);
            overlapMap.get(overlap).get(Enums.Emotions.valueOf(emotion)).put("Sentiment", sentimentPercent);
        }
    }

    /**
     * Stores the keys of each topic in an array concatenated as strings. The index of the keys corresponds with the
     * index of the topic.
     * @param topicKeysFile the path to the topic keys file
     * @param noOfTopics the number of topics that are used
     * @return an array of topic keys
     * @throws IOException
     */
    private static String[] processTopicKeysFile(String topicKeysFile, int noOfTopics) throws IOException {

        String[] topicKeysArray = new String[noOfTopics];

        BufferedReader reader = new BufferedReader(new FileReader(topicKeysFile));
        String line = reader.readLine();

        int idx = 0;
        while (line != null && !line.equals("")) {
            String topicKeys = line.split("\t")[2];
            topicKeysArray[idx++] = topicKeys;
            line = reader.readLine();
        }

        return topicKeysArray;
    }

    /**
     * Prints the number of tokens and types of the pseudo-documents used for topic modelling for each emotion.
     * @throws IOException
     */
    private static void getInputStats() throws IOException {

        List<String> fileNames = Utils.getFileNames(malletInputDir);
        System.out.println("Emotion\t# of tokens\t# of types");
        for (String fileName : fileNames) {
            int noOfTokens = 0;
            Map<String, Boolean> typeMap = new HashMap<String, Boolean>();
            BufferedReader reader = new BufferedReader(new FileReader(Utils.combine(malletInputDir, fileName)));
            String line = reader.readLine();

            while (line != null) {
                String[] lineSplit = line.split(" ");
                for (String token : lineSplit) {
                    noOfTokens++;
                    if (!typeMap.containsKey(token)) {
                        typeMap.put(token, true);
                    }
                }

                line = reader.readLine();
            }

            System.out.printf("%s\t%d\t%d\n", fileName, noOfTokens, typeMap.keySet().size());
        }
    }

    /**
     * Generates pseudo-dcouments for the top 200 bigrams of the NP and S cause (predicate + object) to be used for
     * topic modelling.
     * @throws IOException
     */
    private static void generateEmotionFiles() throws IOException {
        Map<Enums.Emotions, List<Extraction>> emotionsExtractionMap = orderExtractionsByEmotions(ResultsReader.readResults(resultsFilePath, false));
        Map<String, String> bigramEmotionMap = AnnotationTaskGenerator.getBigramsForAnnotation(pmiDir, 200);

        // clean up MALLET input files
        List<String> fileNames = Utils.getFileNames(malletInputDir);
        for (String fileName : fileNames) {
            File file = new File(Utils.combine(malletInputDir, fileName));
            file.delete();
        }

        int count = 0;
        for (Map.Entry<String, String> entry : bigramEmotionMap.entrySet()) {

            String emotion = entry.getValue();
            String bigram = entry.getKey().split("\t")[0];
            String ngramType = entry.getKey().split("\t")[1];
            System.out.printf("#%d\t%s\t%s\t%s\n", ++count, emotion, ngramType, bigram);

            // retrieve the bag-of-words of the causes by matching against the causes
            boolean extractionFound = false;
            Pattern pattern = Pattern.compile(bigram.replace("$", "\\$").replace("NUM", "NUMBER").replace(" ", ".*").toLowerCase() + "([^a-z]|$)");

            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(Utils.combine(malletInputDir, emotion + ".txt"), true)));

            for (Extraction extraction : emotionsExtractionMap.get(Enums.Emotions.valueOf(emotion))) {

                String cause;
                if (ngramType.equals(Enums.NgramSource.np_cause.toString())) {
                    cause = extraction.getNPCause();
                }
                else if (ngramType.equals(Enums.NgramSource.s_cause_pred_dobj.toString())) {
                    cause = extraction.getPredSCause() + " " + extraction.getDobjSCause();
                }
                else {
                    throw new NotImplementedException();
                }

                Matcher m = pattern.matcher(cause);

                if (m.find()) {
                    extractionFound = true;
                    String causeBoW = Extensions.join(extraction.getCauseBoW(), " ").replaceAll("(/([A-Z]|\\$)+|lrb-/-lrb|rrb-/-rrb|-lrb-/-LRB-|-rrb-/-RRB-)", "");
                    // String emotionHolder = extraction.getEmotionHolder().replaceAll("[:_]", " ").replaceAll("/[A-Z]");
                    // System.out.print(extraction.toString());
                    writer.printf("%s\n", causeBoW);
                }
            }

            if (!extractionFound) {
                System.out.println("NO EXTRACTION WAS FOUND!!!");
                System.out.println(pattern);
            }

            writer.close();
        }
    }

    /**
     * Creates a map with the emotion as key and the extractions of that emotion as value.
     * @param extractions a list of all extractions
     * @return the created map
     */
    private static Map<Enums.Emotions, List<Extraction>> orderExtractionsByEmotions(List<Extraction> extractions) {
        Map<Enums.Emotions, List<Extraction>> emotionsExtractionMap = new HashMap<Enums.Emotions, List<Extraction>>();

        for (Enums.Emotions emotion : Enums.Emotions.values()) {
            emotionsExtractionMap.put(emotion, new ArrayList<Extraction>());
        }
        for (Extraction extraction : extractions) {
            emotionsExtractionMap.get(Enums.Emotions.valueOf(extraction.getEmotion())).add(extraction);
        }

        return emotionsExtractionMap;
    }

    // bin/mallet train-topics --input ../sentiment_analysis/mallet/output/topic-input.mallet --num-topics 20 --output-state ../sentiment_analysis/mallet/output/topic-state.gz --output-doc-topics ../sentiment_analysis/mallet/output/topics.txt --output-topic-keys ../sentiment_analysis/mallet/output/topic-keys.txt
}
