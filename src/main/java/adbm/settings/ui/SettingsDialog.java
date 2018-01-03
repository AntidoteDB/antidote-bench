package adbm.settings.ui;

import adbm.settings.MapDBManager;

import javax.swing.*;
import java.io.File;

public class SettingsDialog
{
    private JTextField textFieldRepositoryLocation;
    private JButton buttonSetRepoLocation;
    private JPanel panel;
    private JTextField textFieldConfigLocation;
    private JButton buttonSetConfigLocation;

    public SettingsDialog() {
        JFrame frame = new JFrame("ConsoleLog");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        textFieldRepositoryLocation.setText(MapDBManager.getAppSetting(MapDBManager.GitRepoLocationSetting));
        buttonSetRepoLocation.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("Selected file for Repository: " + selectedFile.getAbsolutePath());
                MapDBManager.setAppSetting(MapDBManager.GitRepoLocationSetting, selectedFile.getAbsolutePath());
                textFieldRepositoryLocation.setText(MapDBManager.getAppSetting(MapDBManager.GitRepoLocationSetting));
            }
        });
        textFieldConfigLocation.setText(MapDBManager.getAppSetting(MapDBManager.ConfigLocSetting));
        buttonSetConfigLocation.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(panel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                MapDBManager.setAppSetting(MapDBManager.ConfigLocSetting, selectedFile.getAbsolutePath());
                textFieldRepositoryLocation.setText(MapDBManager.getAppSetting(MapDBManager.ConfigLocSetting));
            }
        });
    }
}
