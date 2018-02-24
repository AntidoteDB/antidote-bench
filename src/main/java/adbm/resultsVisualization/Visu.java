package adbm.resultsVisualization;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class Visu
{
    private JButton buttonSave;
    public JPanel panelChart;
    private JPanel panel;
    public JTextField xTextField;
    private JButton buttonCreate;
    public JTextField yTextField;
    public JTextField chartNameTextField;
    private JLabel chartNameLabel;
    public JComboBox comboBox1 = null;
    private JPanel panelChart1;
    private JPanel panelChart2;
    ArrayList xParameterList, yParameterList;
    String windowChartName;
    JFrame frame = new JFrame("Result Window");
    ArrayList<JFreeChart> savedChartList;

    public Visu(ChartPanel chartPanel) {
        xParameterList = new ArrayList<String>();
        yParameterList = new ArrayList();
        savedChartList = new ArrayList<JFreeChart>();
        //JFrame frame = new JFrame("Result Window");
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        UIUtils.centerFrameOnScreen(frame);
        frame.setVisible(true);
        if (panelChart == null) System.out.println("Panel");
        if (chartPanel == null) System.out.println("ChartPanel");

        buttonSave.addActionListener(e -> {
            //Main.convertToPdf(chartPanel.getChart(),640,480);
            RMain.convertToPdf(savedChartList,640,480);
            //buttonSave.setEnabled(false);
        });

        buttonCreate.addActionListener(e -> {
            windowChartName = chartNameTextField.getText();
            RMain.createChart(UpdateChartMainWindow());
        });

        initComboBox(comboBox1);
        comboBox1.addActionListener(e -> {
            comboBoxSetting(chartPanel);
        });
    }

    public void setPanelChart1(ChartPanel chartPanel) {
        if(chartPanel == null) System.out.println("panelChart1");
        else {
            RMain.chart = chartPanel.getChart();
            panelChart1.add(chartPanel, BorderLayout.CENTER);
            panelChart1.updateUI();
            savedChartList.add(chartPanel.getChart());
        }
    }

    public void setPanelChart2(ChartPanel chartPanel) {
        if (chartPanel == null) System.out.println("panelChart2");
        else {
            RMain.chart2 = chartPanel.getChart();
            panelChart2.add(chartPanel, BorderLayout.CENTER);
            panelChart2.updateUI();
            savedChartList.add(chartPanel.getChart());
        }
    }

    public void initComboBox(JComboBox comboBox) {

        String[] items =
                {"Read Performance Testing",
                        "Write Performance Testing",
                        "Update Performance Testing",
                        "Delete Performance Testing",
                        "Set Performance Testing"
                };
        System.out.println("Configure the Combobox");
        for(String s : items) {comboBox.addItem(s);}
    }

    XYPlot plot = null;
    public void comboBoxSetting(ChartPanel chartPanel) {
        String item = (String) comboBox1.getSelectedItem();
        //CategoryPlot plot = chartPanel.getChart().getCategoryPlot();
        //XYPlot plot = (XYPlot) chartPanel.getChart().getXYPlot();
        if (!item.isEmpty() && !chartNameTextField.getText().isEmpty()) {
            String s = chartNameTextField.getText();
            //int i = savedChartList.size();
            int i = Integer.parseInt(s);
            if(i <= savedChartList.size()) {
                /*if(i==2) {
                    plot = (XYPlot) (savedChartList.get(i-1)).getXYPlot();
                } else {plot = (XYPlot) (savedChartList.get(0)).getXYPlot();}*/
                //plot = (XYPlot) chartPanel.getChart().getXYPlot();
                plot = (XYPlot) (savedChartList.get(i - 1)).getXYPlot();

                //chartPanel.getChart().setTitle(item);
                (savedChartList.get(i - 1)).setTitle(item);
                String[] yTitel = item.split(" ");
                String yLabel = yTitel[0] + " latency (ms)";
                //Font font = new Font(yTitel[0] + " latency (ms)", Font.PLAIN, 25);
                //plot.getDomainAxis().setLabelFont(font);
                //plot.getRangeAxis().setLabelFont(font);
                plot.getRangeAxis().setLabel(yLabel);
            } else {
                System.out.println("You can only enter 1 or 2.");
                JOptionPane.showMessageDialog(null,"You can only enter 1 or 2!",
                                              "Select the chart",
                                              JOptionPane.WARNING_MESSAGE);
            }
        } else {
            System.out.println("Select the chart to be updated");
            JOptionPane.showMessageDialog(null,"Which chart do you want to change? 1 or 2",
                                          "Select the chart",
                                          JOptionPane.WARNING_MESSAGE);
        }
        setTextFieldEmpty();
        //if(chartPanel.getParent() == panelChart2){System.out.println("TRUE");} else {System.out.println("FALSE");}
    }

    public JTextField getxTextField() {return xTextField;}
    public JTextField getyTextField() {return yTextField;}
    public JTextField getChartNameTextField() {return chartNameTextField;}

    public void setTextFieldEmpty() {
        xTextField.setText("");
        yTextField.setText("");
        chartNameTextField.setText("");
    }

    XYSeriesCollection data = null;

    public void updateDataSet() {
        String [] inx = xTextField.getText().split(",");
        String [] iny = yTextField.getText().split(",");
        for (String s: inx) xParameterList.add(s);
        for (String s: iny) yParameterList.add(s);
        if(data == null) {
            data = new XYSeriesCollection();

            XYSeries series = new XYSeries(windowChartName);

            if (!xTextField.getText().isEmpty() && !yTextField.getText().isEmpty() && !chartNameTextField.getText().isEmpty()) {
                try {
                    int i = xParameterList.size() - 1;
                    int j = yParameterList.size() - 1;
                    if (i == j) {
                        while (i >= 0 && j >= 0) {
                            System.out.println("While: X:" + (String) xParameterList.get(i) + "\nY: " + (String) yParameterList.get(j));
                            series.add(Double.parseDouble((String) xParameterList.get(i)), Double.parseDouble((String) yParameterList.get(j)));
                            i--;
                            j--;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Textfields don´t have the same length");
                }
                RMain.data.addSeries(series);
            } else {
                System.out.println("Textfields don´t contain any elements");
                JOptionPane.showMessageDialog(null,"Enter all parameters for displaying the chart",
                                              "Warning",
                                              JOptionPane.WARNING_MESSAGE);
            }
        }
        else { // data is not null

            XYSeries series = new XYSeries(windowChartName);

            if (!xTextField.getText().isEmpty() && !yTextField.getText().isEmpty()&& !chartNameTextField.getText().isEmpty()) {
                try {
                    int i = xParameterList.size() - 1;
                    int j = yParameterList.size() - 1;
                    if (i == j) {
                        while (i >= 0 && j >= 0) {
                            System.out.println("While: X:" + (String) xParameterList.get(i) + "\nY: " + (String) yParameterList.get(j));
                            series.add(Double.parseDouble((String) xParameterList.get(i)), Double.parseDouble((String) yParameterList.get(j)));
                            i--;
                            j--;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Textfields don´t have the same length");
                }
                RMain.data.addSeries(series);
            }
            else {
                JOptionPane.showMessageDialog(null,"Enter all parameters for displaying the chart",
                                              "Warning",
                                              JOptionPane.WARNING_MESSAGE);
            }
        }
        setTextFieldEmpty();
        xParameterList.clear();
        yParameterList.clear();
    }

    public XYSeriesCollection UpdateChartMainWindow() {
        updateDataSet();
        return data;
    }

}
