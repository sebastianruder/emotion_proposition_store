import java.util.*;

/**
 * Class containing methods to extend Java classes for easier use. Taken from Stackoverflow.
 */
public class Extensions
{
    /**
     * A list containing punctuation symbols.
     */
    private static List<String> punctuation = Arrays.asList(new String[] {":", ",", "''", "''", "``"});

    /**
     * Sorts a map by its values and returns it.
     * @param map the map that should be sorted
     * @param <K> the key type
     * @param <V> the value type
     * @return the map sorted by values
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, final boolean desc) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                if (desc) return -(o1.getValue()).compareTo(o2.getValue());
                else {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Returns a collection as a sorted list.
     * @param c the collection that should be sorted
     * @param <T> the type of the collection
     * @return the collection as a sorted list
     */
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    /**
     * Join the elements of an array with a separator.
     * @param array the input array
     * @param sep the separator that the list should be joined with
     * @return the joined list
     */
    public static String join(String[] array, String sep){

        if (array.length == 0) {
            return "";
        }

        StringBuilder sb = null;
        for (String element : array) {

            if (sb == null) {
                sb = new StringBuilder();
            }
            else if (!element.equals("") || punctuation.contains(element)) {
                sb.append(sep);
            }

            sb.append(element);
        }

        return sb.toString();
    }

    /**
     * Increments the <code>double</code> value of a map if it contains the key, otherwise adds the key to the map with
     * an initial value of 0 and increments it.
     * @param map a map of <code>String</code> and <code>Double</code>
     * @param key the key whose value should be updated
     */
    public static void updateMap(Map<String, Double> map, String key) {

        double value = map.containsKey(key) ? map.get(key) : 0;
        map.put(key, value + 1);
    }

    /**
     * Sorts a map by its values and returns it.
     * @param map the map that should be sorted
     * @return the map sorted by values
     */
    public static Map<String, Map<String, Double>> sortByAggregatedValue(Map<String, Map<String, Double>> map, final boolean desc) {
        List<Map.Entry<String, Map<String, Double>>> list =
                new LinkedList<Map.Entry<String, Map<String, Double>>>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<String, Map<String, Double>>>() {
            public int compare(Map.Entry<String, Map<String, Double>> o1, Map.Entry<String, Map<String, Double>> o2) {
                double score1 = 0;
                for (Map.Entry<String, Double> entry : o1.getValue().entrySet()) {
                    score1 += entry.getValue();
                }

                double score2 = 0;
                for (Map.Entry<String, Double> entry : o2.getValue().entrySet()) {
                    score2 += entry.getValue();
                }

                if (desc) return - Double.compare(score1, score2);
                else {
                    return Double.compare(score1, score2);
                }
            }
        });

        Map<String, Map<String, Double>> result = new LinkedHashMap<String, Map<String, Double>>();
        for (Map.Entry<String, Map<String, Double>> entry : list)
        {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
