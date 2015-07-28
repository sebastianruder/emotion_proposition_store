/**
 * Created by sebastian on 01/06/15.
 */

import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SubCategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Class to generate charts given a sorted list of association scores.
 */
public class Visualizer {

    /**
     * The directory of the scores and the output directory of the generated charts.
     */
    private static String rootDir = "/home/sebastian/git/sentiment_analysis/out/scores/";

    /**
     * Runs all visualization methods.
     * @param args the input arguments
     * @throws java.io.IOException if a file wasn't found or couldn't be read
     */
    public static void main(String[] args) throws IOException {



        for (Enums.Metric metricEnum : Enums.Metric.values()) {
            // if (metricEnum.equals(Enums.Metric.chi_square)) continue;

            String metric = metricEnum.toString();
            String metricDir = Utils.combine(rootDir, metric);
            Utils.cleanDirectory(Utils.combine(metricDir, "graphs"), ".jpg");

            for (Enums.Ngram ngramEnum : Enums.Ngram.values()) {

                if (ngramEnum.equals(Enums.Ngram.unigram)) continue;

                String ngram = ngramEnum.toString();
                for (Enums.NgramType ngramTypeEnum : Enums.NgramType.values()) {

                    String ngramType = ngramTypeEnum.toString();

                    if (ngramTypeEnum.equals(Enums.NgramType.s_cause)) {
                        continue;
                    }

                    if (metricEnum.equals(Enums.Metric.pmi)) {
                        generateLineCharts(metricDir, metric, ngram, ngramType, 50, 10000);
                    }
                    else {
                        generateLineCharts(metricDir, metric, ngram, ngramType, 50, 100);
                    }


                    generateSentimentCharts(metricDir, metric, ngram, ngramType, 50);
                    generateEmotionOverlapCharts(metricDir, metric, ngram, ngramType, 50);
                    generateNRCOverlapChart(metricDir, metric, ngramType, 30);
                }
            }
        }
    }

    public static void generateSentimentCharts(String metricDir, String metric, String ngram, String ngramType, int topN) throws IOException {

        List<String> fileNames = Utils.getFileNames(metricDir, ngram, ngramType, "(\\.pos|\\.neg)");
        for (String fileName : fileNames) {

            String sentiment = fileName.endsWith(".pos") ? "positive" : "negative";
            BufferedReader reader = new BufferedReader(new FileReader(Utils.combine(metricDir, fileName)));

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            String line = reader.readLine();
            int i = 0; // row count
            while (line != null && !line.equals("") && i++ < topN) {
                String expression = line.split("\t")[0];
                double value = Double.parseDouble(line.split("\t")[1]);
                dataset.addValue(value , metric , expression);
                line = reader.readLine();
            }

            reader.close();

            final JFreeChart chart = ChartFactory.createLineChart(
                    String.format("Top %d %s %ss %s scores in %s", topN, sentiment, ngram, metric, ngramType.replace("_", " ")),
                    ngram,
                    metric,             // range axis label
                    dataset,                     // data
                    PlotOrientation.HORIZONTAL,    // the plot orientation
                    true, true, false);


            CategoryPlot categoryPlot = (CategoryPlot) chart.getPlot();
            // rotateXAxisLabels(categoryPlot);
            NumberAxis valueAxis = (NumberAxis) categoryPlot.getRangeAxis();

            // auto-scale y-axis
            valueAxis.setAutoRangeIncludesZero(false);

            String lineChartFileName = String.format("%s_%s_%s_%s_%d.jpg", sentiment, metric, ngram, ngramType, topN);
            saveChart(metricDir, lineChartFileName, chart, 820, 720);
            // displayChart(lineChartFileName, chart);
        }
    }

