package adbm.ycsb.ui;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.util.AntidoteUtil;
import adbm.git.ui.GitDialog;
import adbm.main.Main;
import adbm.main.ui.MainWindow;
import adbm.util.AdbmConstants;
import adbm.util.TextPaneAppender;
import adbm.util.helpers.FileUtil;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                       .edit(new File(AdbmConstants.getWorkloadPath(Main.getAntidoteYCSBConfiguration().getUsedWorkLoad())));
            } catch (IOException e1) {
                log.error("An error occurred while opening the Workload file with standard .txt file editor.", e1);
            }
        });
        buttonRunYCSBBenchmark.addActionListener(e -> {
            executorService.execute(() -> {
                List<String> commits = new ArrayList<>();
                for (Object element : listCommitsModel.toArray()) {
                    commits.add(element.toString());
                }
                Main.getAntidoteYCSBConfiguration().runBenchmark(commits);
            });
        });
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
        for (String operation : AntidoteUtil.typeOperationMap.get(Main.getAntidoteYCSBConfiguration().getUsedKeyType())) {
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
}
