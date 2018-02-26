package adbm.git.ui;

import adbm.main.Main;
import adbm.main.ui.MainWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GitWindow extends JDialog
{
    private JList<String> listBranches;
    private JPanel panel;
    private JList<RevCommit> listCommits;
    private JTextField textFieldSelectedBranch;
    private JTextField textFieldSelectedCommit;
    private JButton buttonSelectCommit;
    private JButton buttonSelectBranch;
    private JTextField textFieldNumberCommits;
    private JButton buttonAddBranch;
    private JButton buttonApplyNumber;
    private JButton buttonUpdate;
    private JButton buttonAddToBenchmark;
    private JList<RevCommit> listBenchmarkCommits;
    private JButton buttonRemoveBenchmarkCommit;
    private JButton resetBenchmarkListButton;

    private DefaultListModel<String> listBranchesModel = new DefaultListModel<>();
    private DefaultListModel<RevCommit> listCommitsModel = new DefaultListModel<>();
    private DefaultListModel<RevCommit> listBenchmarkCommitsModel = new DefaultListModel<>();

    private static final Logger log = LogManager.getLogger(GitWindow.class);

    private static GitWindow gitWindow;

    private static void checkGitWindow()
    {
        if (gitWindow == null) {
            gitWindow = new GitWindow();
        }
    }

    public static GitWindow getGitWindow()
    {
        checkGitWindow();
        return gitWindow;
    }

    public static void showGitWindow()
    {
        checkGitWindow();
        gitWindow.setVisible(true);
    }

    private GitWindow()
    {
        super(MainWindow.getMainWindow(),"Antidote Git Manager", ModalityType.MODELESS);

        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(MainWindow.getMainWindow());
        listBranches.setModel(listBranchesModel);
        listCommits.setModel(listCommitsModel);
        listCommits.setCellRenderer(new ListCellRendererCommits());
        listBenchmarkCommits.setModel(listBenchmarkCommitsModel);
        listBenchmarkCommits.setCellRenderer(new ListCellRendererCommits());
        updateAll();
        buttonAddBranch.addActionListener(e -> {
            AddBranchDialog.showAddBranchDialog();
        });
        buttonSelectBranch.addActionListener(e -> {
            if (listBranches.getSelectedValue() != null && !listBranches.getSelectedValue()
                                                                        .equals(textFieldSelectedBranch
                                                                                        .getText()))//TODO not correct
            {
                Main.getGitManager().checkoutBranch(listBranches.getSelectedValue());
                updateAll();
            }
        });
        buttonSelectCommit.addActionListener(e -> {
            if (listCommits.getSelectedValue() != null && !listCommits.getSelectedValue().toString()
                                                                      .equals(textFieldSelectedCommit
                                                                                      .getText()))//TODO not correct
            {
                Main.getGitManager().checkoutCommit((listCommits.getSelectedValue()).getName());
                updateAll();
            }
        });

        buttonApplyNumber.addActionListener(e -> updateListCommits());
        buttonUpdate.addActionListener(e -> updateAll());
        buttonAddToBenchmark.addActionListener(e -> {
            //TODO
            if (listCommits.getSelectedValue() != null) {
                Main.getSettingsManager().addBenchmarkCommit((listCommits.getSelectedValue()).getName());
            }
            updateListBenchmarkCommits();
        });
        buttonRemoveBenchmarkCommit.addActionListener(e -> {
            if (listBenchmarkCommits.getSelectedValue() != null)//TODO not correct
                Main.getSettingsManager().removeBenchmarkCommit((listBenchmarkCommits.getSelectedValue()).getName());
            updateListBenchmarkCommits();
        });
        resetBenchmarkListButton.addActionListener(e -> {
            Main.getSettingsManager().resetBenchmarkCommits();
            updateListBenchmarkCommits();
        });
    }

    private void updateAll()
    {
        updateListBranches();
        updateListCommits();
        updateTextFieldSelectedBranch();
        updateTextFieldSelectedCommit();
        updateListBenchmarkCommits();
    }

    private void updateTextFieldSelectedBranch()
    {
        textFieldSelectedBranch.setText(Main.getGitManager().getCurrentBranch());
    }

    private void updateTextFieldSelectedCommit()
    {
        textFieldSelectedCommit.setText(Main.getGitManager().getCurrentCommit().getName());
    }


    private void updateListBranches()
    {
        List<String> list = Main.getGitManager().getAllLocalBranches();
        boolean equal = true;
        if (listBranchesModel.getSize() != list.size()) {
            equal = false;
        }
        for (int i = 0; i < listBranchesModel.getSize(); i++) {
            if (!list.contains(listBranchesModel.get(i))) {
                equal = false;
                break;
            }
        }
        if (!equal) {
            listBranchesModel.clear();
            for (String branch : list) {
                listBranchesModel.addElement(branch);
            }
        }
    }

    private void updateListCommits()
    {
        int number = 10;
        try {
            number = Integer.parseInt(textFieldNumberCommits.getText());
        } catch (NumberFormatException e) {
            log.error("An error occurred while parsing a number!", e);
        }
        List<RevCommit> list = Main.getGitManager().getCommitsForCurrentHead(number);
        boolean equal = true;
        if (listCommitsModel.getSize() != list.size()) {
            equal = false;
        }
        for (int i = 0; i < listCommitsModel.getSize(); i++) {
            if (!list.contains(listCommitsModel.get(i))) {
                equal = false;
                break;
            }
        }
        if (!equal) {
            listCommitsModel.clear();
            for (RevCommit commit : list) {
                listCommitsModel.addElement(commit);
            }
        }
    }

    private void updateListBenchmarkCommits()
    {
        List<RevCommit> list = new ArrayList<>();
        Main.getSettingsManager().getBenchmarkCommits().forEach(commitId -> {
            RevCommit commit = Main.getGitManager().getCommitFromID(commitId);
            if (commit != null) list.add(commit);
        });
        list.sort((o1, o2) -> {
            if (o1.getCommitTime() > o2.getCommitTime()) return -1;
            if (o1.getCommitTime() < o2.getCommitTime()) return 1;
            return 0;
        });
        boolean equal = true;
        if (listBenchmarkCommitsModel.getSize() != list.size()) {
            equal = false;
        }
        for (int i = 0; i < listBenchmarkCommitsModel.getSize(); i++) {
            if (!list.contains(listBenchmarkCommitsModel.get(i))) {
                equal = false;
                break;
            }
        }
        if (!equal) {
            listBenchmarkCommitsModel.clear();
            for (RevCommit commit : list) {
                listBenchmarkCommitsModel.addElement(commit);
            }
        }
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
        panel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 6, new Insets(20, 20, 20, 20), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Available Branches");
        panel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1,
                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2,
                                                                                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                null, null, null, 0, false));
        listBranches = new JList();
        listBranches.setSelectionMode(0);
        scrollPane1.setViewportView(listBranches);
        buttonUpdate = new JButton();
        buttonUpdate.setText("Update");
        panel.add(buttonUpdate, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                 null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel.add(scrollPane2, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 3,
                                                                                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                null, new Dimension(300, 500), null, 0,
                                                                                false));
        listCommits = new JList();
        listCommits.setSelectionMode(0);
        scrollPane2.setViewportView(listCommits);
        textFieldSelectedBranch = new JTextField();
        panel.add(textFieldSelectedBranch, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 2,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                            null,
                                                                                            new Dimension(150, -1),
                                                                                            null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Selected Branch:");
        panel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2,
                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Available Commits");
        panel.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 3,
                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Selected Commit:");
        panel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1,
                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           null, null, null, 0, false));
        textFieldSelectedCommit = new JTextField();
        panel.add(textFieldSelectedCommit, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 3,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                            null,
                                                                                            new Dimension(150, -1),
                                                                                            null, 0, false));
        buttonSelectBranch = new JButton();
        buttonSelectBranch.setText("Checkout Branch");
        panel.add(buttonSelectBranch, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                       null, null, null, 0, false));
        buttonSelectCommit = new JButton();
        buttonSelectCommit.setText("Checkout  Commit");
        panel.add(buttonSelectCommit, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                       null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Number of Commits");
        panel.add(label5, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1,
                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           null, null, null, 0, false));
        buttonAddBranch = new JButton();
        buttonAddBranch.setText("Add Local Branch");
        panel.add(buttonAddBranch, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 2,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                    com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                    null, null, null, 0, false));
        textFieldNumberCommits = new JTextField();
        textFieldNumberCommits.setText("10");
        panel.add(textFieldNumberCommits, new com.intellij.uiDesigner.core.GridConstraints(5, 3, 1, 1,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                           null, new Dimension(150, -1),
                                                                                           null, 0, false));
        buttonApplyNumber = new JButton();
        buttonApplyNumber.setText("Apply Number");
        panel.add(buttonApplyNumber, new com.intellij.uiDesigner.core.GridConstraints(5, 4, 1, 1,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                      com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                      null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Commits for Benchmark");
        panel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1,
                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                           null, null, null, 0, false));
        buttonAddToBenchmark = new JButton();
        buttonAddToBenchmark.setText("Add Commit to Benchmark");
        panel.add(buttonAddToBenchmark, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                         null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel.add(scrollPane3, new com.intellij.uiDesigner.core.GridConstraints(2, 5, 1, 1,
                                                                                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH,
                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                null, null, null, 0, false));
        listBenchmarkCommits = new JList();
        listBenchmarkCommits.setSelectionMode(0);
        scrollPane3.setViewportView(listBenchmarkCommits);
        buttonRemoveBenchmarkCommit = new JButton();
        buttonRemoveBenchmarkCommit.setText("Remove Commit From Benchmark");
        panel.add(buttonRemoveBenchmarkCommit, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1,
                                                                                                com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                                com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                                com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                                null, null, null, 0,
                                                                                                false));
        resetBenchmarkListButton = new JButton();
        resetBenchmarkListButton.setText("Reset Benchmark List");
        panel.add(resetBenchmarkListButton, new com.intellij.uiDesigner.core.GridConstraints(4, 5, 1, 1,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                             null, null, null, 0,
                                                                                             false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel;
    }

    private class ListCellRendererCommits extends DefaultListCellRenderer
    {

        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            RevCommit revCommit = (RevCommit) value;
            String sha = revCommit.getName();
            String author = revCommit.getAuthorIdent().toExternalString();
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            String time = df.format(new Date((long) revCommit.getCommitTime() * 1000));
            String message = revCommit.getShortMessage().trim();
            String cellText = "<html>SHA: " + sha + "<br/>Author: " + author + "<br/>Date: " + time + "<br/>Message: " + message;
            setBorder(BorderFactory.createEtchedBorder());
            setText(cellText.trim());

            return this;
        }

    }
}
