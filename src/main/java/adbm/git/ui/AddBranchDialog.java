package adbm.git.ui;

import adbm.main.Main;
import adbm.util.EverythingIsNonnullByDefault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

@EverythingIsNonnullByDefault
public class AddBranchDialog extends JDialog
{
    private static final Logger log = LogManager.getLogger(AddBranchDialog.class);

    private JList<String> listAvailableBranches;
    private JPanel panel;
    private JButton buttonAddBranch;

    private static AddBranchDialog addBranchDialog;

    public static void showAddBranchDialog() {
        addBranchDialog = new AddBranchDialog();
        addBranchDialog.setVisible(true);
    }

    private AddBranchDialog()
    {
        super(GitDialog.getGitDialog(), "Add Branch", ModalityType.APPLICATION_MODAL);
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(GitDialog.getGitDialog());
        DefaultListModel<String> listAvailableBranchesModel = new DefaultListModel<>();
        Main.getGitManager().getAllNonLocalRemoteBranches().forEach(listAvailableBranchesModel::addElement);
        listAvailableBranches.setModel(listAvailableBranchesModel);

        buttonAddBranch.addActionListener(e -> {
            if (listAvailableBranches.getSelectedValue() != null) {
                Main.getGitManager().checkoutBranch(listAvailableBranches.getSelectedValue());
                this.dispose();
            }
        });
    }

}
