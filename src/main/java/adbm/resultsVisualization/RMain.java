package adbm.resultsVisualization;

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
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RMain
{

    private static JButton save;
    private static JLabel jLabel;
    public static ChartPanel chartPanel = null;
    public static JFreeChart chart = null;
    public static JFreeChart chart2 = null; // second chart to be added to the chartPanel
    public static XYSeriesCollection data;
    public static Visu mainWindow;
    public static String panelName = null;
    public static boolean begining = false;


    public RMain() {
        /*data = initDataset();
        chart = createChart(data);
        chartPanel = new ChartPanel(chart);
        chart2 = createChart(initDataset2());
        chartPanel = new ChartPanel(chart2);
        mainWindow=new MainWindow(chartPanel);*/

        if(begining) {
            if (chart == null) {
                data = initDataset();
                chart = createChart(data);
                chartPanel = new ChartPanel(chart);
                mainWindow = new Visu(chartPanel);
                mainWindow.setPanelChart1(chartPanel);
            }
            if (chart2 == null) {
                chart2 = createChart(initDataset2());
                chartPanel = new ChartPanel(chart2);
                //mainWindow = new MainWindow(chartPanel);
                mainWindow.setPanelChart2(chartPanel);
            }
            begining = false;
        }
    }

    Visu getMainWindow(){
        if(mainWindow instanceof Visu)
            return mainWindow;
        else{
            data = initDataset();
            chart = createChart(data);
            chartPanel = new ChartPanel(chart);
            chart2 = createChart(initDataset2());
            chartPanel = new ChartPanel(chart2);
            return new Visu(chartPanel);
        }
    }

    public static void main(String[] args) {
        begining = true;
        RMain mainTest = new RMain();
    }

    public JFreeChart getChart1() {
        return chart;
    }

    public JFreeChart getChart2() {
        return chart2;
    }

    public static void setPanelName(String _panelName) { panelName = _panelName;}

    /**
     * A simple demo showing a dataset created using the {@link XYSeriesCollection} class.
     *
     */
    public static class XYSeriesDemo extends ApplicationFrame {

        /**
         * A demonstration application showing an XY series containing a null value.
         *
         * @param title the frame title.
         */
        public XYSeriesDemo(final String title) {

            super(title);
            //data = createDataset();
            chart = createChart(data);
            chartPanel = new ChartPanel(chart);
            chart2 = createChart(initDataset2());
            chartPanel = new ChartPanel(chart2);
            //this.add(chartPanel);
            chartPanel.setPreferredSize(new java.awt.Dimension(14, 80));
            setContentPane(chartPanel);
            /*save = new JButton("Save");
            jLabel  = new JLabel();
            jLabel.setVisible(false);

            this.getContentPane().setLayout(new BorderLayout());
            chartPanel.add(save, BorderLayout.SOUTH);
            chartPanel.add(jLabel, BorderLayout.SOUTH);
            //save.setAlignmentX(CENTER_ALIGNMENT);
            Point position = new Point(10,10);
            /*save.setLocation(position);
            save.setSize(position.xParameterList,position.yParameterList);*/

            /*save.setSize(getWidth()/2,20);
            save.setLocation(getWidth()/2-save.getSize().width/2,
                    getHeight()/2-save.getSize().height/2);*/

            /*XYPlot plot = (XYPlot) chart.getXYPlot();
            //plot.setDataset(0, date );
            //plot.setDataset(1, xyDataset2);
            XYLineAndShapeRenderer renderer0 = new XYLineAndShapeRenderer();
            //XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
            plot.setRenderer(0, renderer0);
            //plot.setRenderer(1, renderer1);
            plot.getRendererForDataset(plot.getDataset(0)).setSeriesPaint(0, Color.BLACK);
            //plot.getRendererForDataset(plot.getDataset(1)).setSeriesPaint(1, Color.RED);*/

            //convertToPdf(chart,640,480/*,"Test3.pdf"*/);
        }
    }

    public static void convertToPdf (/*JFreeChart savedChart*/ ArrayList<JFreeChart> savedChartListMain, int width, int height/*, String filename*/) {

        String filename = null;

        //For opening a file
        /*JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        //fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setDialogTitle("Choose a directory to save your file: ");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            filename = selectedFile.getName();
        }*/

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
                int i = 0;
                int size = savedChartListMain.size();
                for(JFreeChart savedChart : savedChartListMain) {
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

    public static XYSeriesCollection initDataset() {

        XYSeriesCollection data = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("HBase");
        series1.add(1.0, 69.0);
        series1.add(3.0, 35.0);
        series1.add(6.0, 17.0);
        series1.add(8.0, 27.0);
        series1.add(10.0, 19.0);
        series1.add(12.0, 22.0);

        XYSeries series2 = new XYSeries("Cassandra");
        series2.add(3.0,8.0);
        series2.add(6.0,10.0);
        series2.add(8.0,9.5);
        series2.add(10.0,10.5);
        series2.add(12.0,11.5);

        XYSeries series3 = new XYSeries("PNUTS");
        series3.add(1.0, 8.0);
        series3.add(3.0, 9.0);
        series3.add(6.0, 8.5);
        series3.add(8.0, 6.5);
        series3.add(10.0, 8.5);
        series3.add(12.0, 6.5);

        data.addSeries(series1);
        data.addSeries(series2);
        data.addSeries(series3);

        return data;
    }

    public static XYSeriesCollection initDataset2() {

        XYSeriesCollection data = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("HBase");
        series1.add(1.0, 69.0);
        series1.add(3.0, 35.0);
        series1.add(6.0, 17.0);
        series1.add(8.0, 27.0);
        series1.add(10.0, 19.0);
        series1.add(12.0, 22.0);

        XYSeries series2 = new XYSeries("Cassandra");
        series2.add(3.0,8.0);
        series2.add(6.0,10.0);
        series2.add(8.0,9.5);
        series2.add(10.0,10.5);
        series2.add(12.0,11.5);

        data.addSeries(series1);
        data.addSeries(series2);

        return data;
    }

    /**
     * Creates a sample chart.
     *
     * @param dataset  the dataset.
     *
     * @return A sample chart.
     */
    public static JFreeChart createChart(XYDataset dataset) {
        panelName = "Read Performance Testing";
        //JFreeChart testChart = ChartFactory.createMultiplePieChart(panelName,(CategoryDataset) dataset, TableOrder.BY_ROW,true,true,false);
        JFreeChart _chart = ChartFactory.createXYLineChart(
                panelName,
                "Servers",
                "Read latency (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = (XYPlot) _chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(0, renderer);
        plot.getRendererForDataset(plot.getDataset(0)).setSeriesPaint(0, Color.BLACK);
        return _chart;
    }

}
