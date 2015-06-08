/**
 * Created by sebastian on 01/06/15.
 */

import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

/**
 * Class to generate charts given a sorted list of association scores.
 */
public class Visualizer {

    /**
     * The directory of the scores and the output directory of the generated charts.
     */
    private static String dir = "/home/sebastian/git/sentiment_analysis/out/scores/";

    /**
     * Produces charts with y := the association score, x : = the expression.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        File fileDir = new File(dir);
        String[] fileNames = fileDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });

        for (String fileName : fileNames) {
            String emotion = fileName.split("_")[0];
            String metric = fileName.split("_")[1].split("\\.")[0];
            BufferedReader reader = new BufferedReader(new FileReader(dir + fileName));
            DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
            String line = reader.readLine();
            int i = 0; // row count
            while (line != null && !line.equals("") && i++ < 50) {
                String expression = line.split("\t")[0];
                double value = Double.parseDouble(line.split("\t")[1]);
                line_chart_dataset.addValue(value , metric , expression);
                line = reader.readLine();
            }

            JFreeChart lineChartObject = ChartFactory.createLineChart(
                    String.format("%s expressions %s scores", emotion, metric),"Expressions",
                    metric,
                    line_chart_dataset,PlotOrientation.VERTICAL,
                    true,true,false);

            CategoryPlot categoryPlot = (CategoryPlot) lineChartObject.getPlot();
            CategoryAxis categoryAxis = categoryPlot.getDomainAxis();

            // rotate x-axis labels
            categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
            NumberAxis valueAxis = (NumberAxis) categoryPlot.getRangeAxis();

            // auto-scale y-axis
            valueAxis.setAutoRangeIncludesZero(false);

            int width = 640; /* Width of the image */
            int height = 480; /* Height of the image */
            File lineChart = new File( String.format("%s%s_%s.jpeg", dir, emotion, metric));
            ChartUtilities.saveChartAsJPEG(lineChart, lineChartObject, width, height);

            //ChartFrame frame = new ChartFrame(emotion + "_" + metric, lineChartObject);
            //frame.pack();
            //frame.setVisible(true);
        }
    }
}
