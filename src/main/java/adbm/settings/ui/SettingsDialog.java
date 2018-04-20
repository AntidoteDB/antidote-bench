package adbm.settings.ui;

import adbm.main.Main;
import adbm.main.ui.MainWindow;
import adbm.settings.ISettingsManager;
import adbm.util.AdbmConstants;
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
        setIconImage(new ImageIcon(AdbmConstants.AD_ICON_URL).getImage());
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(MainWindow.getMainWindow());
        textFieldRepositoryLocation.setText(Main.getSettingsManager().getAppSetting(ISettingsManager.GIT_REPO_PATH_SETTING));
        buttonSetRepoLocation.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                log.info("Selected file for Repository: {}", selectedFile.getAbsolutePath());
                Main.getSettingsManager().setAppSetting(ISettingsManager.GIT_REPO_PATH_SETTING, selectedFile.getAbsolutePath());
                textFieldRepositoryLocation.setText(Main.getSettingsManager().getAppSetting(ISettingsManager.GIT_REPO_PATH_SETTING));
                dispose();
            }
        });
    }

}
