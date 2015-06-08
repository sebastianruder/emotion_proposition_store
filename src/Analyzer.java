import java.io.*;
import java.util.*;

/**
 * Class to analyze extractions.
 *
 * Created by sebastian on 20/05/15.
 */
public class Analyzer {

    /**
     * Key: Emotion. Value: Frequency.
     */
    private Map<String, Double> emotionFreqs = new HashMap<String, Double>();

    /**
     * Key: S or NP. Value: Map with key: Unigram in cause; value: frequency of appearance.
     */
    private Map<String, Map<String, Double>> causeUnigramFreqs = new HashMap<String, Map<String, Double>>();

    /**
     * Key: S or NP. Value: Map with key: emotion tab unigram in cause; value: frequency of appearing together.
     */
    private Map<String, Map<String, Double>> emotionCauseUnigramFreqs = new HashMap<String, Map<String, Double>>();

    /**
     * Key: S or NP. Value: Map with key: bigram in cause; value: frequency of appearance.
     */
    private Map<String, Map<String, Double>> causeBigramFreqs = new HashMap<String, Map<String, Double>>();

    /**
     * Key: S or NP. Value: Map with key: emotion tab bigram in cause; value: frequency of appearing together.
     */
    private Map<String, Map<String, Double>> emotionCauseBigramFreqs = new HashMap<String, Map<String, Double>>();

    /**
     *  Key: unigram. Value: number of unigrams (tokens) that appeared in cause.
     */
    private Map<String, Double> causeUnigramCount = new HashMap<String, Double>();

    /**
     * Number of emotions that appeared in total.
     */
    private double emotionCount = 0;

    /**
     * List of stop words in English.
     */
    private List<String> stopWords = new ArrayList<String>();

    /**
     * Initializes an <code>Analyzer</code>.
     * @param stopWordsFile the path to the stop words file
     * @throws IOException in case stop word file wasn't found or couldn't be read.
     */
    public Analyzer(String stopWordsFile) throws IOException {

        File file = new File(stopWordsFile);
        if (!file.exists()) {
            throw new FileNotFoundException(String.format("Couldn't find {0}", stopWordsFile));
        }

        BufferedReader reader = new BufferedReader(new FileReader(stopWordsFile));
        String line = reader.readLine();
        while (line != null && !line.equals("")) {
            this.stopWords.add(line);
            line = reader.readLine();
        }

        // initialize maps
        String[] causeTypes = { "NP", "S" };
        for (String causeType : causeTypes) {
            this.causeUnigramFreqs.put(causeType, new HashMap<String, Double>());
            this.causeBigramFreqs.put(causeType, new HashMap<String, Double>());
            this.emotionCauseUnigramFreqs.put(causeType, new HashMap<String, Double>());
            this.emotionCauseBigramFreqs.put(causeType, new HashMap<String, Double>());
        }
    }

    /**
     * Counts independent as well as co-occurrence frequencies of emotions, unigrams, and bigrams in cause given a list
     * of <code>Extraction</code> and stores them in fields.
     * @param extractions the list of <code>Extraction</code>
     */
    public void countFrequencies(List<Extraction> extractions) {

        // iterate over the extractions
        for (Extraction extraction : extractions) {

            // increment emotion count since one extraction contains one emotion
            this.emotionCount++;
            String emotion = extraction.getEmotion();

            // add emotion frequency
            Extensions.updateMap(this.emotionFreqs, emotion);

            // get NP and S cause elements
            String[] extractionNPCauseSplit = extraction.getNPCause().split(" ");
            String[] extractionSCauseSplit = String.format("%s %s %s", extraction.getSubjSCause(),
                    extraction.getPredSCause(), extraction.getDobjSCause()).split(" ");
            String[][] causes = { extractionNPCauseSplit, extractionSCauseSplit};

            // iterate over NP and S causes one after the other
            for (int j = 0; j < causes.length; j++) {
                String causeType = j == 0 ? "NP" : "S";

                for (int i = 1; i < causes[j].length + 1; i++) {

                    Extensions.updateMap(this.causeUnigramCount, causeType);
                    String unigram = causes[j][i - 1];
                    String emotionUnigram = emotion + "\t" + unigram;

                    // add unigram and emotion - unigram frequencies
                    Extensions.updateMap(causeUnigramFreqs.get(causeType), unigram);
                    Extensions.updateMap(emotionCauseUnigramFreqs.get(causeType), emotionUnigram);

                    if (i < causes[j].length) {

                        String bigram = unigram + " " + causes[j][i];
                        String emotionBigram = emotion + "\t" + bigram;

                        // add bigram and emotion - bigram frequencies
                        Extensions.updateMap(causeBigramFreqs.get(causeType), bigram);
                        Extensions.updateMap(emotionCauseBigramFreqs.get(causeType), emotionBigram);
                    }
                }
            }
        }
    }

