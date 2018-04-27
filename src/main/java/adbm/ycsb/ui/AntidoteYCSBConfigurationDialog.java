package adbm.ycsb.ui;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.util.AntidoteUtil;
import adbm.git.ui.GitDialog;
import adbm.main.Main;
import adbm.main.ui.MainWindow;
import adbm.util.AdbmConstants;
import adbm.util.EverythingIsNonnullByDefault;
import adbm.util.TextPaneAppender;
import adbm.util.helpers.FileUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EverythingIsNonnullByDefault
public class AntidoteYCSBConfigurationDialog extends JDialog
{
    private static final Logger log = LogManager.getLogger(AntidoteYCSBConfigurationDialog.class);

    private JTextPane textPaneConsole;
    private JButton buttonRunYCSBBenchmark;
    private JComboBox<AntidotePB.CRDT_type> comboBoxCrdt;
    private JComboBox<String> comboBoxOperation;
    private JComboBox<IAntidoteClientWrapper.TransactionType> comboBoxTxType;
    private JComboBox<String> comboBoxWorkloadFile;
    private JButton buttonEditWorkloadFile;
    private JCheckBox checkBoxShowStatus;
    private JSpinner spinnerThreads;
    private JSpinner spinnerTarget;
    private JList<String> listCommits;
    private JButton buttonChangeListOfCommit;
    private JPanel panel;

    private DefaultComboBoxModel<String> comboBoxWorkloadModel = new DefaultComboBoxModel<>();
    private DefaultComboBoxModel<AntidotePB.CRDT_type> comboBoxCrdtModel = new DefaultComboBoxModel<>(
            AntidotePB.CRDT_type.values());
    private DefaultComboBoxModel<IAntidoteClientWrapper.TransactionType> comboBoxTxTypeModel = new DefaultComboBoxModel<>(
            IAntidoteClientWrapper.TransactionType.values());
    private DefaultComboBoxModel<String> comboBoxOperationModel = new DefaultComboBoxModel<>();
    private DefaultListModel<String> listCommitsModel = new DefaultListModel<>();

    @Nullable
    private static AntidoteYCSBConfigurationDialog antidoteYCSBConfigurationDialog;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static void checkBenchmarkDialog()
    {
        if (antidoteYCSBConfigurationDialog == null) {
            antidoteYCSBConfigurationDialog = new AntidoteYCSBConfigurationDialog();
        }
    }

    public static AntidoteYCSBConfigurationDialog getAntidoteYCSBConfigurationDialog()
    {
        checkBenchmarkDialog();
        return antidoteYCSBConfigurationDialog;
    }

    public static void showBenchmarkDialog()
    {
        checkBenchmarkDialog();
        antidoteYCSBConfigurationDialog.invokeCommitChange();
        antidoteYCSBConfigurationDialog.setVisible(true);
    }