    /**
     * Produces line charts for the top n expressions per emotion for a metric with y := the association score,
     * x : = the expression. Produces one chart for unigrams and one chart for bigrams.
     * Produces one series chart for unigrams and one series chart for bigrams comparing the scores of the top n
     * expressions of all emotions.
     * @param rootDir the directory below the metric directory
     * @param metric the association metric which has been used for generating the data
     * @param topN the top n expressions whose score should be visualized for each emotion
     * @param topNSeries the top n expressions that should be compared in the series chart
     * @throws IOException if a file can't be read
     */
    public static void generateLineCharts(String metricDir, String metric, String ngram, String ngramType, int topN, int topNSeries) throws IOException {

        XYSeriesCollection seriesData = new XYSeriesCollection();

        List<String> fileNames = Utils.getFileNames(metricDir, ngram, ngramType, ".txt");
        Collections.sort(fileNames);
        for (String fileName : fileNames) {
            String emotion = fileName.split("_")[0];

            // create a timeseries for comparison
            XYSeries series = new XYSeries(emotion);

            BufferedReader reader = new BufferedReader(new FileReader(Utils.combine(metricDir, fileName)));
            DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
            String line = reader.readLine();
            int i = 0; // row count
            while (line != null && !line.equals("") && i++ < topNSeries) {
                String expression = line.split("\t")[0];
                double value = Double.parseDouble(line.split("\t")[1]);
                if (i < topN) {
                    line_chart_dataset.addValue(value , metric , expression);
                }

                series.add(i, value);
                line = reader.readLine();
            }

            seriesData.addSeries(series);

            JFreeChart lineChartObject = ChartFactory.createLineChart(
                    String.format("Top %d %s %ss %s scores", topN, emotion, ngram, metric),
                    ngram,
                    metric,
                    line_chart_dataset,
                    PlotOrientation.HORIZONTAL,
                    true, true, false);

            CategoryPlot categoryPlot = (CategoryPlot) lineChartObject.getPlot();
            // rotateXAxisLabels(categoryPlot);
            NumberAxis valueAxis = (NumberAxis) categoryPlot.getRangeAxis();

            // auto-scale y-axis
            valueAxis.setAutoRangeIncludesZero(false);

            String lineChartFileName = String.format("%s_%s_%s_%s_%d.jpg", emotion, metric, ngram, ngramType, topN);
            saveChart(metricDir, lineChartFileName, lineChartObject, 820, 720);
            //displayChart(emotion + "_" + metric, lineChartObject);
        }

        if (seriesData.getSeriesCount() > 0) {
            JFreeChart seriesChart = ChartFactory.createXYLineChart(
                    String.format("%s score of top %d %ss per emotion in %s", metric, topNSeries, ngram, ngramType.replace("_", " ")),
                    "Top expressions",
                    metric,
                    seriesData,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            XYPlot xyPlot = seriesChart.getXYPlot();
            XYItemRenderer renderer = xyPlot.getRenderer();
            for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {
                renderer.setSeriesPaint(emotionEnum.ordinal(), emotionToColor(emotionEnum));
            }

            seriesChart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 25));
            ((AbstractRenderer) xyPlot.getRenderer()).setBaseLegendShape(new Rectangle(30,30));

            String seriesFileName = String.format("%s_%s_%s_comparison_%s.jpg", metric, ngram, ngramType, topNSeries);
            saveChart(metricDir, seriesFileName, seriesChart, 1280, 800);
            // displayChart("", seriesChart);
        }
    }

    /**
     * Generates one grouped stacked bar chart for the top n expressions per emotion for one metric to visualize if
     * expressions occurred with the same emotion and the same sentiment in the NRC Emotion Lexicon.
     * Since the emotion lexicon only contains unigrams, this is only done for those.
     * @param rootDir the directory below the metric directory
     * @param metric the association metric which has been used for generating the data
     * @param topN the top n expressions whose overlap should be visualized
     * @throws IOException if a file wasn't found or couldn't be read
     */
    public static void generateNRCOverlapChart(String metricDir, String metric, String ngramType, int topN) throws IOException {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> fileNames = Utils.getFileNames(metricDir, Enums.Ngram.bigram.toString(), ngramType, ".txt");
        Collections.sort(fileNames);

        Map<Enums.NRCOverlap, Map<Enums.Emotions, Map<String, Double>>> overlapMap = new HashMap<Enums.NRCOverlap, Map<Enums.Emotions, Map<String, Double>>>();
        for (Enums.NRCOverlap overlap : Enums.NRCOverlap.values()) {
            overlapMap.put(overlap, new HashMap<Enums.Emotions, Map<String, Double>>());
        }

        for (String fileName : fileNames) {
            String emotion = fileName.split("_")[0];

            BufferedReader reader = new BufferedReader(new FileReader(Utils.combine(metricDir, fileName)));
            String line = reader.readLine();

            // count the occurrences of FALSE, TRUE, and NA per emotion and sentiment for each file (i.e. emotion)
            int[] NRCEmotionOverlapCounts = new int[3];
            int[] NRCSentimentOverlapCounts = new int[3];
            int i = 0; // row count
            while (line != null && !line.equals("") && i++ < topN) {
                String NRCEmotionOverlap = line.split("\t")[2];
                String NRCSentimentOverlap = line.split("\t")[3];

                NRCEmotionOverlapCounts[Enums.NRCOverlap.valueOf(NRCEmotionOverlap).ordinal()]++;
                NRCSentimentOverlapCounts[Enums.NRCOverlap.valueOf(NRCSentimentOverlap).ordinal()]++;
                line = reader.readLine();
            }

            // System.out.printf("\nNRC Overlap %s %s %s\n", emotion, metric, ngramType);
            // System.out.printf("Overlap\tEmotion\tSentiment\n");

            // add the counts to the dataset
            for (Enums.NRCOverlap overlap : Enums.NRCOverlap.values()) {
                // get percentages
                double emotionPercent = (double)NRCEmotionOverlapCounts[overlap.ordinal()] / (double)topN * 100;
                double sentimentPercent = (double)NRCSentimentOverlapCounts[overlap.ordinal()] / (double)topN * 100;
                // store values in map
                overlapMap.get(overlap).put(Enums.Emotions.valueOf(emotion), new HashMap<String, Double>());
                overlapMap.get(overlap).get(Enums.Emotions.valueOf(emotion)).put("Emotion", emotionPercent);
                overlapMap.get(overlap).get(Enums.Emotions.valueOf(emotion)).put("Sentiment", sentimentPercent);

                // System.out.printf("%s\t%.2f\t%.2f\n", overlap.toString(), emotionPercent, sentimentPercent);
                dataset.addValue(emotionPercent, "Emotion overlap (" + overlap.toString() + ")", emotion);
                dataset.addValue(sentimentPercent, "Sentiment overlap (" + overlap.toString() + ")", emotion);
            }

            reader.close();
        }

        final JFreeChart chart = ChartFactory.createStackedBarChart(
                String.format("Emotion/sentiment NRC overlap per emotion for top %d %s expressions", topN, metric),
                "Emotion",                  // domain axis label
                "Percentage of expressions",                // range axis label
                dataset,                     // data
                PlotOrientation.VERTICAL,    // the plot orientation
                true, true, false);

        // group data points per emotion and sentiment; produces eight emotion groups
        GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();
        KeyToGroupMap map = new KeyToGroupMap("G1");
        map.mapKeyToGroup("Emotion overlap (NA)", "G1");
        map.mapKeyToGroup("Emotion overlap (TRUE)", "G1");
        map.mapKeyToGroup("Emotion overlap (FALSE)", "G1");
        map.mapKeyToGroup("Sentiment overlap (NA)", "G2");
        map.mapKeyToGroup("Sentiment overlap (TRUE)", "G2");
        map.mapKeyToGroup("Sentiment overlap (FALSE)", "G2");
        renderer.setSeriesToGroupMap(map);
        renderer.setItemMargin(0.0);
        SubCategoryAxis domainAxis = new SubCategoryAxis("Overlap / Emotion");
        domainAxis.setCategoryMargin(0.15);
        domainAxis.addSubCategory("Emotion");
        domainAxis.addSubCategory("Sentiment");
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setDomainAxis(domainAxis);

        // set FALSE to red, TRUE, to green, and NA to gray
        renderer.setSeriesPaint(0, Color.red);
        renderer.setSeriesPaint(1, Color.red);
        renderer.setSeriesPaint(2, Color.green);
        renderer.setSeriesPaint(3, Color.green);
        renderer.setSeriesPaint(4, Color.gray);
        renderer.setSeriesPaint(5, Color.gray);

        plot.setRenderer(renderer);

        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 25));
        ((AbstractRenderer) plot.getRenderer()).setBaseLegendShape(new Rectangle(30,30));

        String chartFileName = String.format("%s_%s_nrc_overlap_%d.jpg", metric, ngramType, topN);
        saveChart(metricDir, chartFileName, chart, 1280, 800);

        System.out.printf("\nNRC Overlap %s %s\n", metric, ngramType);
        printEmotionOverlapValues(0, 4, overlapMap);
        printEmotionOverlapValues(4, 8, overlapMap);
    }

    public static void printEmotionOverlapValues(int start, int end, Map<Enums.NRCOverlap, Map<Enums.Emotions, Map<String, Double>>> overlapMap) {

        System.out.print("Overlap\t");
        for (int i = start; i < end; i++) {
            Enums.Emotions[] emotions = Enums.Emotions.values();
            if (i == end - 1) {
                System.out.println(emotions[i].toString());
            }
            else {
                System.out.print(emotions[i].toString() + "\t\t");
            }
        }

        for (int i = start; i < end; i++) {
            System.out.print("\tEmo\tSen");
        }

        for (Enums.NRCOverlap overlap : overlapMap.keySet()) {
            System.out.print("\n" + overlap.toString());
            for (int i = start; i < end; i++) {
                Enums.Emotions[] emotions = Enums.Emotions.values();
                double emotionPercent = overlapMap.get(overlap).get(emotions[i]).get("Emotion");
                double sentimentPercent = overlapMap.get(overlap).get(emotions[i]).get("Sentiment");
                System.out.printf(Locale.US, "\t%.2f\t%.2f", emotionPercent, sentimentPercent);
            }
        }

        System.out.println();
    }

    /**
     * Generates a stacked bar chart visualizing how much the score of the top n expressions with the highest
     * aggregated association score is distributed among all emotions. Produces one chart for unigrams and one for
     * bigrams.
     * @param rootDir the directory below the metric directory
     * @param metric the metric that has been used to generate the data
     * @param topN the top n expressions with the highest aggregated association score that should be visualized
     * @throws IOException if a file wasn't found or couldn't be read
     */
    public static void generateEmotionOverlapCharts(String metricDir, String metric, String ngram, String ngramType, int topN) throws IOException {

        List<String> fileNames = Utils.getFileNames(metricDir, ngram, ngramType, ".overlap");
        Collections.sort(fileNames);

        for (String fileName : fileNames) {
            if (!fileName.contains(ngram) || fileName.contains("percent")) {
                continue;
            }

            BufferedReader reader = new BufferedReader(new FileReader(Utils.combine(metricDir, fileName)));

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            String line = reader.readLine();
            int i = 0; // row count
            while (line != null && !line.equals("") && i++ < topN) {

                // skip column names
                if (i == 1) {
                    line = reader.readLine();
                    continue;
                }

                String[] lineSplit = line.split(",");
                String expression = lineSplit[0];
                for (int j = 1; j < lineSplit.length; j++) {
                    double value = 0;
                    try {
                        value = Double.parseDouble(lineSplit[j]);
                    }
                    catch (NumberFormatException ex) {
                        System.out.println();
                    }
                    if (fileName.contains("_pos_neg")) {
                        String sentiment = Enums.Sentiment.values()[j - 1].toString();
                        dataset.addValue(value, sentiment, expression);
                    }
                    else {
                        String emotion = Enums.Emotions.values()[j - 1].toString();
                        dataset.addValue(value, emotion, expression);
                    }
                }

                line = reader.readLine();
            }

            reader.close();

            String emotionOrSentiment = "Emotion";
            if (fileName.contains("_pos_neg")) {
                emotionOrSentiment = "Sentiment";
            }

            final JFreeChart chart = ChartFactory.createStackedBarChart(
                    String.format("%s overlap of top %d %ss by %s in %s", emotionOrSentiment, topN, ngram, metric, ngramType.replace("_", " ")),    // chart title
                    "Expression",                  // domain axis label
                    metric,                // range axis label
                    dataset,                     // data
                    PlotOrientation.HORIZONTAL,    // the plot orientation
                    true, true, false);

            CategoryPlot categoryPlot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) categoryPlot.getRenderer();
            if (fileName.contains("_pos_neg")) {
                renderer.setSeriesPaint(0, Color.green);
                renderer.setSeriesPaint(1, Color.red);
                renderer.setSeriesPaint(2, Color.lightGray);
            }
            else {
                for (Enums.Emotions emotionEnum : Enums.Emotions.values()) {
                    renderer.setSeriesPaint(emotionEnum.ordinal(), emotionToColor(emotionEnum));
                }
            }

            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 25));
            ((AbstractRenderer) categoryPlot.getRenderer()).setBaseLegendShape(new Rectangle(30,30));

            String chartFileName = String.format("%s_%s_%s_%s_overlap_%d.jpg", metric, ngram, ngramType, emotionOrSentiment.toLowerCase(), topN);
            saveChart(metricDir, chartFileName, chart, 1280, 800);
        }
    }

    /**
     * Save a chart in a sub-directory called graphs of the original directory.
     * @param dir the directory in whose graphs sub-directory the chart should be saved
     * @param fileName the name of the file that should be used for saving the chart
     * @param chart the <code>JFreeChart</code> that should be saved
     * @param width the width of the image used for saving
     * @param height the height of the image used for saving
     * @throws IOException if the file wasn't found or couldn't be read
     */
    private static void saveChart(String dir, String fileName, JFreeChart chart, int width, int height) throws IOException {

        // check if graph directory exists
        File graphDir = new File(Utils.combine(dir, "graphs"));
        if (!graphDir.isDirectory()) {
            // chiSquareDir.mkdir(); // create directory
            throw new IOException(graphDir + " is not a directory.");
        }

        String filePath = Utils.combine(graphDir.getPath(), fileName);
        File lineChartFile = new File(filePath);
        ChartUtilities.saveChartAsJPEG(lineChartFile, chart, width, height);
    }

    /**
     * Rotates the labels of the X axis of a specified plot.
     * @param categoryPlot the category plot whose labels should be rotated
     */
    private static void rotateXAxisLabels(CategoryPlot categoryPlot) {
        CategoryAxis categoryAxis = categoryPlot.getDomainAxis();
        categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
    }

    /**
     * Produces an emotion's color according to a plausible emotion color scheme.
     * @param emotion the emotion whose color should be retrieved
     */
    public static Color emotionToColor(Enums.Emotions emotion) {

        switch (emotion) {
            case anger:
                return Color.red;
            case sadness:
                return Color.blue;
            case disgust:
                return Color.cyan;
            case trust:
                return Color.green;
            case fear:
                return Color.black;
            case surprise:
                return Color.yellow;
            case joy:
                return Color.magenta;
            case anticipation:
                return Color.gray;
            default:
                throw new NotImplementedException();
        }
    }

    /**
     * Displays chart.
     * @param title the title of the chart
     * @param chart the <code>JFreeChart</code> that should be displayed
     */
    public static void displayChart(String title, JFreeChart chart) {
        ChartFrame frame = new ChartFrame(title, chart);
        frame.pack();
        frame.setVisible(true);
    }
}
