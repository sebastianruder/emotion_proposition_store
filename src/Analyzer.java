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
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: Unigram in ngram type; value: frequency of appearance.
     */
    private Map<Enums.NgramSource, Map<String, Double>> ngramTypeUnigramFreqs = new HashMap<Enums.NgramSource, Map<String, Double>>();

    /**
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: emotion tab unigram in ngram type; value: frequency of appearing together.
     */
    private Map<Enums.NgramSource, Map<String, Double>> emotionNgramTypeUnigramFreqs = new HashMap<Enums.NgramSource, Map<String, Double>>();

    /**
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: bigram in ngram type; value: frequency of appearance.
     */
    private Map<Enums.NgramSource, Map<String, Double>> ngramTypeBigramFreqs = new HashMap<Enums.NgramSource, Map<String, Double>>();

    /**
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: emotion tab bigram in ngram type; value: frequency of appearing together.
     */
    private Map<Enums.NgramSource, Map<String, Double>> emotionNgramTypeBigramFreqs = new HashMap<Enums.NgramSource, Map<String, Double>>();

    /**
     *  Key: ngramType. Value: number of unigrams (tokens) that appeared in cause.
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
        for (Enums.NgramSource ngramSource : Enums.NgramSource.values()) {
            this.ngramTypeUnigramFreqs.put(ngramSource, new HashMap<String, Double>());
            this.ngramTypeBigramFreqs.put(ngramSource, new HashMap<String, Double>());
            this.emotionNgramTypeUnigramFreqs.put(ngramSource, new HashMap<String, Double>());
            this.emotionNgramTypeBigramFreqs.put(ngramSource, new HashMap<String, Double>());
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
            String[] extractionEmotionHolder = extraction.getEmotionHolder().split(" ");
            String[][] causes = { extractionNPCauseSplit, extractionSCauseSplit, extractionEmotionHolder};

            // iterate over NP cause, S cause, and emotion holder
            Enums.NgramSource[] ngramSources = new Enums.NgramSource[] {Enums.NgramSource.np_cause, Enums.NgramSource.s_cause,
                    Enums.NgramSource.emotion_holder};
            for (int j = 0; j < ngramSources.length; j++) {
                Enums.NgramSource ngramSource = ngramSources[j];

                for (int i = 1; i < causes[j].length + 1; i++) {

                    String unigram = ngramToLowerCase(causes[j][i - 1]);
                    if (unigram.equals("") || stopWords.contains(unigram)) {
                        continue;
                    }

                    Extensions.updateMap(this.causeUnigramCount, ngramSource.toString());

                    String emotionUnigram = emotion + "\t" + unigram;

                    // add unigram and emotion - unigram frequencies
                    Extensions.updateMap(ngramTypeUnigramFreqs.get(ngramSource), unigram);
                    Extensions.updateMap(emotionNgramTypeUnigramFreqs.get(ngramSource), emotionUnigram);

                    if (i < causes[j].length) {

                        String bigram = unigram + " " + ngramToLowerCase(causes[j][i]);
                        if (causes[j][i].equals("") || stopWords.contains(causes[j][i])) {
                            continue;
                        }

                        String emotionBigram = emotion + "\t" + bigram;

                        // add bigram and emotion - bigram frequencies
                        Extensions.updateMap(ngramTypeBigramFreqs.get(ngramSource), bigram);
                        Extensions.updateMap(emotionNgramTypeBigramFreqs.get(ngramSource), emotionBigram);
                    }
                }
            }

            // S cause subj + pred
            String[] subjSCause = extraction.getSubjSCause().split(" ");

            // convert ngram to lower case; replace NE tags
            String predSCause = ngramToLowerCase(extraction.getPredSCause());
            for (String token : subjSCause) {

                // don't consider empty tokens or stop words
                if (token.equals("") || stopWords.contains(token)) {
                    continue;
                }

                Enums.NgramSource ngramSource = Enums.NgramSource.s_cause_subj_pred;
                String bigram = ngramToLowerCase(token) + " " + predSCause;
                String emotionBigram = emotion + "\t" + bigram;
                Extensions.updateMap(ngramTypeBigramFreqs.get(ngramSource), bigram);
                Extensions.updateMap(emotionNgramTypeBigramFreqs.get(ngramSource), emotionBigram);
                Extensions.updateMap(this.causeUnigramCount, ngramSource.toString());
            }

            // S cause pred + dobj
            String[] dobjSCause = extraction.getDobjSCause().split(" ");

            for (int i = dobjSCause.length - 1; i > 0; i--) {
                String token = dobjSCause[i];
                if (token.equals("") || stopWords.contains(token) || token.contains(":")) {
                    continue;
                }
                else {
                    Enums.NgramSource ngramSource = Enums.NgramSource.s_cause_pred_dobj;
                    String bigram = predSCause + " " + ngramToLowerCase(token);
                    String emotionBigram = emotion + "\t" + bigram;
                    Extensions.updateMap(ngramTypeBigramFreqs.get(ngramSource), bigram);
                    Extensions.updateMap(emotionNgramTypeBigramFreqs.get(Enums.NgramSource.s_cause_pred_dobj), emotionBigram);
                    Extensions.updateMap(this.causeUnigramCount, ngramSource.toString());
                    break;
                }
            }
        }

        printFrequencies();
    }

    /**
     * Prints out the total unigram and bigram frequencies for all ngram types.
     */
    private void printFrequencies() {
        System.out.println("Ngram type\t# unigrams");
        printTotal(ngramTypeUnigramFreqs);
        System.out.println("\nNgram type\t# bigrams");
        printTotal(ngramTypeBigramFreqs);
    }

    /**
     * Prints out the total frequency of an ngram type given an ngram type map.
     * @param ngramTypeMap map with key: ngram type; value: map with key: ngram, value: frequency
     */
    private void printTotal(Map<Enums.NgramSource, Map<String, Double>> ngramTypeMap) {
        for (Enums.NgramSource ngramSource : ngramTypeMap.keySet()) {
            double total = 0;
            for (Map.Entry<String, Double> entry : ngramTypeMap.get(ngramSource).entrySet()) {
                total += entry.getValue();
            }

            System.out.printf("%s\t%f\n", ngramSource.toString(), total);
        }
    }

    /**
     * Convert ngram to lower if it isn't a named entity. Shorten named entity tags.
     * @param ngram the ngram to be converted to lower
     * @return the ngram to lower
     */
    private String ngramToLowerCase(String ngram) {
        if (ngram.contains("/")) {
            return ngram.replace("/PERSON", "/PERS").replace("/ORGANIZATION", "/ORG").replace("/LOCATION", "/LOC");
        }
        else if (ngram.equals("NUMBER")) {
            return "NUM";
        }
        else {
            return ngram.toLowerCase();
        }
    }

    /**
     * Calculates discounted point-wise mutual information. Discount filters out expressions that occur very rarely.
     * Stores them in a map with the ngram as key and the PMI score as value.
     * @param ngramSource ngram source that the PMI score should be calculated from
     * @param ngramEnum the ngram that should be used for calculation; unigram or bigram
     * @return the map of ngrams and their PMI score
     */
    public Map<String, Double> calculatePMI(Enums.NgramSource ngramSource, Enums.Ngram ngramEnum) {

        // get the appropriate parameters
        Map<String, Double> ngramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.ngramTypeUnigramFreqs.get(ngramSource) : this.ngramTypeBigramFreqs.get(ngramSource);
        Map<String, Double> emotionNgramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.emotionNgramTypeUnigramFreqs.get(ngramSource) : this.emotionNgramTypeBigramFreqs.get(ngramSource);
        double ngramCount = 0;
        ngramCount = this.causeUnigramCount.get(ngramSource.toString());

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
     * @param ngramSource ngram souce that chi-square should be calculated from
     * @param ngramEnum the ngram used for calculation
     * @return the map of ngrams and their chi-square score
     */
    public Map<String, Double> calculateChiSquare(Enums.NgramSource ngramSource, Enums.Ngram ngramEnum) {

        // get the appropriate parameters
        Map<String, Double> ngramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.ngramTypeUnigramFreqs.get(ngramSource) : this.ngramTypeBigramFreqs.get(ngramSource);
        Map<String, Double> emotionNgramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.emotionNgramTypeUnigramFreqs.get(ngramSource) : this.emotionNgramTypeBigramFreqs.get(ngramSource);
        double ngramCount = 0;
        ngramCount = this.causeUnigramCount.get(ngramSource.toString());

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

    // counts which unigrams/bigrams are shared per emotion

    /**
     * Puts the ngrams in a map that lists them along with their positive scores for each emotion.
     * @param ngramSource the source where the ngrams should be taken from
     * @param ngramEnum the ngrams that should be used
     * @param metricMap a map with key: emotion tab ngram; value: their association score
     * @return a map with key: ngram; value: a map with key: emotion, value: the association score for that emotion
     */
    public Map<String, Map<String, Double>> calculcateEmotionOverlap(Enums.NgramSource ngramSource, Enums.Ngram ngramEnum, Map<String, Double> metricMap) {

        // get the appropriate parameters
        Map<String, Double> ngramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.ngramTypeUnigramFreqs.get(ngramSource) : this.ngramTypeBigramFreqs.get(ngramSource);

        // create the overlap map
        Map<String, Map<String, Double>> overlapMap = new HashMap<String, Map<String, Double>>();

        // iterate through all ngrams and all emotions
        for (String ngram : ngramFreqs.keySet()) {

            for (String emotion : this.emotionFreqs.keySet()) {

                String emotionNgram = emotion + "\t" + ngram;

                // skip emotion if metric map doesn't contain a score for that emotion or score is smaller than 0
                if (!metricMap.containsKey(emotionNgram) || metricMap.get(emotionNgram) < 0) {
                    continue;
                }
                else if (!overlapMap.containsKey(ngram)) {

                    // otherwise create a new map if ngram doesn't exist yet
                    overlapMap.put(ngram, new HashMap<String, Double>());
                }

                // add ngram along with score for that emotion
                overlapMap.get(ngram).put(emotion, metricMap.get(emotionNgram));
            }
        }

        return overlapMap;
    }
}
