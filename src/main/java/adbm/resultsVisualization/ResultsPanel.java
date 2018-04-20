package adbm.resultsVisualization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static adbm.util.helpers.FormatUtil.format;

public class ResultsPanel
{
    private static final Logger log = LogManager.getLogger(ResultsPanel.class);

    private Integer[] chartsPerRow = {1, 2, 3, 4};
    private Integer[] chartSize = {100, 200, 400, 800, 1200, 1600, 2400, 3200, 10000};

    private JPanel panel;
    private JButton buttonSave;
    private JPanel panel1;
    private JPanel panel2;
    private JComboBox<FileType> comboBoxFileType;
    private JComboBox<Integer> comboBoxChartWidth;
    private JComboBox<Integer> comboBoxChartHeight;
    private JComboBox<Integer> comboBoxChartsPerRow;
    private DefaultComboBoxModel<FileType> comboBoxFileTypeModel = new DefaultComboBoxModel<>(FileType.values());
    private DefaultComboBoxModel<Integer> comboBoxChartsPerRowModel = new DefaultComboBoxModel<>(chartsPerRow);
    private DefaultComboBoxModel<Integer> comboBoxChartHeightModel = new DefaultComboBoxModel<>(chartSize);
    private DefaultComboBoxModel<Integer> comboBoxChartWidthModel = new DefaultComboBoxModel<>(chartSize);

    private ArrayList<JFreeChart> savedChartList = new ArrayList<>();

    public static JPanel getPanel(ChartPanel... chartPanels)
    {
        ResultsPanel resultsPanel = new ResultsPanel(chartPanels);
        return resultsPanel.panel;
    }

    private ResultsPanel(ChartPanel... chartPanels)
    {
        int i = 1;
        for (ChartPanel chartPanel : chartPanels) {
            savedChartList.add(chartPanel.getChart());
            switch (i) {
                case 1:
                    panel1.add(chartPanel, BorderLayout.CENTER);
                    panel1.updateUI();
                    break;
                case 2:
                    panel2.add(chartPanel, BorderLayout.CENTER);
                    panel2.updateUI();
                    break;
                default:
                    break; //TODO log
            }
            i++;
        }
        comboBoxFileType.setModel(comboBoxFileTypeModel);
        comboBoxChartHeight.setModel(comboBoxChartHeightModel);
        comboBoxChartWidth.setModel(comboBoxChartWidthModel);
        comboBoxChartsPerRow.setModel(comboBoxChartsPerRowModel);
        comboBoxFileType.setSelectedIndex(0);
        comboBoxChartsPerRow.setSelectedIndex(1);
        comboBoxChartWidth.setSelectedIndex(3);
        comboBoxChartHeight.setSelectedIndex(3);
        selectedFileType = (FileType) comboBoxFileType.getSelectedItem();
        selectedChartNumberPerRows = (int) comboBoxChartsPerRow.getSelectedItem();
        selectedChartWidth = (int) comboBoxChartWidth.getSelectedItem();
        selectedChartHeight = (int) comboBoxChartHeight.getSelectedItem();
        buttonSave.addActionListener(e -> convertChartToFile(savedChartList));
        comboBoxFileType.addActionListener(e -> selectedFileType = (FileType) comboBoxFileType.getSelectedItem());
        comboBoxChartsPerRow.addActionListener(e -> {
            selectedChartNumberPerRows = (int) comboBoxChartsPerRow.getSelectedItem();
        });
        comboBoxChartWidth.addActionListener(e -> {
            selectedChartWidth = (int) comboBoxChartWidth.getSelectedItem();
        });
        comboBoxChartHeight.addActionListener(e -> {
            selectedChartHeight = (int) comboBoxChartHeight.getSelectedItem();
        });
    }

    private int selectedChartNumberPerRows;

    private int selectedChartWidth;

    private int selectedChartHeight;

    private FileType selectedFileType;

    public enum FileType
    {
        PNG,
        JPEG
    }

    private boolean convertChartToFile(ArrayList<JFreeChart> savedChartList)
    {
        String fileExtension = selectedFileType.name().toLowerCase();
        String dotFileExtension = format(".{}", fileExtension);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(format("Enter the {} File Name", selectedFileType));
        fileChooser.addChoosableFileFilter(
                new FileNameExtensionFilter(format("{} Files", selectedFileType), fileExtension));

        int userSelection = fileChooser.showSaveDialog(panel.getParent());

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                String fileName = fileToSave.getCanonicalPath();
                if (!fileName.endsWith(dotFileExtension)) {
                    fileToSave = new File(fileName + dotFileExtension);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("Save as file: " + fileToSave.getAbsolutePath());
            //savedFileName = fileToSave.getName();
            BufferedImage allCharts = concatImages(savedChartList, selectedChartNumberPerRows, selectedChartWidth, selectedChartHeight);

            switch (selectedFileType) {
                case PNG:
                case JPEG:
                default:
                    try {
                        ImageIO.write(allCharts, fileExtension, fileToSave.getCanonicalFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }
        return false; //TODO
    }

    private BufferedImage concatImages(ArrayList<JFreeChart> charts, int columns, int chartWidth, int chartHeight)
    {
        int size = charts.size();
        int rows = (int) Math.ceil((float) size / (float) columns);
        BufferedImage image = new BufferedImage(chartWidth * columns, chartHeight * rows, BufferedImage.TYPE_INT_ARGB);
        int chartNumber = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (chartNumber < size) {
                    image.createGraphics()
                         .drawImage(charts.get(chartNumber)
                                          .createBufferedImage(chartWidth, chartHeight,
                                                               BufferedImage.TYPE_INT_ARGB, null), chartWidth * column,
                                    chartHeight * row, null);
                }
                else {
                    image.createGraphics().drawImage(new BufferedImage(chartWidth, chartHeight, BufferedImage.TYPE_INT_ARGB), chartWidth * column,
                                                     chartHeight * row, null);
                }
                chartNumber++;
            }
        }
        return image;
    }


    // This method is using for storing the chart as a pdf file
    /*public void convertToPdf(ArrayList<JFreeChart> savedChartList, int width, int height)
    {

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
            //savedFileName = fileToSave.getName();

            PDDocument document = new PDDocument();
            try {
                document.save(fileToSave);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //new Rectangle(width, height)
            try {
                //PdfWriter writer;
                //writer = PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
                //document.open();
                //PdfContentByte cb = writer.getDirectContent();
                //PdfTemplate tp = cb.createTemplate(width, height);
                //Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
                int i = 0;
                int size = savedChartList.size();
                for (JFreeChart savedChart : savedChartList) {
                    Rectangle2D r2d = new Rectangle2D.Double(0, 0, width / size, height);
                    if (i == 1) {
                        r2d = new Rectangle2D.Double(width / size, 0, width / size, height);
                    }
                    //savedChart.draw(g2d, r2d);
                    i++;
                }
                //g2d.dispose();
                //cb.addTemplate(tp, 0, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

}