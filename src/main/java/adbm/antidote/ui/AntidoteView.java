package adbm.antidote.ui;

import adbm.antidote.AntidoteClientWrapper;
import adbm.docker.DockerManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.List;

import static adbm.antidote.AntidoteUtil.*;

public class AntidoteView
{
    private JPanel panel1;

    private JList listViewAllDCs;

    private JList listViewKeySelection;
    private JList listViewOperationSelection;
    private JList listViewKeyValue;

    private JList listViewConnectedDCs;

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

    private JComboBox comboBoxWindowDC;
    private JComboBox comboBoxKeyDatatype; // No Change
    private JComboBox comboBoxRunningDC;

    private DefaultListModel listViewAllDCsModel = new DefaultListModel();
    private DefaultListModel listViewKeySelectionModel = new DefaultListModel();
    private DefaultListModel listViewOperationSelectionModel = new DefaultListModel();
    private DefaultListModel listViewKeyValueModel = new DefaultListModel();
    private DefaultListModel listViewConnectedDCsModel = new DefaultListModel();

    private DefaultComboBoxModel comboBoxWindowDCModel = new DefaultComboBoxModel();
    private DefaultComboBoxModel comboBoxKeyDatatypeModel = new DefaultComboBoxModel();
    private DefaultComboBoxModel comboBoxRunningDCModel = new DefaultComboBoxModel();

    private AntidoteClientWrapper activeAntidoteClient;

    public AntidoteView(AntidoteClientWrapper startClient)
    {
        activeAntidoteClient = startClient;
        JFrame frame = new JFrame("AntidoteView");

        frame.setContentPane(panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // TODO set initial values for everything

        comboBoxKeyDatatype.setModel(comboBoxKeyDatatypeModel);
        for (String type : guiTypeMap.keySet())
            comboBoxKeyDatatypeModel.addElement(type);
        comboBoxKeyDatatype.setSelectedIndex(0);

        comboBoxRunningDC.setModel(comboBoxRunningDCModel);
        comboBoxWindowDC.setModel(comboBoxWindowDCModel);

        listViewAllDCs.setModel(listViewAllDCsModel);
        listViewKeySelection.setModel(listViewKeySelectionModel);
        listViewOperationSelection.setModel(listViewOperationSelectionModel);
        listViewKeyValue.setModel(listViewKeyValueModel);
        listViewConnectedDCs.setModel(listViewConnectedDCsModel);

        refreshDCList();

        refreshKeyDatatype();



        executeButton.addActionListener(e -> {
            if (activeAntidoteClient != null) {
                //activeAntidoteClient.ExecuteKeyOperation(listViewKeySelection.getSelectedValue().toString(),
                                                         //listViewOperationSelection.getSelectedValue().toString(),
                                                         //textFieldOperationValue.getText());
            }
            //refreshCurrentValue();
        });
        listViewOperationSelection.addListSelectionListener(e -> refreshGUICommand());
        comboBoxKeyDatatype.addActionListener(e -> refreshKeyDatatype());
        listViewKeySelection.addListSelectionListener(e ->
                                                      {
                                                          //refreshCurrentValue();
                                                      });
        textFieldOperationValue.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                //refreshGUICommand();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                //refreshGUICommand();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                //refreshGUICommand();
            }
        });
        buttonAddDC.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                //if (textFieldDCName.getText() != null && !textFieldDCName.getText().isEmpty())
                    //modelPropertyChange(new PropertyChangeEvent(this, "addDC", "", textFieldDCName.getText()));
            }
        });
    }

    private void refreshDCList()
    {
        List<String> runningContainers = DockerManager.getRunningContainers();
        if (!runningContainers.contains(activeAntidoteClient.name)) {
            //TODO NOT ALLOWED!
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
                //TODO NOT ALLOWED!
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
                    //TODO NOT ALLOWED!
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
            } else {
                listViewAllDCsModel.addElement(container + " (Stopped)");
            }
        }
        // TODO Connected DCs
    }

    private void refreshKeyDatatype()
    {
        if (comboBoxKeyDatatype.getSelectedItem() == null) {
            comboBoxKeyDatatype.setSelectedIndex(0);
        }
        String selectedKey = comboBoxKeyDatatype.getSelectedItem().toString();
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
        refreshGUICommand();
    }

    private void refreshGUICommand()
    {
        if (listViewKeySelection.getSelectedValue() != null)
            textFieldCommitKey.setText(listViewKeySelection.getSelectedValue().toString());
        if (listViewOperationSelection.getSelectedValue() != null)
            textFieldCommitOperation.setText(listViewOperationSelection.getSelectedValue().toString());
        if (textFieldOperationValue.getText() != null)
            textFieldCommitValue.setText(textFieldOperationValue.getText());
    }

    private void refreshCurrentValue()
    {
        //if (listViewKeySelection.getSelectedValue() != null && activeAntidoteClient != null)
        //textFieldCurrentValue
        //.setText(activeAntidoteClient.getKeyValue(listViewKeySelection.getSelectedValue().toString()));
    }

    public void modelPropertyChange(final PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(AntidoteController.DCListChanged)) {
            refreshDCList();
        }
    }
}
