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
        joy, trust, fear, surprise, sadness, disgust, anger, anticipation
    }
}
