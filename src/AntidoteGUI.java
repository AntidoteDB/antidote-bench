import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Arrays;

public class AntidoteGUI {
    private JPanel panel1;
    private JButton executeButton;
    private JTextField textFieldKey;
    private JComboBox comboBoxDatatype;
    private JList listViewCommandSelection;
    private JList listViewVarSelection;
    private JTextField textFieldCommandValue;
    private JLabel labelDataTypeVar;
    private JLabel labelDatatypeCommand;
    private JTextField textFieldCurrentValue;
    private JLabel labelCurrentValue;
    private JTextField textFieldCommand;
    private JTextField textFieldValue;

    private DefaultListModel listViewVarSelectionModel = new DefaultListModel();
    private DefaultListModel listViewCommandSelectionModel = new DefaultListModel();

    public AntidoteGUI() {

        JFrame frame = new JFrame("AntidoteGUI");

        frame.setContentPane(panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // TODO set initial values for everything

        DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
        comboBoxDatatype.setModel(comboModel);
        for (String type : Main.guiTypeMap.keySet())
            comboModel.addElement(type);

        comboBoxDatatype.setSelectedIndex(0);
        listViewVarSelection.setModel(listViewVarSelectionModel);
        listViewCommandSelection.setModel(listViewCommandSelectionModel);

        refreshGUIDatatype();

        executeButton.addActionListener(e -> {
            Main.setKeyData(listViewVarSelection.getSelectedValue().toString(), listViewCommandSelection.getSelectedValue().toString(), textFieldCommandValue.getText());
            refreshCurrentValue();
        });
        listViewCommandSelection.addListSelectionListener(e -> refreshGUICommand());
        comboBoxDatatype.addActionListener(e -> refreshGUIDatatype());
        listViewVarSelection.addListSelectionListener(e ->
        {
            refreshCurrentValue();
        });
        textFieldCommandValue.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshGUICommand();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshGUICommand();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshGUICommand();
            }
        });
    }

    private void refreshGUIDatatype() {

        String selectedKey = comboBoxDatatype.getSelectedItem().toString();
        labelDataTypeVar.setText("Select " + selectedKey);
        listViewVarSelectionModel.clear();
        for (String key : Main.getKeysForType(Main.guiTypeMap.get(selectedKey)))
            listViewVarSelectionModel.addElement(key);
        if (listViewVarSelection.getModel().getSize() > 0)
            listViewVarSelection.setSelectedIndex(0);
        labelDatatypeCommand.setText(selectedKey + " Commands");
        labelCurrentValue.setText(selectedKey + " Value");
        listViewCommandSelectionModel.clear();
        for (String key : Main.typeCommandMap.get(Main.guiTypeMap.get(selectedKey)))
            listViewCommandSelectionModel.addElement(key);
        if (listViewCommandSelection.getModel().getSize() > 0)
            listViewCommandSelection.setSelectedIndex(0);
        refreshGUICommand();
    }

    private void refreshGUICommand() {
        if (listViewVarSelection.getSelectedValue() != null)
            textFieldKey.setText(listViewVarSelection.getSelectedValue().toString());
        if (listViewCommandSelection.getSelectedValue() != null)
            textFieldCommand.setText(listViewCommandSelection.getSelectedValue().toString());
        if (textFieldCommandValue.getText() != null)
            textFieldValue.setText(textFieldCommandValue.getText());
    }

    private void refreshCurrentValue() {
        if (listViewVarSelection.getSelectedValue() != null)
            textFieldCurrentValue.setText(Main.getKeyValue(listViewVarSelection.getSelectedValue().toString()));
    }

}
