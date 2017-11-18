import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AntidoteGUI {
    private JPanel panel1;
    private JButton executeButton;
    private JTextField textFieldCommand;
    private JComboBox comboBoxDatatype;
    private JList listViewCommandSelection;
    private JList listViewVarSelection;
    private JTextField textFieldCommandValue;
    private JLabel labelDataTypeVar;
    private JLabel labelDatatypeCommand;
    private JTextField textFieldCurrentValue;
    private JLabel labelCurrentValue;

    public AntidoteGUI() {

        JFrame frame = new JFrame("AntidoteGUI");
        frame.setContentPane(panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        // TODO set initial values for everything

        DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
        comboBoxDatatype.setModel(comboModel);
        for (String type : Main.types)
            comboModel.addElement(type);

        comboBoxDatatype.setSelectedIndex(0);
        DefaultListModel listModel = new DefaultListModel();
        listViewVarSelection.setModel(listModel);

        DefaultListModel listModel2 = new DefaultListModel();
        listViewCommandSelection.setModel(listModel2);

        refreshGUIDatatype();

        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        listViewCommandSelection.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshGUICommand();
            }
        });
        comboBoxDatatype.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshGUIDatatype();
            }
        });
        listViewVarSelection.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                textFieldCurrentValue.setText(Main.getKeyData(listViewVarSelection.getSelectedValue().toString()));
            }
        });
    }

    private void refreshGUIDatatype() {

        String selectedKey = comboBoxDatatype.getSelectedItem().toString();
        labelDataTypeVar.setText("Select " + selectedKey);
        DefaultListModel listModel = (DefaultListModel) listViewVarSelection.getModel();
        listModel.clear();
        for (String key : Main.getKeys(selectedKey))
            listModel.addElement(key);
        listViewVarSelection.setSelectedIndex(0);
        labelDatatypeCommand.setText(selectedKey + " Commands");
        labelCurrentValue.setText(selectedKey + " Value");
        DefaultListModel listModel2 = (DefaultListModel) listViewCommandSelection.getModel();
        listModel2.clear();
        for (String key : Main.typeCommandMap.get(selectedKey))
            listModel2.addElement(key);
        listViewCommandSelection.setSelectedIndex(0);
        refreshGUICommand();
    }

    private void refreshGUICommand() {

        textFieldCommand.setText("Key: " + listViewVarSelection.getSelectedValue() + " Command: " + listViewCommandSelection.getSelectedValue().toString() + " Value: " + textFieldCommandValue.getText());

    }

}