    private AntidoteYCSBConfigurationDialog()
    {
        super(MainWindow.getMainWindow(), "YCSB Benchmark Manager", ModalityType.MODELESS);
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(MainWindow.getMainWindow());
        spinnerThreads.setValue(Main.getAntidoteYCSBConfiguration().getNumberOfThreads());
        JComponent comp = spinnerThreads.getEditor();
        JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
        DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
        formatter.setCommitsOnValidEdit(true);
        spinnerThreads.addChangeListener(e -> {
            int numberOfThreads = 0;
            try {
                numberOfThreads = Integer.parseInt(spinnerThreads.getValue().toString());
            } catch (NumberFormatException e1) {
                log.error("Thread Count is not a Number!", e1);
            }
            Main.getAntidoteYCSBConfiguration().setNumberOfThreads(numberOfThreads);
            spinnerThreads.setValue(Main.getAntidoteYCSBConfiguration().getNumberOfThreads());
        });
        spinnerTarget.setValue(Main.getAntidoteYCSBConfiguration().getTargetNumber());
        comp = spinnerTarget.getEditor();
        field = (JFormattedTextField) comp.getComponent(0);
        formatter = (DefaultFormatter) field.getFormatter();
        formatter.setCommitsOnValidEdit(true);
        spinnerTarget.addChangeListener(e -> {
            int targetNumber = 0;
            try {
                targetNumber = Integer.parseInt(spinnerTarget.getValue().toString());
            } catch (NumberFormatException e1) {
                log.error("Target Count is not a Number!", e1);
            }
            Main.getAntidoteYCSBConfiguration().setTargetNumber(targetNumber);
            spinnerTarget.setValue(Main.getAntidoteYCSBConfiguration().getTargetNumber());
        });

        comboBoxCrdt.setModel(comboBoxCrdtModel);
        comboBoxOperation.setModel(comboBoxOperationModel);
        comboBoxTxType.setModel(comboBoxTxTypeModel);
        comboBoxWorkloadFile.setModel(comboBoxWorkloadModel);
        listCommits.setModel(listCommitsModel);
        updateWorkloads();
        updateTransactionType();
        updateCrdtType();
        updateOperations();
        updateStatus();
        invokeCommitChange();
        TextPaneAppender.addTextPane(textPaneConsole);

        buttonChangeListOfCommit.addActionListener(e -> {
            if (!Main.getGitManager().isReady()) Main.getGitManager().start();
            GitDialog.showGitDialog();
        });
        comboBoxCrdt.addActionListener(e -> {
            AntidotePB.CRDT_type selectedItem = (AntidotePB.CRDT_type) comboBoxCrdtModel.getSelectedItem();
            if (selectedItem == null) return;
            if (!Main.getAntidoteYCSBConfiguration().getUsedKeyType().equals(selectedItem)) {
                Main.getAntidoteYCSBConfiguration().setUsedKeyType(selectedItem);
                updateCrdtType();
            }
        });
        comboBoxOperation.addActionListener(e -> {
            Object selectedObject = comboBoxOperationModel.getSelectedItem();
            if (selectedObject == null) return;
            String selectedItem = comboBoxOperationModel.getSelectedItem().toString();
            if (!Main.getAntidoteYCSBConfiguration().getUsedOperation().equals(selectedItem)) {
                Main.getAntidoteYCSBConfiguration().setUsedOperation(selectedItem);
                updateOperations();
            }
        });
        comboBoxTxType.addActionListener(e -> {
            IAntidoteClientWrapper.TransactionType selectedItem = (IAntidoteClientWrapper.TransactionType) comboBoxTxTypeModel
                    .getSelectedItem();
            if (selectedItem == null) return;
            if (!Main.getAntidoteYCSBConfiguration().getUsedTransactionType().equals(selectedItem)) {
                Main.getAntidoteYCSBConfiguration().setUsedTransactionType(selectedItem);
                updateTransactionType();
            }
        });
        comboBoxWorkloadFile.addActionListener(e -> {
            // TODO checks
            Object selectedObject = comboBoxOperationModel.getSelectedItem();
            if (selectedObject == null) return;
            String selectedItem = comboBoxWorkloadModel.getSelectedItem().toString();
            if (!Main.getAntidoteYCSBConfiguration().getUsedWorkLoad().equals(selectedItem)) {
                Main.getAntidoteYCSBConfiguration().setUsedWorkload(selectedItem);
                updateWorkloads();
            }
        });
        checkBoxShowStatus.addActionListener(e -> {
            Main.getAntidoteYCSBConfiguration().setShowStatus(checkBoxShowStatus.isSelected());
        });
        buttonEditWorkloadFile.addActionListener(e -> {
            try {
                Desktop.getDesktop()
                       .edit(new File(
                               AdbmConstants.getWorkloadPath(Main.getAntidoteYCSBConfiguration().getUsedWorkLoad())));
            } catch (IOException e1) {
                log.error("An error occurred while opening the Workload file with standard .txt file editor.", e1);
            }
        });
        buttonRunYCSBBenchmark.addActionListener(e -> executorService.execute(() -> {
            List<String> commits = new ArrayList<>();
            for (Object element : listCommitsModel.toArray()) {
                commits.add(element.toString());
            }
            Main.getAntidoteYCSBConfiguration().runBenchmark(commits);
        }));
    }

    public void invokeCommitChange()
    {
        SwingUtilities.invokeLater(() -> {
            listCommitsModel.removeAllElements();
            for (String commit : Main.getSettingsManager().getBenchmarkCommits())
                listCommitsModel.addElement(commit);
        });
        log.trace("Test");
    }

    private void updateStatus()
    {
        checkBoxShowStatus.setSelected(Main.getAntidoteYCSBConfiguration().getShowStatus());
    }

