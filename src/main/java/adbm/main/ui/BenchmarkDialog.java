package adbm.main.ui;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.util.AntidoteUtil;
import adbm.git.managers.GitManager;
import adbm.git.ui.GitDialog;
import adbm.main.Main;
import adbm.util.AdbmConstants;
import adbm.util.TextPaneAppender;
import adbm.util.helpers.FileUtil;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BenchmarkDialog extends JDialog
{
    private static final Logger log = LogManager.getLogger(BenchmarkDialog.class);

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
    private DefaultComboBoxModel<AntidotePB.CRDT_type> comboBoxCrdtModel = new DefaultComboBoxModel<>(AntidotePB.CRDT_type.values());
    private DefaultComboBoxModel<IAntidoteClientWrapper.TransactionType> comboBoxTxTypeModel = new DefaultComboBoxModel<>(
            IAntidoteClientWrapper.TransactionType.values());
    private DefaultComboBoxModel<String> comboBoxOperationModel = new DefaultComboBoxModel<>();
    private DefaultListModel<String> listCommitsModel = new DefaultListModel<>();

    private static BenchmarkDialog benchmarkDialog;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static void checkBenchmarkDialog()
    {
        if (benchmarkDialog == null) {
            benchmarkDialog = new BenchmarkDialog();
        }
    }

    public static BenchmarkDialog getBenchmarkDialog()
    {
        checkBenchmarkDialog();
        return benchmarkDialog;
    }

    public static void showBenchmarkDialog()
    {
        checkBenchmarkDialog();
        benchmarkDialog.invokeCommitChange();
        benchmarkDialog.setVisible(true);
    }

    private BenchmarkDialog() {
        super(MainWindow.getMainWindow(),"YCSB Benchmark Manager", ModalityType.MODELESS);
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(MainWindow.getMainWindow());
        spinnerThreads.setValue(Main.getBenchmarkConfig().getNumberOfThreads());
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
            Main.getBenchmarkConfig().setNumberOfThreads(numberOfThreads);
            spinnerThreads.setValue(Main.getBenchmarkConfig().getNumberOfThreads());
        });
        spinnerTarget.setValue(Main.getBenchmarkConfig().getNumberOfThreads());
        comp = spinnerTarget.getEditor();
        field = (JFormattedTextField) comp.getComponent(0);
        formatter = (DefaultFormatter) field.getFormatter();
        formatter.setCommitsOnValidEdit(true);
        spinnerTarget.addChangeListener(e -> {
            int numberOfThreads = 0;
            try {
                numberOfThreads = Integer.parseInt(spinnerTarget.getValue().toString());
            } catch (NumberFormatException e1) {
                log.error("Thread Count is not a Number!", e1);
            }
            Main.getBenchmarkConfig().setNumberOfThreads(numberOfThreads);
            spinnerTarget.setValue(Main.getBenchmarkConfig().getNumberOfThreads());
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
            IAntidoteClientWrapper.TransactionType selectedItem = (IAntidoteClientWrapper.TransactionType) comboBoxTxTypeModel.getSelectedItem();
            if (!Main.getBenchmarkConfig().getUsedTransactionType().equals(selectedItem)) {
                Main.getBenchmarkConfig().setUsedTransactionType(selectedItem);
                updateTransactionType();
            }
        });
        comboBoxOperation.addActionListener(e -> {

        });
        comboBoxTxType.addActionListener(e -> {
            AntidotePB.CRDT_type selectedItem = (AntidotePB.CRDT_type) comboBoxCrdtModel.getSelectedItem();
            if (!Main.getBenchmarkConfig().getUsedKeyType().equals(selectedItem)) {
                Main.getBenchmarkConfig().setUsedKeyType(selectedItem);
                updateCrdtType();
            }
        });
        comboBoxWorkloadFile.addActionListener(e -> {
            // TODO checks
            String selectedItem = comboBoxWorkloadModel.getSelectedItem().toString();
            if (!Main.getBenchmarkConfig().getUsedWorkLoad().equals(selectedItem)) {
                Main.getBenchmarkConfig().setUsedWorkload(selectedItem);
                updateWorkloads();
            }
        });
        checkBoxShowStatus.addActionListener(e -> {
            Main.getBenchmarkConfig().setShowStatus(checkBoxShowStatus.isSelected());
        });
        buttonEditWorkloadFile.addActionListener(e -> {
            try {
                Desktop.getDesktop().edit(new File(AdbmConstants.getWorkloadPath(Main.getBenchmarkConfig().getUsedWorkLoad())));
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
                Main.getBenchmarkConfig().runBenchmark(commits);
            });
        });
    }

    public void invokeCommitChange() {
        SwingUtilities.invokeLater(() -> {
            listCommitsModel.removeAllElements();
            for (String commit : Main.getSettingsManager().getBenchmarkCommits())
                listCommitsModel.addElement(commit);
        });
    }

    private void updateStatus() {
        checkBoxShowStatus.setSelected(Main.getBenchmarkConfig().getShowStatus());
    }

    private void updateOperations() {
        comboBoxOperationModel.removeAllElements();
        for (String operation : AntidoteUtil.typeOperationMap.get(Main.getBenchmarkConfig().getUsedKeyType())) {
            comboBoxOperationModel.addElement(operation);
        }
        if (comboBoxOperationModel.getIndexOf(Main.getBenchmarkConfig().getUsedOperation()) == -1)
        {
            Main.getBenchmarkConfig().setUsedOperation(comboBoxOperationModel.getElementAt(0));
            comboBoxOperationModel.setSelectedItem(Main.getBenchmarkConfig().getUsedOperation());
        }
    }

    private void updateCrdtType() {
        comboBoxCrdtModel.setSelectedItem(Main.getBenchmarkConfig().getUsedKeyType());
        updateOperations();
    }

    private void updateTransactionType() {
        comboBoxTxTypeModel.setSelectedItem(Main.getBenchmarkConfig().getUsedTransactionType());
    }

    private void updateWorkloads()
    {
        comboBoxWorkloadModel.removeAllElements();
        for (String fileName : FileUtil.getAllFileNamesInFolder(AdbmConstants.YCSB_WORKLOADS_PATH))
            comboBoxWorkloadModel.addElement(FilenameUtils.removeExtension(fileName));
        comboBoxWorkloadModel.setSelectedItem(Main.getBenchmarkConfig().getUsedWorkLoad());
    }
}
