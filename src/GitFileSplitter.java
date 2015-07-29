import java.io.*;

/**
 * Java class to split files larger than 100 MB in smaller chunks so that they can be uploaded via GitHub.
 *
 * Created by sebastian on 28/07/15.
 */
public class GitFileSplitter {

    /**
     * The main method to split the files.
     * @param args the input arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String filePath = "/home/sebastian/git/sentiment_analysis/out/results_final/collocations.txt";
        String targetDirectory = "/home/sebastian/git/sentiment_analysis/out/sentences";
        splitFile(filePath, targetDirectory);
    }

    /**
     * Splits a file in chunks in a target directory that are 100,000 lines long.
     * @param filePath the path to the file that should be split
     * @param targetDirectory the path to the target directory where the output files should be stored
     * @throws IOException
     */
    private static void splitFile(String filePath, String targetDirectory) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();

        int fileIdx = 0;
        PrintWriter writer = initializeWriter(targetDirectory, fileIdx++);

        int count = 0;
        while (line != null) {

            if (++count % 100000 == 0) {
                writer.close();
                writer = initializeWriter(targetDirectory, fileIdx++);
            }

            writer.println(line);
            line = reader.readLine();
        }

        writer.close();
    }

    private static PrintWriter initializeWriter(String targetDirectory, int fileIdx) throws IOException {
        String fileName = String.format("shelf_%d.txt", fileIdx);
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(Utils.combine(targetDirectory, fileName), true)));
        return writer;
    }
}
