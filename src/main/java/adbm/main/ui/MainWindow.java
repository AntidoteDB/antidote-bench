package adbm.main.ui;

import adbm.git.ui.GitDialog;
import adbm.main.Main;
import adbm.settings.ui.SettingsDialog;
import adbm.util.AdbmConstants;
import adbm.util.EverythingIsNonnullByDefault;
import adbm.util.TextPaneAppender;
import adbm.util.helpers.FileUtil;
import adbm.ycsb.ui.AntidoteYCSBConfigurationDialog;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static adbm.util.helpers.FormatUtil.format;

@EverythingIsNonnullByDefault
public class MainWindow extends JFrame
{

    private static final Logger log = LogManager.getLogger(MainWindow.class);

    private JTextPane textPaneConsole;
    private JPanel panel;
    private JButton buttonSettings;
    //private JButton buttonStartDocker;
    private JButton buttonStartGit;
    private JButton buttonShowGitSettings;
    //private JButton buttonStartAntidote;
    private JButton buttonOpenBenchmarkDialog;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private DefaultComboBoxModel<String> comboBoxWorkloadModel = new DefaultComboBoxModel<>();

    @Nullable
    private static MainWindow mainWindow;

    public static MainWindow getMainWindow()
    {
        if (mainWindow == null) {
            mainWindow = new MainWindow();
        }
        return mainWindow;
    }

    public static void showMainWindow()
    {
        if (mainWindow == null) {
            mainWindow = new MainWindow();
        }
        mainWindow.setVisible(true);
    }

    private MainWindow()
    {
        super();
        setTitle(AdbmConstants.APP_NAME);
        //setIconImage(new ImageIcon(AdbmConstants.AD_ICON_URL).getImage()); //TODO
        setContentPane(panel);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                int i = JOptionPane
                        .showConfirmDialog(null,
                                           format("Do you want to close the {} application?", AdbmConstants.APP_NAME),
                                           "Confirmation", JOptionPane.YES_NO_OPTION);
                if (i == JOptionPane.YES_OPTION) {
                    log.info("The application will be closed now.");
                    Main.closeApp();
                    System.exit(0);
                }
                else {
                    log.info("The application will not be closed because it was not confirmed.");
                }
            }
        });
        updateWorkloads();
        TextPaneAppender.addTextPane(textPaneConsole);
        buttonSettings.addActionListener(e -> {
            if (Main.getSettingsManager().isReady())
                SettingsDialog.showSettingsDialog();
        });
        buttonStartGit.addActionListener(e -> {
            if (!Main.getGitManager().isReady())
                executorService.execute(Main.getGitManager()::start);
            else log.info("Git is already started!");
        });
        /*buttonStartDocker.addActionListener(e -> {
            if (!Main.getDockerManager().isReady())
                executorService.execute(() -> Main.getDockerManager().start());
            else log.info("Docker is already started!");
        });*/
        buttonShowGitSettings.addActionListener(e -> {
            if (Main.getGitManager().isReady())
                GitDialog.showGitDialog();
        });
        /*buttonStartAntidote.addActionListener(e -> {
            if (Main.getDockerManager().isReady())
                new AntidoteView(new AntidoteClientWrapperGui("AntidoteGuiClient",
                                                              AdbmConstants.ADBM_CONTAINER_NAME)); //TODO change this!
            //TODO
        });*/
        /*buttonBuildBenchmarkImages.addActionListener(e -> {
            if (Main.getDockerManager().isReady()) {
                int confirm = JOptionPane.showConfirmDialog(
                        mainWindow,
                        "Are you sure you want to build the Image of the Antidote Benchmark?\n" +
                                "This will take about 5 minutes and shows progress every 10 seconds.",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                if (Main.getDockerManager().antidoteBenchmarkImageExists()) {
                    confirm = JOptionPane.showConfirmDialog(
                            mainWindow,
                            "A previously built Image of the Antidote Benchmark exists.\n" +
                                    "Do you want to rebuilt the image and remove the existing image?\n" +
                                    "If you choose \"Yes\" then all containers that were created from the existing image will be stopped and removed.",
                            "Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;
                }
                executorService.execute(() -> {
                    Main.getDockerManager().buildAntidoteBenchmarkImage(false);
                });
                buttonBuildBenchmarkImages.setEnabled(false);
                Executors.newSingleThreadExecutor().execute(() -> {
                    StopWatch watch = new StopWatch();
                    watch.start();
                    //TODO race condition
                    while (Main.getDockerManager().isBuildingImage()) {
                        log.info("Image is building... Elapsed time: {}s", watch.getTime() / 1000);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ex) {
                            log.error("An error occurred while waiting on image build process!", ex);
                        }
                    }
                });
            }
        });*/
        buttonOpenBenchmarkDialog.addActionListener(e -> {
            AntidoteYCSBConfigurationDialog.showBenchmarkDialog();
        });
    }


    private void updateWorkloads()
    {
        comboBoxWorkloadModel.removeAllElements();
        for (String fileName : FileUtil.getAllFileNamesInFolder(AdbmConstants.YCSB_WORKLOADS_FOLDER_PATH))
            comboBoxWorkloadModel.addElement(fileName);
        comboBoxWorkloadModel.setSelectedItem(Main.getAntidoteYCSBConfiguration().getUsedWorkLoad());
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
        panel.setLayout(new GridLayoutManager(6, 1, new Insets(20, 20, 20, 20), -1, -1));
        panel.setAutoscrolls(true);
        buttonSettings = new JButton();
        buttonSettings.setEnabled(true);
        buttonSettings.setText("Open Application Settings");
        panel.add(buttonSettings,
                  new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStartGit = new JButton();
        buttonStartGit.setEnabled(true);
        buttonStartGit.setText("Start Git Management");
        panel.add(buttonStartGit,
                  new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                   null, new Dimension(500, 500), null, 0, false));
        textPaneConsole = new JTextPane();
        textPaneConsole.setEditable(false);
        scrollPane1.setViewportView(textPaneConsole);
        buttonOpenBenchmarkDialog = new JButton();
        buttonOpenBenchmarkDialog.setText("Open YCSB Benchmark Dialog");
        panel.add(buttonOpenBenchmarkDialog,
                  new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonShowGitSettings = new JButton();
        buttonShowGitSettings.setEnabled(true);
        buttonShowGitSettings.setText("Open Git Management");
        panel.add(buttonShowGitSettings,
                  new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Console Output");
        panel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel;
    }
}
