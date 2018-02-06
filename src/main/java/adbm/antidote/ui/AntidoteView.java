package adbm.antidote.ui;

import adbm.antidote.AntidoteClientWrapper;
import adbm.antidote.Operation;
import adbm.docker.DockerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
        List<String> runningContainers = DockerManager.getRunningContainers();
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
        List<String> allContainers = DockerManager.getAllContainers();
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
                .addElement(activeAntidoteClient.getKeyValueNoTransaction(listViewKeySelection.getSelectedValue()));
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

}