    private void updateOperations()
    {
        comboBoxOperationModel.removeAllElements();
        for (String operation : AntidoteUtil.typeOperationMap
                .get(Main.getAntidoteYCSBConfiguration().getUsedKeyType())) {
            comboBoxOperationModel.addElement(operation);
        }
        if (comboBoxOperationModel.getIndexOf(Main.getAntidoteYCSBConfiguration().getUsedOperation()) == -1) {
            Main.getAntidoteYCSBConfiguration().setUsedOperation(comboBoxOperationModel.getElementAt(0));
        }
        comboBoxOperationModel.setSelectedItem(Main.getAntidoteYCSBConfiguration().getUsedOperation());
    }

    private void updateCrdtType()
    {
        comboBoxCrdtModel.setSelectedItem(Main.getAntidoteYCSBConfiguration().getUsedKeyType());
        updateOperations();
    }

    private void updateTransactionType()
    {
        comboBoxTxTypeModel.setSelectedItem(Main.getAntidoteYCSBConfiguration().getUsedTransactionType());
    }

    private void updateWorkloads()
    {
        comboBoxWorkloadModel.removeAllElements();
        for (String fileName : FileUtil.getAllFileNamesInFolder(AdbmConstants.YCSB_WORKLOADS_FOLDER_PATH))
            comboBoxWorkloadModel.addElement(fileName);
        comboBoxWorkloadModel.setSelectedItem(Main.getAntidoteYCSBConfiguration().getUsedWorkLoad());
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
        panel = new JPanel();
        panel.setLayout(new GridLayoutManager(12, 4, new Insets(20, 20, 20, 20), -1, -1));
        panel.setMinimumSize(new Dimension(600, 600));
        panel.setPreferredSize(new Dimension(600, 600));
        final JLabel label1 = new JLabel();
        label1.setText("Console Output");
        panel.add(label1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        buttonRunYCSBBenchmark = new JButton();
        buttonRunYCSBBenchmark.setText("Run YCSB Benchmark");
        panel.add(buttonRunYCSBBenchmark,
                  new GridConstraints(11, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxCrdt = new JComboBox();
        panel.add(comboBoxCrdt,
                  new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                      null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Antidote CRDT Type");
        panel.add(label2, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        comboBoxOperation = new JComboBox();
        panel.add(comboBoxOperation,
                  new GridConstraints(6, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                      null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("CRDT Operation");
        panel.add(label3, new GridConstraints(5, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Transaction Type");
        panel.add(label4, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Workload File");
        panel.add(label5, new GridConstraints(7, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        comboBoxTxType = new JComboBox();
        panel.add(comboBoxTxType,
                  new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                      null, 0, false));
        comboBoxWorkloadFile = new JComboBox();
        panel.add(comboBoxWorkloadFile,
                  new GridConstraints(8, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null,
                                      null, 0, false));
        buttonEditWorkloadFile = new JButton();
        buttonEditWorkloadFile.setText("Edit Workload File");
        panel.add(buttonEditWorkloadFile,
                  new GridConstraints(9, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBoxShowStatus = new JCheckBox();
        checkBoxShowStatus.setText("Show Status");
        panel.add(checkBoxShowStatus,
                  new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Number of Threads");
        panel.add(label6, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        spinnerThreads = new JSpinner();
        panel.add(spinnerThreads,
                  new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                      null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Target Number of Operations");
        panel.add(label7, new GridConstraints(10, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        spinnerTarget = new JSpinner();
        panel.add(spinnerTarget,
                  new GridConstraints(10, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                      null, null, 0, false));
        listCommits = new JList();
        listCommits.setSelectionMode(0);
        panel.add(listCommits, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                   GridConstraints.SIZEPOLICY_CAN_GROW,
                                                   GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(150, 50),
                                                   null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("List of Commits that will be benchmarked");
        panel.add(label8, new GridConstraints(2, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        buttonChangeListOfCommit = new JButton();
        buttonChangeListOfCommit.setEnabled(true);
        buttonChangeListOfCommit.setText("Change List of Commit");
        panel.add(buttonChangeListOfCommit,
                  new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel.add(scrollPane1, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                   null, null, null, 0, false));
        textPaneConsole = new JTextPane();
        textPaneConsole.setDoubleBuffered(true);
        textPaneConsole.setEditable(false);
        scrollPane1.setViewportView(textPaneConsole);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel;
    }
}