    /**
     * Calculates discounted point-wise mutual information. Discount filters out expressions that occur very rarely.
     * Stores them in a map with the ngram as key and the PMI score as value.
     * @param isNP if PMI should be calculated from NP ngrams; else: S ngrams
     * @param isUnigram if PMI should be calculated from unigrams; else: bigrams
     * @return the map of ngrams and their PMI score
     */
    public Map<String, Double> calculatePMI(boolean isNP, boolean isUnigram) {

        // get the appropriate parameters
        String causeType = isNP ? "NP" : "S";
        Map<String, Double> ngramFreqs = isUnigram ? this.causeUnigramFreqs.get(causeType) : this.causeBigramFreqs.get(causeType);
        Map<String, Double> emotionNgramFreqs = isUnigram ? this.emotionCauseUnigramFreqs.get(causeType) : this.emotionCauseBigramFreqs.get(causeType);
        double ngramCount = this.causeUnigramCount.get(causeType);

        // initializes the final ngram - PMI map
        Map<String, Double> PMIMap = new HashMap<String, Double>();

        // iterate over all emotions and all tokens (i.e. unigrams or bigrams)
        for (String emotion : this.emotionFreqs.keySet()) {
            for (String ngram : ngramFreqs.keySet()) {

                String emotionNgram = emotion + "\t" + ngram;
                if (!emotionNgramFreqs.containsKey(emotionNgram)) {
                    continue;
                }

                // get frequencies
                double emotionFreq = this.emotionFreqs.get(emotion);
                double ngramFreq = ngramFreqs.get(ngram);
                double emotionNgramFreq = emotionNgramFreqs.get(emotionNgram);

                // continue if ngram never appeared; shouldn't happen but just to be safe
                if (ngramFreq == 0) {
                    continue;
                }

                // calculate probabilities
                double pEmotion = (emotionFreq / this.emotionCount); // P(x)
                double pNgram = ngramFreq / ngramCount; // P(y)
                double pJoint = emotionNgramFreq / this.emotionCount; // P(x,y)

                // calculate discounted probabilities (multiplied with freq / (freq + 1))
                double pEmotionNgramDiscount = (emotionFreq * ngramFreq) / (Math.pow(this.emotionCount, 2) * (ngramFreq / (ngramFreq + 1)));
                double pJointDiscount = (emotionNgramFreq / this.emotionCount) * (emotionNgramFreq / (emotionNgramFreq + 1));

                double pmi = Math.log(pJoint / (pEmotion * pNgram));
                double pmiDiscount = Math.log(pJointDiscount / pEmotionNgramDiscount);
                // double pmiNormalized = pmiDiscount / (- Math.log(pJointDiscount));

                PMIMap.put(emotionNgram, pmiDiscount);
            }
        }

        return PMIMap;
    }

    /**
     * Calculates chi-square. Stores the scores in a map with the ngram as key and the chi-square score as value.
     * @param isNP if chi-square should be calculated from NP ngrams; else: S ngrams
     * @param isUnigram if chi-square should be calculated from unigrams; else: bigrams
     * @return the map of ngrams and their chi-square score
     */
    public Map<String, Double> calculateChiSquare(boolean isNP, boolean isUnigram) {

        // get the appropriate parameters
        String causeType = isNP ? "NP" : "S";
        Map<String, Double> ngramFreqs = isUnigram ? this.causeUnigramFreqs.get(causeType) : this.causeBigramFreqs.get(causeType);
        Map<String, Double> emotionNgramFreqs = isUnigram ? this.emotionCauseUnigramFreqs.get(causeType) : this.emotionCauseBigramFreqs.get(causeType);
        double ngramCount = this.causeUnigramCount.get(causeType);

        // initialize final chi-square map
        Map<String, Double> chiSquareMap = new HashMap<String, Double>();

        // iterate over all emotions and ngrams
        for (String emotion : this.emotionFreqs.keySet()) {
            for (String ngram : ngramFreqs.keySet()) {

                String emotionNgram = emotion + "\t" + ngram;
                if (!emotionNgramFreqs.containsKey(emotionNgram)) {
                    continue;
                }

                double emotionFreq = this.emotionFreqs.get(emotion);
                double ngramFreq = ngramFreqs.get(ngram);
                double emotionNgramFreq = emotionNgramFreqs.get(emotionNgram);

                // continue if token never appeared; shouldn't happen but just to be safe
                if (ngramFreq == 0) {
                    continue;
                }

                // joint probability of x and y P(x, y)
                double pJoint = emotionNgramFreq / this.emotionCount;

                double pNgram = ngramFreq / ngramCount; // P(x)

                // fraction of documents, i.e extractions which contain token
                double Fw = ngramFreq / this.emotionCount; // F(w)

                // fraction of documents which contain emotion
                double P_i = emotionFreq / this.emotionCount;

                // conditional prob of class i for extractions which contain w: p(w, i) / p(w)
                double p_iw = pJoint / pNgram;

                double chiSquare = (this.emotionCount * Math.pow(Fw, 2) * Math.pow((p_iw - P_i), 2)) / (Fw * (1 - Fw) * P_i * (1 - P_i));

                chiSquareMap.put(emotionNgram, chiSquare);
            }
        }

        return chiSquareMap;
    }
}
