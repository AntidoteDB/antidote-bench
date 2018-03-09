package adbm.resultsVisualization;

import adbm.util.AdbmConstants;
import au.com.bytecode.opencsv.CSVReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static adbm.util.helpers.FormatUtil.format;

public class ResultsDialog extends JDialog
{

    private static final Logger log = LogManager.getLogger(ResultsDialog.class);

    public static void showResultsWindow(String... _fileNames)
    {
        ResultsDialog resultsWindow = new ResultsDialog(_fileNames);
        resultsWindow.setVisible(true);
    }

    private ResultsDialog(String... fileNames)
    {
        super(null, "Results Visualization", ModalityType.MODELESS);
//super("Results Visualization");
        this.setMinimumSize(new Dimension(1000, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        pack();
        UIUtils.centerFrameOnScreen(this);
        log.trace("Starting ResultsDialog!");
        List<ChartPanel> chartPanelList = new ArrayList<>();
        chartPanelList.add(createChart("READ", "[READ]", fileNames));
        chartPanelList.add(createChart("UPDATE", "[UPDATE]", fileNames));
        setContentPane(ResultsPanel.getPanel(chartPanelList.toArray(new ChartPanel[0])));
    }

    public static void main(String[] args)
    {
        showResultsWindow();
    }

    private ChartPanel createChart(String chartName, String operationType, String... fileNames)
    {
        XYSeriesCollection dataset = createDataset(chartName, operationType, fileNames);
        JFreeChart chart = ChartFactory.createXYLineChart(format("Benchmark Result for {} Latency", chartName), // chart
                                                          // title
                                                          format("{} Operations", operationType), // domain axis label
                                                          "Latency (µs)", // range axis label
                                                          dataset, // data
                                                          PlotOrientation.VERTICAL, // the plot orientation
                                                          true, // legend
                                                          true, // tooltips
                                                          false); // urls

        return new ChartPanel(chart);
    }

    private XYSeriesCollection createDataset(String name, String operation, String... fileNames)
    {
        log.trace("Updating {} Dataset with Files {}!", name, fileNames);
        int i = 1;
        if (fileNames.length == 0) {
            fileNames = new String[]{AdbmConstants.YCSB_SAMPLE_RESULT_PATH};
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        try {
            for (String fileName : fileNames) {
                if (fileName != null && new File(fileName).isFile()) {
                    CSVReader reader = new CSVReader(new FileReader(fileName), ',');
                    // Read the header and chuck it away
                    String[] readNextLine;

                    // Set up series
                    final XYSeries seriesRead = new XYSeries(format("{} for Commit {}", operation , i));
                    i++;
                    while ((readNextLine = reader.readNext()) != null) {
                        // variables declaration
                        boolean isValid = true;
                        String operationsType = readNextLine[0];
                        double X = 0;
                        double Y = 0;
                        // add values to dataset for an operation READ or UPDATE
                        if (operationsType.equals(operation)) {
                            try {
                                X = Double.parseDouble(readNextLine[1]);
                                Y = Double.parseDouble(readNextLine[2]);
                            } catch (NumberFormatException e) {
                                isValid = false;
                            }
                            if (isValid) {
                                seriesRead.add(X, Y);
                            }
                        }
                    }
                    dataset.addSeries(seriesRead);
                }
                else {
                    log.error("File was null or not a File! File Path: {}", fileName);
                }
            }
        } catch (IOException e) {
            System.out.println("File could not be read!");
        }
        return dataset;
    }



}