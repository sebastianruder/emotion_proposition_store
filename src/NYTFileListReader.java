import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebastian on 28/03/15.
 */
public class NYTFileListReader {

    /**
     * Read the fileList file as produced by Gorski's jar
     * @param fileName the name of the file
     * @return a list of the NYT XML doc ids
     */
    public static List<String> readFileList(String fileName) throws IOException {

        InputStream inputStream = new FileInputStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        List<String> ids = new ArrayList<String>();

        String line = reader.readLine();
        while (line != null) {
            String[] list = line.split("/");
            String docId = list[3].split("\\.")[0];
            if (docId.length() != 7) {
                throw new InvalidObjectException(String.format("{0} is not a valid NYT XML file ID. " +
                        "ID should be 7 digits long.", docId));
            }

            ids.add(docId);

            System.out.println(docId);

            // read next line
            line = reader.readLine();
        }

        return ids;
    }

    public static void main(String[] args) throws IOException {
        readFileList("/home/sebastian/git/sentiment_analysis/gorski/tools/fileList_Reviews.txt");
    }
}
