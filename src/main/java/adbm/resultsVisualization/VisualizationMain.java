package adbm.resultsVisualization;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import au.com.bytecode.opencsv.CSVReader;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class VisualizationMain extends JFrame {

    private static final Logger log = LogManager.getLogger(VisualizationMain.class);


    public static XYSeriesCollection readDataset;
    public static XYSeriesCollection updateDataset;
    public static JFreeChart readChart = null;
    public static JFreeChart updateChart = null;
    public static ChartPanel chartPanel;
    private static CSVReader reader;
    private static String[] readNextLine;
    public static VisualizationPanel visualizationPanel;
    public static boolean started = false;
    private String[] fileNames;
    public static String chartName;

    public VisualizationMain(String... _fileNames) {
        this.fileNames = _fileNames;
        if (started) {
            if (readChart == null) {
                readDataset = createReadDataset(_fileNames);
                readChart = createChart(readDataset);
                chartPanel = new ChartPanel(readChart);
                visualizationPanel = new VisualizationPanel(chartPanel);
                visualizationPanel.setReadPanel(chartPanel);
            }
            if (updateChart == null) {
                updateDataset = createUpdateDataset(_fileNames);
                updateChart = createChart(updateDataset);
                chartPanel = new ChartPanel(updateChart);
                visualizationPanel.setUpdatePanel(chartPanel);
            }
        }
        started = false;
    }
    public static void main(String[] args) {
        started = true;
        VisualizationMain visualizationMain = new VisualizationMain();
    }

    public static XYSeriesCollection createReadDataset(String... fileNames) {
        int i = 1;
        if (fileNames.length == 0) {
            fileNames = new String[]{"transaction3.csv"};
        }
        readDataset = new XYSeriesCollection();
        try {
            for (String fileName : fileNames) {
                if (fileName != null && new File(fileName).isFile()) {
                    reader = new CSVReader(new FileReader(fileName), ',');
                    // Read the header and chuck it away
                    readNextLine = reader.readNext();

                    // Set up series
                    final XYSeries seriesRead = new XYSeries("[READ] for Commit " + i);
                    i++;
                    while ((readNextLine = reader.readNext()) != null) {
                        // variables declaration
                        boolean isValid = true;
                        String operationsType = readNextLine[0];
                        double X = 0;
                        double Y = 0;
                        // add values to dataset for READ
                        if (operationsType.equals("[READ]")) {
                            chartName = "READ";
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
                    readDataset.addSeries(seriesRead);
                } else {
                    log.error("File was null or not a File! File Path: {}", fileName);
                }
            }
        } catch (IOException e) {
            System.out.println("File could not be read!");
        }
        return readDataset;
    }

    public static XYSeriesCollection createUpdateDataset(String... fileNames) {
        int i = 1;
        if (fileNames.length == 0) {
            fileNames = new String[]{"transaction3.csv"};
        }
        updateDataset = new XYSeriesCollection();
        try {
            for (String fileName : fileNames) {
                if (fileName != null && new File(fileName).isFile()) {
                    reader = new CSVReader(new FileReader(fileName), ',');
                    // Read the header and chuck it away
                    readNextLine = reader.readNext();

                    // Set up series
                    final XYSeries seriesUpdate = new XYSeries("[UPDATE] for Commit " + i);
                    i++;
                    while ((readNextLine = reader.readNext()) != null) {
                        // variables declaration
                        boolean isValid = true;
                        String operationsType = readNextLine[0];
                        double X = 0;
                        double Y = 0;
                        // add values to dataset for UPDATE
                        if (operationsType.equals("[UPDATE]")) {
                            chartName = "UPDATE";
                            try {
                                X = Double.parseDouble(readNextLine[1]);
                                Y = Double.parseDouble(readNextLine[2]);
                            } catch (NumberFormatException e) {
                                isValid = false;
                            }
                            if (isValid) {
                                seriesUpdate.add(X, Y);
                            }
                        }
                    }
                    updateDataset.addSeries(seriesUpdate);
                } else {
                    log.error("File was null or not a File! File Path: {}", fileName);
                }
            }
        } catch (IOException e) {
            System.out.println("File could not be read!");
        }
        return updateDataset;
    }

    public static JFreeChart createChart(XYDataset dataset){
        JFreeChart chart = ChartFactory.createXYLineChart("Benchmarking for " + chartName, // chart
                // title
                chartName + " Operations", // domain axis label
                "Latency (us)", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // the plot orientation
                true, // legend
                true, // tooltips
                false); // urls

        return chart;
    }

    // Use for storing the chart as a pdf file
    public static void convertToPdf (ArrayList<JFreeChart> savedChartList, int width, int height) {

        String savedFileName = null;
        // parent component of the dialog
        JFrame parentFrame = new JFrame();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Enter the pdf file name");
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));

        int userSelection = fileChooser.showSaveDialog(parentFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                String fileName = fileToSave.getCanonicalPath();
                if (!fileName.endsWith(".pdf")) {
                    fileToSave = new File(fileName + ".pdf");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Save as file: " + fileToSave.getAbsolutePath());
            savedFileName = fileToSave.getName();

            Document document = new Document(new Rectangle(width, height));

            try {
                PdfWriter writer;
                writer = PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                document.open();
                PdfContentByte cb = writer.getDirectContent();
                PdfTemplate tp = cb.createTemplate(width, height);
                Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
                int i = 0;
                int size = savedChartList.size();
                for(JFreeChart savedChart : savedChartList) {
                    Rectangle2D r2d = new Rectangle2D.Double(0, 0, width/size, height);
                    if (i == 1){
                        r2d = new Rectangle2D.Double(width/size, 0, width/size, height);
                    }
                    savedChart.draw(g2d, r2d);
                    i++;
                }
                g2d.dispose();
                cb.addTemplate(tp, 0, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            document.close();
        }
    }
}