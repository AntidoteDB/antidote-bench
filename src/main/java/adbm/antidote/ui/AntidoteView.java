package adbm.antidote.ui;

import adbm.antidote.AntidoteClientWrapper;
import adbm.antidote.Operation;
import adbm.docker.DockerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.util.List;

import static adbm.antidote.AntidoteUtil.*;

public class AntidoteView
{

    private static final Logger log = LogManager.getLogger(AntidoteView.class);
    private JPanel panel1;

    private JList<String> listViewAllDCs;

    private JList<String> listViewKeySelection;
    private JList<String> listViewOperationSelection;
    private JList<String> listViewKeyValue;

    private JList<String> listViewConnectedDCs;

    private JTextField textFieldDCName;

    private JTextField textFieldAddKey;
    private JTextField textFieldOperationValue;
    private JTextField textFieldCommitKey;
    private JTextField textFieldCommitOperation;
    private JTextField textFieldCommitValue;

    private JButton executeButton;
    private JButton buttonAddDC;
    private JButton buttonStartDC;
    private JButton buttonStopDC;
    private JButton buttonRemoveDC;

    private JButton buttonAddKey;
    private JButton buttonRemoveKey;

    private JButton buttonResetConnections;
    private JButton buttonOpenDCInNewWindow;
    private JButton buttonAddDCConnection;
    private JButton buttonRemoveDCConnection;
    private JButton buttonSuspendDCConnection;

    private JComboBox<String> comboBoxWindowDC;

    private JComboBox<String> comboBoxKeyType; // No Change

    private JComboBox<String> comboBoxRunningDC;

    private DefaultListModel<String> listViewAllDCsModel = new DefaultListModel<>();
    private DefaultListModel<String> listViewKeySelectionModel = new DefaultListModel<>();
    private DefaultListModel<String> listViewOperationSelectionModel = new DefaultListModel<>();
    private DefaultListModel<String> listViewKeyValueModel = new DefaultListModel<>(); // TODO could  be different type
    private DefaultListModel<String> listViewConnectedDCsModel = new DefaultListModel<>();

    private DefaultComboBoxModel<String> comboBoxWindowDCModel = new DefaultComboBoxModel<>();
    private DefaultComboBoxModel<String> comboBoxKeyTypeModel = new DefaultComboBoxModel<>();
    private DefaultComboBoxModel<String> comboBoxRunningDCModel = new DefaultComboBoxModel<>();

    private AntidoteClientWrapper activeAntidoteClient;

    public boolean isReady()
    {
        if (activeAntidoteClient == null) {
            log.error("ERROR: Antidote Client was null!");
            return false;
        }
        if (activeAntidoteClient.isReady()) return true;
        log.warn("Antidote Client is inactive!");
        return false;
    }

