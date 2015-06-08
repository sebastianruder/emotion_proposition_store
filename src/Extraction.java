import java.util.Arrays;

/**
 * The class that stores information pertaining to each extraction.
 *
 * Created by sebastian on 21/05/15.
 */
public class Extraction {

    /**
     * The id of the extraction in the format document ID slash sentence ID.
     */
    private String id;

    /**
     * The emotion of the extraction.
     */
    private String emotion;

    /**
     * The pattern that yielded the extraction.
     */
    private String pattern;

    /**
     * The emotion holder.
     */
    private String emotionHolder;

    /**
     * The NP cause. Can be empty.
     */
    private String NPCause;

    /**
     * The subject of the S cause. Can be empty.
     */
    private String subjSCause;

    /**
     * The predicate of the S cause. Can be empty.
     */
    private String predSCause;

    /**
     * The direct object of the S cause. Can be empty.
     */
    private String dobjSCause;

    /**
     * The prepositional objects of the S cause in the format [pobj1, pobj2, ...]. Can be empty.
     */
    private String pobjsString;

    /**
     * The cause as a bag-of-words, tagged with parts-of-speech, in the format [word1, word2, ...].
     */
    private String causeBoWString;

    /**
     * The prepositional objects of the S cause as an array. Can be empty.
     */
    private String[] pobjs;

    /**
     * The bag-of-words of the cause as an array.
     */
    private String[] causeBoW;

    /**
     * Creates an instance of an <code>Extraction</code>.
     * @param id the id of the extraction
     * @param emotion the emotion
     * @param pattern the pattern
     * @param emotionHolder the emotion holder
     * @param NPCause the NP cause
     * @param subjSCause the subject of the S cause
     * @param predSCause the predicate of the S cause
     * @param dobjSCause the direct object of the S cause
     * @param pobjs the prepositional object string of the S cause
     * @param causeBoW the bag-of-words string of the cause
     */
    public Extraction(String id, String emotion, String pattern, String emotionHolder, String NPCause, String subjSCause,
                      String predSCause, String dobjSCause, String pobjs, String causeBoW) {
        this.id = id;
        this.emotion = emotion;
        this.pattern = pattern;
        this.emotionHolder = emotionHolder;
        this.NPCause = NPCause;
        this.subjSCause = subjSCause;
        this.predSCause = predSCause;
        this.dobjSCause = dobjSCause;
        this.pobjsString = pobjs;
        this.causeBoWString = causeBoW;
        this.pobjs = pobjs.replaceAll("^\\[|\\]$", "").split(", ");
        this.causeBoW = causeBoW.replaceAll("^\\[|\\]$", "").split(", ");
    }

    /**
     * Transforms the extraction to its output format.
     * @return the string representation of an extraction
     */
    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", this.id, this.emotion, this.pattern,
                this.emotionHolder, this.NPCause, this.subjSCause, this.predSCause, this.dobjSCause, this.pobjsString,
                this.causeBoWString);
    }

    /**
     * Compares to extractions with each other. They are only equal if all fields are equal except for the id.
     * @param object the object against which the extraction should be compared
     * @return 0 if the extractions are equal, else -1 or +1
     */
    @Override
    public boolean equals(Object object) {

        if (object == null || object.getClass() != Extraction.class) {
            return false;
        }

        Extraction other = (Extraction)object;

        // ids can be different
        return this.emotion.equals(other.emotion) && this.pattern.equals(other.pattern) &&
                this.emotionHolder.equals(other.emotionHolder) && this.NPCause.equals(other.NPCause) &&
                this.subjSCause.equals(other.subjSCause) && this.predSCause.equals(other.predSCause) &&
                this.dobjSCause.equals(other.subjSCause) && this.pobjsString.equals(other.pobjsString) &&
                this.causeBoWString.equals(other.causeBoWString);
    }

    /**
     * Generate the hash code of an extraction.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = emotion.hashCode();
        result = 31 * result + pattern.hashCode();
        result = 31 * result + emotionHolder.hashCode();
        result = 31 * result + NPCause.hashCode();
        result = 31 * result + subjSCause.hashCode();
        result = 31 * result + predSCause.hashCode();
        result = 31 * result + dobjSCause.hashCode();
        result = 31 * result + pobjsString.hashCode();
        result = 31 * result + causeBoWString.hashCode();
        return result;
    }

    public String getId() {
        return id;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getPattern() {
        return pattern;
    }

    public String getEmotionHolder() {
        return emotionHolder;
    }

    public String getNPCause() {
        return NPCause;
    }

    public String getSubjSCause() {
        return subjSCause;
    }

    public String getPredSCause() {
        return predSCause;
    }

    public String getDobjSCause() {
        return dobjSCause;
    }

    public String getPobjsString() {
        return pobjsString;
    }

    public String getCauseBoWString() {
        return causeBoWString;
    }

    public String[] getPobjs() {
        return pobjs;
    }

    public String[] getCauseBoW() {
        return causeBoW;
    }
}
