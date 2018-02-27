package adbm.settings.ui;

import adbm.main.Main;
import adbm.main.ui.MainWindow;
import adbm.util.AdbmConstants;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static adbm.util.helpers.FormatUtil.format;

public class SettingsDialog extends JDialog
{

    private static final Logger log = LogManager.getLogger(SettingsDialog.class);

    private JTextField textFieldRepositoryLocation;
    private JButton buttonSetRepoLocation;
    private JPanel panel;

    public static void showSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog();
        settingsDialog.setVisible(true);
    }

    private SettingsDialog()
    {
        super(MainWindow.getMainWindow(), "Settings", ModalityType.APPLICATION_MODAL);
        setTitle("Settings");
        setIconImage(new ImageIcon(format("{}/AntidoteIcon.PNG", AdbmConstants.imagesPath)).getImage());
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(MainWindow.getMainWindow());
        textFieldRepositoryLocation.setText(Main.getSettingsManager().getGitRepoLocation());
        buttonSetRepoLocation.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                log.info("Selected file for Repository: {}", selectedFile.getAbsolutePath());
                Main.getSettingsManager().setGitRepoLocation(selectedFile.getAbsolutePath());
                textFieldRepositoryLocation.setText(Main.getSettingsManager().getGitRepoLocation());
                dispose();
            }
        });
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
        panel.setLayout(new GridLayoutManager(7, 1, new Insets(20, 20, 20, 20), -1, -1));
        textFieldRepositoryLocation = new JTextField();
        textFieldRepositoryLocation.setEditable(false);
        panel.add(textFieldRepositoryLocation,
                  new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null,
                                      new Dimension(150, -1), null, 0, false));
        buttonSetRepoLocation = new JButton();
        buttonSetRepoLocation.setText("Set Antidote Repository Location");
        panel.add(buttonSetRepoLocation,
                  new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(10);
        label1.setText("Set the Location for the Antidote Repository");
        panel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Set the Location of Configuration Files");
        panel.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel.add(spacer1,
                  new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
                                      GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel;
    }
}
