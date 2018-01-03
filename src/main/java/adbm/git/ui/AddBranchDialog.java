package adbm.git.ui;

import adbm.git.GitManager;

import javax.swing.*;
import java.awt.event.WindowEvent;

public class AddBranchDialog
{
    private JList listAvailableBranches;
    private JPanel panel;
    private JButton buttonAddBranch;
    private JFrame frame;

    public AddBranchDialog()
    {
        frame = new JFrame("Add Branch");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        DefaultListModel listAvailableBranchesModel = new DefaultListModel();
        GitManager.getAllNonLocalRemoteBranches().forEach(listAvailableBranchesModel::addElement);
        listAvailableBranches.setModel(listAvailableBranchesModel);

        buttonAddBranch.addActionListener(e -> {
            if (listAvailableBranches.getSelectedValue() != null)
            {
                GitManager.checkoutBranch(listAvailableBranches.getSelectedValue().toString());
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });
    }
}
