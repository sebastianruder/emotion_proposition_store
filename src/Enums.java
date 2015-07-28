import sun.reflect.generics.reflectiveObjects.NotImplementedException;
/**
 * Created by sebastian on 28/03/15.
 *
 * Enum class containing enums that are used for consistency throughout the project.
 */
public class Enums {

    /**
     * Enumeration pertaining to statistics
     */
    public enum Stats {

        /**
         * The number of matches
         */
        matches,

        /**
         * The emotion
         */
        emotion
    }

    /**
     * Enumeration pertaining to features
     */
    public enum Features {

        /**
         * If the object of a pattern is an NP; else it is whole sub-clause, i.e. S.
         */
        isNP,

        /**
         * If the regular order of subject := emotion holder and object := cause is reversed, e.g. for "scare".
         */
        orderIsReversed
    }

    /**
     * Enumeration containing Plutchik's eight emotions that are used.
     */
    public enum Emotions {
        anger, anticipation, disgust, fear, joy, sadness, surprise, trust
    }

    /**
     * Enumeration for the two association metrics that are used.
     */
    public enum Metric {
        pmi, chi_square
    }

    /**
     * Enumeration containing the two ngrams that are used.
     */
    public enum Ngram {
        unigram, bigram
    }

    /**
     * Enumeration containing the different sources the ngrams can come from.
     */
    public enum NgramSource {
        np_cause, s_cause, s_cause_subj_pred, s_cause_pred_dobj, emotion_holder
    }

    /**
     * Enumeration containing the different possible sentiments.
     */
    public enum Sentiment {
        positive, negative, neutral
    }

    /**
     * Enumeration containing the possible degrees of overlap with the NRC Emotion-Association Lexicon (EmoLex).
     */
    public enum NRCOverlap {
        FALSE, TRUE, NA
    }

    /**
     * Converts an emotion enum to its corresponding sentiment.
     * @param emotion the emotion enum
     * @return the corresponding sentiment
     */
    public static Sentiment emotionToSentiment(Emotions emotion) {
        switch (emotion) {
            case anger:
            case disgust:
            case fear:
            case sadness:
                return Sentiment.negative;
            case joy:
            case trust:
                return Sentiment.positive;
            case anticipation:
            case surprise:
                return Sentiment.neutral;
            default:
                throw new NotImplementedException();
        }
    }
}
