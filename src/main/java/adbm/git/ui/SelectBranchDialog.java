package adbm.git.ui;

import adbm.git.GitManager;
import adbm.settings.MapDBManager;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SelectBranchDialog extends JFrame
{
    private JList listBranches;
    private JPanel panel;
    private JList listCommits;
    private JTextField textFieldSelectedBranch;
    private JTextField textFieldSelectedCommit;
    private JButton buttonSelectCommit;
    private JButton buttonSelectBranch;
    private JTextField textFieldNumberCommits;
    private JButton buttonAddBranch;
    private JButton buttonApplyNumber;
    private JButton buttonUpdate;
    private JButton buttonAddToBenchmark;
    private JList listBenchmarkCommits;
    private JButton buttonRemoveBenchmarkCommit;
    private JButton resetBenchmarkListButton;

    private DefaultListModel listBranchesModel = new DefaultListModel();
    private DefaultListModel listCommitsModel = new DefaultListModel();
    private DefaultListModel listBenchmarkCommitsModel = new DefaultListModel();

    public SelectBranchDialog()
    {
        super("Select Branch");

        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        listBranches.setModel(listBranchesModel);
        listCommits.setModel(listCommitsModel);
        listCommits.setCellRenderer(new ListCellRendererCommits());
        listBenchmarkCommits.setModel(listBenchmarkCommitsModel);
        listBenchmarkCommits.setCellRenderer(new ListCellRendererCommits());
        updateAll();
        buttonAddBranch.addActionListener(e -> {
            new AddBranchDialog();
        });
        buttonSelectBranch.addActionListener(e -> {
            if (listBranches.getSelectedValue() != null && !listBranches.getSelectedValue().toString()
                                                                        .equals(textFieldSelectedBranch
                                                                                        .getText()))//TODO not correct
            {
                GitManager.checkoutBranch(listBranches.getSelectedValue().toString());
                updateAll();
            }
        });
        buttonSelectCommit.addActionListener(e -> {
            if (listCommits.getSelectedValue() != null && !listCommits.getSelectedValue().toString()
                                                                      .equals(textFieldSelectedCommit
                                                                                      .getText()))//TODO not correct
            {
                GitManager.checkoutCommit(((RevCommit) listCommits.getSelectedValue()).getName());
                updateAll();
            }
        });

        buttonApplyNumber.addActionListener(e -> {
            updateListCommits();
        });
        buttonUpdate.addActionListener(e -> {
            updateAll();
        });
        buttonAddToBenchmark.addActionListener(e -> {
            //TODO
            if (listCommits.getSelectedValue() != null) {
                MapDBManager.addBenchmarkCommit(((RevCommit) listCommits.getSelectedValue()).getName());
            }
            updateListBenchmarkCommits();
        });
        buttonRemoveBenchmarkCommit.addActionListener(e -> {
            if (listBenchmarkCommits.getSelectedValue() != null)//TODO not correct
                MapDBManager.removeBenchmarkCommit(((RevCommit) listBenchmarkCommits.getSelectedValue()).getName());
            updateListBenchmarkCommits();
        });
        resetBenchmarkListButton.addActionListener(e -> {
            MapDBManager.resetBenchmarkCommits();
            updateListBenchmarkCommits();
        });
    }

    public void updateAll()
    {
        updateListBranches();
        updateListCommits();
        updateTextFieldSelectedBranch();
        updateTextFieldSelectedCommit();
        updateListBenchmarkCommits();
    }

    public void updateTextFieldSelectedBranch()
    {
        textFieldSelectedBranch.setText(GitManager.getCurrentBranch());
    }

    public void updateTextFieldSelectedCommit()
    {
        textFieldSelectedCommit.setText(GitManager.getCurrentCommit().getName());
    }


    public void updateListBranches()
    {
        List<String> list = GitManager.getAllLocalBranches();
        boolean equal = true;
        if (listBranchesModel.getSize() != list.size()) {
            equal = false;
        }
        for (int i = 0; i < listBranchesModel.getSize(); i++) {
            if (!list.contains(listBranchesModel.get(i).toString())) {
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

    public void updateListCommits()
    {
        int number = 10;
        try {
            number = Integer.parseInt(textFieldNumberCommits.getText());
        } catch (Exception e) {

        }
        List<RevCommit> list = GitManager.getCommitsForCurrentHead(number);
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

    public void updateListBenchmarkCommits()
    {
        List<RevCommit> list = new ArrayList<>();
        MapDBManager.getBenchmarkCommits().forEach(commitId -> {
            RevCommit commit = GitManager.getCommitFromID(commitId);
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
