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
    private Map<Enums.NgramType, Map<String, Double>> ngramTypeUnigramFreqs = new HashMap<Enums.NgramType, Map<String, Double>>();

    /**
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: emotion tab unigram in ngram type; value: frequency of appearing together.
     */
    private Map<Enums.NgramType, Map<String, Double>> emotionNgramTypeUnigramFreqs = new HashMap<Enums.NgramType, Map<String, Double>>();

    /**
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: bigram in ngram type; value: frequency of appearance.
     */
    private Map<Enums.NgramType, Map<String, Double>> ngramTypeBigramFreqs = new HashMap<Enums.NgramType, Map<String, Double>>();

    /**
     * Key: np_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder. Value: Map with key: emotion tab bigram in ngram type; value: frequency of appearing together.
     */
    private Map<Enums.NgramType, Map<String, Double>> emotionNgramTypeBigramFreqs = new HashMap<Enums.NgramType, Map<String, Double>>();

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
        for (Enums.NgramType ngramType : Enums.NgramType.values()) {
            this.ngramTypeUnigramFreqs.put(ngramType, new HashMap<String, Double>());
            this.ngramTypeBigramFreqs.put(ngramType, new HashMap<String, Double>());
            this.emotionNgramTypeUnigramFreqs.put(ngramType, new HashMap<String, Double>());
            this.emotionNgramTypeBigramFreqs.put(ngramType, new HashMap<String, Double>());
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
            Enums.NgramType[] ngramTypes = new Enums.NgramType[] {Enums.NgramType.np_cause, Enums.NgramType.s_cause,
                    Enums.NgramType.emotion_holder};
            for (int j = 0; j < ngramTypes.length; j++) {
                Enums.NgramType ngramType = ngramTypes[j];

                for (int i = 1; i < causes[j].length + 1; i++) {

                    String unigram = ngramToLowerCase(causes[j][i - 1]);
                    if (unigram.equals("") || stopWords.contains(unigram)) {
                        continue;
                    }

                    Extensions.updateMap(this.causeUnigramCount, ngramType.toString());

                    String emotionUnigram = emotion + "\t" + unigram;

                    // add unigram and emotion - unigram frequencies
                    Extensions.updateMap(ngramTypeUnigramFreqs.get(ngramType), unigram);
                    Extensions.updateMap(emotionNgramTypeUnigramFreqs.get(ngramType), emotionUnigram);

                    if (i < causes[j].length) {

                        String bigram = unigram + " " + ngramToLowerCase(causes[j][i]);
                        if (causes[j][i].equals("") || stopWords.contains(causes[j][i])) {
                            continue;
                        }

                        String emotionBigram = emotion + "\t" + bigram;

                        // add bigram and emotion - bigram frequencies
                        Extensions.updateMap(ngramTypeBigramFreqs.get(ngramType), bigram);
                        Extensions.updateMap(emotionNgramTypeBigramFreqs.get(ngramType), emotionBigram);
                    }
                }
            }

            // S cause subj + pred
            String[] subjSCause = extraction.getSubjSCause().split(" ");
            String predSCause = ngramToLowerCase(extraction.getPredSCause());
            for (String token : subjSCause) {
                if (token.equals("") || stopWords.contains(token)) {
                    continue;
                }

                Enums.NgramType ngramType = Enums.NgramType.s_cause_subj_pred;
                String bigram = ngramToLowerCase(token) + " " + predSCause;
                String emotionBigram = emotion + "\t" + bigram;
                Extensions.updateMap(ngramTypeBigramFreqs.get(ngramType), bigram);
                Extensions.updateMap(emotionNgramTypeBigramFreqs.get(ngramType), emotionBigram);
                Extensions.updateMap(this.causeUnigramCount, ngramType.toString());
            }

            // S cause pred + dobj
            String[] dobjSCause = extraction.getDobjSCause().split(" ");

            for (int i = dobjSCause.length - 1; i > 0; i--) {
                String token = dobjSCause[i];
                if (token.equals("") || stopWords.contains(token) || token.contains(":")) {
                    continue;
                }
                else {
                    Enums.NgramType ngramType = Enums.NgramType.s_cause_pred_dobj;
                    String bigram = predSCause + " " + ngramToLowerCase(token);
                    String emotionBigram = emotion + "\t" + bigram;
                    Extensions.updateMap(ngramTypeBigramFreqs.get(ngramType), bigram);
                    Extensions.updateMap(emotionNgramTypeBigramFreqs.get(Enums.NgramType.s_cause_pred_dobj), emotionBigram);
                    Extensions.updateMap(this.causeUnigramCount, ngramType.toString());
                    break;
                }
            }
        }

        printFrequencies();
    }

    private void printFrequencies() {
        System.out.println("Ngram type\t# unigrams");
        printTotal(ngramTypeUnigramFreqs);
        System.out.println("\nNgram type\t# bigrams");
        printTotal(ngramTypeBigramFreqs);
    }

    private void printTotal(Map<Enums.NgramType, Map<String, Double>> ngramTypeMap) {
        for (Enums.NgramType ngramType: ngramTypeMap.keySet()) {
            double total = 0;
            for (Map.Entry<String, Double> entry : ngramTypeMap.get(ngramType).entrySet()) {
                total += entry.getValue();
            }

            System.out.printf("%s\t%f\n", ngramType.toString(), total);
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
     * @param isNP if PMI should be calculated from NP ngrams; else: S ngrams
     * @param isUnigram if PMI should be calculated from unigrams; else: bigrams
     * @return the map of ngrams and their PMI score
     */
    public Map<String, Double> calculatePMI(Enums.NgramType ngramType, Enums.Ngram ngramEnum) {

        // get the appropriate parameters
        Map<String, Double> ngramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.ngramTypeUnigramFreqs.get(ngramType) : this.ngramTypeBigramFreqs.get(ngramType);
        Map<String, Double> emotionNgramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.emotionNgramTypeUnigramFreqs.get(ngramType) : this.emotionNgramTypeBigramFreqs.get(ngramType);
        double ngramCount = 0;
        try {
            ngramCount = this.causeUnigramCount.get(ngramType.toString());
        }
        catch (NullPointerException ex) {
            System.out.println(ngramType.toString() + " " + ngramEnum.toString());
            System.out.println();
        }

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
    public Map<String, Double> calculateChiSquare(Enums.NgramType ngramType, Enums.Ngram ngramEnum) {

        // get the appropriate parameters
        Map<String, Double> ngramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.ngramTypeUnigramFreqs.get(ngramType) : this.ngramTypeBigramFreqs.get(ngramType);
        Map<String, Double> emotionNgramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.emotionNgramTypeUnigramFreqs.get(ngramType) : this.emotionNgramTypeBigramFreqs.get(ngramType);
        double ngramCount = 0;
        try {
            ngramCount = this.causeUnigramCount.get(ngramType.toString());
        }
        catch (NullPointerException ex) {
            System.out.println(ngramType.toString() + " " + ngramEnum.toString());
            System.out.println();
        }

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
    public Map<String, Map<String, Double>> calculcateEmotionOverlap(Enums.NgramType ngramType, Enums.Ngram ngramEnum, Map<String, Double> metricMap) {

        // get the appropriate parameters
        Map<String, Double> ngramFreqs = ngramEnum.equals(Enums.Ngram.unigram) ? this.ngramTypeUnigramFreqs.get(ngramType) : this.ngramTypeBigramFreqs.get(ngramType);

        Map<String, Map<String, Double>> overlapMap = new HashMap<String, Map<String, Double>>();

        for (String ngram : ngramFreqs.keySet()) {

            for (String emotion : this.emotionFreqs.keySet()) {

                String emotionNgram = emotion + "\t" + ngram;

                if (!metricMap.containsKey(emotionNgram) || metricMap.get(emotionNgram) < 0) {
                    continue;
                }
                else if (!overlapMap.containsKey(ngram)) {
                    overlapMap.put(ngram, new HashMap<String, Double>());
                }

                overlapMap.get(ngram).put(emotion, metricMap.get(emotionNgram));
            }
        }

        return overlapMap;
    }
}
