import java.util.*;

/**
 * Class containing methods to extend Java classes for easier use. Taken from Stackoverflow.
 */
public class Extensions
{
    /**
     * Sorts a map by its values and returns it.
     * @param map the map that should be sorted
     * @param <K> the key type
     * @param <V> the value type
     * @return the map sorted by values
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return - (o1.getValue()).compareTo(o2.getValue());
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
            else if (!element.equals("")) {
                sb.append(sep);
            }

            sb.append(element);
        }

        return sb.toString();
    }
}