    public AntidoteView(AntidoteClientWrapper startClient)
    {
        if (startClient == null) return;
        activeAntidoteClient = startClient;
        JFrame frame = new JFrame("AntidoteView");

        frame.setContentPane(panel1);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // TODO set initial values for everything

        comboBoxKeyType.setModel(comboBoxKeyTypeModel);
        for (String type : guiTypeMap.keySet())
            comboBoxKeyTypeModel.addElement(type);
        comboBoxKeyType.setSelectedIndex(0);

        comboBoxRunningDC.setModel(comboBoxRunningDCModel);
        comboBoxWindowDC.setModel(comboBoxWindowDCModel);

        listViewAllDCs.setModel(listViewAllDCsModel);
        listViewKeySelection.setModel(listViewKeySelectionModel);
        listViewOperationSelection.setModel(listViewOperationSelectionModel);
        listViewKeyValue.setModel(listViewKeyValueModel);
        listViewConnectedDCs.setModel(listViewConnectedDCsModel);

        refreshDCList();

        refreshKeyList();


        executeButton.addActionListener(e -> {
            if (activeAntidoteClient != null) {
                activeAntidoteClient.getKeyUpdate(new Operation(listViewKeySelection.getSelectedValue(),
                                                                listViewOperationSelection.getSelectedValue(),
                                                                textFieldOperationValue.getText()));
            }
        });
        listViewOperationSelection.addListSelectionListener(e -> refreshCommandTextFields());
        comboBoxKeyType.addActionListener(e -> refreshKeyList());
        listViewKeySelection.addListSelectionListener(e -> refreshKeyValue());
        textFieldOperationValue.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                refreshCommandTextFields();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                refreshCommandTextFields();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                refreshCommandTextFields();
            }
        });
        buttonAddDC.addActionListener(e -> {
            if (textFieldDCName.getText() != null && !textFieldDCName.getText().isEmpty())
                modelPropertyChange(new PropertyChangeEvent(this, "addDC", "", textFieldDCName.getText()));
        });
        //TODO add all commands
        buttonAddKey.addActionListener(e -> {

        });
    }

    private void refreshDCList()
    {
        List<String> runningContainers = DockerManager.getNamesOfRunningContainers();
        if (!runningContainers.contains(activeAntidoteClient.name)) {
            log.error(
                    "ERROR: The Antidote Client with the name {} is no longer running despite there being a window opened on it!\n",
                    activeAntidoteClient.name);
        }
        comboBoxWindowDCModel.removeAllElements();
        for (String container : runningContainers)
            comboBoxWindowDCModel.addElement(container);
        comboBoxWindowDCModel.setSelectedItem(activeAntidoteClient.name);

        Object selectedItem = comboBoxRunningDCModel.getSelectedItem();
        if (selectedItem == null) {
            comboBoxRunningDCModel.removeAllElements();
            for (String container : runningContainers)
                comboBoxRunningDCModel.addElement(container);
            if (comboBoxRunningDCModel.getIndexOf(activeAntidoteClient.name) == -1) {
                log.error(
                        "ERROR: The Antidote Client with the name {} was not found despite there being a window opened on it!\n",
                        activeAntidoteClient.name);
            }
            selectedItem = activeAntidoteClient.name;
            comboBoxRunningDCModel.setSelectedItem(selectedItem);
        }
        else {
            comboBoxRunningDCModel.removeAllElements();
            for (String container : runningContainers)
                comboBoxRunningDCModel.addElement(container);
            if (comboBoxRunningDCModel.getIndexOf(selectedItem) == -1) {
                if (comboBoxRunningDCModel.getIndexOf(activeAntidoteClient.name) == -1) {
                    log.error(
                            "ERROR: The Antidote Client with the name {} was not found despite there being a window opened on it!\n",
                            activeAntidoteClient.name);
                }
                selectedItem = activeAntidoteClient.name;
            }
            comboBoxRunningDCModel.setSelectedItem(selectedItem);
        }
        List<String> allContainers = DockerManager.getNamesOfAllContainers();
        listViewAllDCsModel.clear();
        for (String container : allContainers) {
            if (runningContainers.contains(container)) {
                listViewAllDCsModel.addElement(container + " (Running)");
            }
            else {
                listViewAllDCsModel.addElement(container + " (Not Running)");
            }
        }
        // TODO Connected DCs
    }

    private void refreshKeyList()
    {
        if (comboBoxKeyType.getSelectedItem() == null) {
            comboBoxKeyType.setSelectedIndex(0);
        }
        if (comboBoxKeyType.getSelectedItem() == null) {
            log.error("ERROR: No Antidote Key Type was found!");
            return;
        }
        String selectedKey = comboBoxKeyType.getSelectedItem().toString();
        listViewKeySelectionModel.clear();
        for (String key : getKeysForType(guiTypeMap.get(selectedKey)))
            listViewKeySelectionModel.addElement(key);
        if (listViewKeySelection.getModel().getSize() > 0)
            listViewKeySelection.setSelectedIndex(0);
        listViewOperationSelectionModel.clear();
        for (String key : typeOperationMap.get(guiTypeMap.get(selectedKey)))
            listViewOperationSelectionModel.addElement(key);
        if (listViewOperationSelection.getModel().getSize() > 0)
            listViewOperationSelection.setSelectedIndex(0);
        refreshCommandTextFields();
    }

    private void refreshKeyValue()
    {
        if (listViewKeySelection.getSelectedValue() != null && activeAntidoteClient != null)
            listViewKeyValueModel.clear();
        listViewKeyValueModel
                .addElement(activeAntidoteClient.getKeyValueNoTx(listViewKeySelection.getSelectedValue()));
        if (listViewOperationSelection.getModel().getSize() > 0)
            listViewOperationSelection.setSelectedIndex(0);
    }

    private void refreshCommandTextFields()
    {
        if (listViewKeySelection.getSelectedValue() != null)
            textFieldCommitKey.setText(listViewKeySelection.getSelectedValue());
        if (listViewOperationSelection.getSelectedValue() != null)
            textFieldCommitOperation.setText(listViewOperationSelection.getSelectedValue());
        if (textFieldOperationValue.getText() != null)
            textFieldCommitValue.setText(textFieldOperationValue.getText());
    }

    public void modelPropertyChange(final PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(AntidoteController.DCListChanged)) {
            refreshDCList();
        }
        if (evt.getPropertyName().equals(AntidoteController.KeyValueChanged)) {
            refreshKeyValue();
        }
        if (evt.getPropertyName().equals(AntidoteController.KeyListChanged)) {
            refreshKeyList();
        }
    }

    public String getSelectedKey()
    {
        return null;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(20, 20, 20, 20), -1, -1));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        panel1.add(tabbedPane1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                 null, new Dimension(200, 200), null, 0,
                                                                                 false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(8, 2, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Antidote Configuration", panel2);
        textFieldDCName = new JTextField();
        panel2.add(textFieldDCName, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                     null, new Dimension(150, -1), null,
                                                                                     0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Add new Antidote Data Center");
        panel2.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Antidote Data Centers");
        panel2.add(label2, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        listViewAllDCs = new JList();
        panel2.add(listViewAllDCs, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 5, 1,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                    null, new Dimension(150, 50), null,
                                                                                    0, false));
        buttonStopDC = new JButton();
        buttonStopDC.setText("Stop");
        panel2.add(buttonStopDC, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel2.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL,
                                                                             1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                             null, null, null, 0, false));
        buttonAddDC = new JButton();
        buttonAddDC.setText("Add");
        panel2.add(buttonAddDC, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                 null, null, null, 0, false));
        buttonOpenDCInNewWindow = new JButton();
        buttonOpenDCInNewWindow.setText("Open in new Window");
        panel2.add(buttonOpenDCInNewWindow, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                             null, null, null, 0,
                                                                                             false));
        buttonStartDC = new JButton();
        buttonStartDC.setText("Start");
        panel2.add(buttonStartDC, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                   null, null, null, 0, false));
        buttonRemoveDC = new JButton();
        buttonRemoveDC.setText("Remove");
        panel2.add(buttonRemoveDC, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                    null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(14, 3, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Antidote Data Access", panel3);
        final JLabel label3 = new JLabel();
        label3.setText("Select Key Datatype");
        panel3.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        comboBoxKeyType = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comboBoxKeyType.setModel(defaultComboBoxModel1);
        panel3.add(comboBoxKeyType, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                     null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Key Value");
        panel3.add(label4, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 3, 2,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                 null, null, null, 0, false));
        listViewKeySelection = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        listViewKeySelection.setModel(defaultListModel1);
        listViewKeySelection.setSelectionMode(0);
        scrollPane1.setViewportView(listViewKeySelection);
        final JLabel label5 = new JLabel();
        label5.setText("Select Key Operation");
        panel3.add(label5, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 2,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Specify Operation Value");
        panel3.add(label6, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 2, 2,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                 null, null, null, 0, false));
        listViewOperationSelection = new JList();
        final DefaultListModel defaultListModel2 = new DefaultListModel();
        defaultListModel2.addElement("increment");
        defaultListModel2.addElement("decrement");
        listViewOperationSelection.setModel(defaultListModel2);
        listViewOperationSelection.setSelectionMode(0);
        scrollPane2.setViewportView(listViewOperationSelection);
        textFieldOperationValue = new JTextField();
        panel3.add(textFieldOperationValue, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                             null,
                                                                                             new Dimension(150, -1),
                                                                                             null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Key");
        panel3.add(label7, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 1,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Operation");
        panel3.add(label8, new com.intellij.uiDesigner.core.GridConstraints(11, 0, 1, 2,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Value");
        panel3.add(label9, new com.intellij.uiDesigner.core.GridConstraints(12, 0, 1, 2,
                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                            null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Execute");
        panel3.add(label10, new com.intellij.uiDesigner.core.GridConstraints(13, 0, 1, 2,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             null, null, null, 0, false));
        executeButton = new JButton();
        executeButton.setText("Execute");
        panel3.add(executeButton, new com.intellij.uiDesigner.core.GridConstraints(13, 2, 1, 1,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                   com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                   null, null, null, 0, false));
        textFieldCommitValue = new JTextField();
        textFieldCommitValue.setEditable(false);
        panel3.add(textFieldCommitValue, new com.intellij.uiDesigner.core.GridConstraints(12, 2, 1, 1,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                          null, new Dimension(150, -1),
                                                                                          null, 0, false));
        textFieldCommitOperation = new JTextField();
        textFieldCommitOperation.setEditable(false);
        panel3.add(textFieldCommitOperation, new com.intellij.uiDesigner.core.GridConstraints(11, 2, 1, 1,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                              null,
                                                                                              new Dimension(150, -1),
                                                                                              null, 0, false));
        textFieldCommitKey = new JTextField();
        textFieldCommitKey.setEditable(false);
        panel3.add(textFieldCommitKey, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1,
                                                                                        com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                        com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                        com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                        com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                        null, new Dimension(150, -1),
                                                                                        null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel3.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL,
                                                                             1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                             null, null, null, 0, false));
        listViewKeyValue = new JList();
        listViewKeyValue.setSelectionMode(2);
        panel3.add(listViewKeyValue, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                      null, new Dimension(150, 50),
                                                                                      null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Add new Key");
        panel3.add(label11, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Select Key");
        panel3.add(label12, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             null, null, null, 0, false));
        textFieldAddKey = new JTextField();
        panel3.add(textFieldAddKey, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                     null, new Dimension(150, -1), null,
                                                                                     0, false));
        buttonAddKey = new JButton();
        buttonAddKey.setText("Add Key");
        panel3.add(buttonAddKey, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        buttonRemoveKey = new JButton();
        buttonRemoveKey.setActionCommand("RemoveKey");
        buttonRemoveKey.setText("Remove Key");
        panel3.add(buttonRemoveKey, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                     com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                     null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(7, 2, new Insets(10, 10, 10, 10), -1, -1));
        tabbedPane1.addTab("Antidote Connections", panel4);
        final JLabel label13 = new JLabel();
        label13.setText("Select Running Antidote Data Center");
        panel4.add(label13, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             null, null, null, 0, false));
        listViewConnectedDCs = new JList();
        panel4.add(listViewConnectedDCs, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 4, 1,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                          com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                          null, new Dimension(150, 50),
                                                                                          null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer3 = new com.intellij.uiDesigner.core.Spacer();
        panel4.add(spacer3, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_VERTICAL,
                                                                             1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                             null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Connected Antidote Data Centers");
        panel4.add(label14, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             null, null, null, 0, false));
        comboBoxRunningDC = new JComboBox();
        panel4.add(comboBoxRunningDC, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                       null, null, null, 0, false));
        buttonResetConnections = new JButton();
        buttonResetConnections.setText("Reset");
        panel4.add(buttonResetConnections, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                            null, null, null, 0,
                                                                                            false));
        buttonAddDCConnection = new JButton();
        buttonAddDCConnection.setText("Add Connection");
        panel4.add(buttonAddDCConnection, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                           null, null, null, 0, false));
        buttonSuspendDCConnection = new JButton();
        buttonSuspendDCConnection.setText("Suspend Connection");
        panel4.add(buttonSuspendDCConnection, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                               null, null, null, 0,
                                                                                               false));
        buttonRemoveDCConnection = new JButton();
        buttonRemoveDCConnection.setText("Remove Connection");
        panel4.add(buttonRemoveDCConnection, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                              null, null, null, 0,
                                                                                              false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Benchmark Configuration", panel5);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Results Visualization", panel6);
        comboBoxWindowDC = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("Antidote1");
        comboBoxWindowDC.setModel(defaultComboBoxModel2);
        panel1.add(comboBoxWindowDC, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                      null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("Current Antidote Data Center");
        panel1.add(label15, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                             null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer4 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer4, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                             1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel1;
    }
}
