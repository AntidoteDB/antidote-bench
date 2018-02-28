package adbm.resultsVisualization;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
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

    public static XYSeriesCollection dataset;
    public static JFreeChart chart;
    public static ChartPanel chartPanel;
    /*private final static int chartWidth = 5000;
    private final static int chartHeight = 200;*/
    private static CSVReader reader;
    private static String[] readNextLine;
    public static VisualizationPanel visualizationPanel;
    public static boolean started = false;

    public VisualizationMain() {
        /*visualizationPanel = new VisualizationPanel(chartPanel);
        visualizationPanel.setPanelChart(chartPanel);*/
        if (started) {
            if (chart == null) {
                try {
                    dataset = createDataset();
                    chart = createChart(dataset);
                    chartPanel = new ChartPanel(chart);
                    visualizationPanel = new VisualizationPanel(chartPanel);
                    visualizationPanel.setPanelChart(chartPanel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        started = false;
    }

    /*VisualizationPanel getVisualizationPanel(){
        if(visualizationPanel instanceof VisualizationPanel) {
            return visualizationPanel;
        }
        else {
            try {
                dataset = createDataset();
                chart = createChart(dataset);
                chartPanel = new ChartPanel(chart);
                visualizationPanel = new VisualizationPanel(chartPanel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return visualizationPanel;
        }
    }*/

    public static void main(String[] args) {
        started = true;
        VisualizationMain demo = new VisualizationMain();
    }

    /*public static class TestSeries extends ApplicationFrame {
        public TestSeries(final String applicationTitle) throws IOException {
            super(applicationTitle);
            dataset = createDataset();
            chart = createChart(dataset);
            chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(chartHeight, chartWidth));
            this.add(chartPanel);
            setContentPane(chartPanel);
        }
    }*/

    public static XYSeriesCollection createDataset() throws NumberFormatException, IOException {
        dataset = new XYSeriesCollection();
        try {
            reader = new CSVReader(new FileReader("transaction1.csv"), ',');
            // Read the header and chuck it away
            readNextLine = reader.readNext();

            // Set up series
            final XYSeries seriesRead = new XYSeries("[READ]");
            final XYSeries seriesUpdate = new XYSeries("[UPDATE]");

            while ((readNextLine = reader.readNext()) != null) {
                // variables declaration
                boolean isValid = true;
                String operationsType = readNextLine[0];
                double X = 0;
                double Y = 0;
                // add values to dataset for READ
                if (operationsType.equals("[READ]")) {
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
                // add values to dataset for UPDATE
                if (operationsType.equals("[UPDATE]")) {
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

            dataset.addSeries(seriesRead);
            dataset.addSeries(seriesUpdate);
        } catch (FileNotFoundException e) {
            System.out.println("File not found!");
        }
        return dataset;
    }

    public static JFreeChart createChart(XYDataset dataset) throws IOException {
        chart = ChartFactory.createXYLineChart("Operations vs Latency for READ and UPDATE", // chart
                // title
                "Operations", // domain axis label
                "Latency (us)", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // the plot orientation
                true, // legend
                true, // tooltips
                false); // urls

        return chart;
    }

    // Use for storing the chart as a pdf file
    public static void convertToPdf(JFreeChart savedChart, int width, int height) {

        String filename = null;
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
            filename = fileToSave.getName();

            Document document = new Document(new Rectangle(width, height));

            try {
                PdfWriter writer;
                writer = PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                document.open();
                PdfContentByte cb = writer.getDirectContent();
                PdfTemplate tp = cb.createTemplate(width, height);
                Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
                Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
                savedChart.draw(g2d, r2d);
                g2d.dispose();
                cb.addTemplate(tp, 0, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            document.close();
        }
    }
}