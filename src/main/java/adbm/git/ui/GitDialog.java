package adbm.git.ui;

import adbm.main.Main;
import adbm.main.ui.MainWindow;
import adbm.util.EverythingIsNonnullByDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@EverythingIsNonnullByDefault
public class GitDialog extends JDialog
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

    private static final Logger log = LogManager.getLogger(GitDialog.class);

    @Nullable
    private static GitDialog gitDialog;

    public static GitDialog getGitDialog()
    {
        if (gitDialog == null) {
            gitDialog = new GitDialog();
        }
        return gitDialog;
    }

    public static void showGitDialog()
    {
        if (gitDialog == null) {
            gitDialog = new GitDialog();
        }
        gitDialog.setVisible(true);
    }

    private GitDialog()
    {
        super(MainWindow.getMainWindow(),"Antidote Git Repo Manager", ModalityType.MODELESS);

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
            RevCommit commit = Main.getGitManager().getCommitFromId(commitId);
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
