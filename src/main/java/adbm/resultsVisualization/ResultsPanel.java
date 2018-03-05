package adbm.resultsVisualization;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ResultsPanel
{
    private JPanel panel;
    private JButton buttonSave;
    private JPanel panel1;
    private JPanel panel2;
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
                    panel1.add(chartPanel,BorderLayout.CENTER);
                    panel1.updateUI();
                    break;
                case 2:
                    panel2.add(chartPanel,BorderLayout.CENTER);
                    panel2.updateUI();
                    break;
                default:
                    break; //TODO log
            }
            i++;
        }
        buttonSave.addActionListener(e -> ResultsDialog.convertToPdf(savedChartList, 640, 480));
    }

}