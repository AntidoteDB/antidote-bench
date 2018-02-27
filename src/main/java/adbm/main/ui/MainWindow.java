package adbm.main.ui;

import adbm.antidote.ui.AntidoteView;
import adbm.antidote.wrappers.AntidoteClientWrapperGui;
import adbm.docker.util.DockerfileBuilder;
import adbm.git.ui.GitWindow;
import adbm.main.Main;
import adbm.settings.ui.SettingsDialog;
import adbm.util.AdbmConstants;
import adbm.util.TextPaneAppender;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static adbm.util.helpers.FormatUtil.format;

public class MainWindow extends JFrame
{

    private static final Logger log = LogManager.getLogger(MainWindow.class);

    private JTextPane textPaneConsole;
    private JPanel panel;
    private JButton buttonSettings;
    private JButton buttonStartDocker;
    private JButton buttonStartGit;
    private JButton buttonShowGitSettings;
    private JButton buttonStartAntidote;
    private JButton buttonCreateDockerfile;
    private JButton buttonBuildBenchmarkImages;
    private JButton buttonRunBenchmark;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static MainWindow mainWindow;

    private static void checkMainWindow()
    {
        if (mainWindow == null) {
            mainWindow = new MainWindow();
        }
    }

    public static MainWindow getMainWindow()
    {
        checkMainWindow();
        return mainWindow;
    }

    public static void showMainWindow()
    {
        checkMainWindow();
        mainWindow.setVisible(true);
    }

    private MainWindow()
    {
        super();
        setTitle(AdbmConstants.appName);
        setIconImage(new ImageIcon(format("{}/AntidoteIcon.PNG", AdbmConstants.imagesPath)).getImage());
        setContentPane(panel);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                int i = JOptionPane
                        .showConfirmDialog(null, format("Do you want to close the {} application?", AdbmConstants.appName),
                                           "Confirmation", JOptionPane.YES_NO_OPTION);
                if (i == JOptionPane.YES_OPTION) {
                    log.info("The application will be closed now.");
                    Main.getDockerManager().stopAllContainers();
                    System.exit(0);
                }
                else {
                    log.info("The application will not be closed because it was not confirmed.");
                }
            }
        });
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
        buttonStartDocker.addActionListener(e -> {
            if (!Main.getDockerManager().isReady())
                executorService.execute(() -> Main.getDockerManager().start());
            else log.info("Docker is already started!");
        });
        buttonShowGitSettings.addActionListener(e -> {
            if (Main.getGitManager().isReady())
                GitWindow.showGitWindow();
        });
        buttonStartAntidote.addActionListener(e -> {
            if (Main.getDockerManager().isReady())
                new AntidoteView(new AntidoteClientWrapperGui("AntidoteGuiClient"));
            //TODO
        });
        buttonCreateDockerfile.addActionListener(e -> {
            if (Main.getGitManager().isReady())
                DockerfileBuilder.createDockerfile(false);
        });
        buttonBuildBenchmarkImages.addActionListener(e -> {
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
        });
        buttonRunBenchmark.addActionListener(e -> {
            Main.benchmarkTest();
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
        panel.setLayout(new GridLayoutManager(6, 2, new Insets(20, 20, 20, 20), -1, -1));
        panel.setAutoscrolls(true);
        buttonSettings = new JButton();
        buttonSettings.setText("Open Application Settings");
        panel.add(buttonSettings,
                  new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStartDocker = new JButton();
        buttonStartDocker.setText("Start Docker Connection");
        panel.add(buttonStartDocker,
                  new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStartGit = new JButton();
        buttonStartGit.setEnabled(true);
        buttonStartGit.setText("Start Git Connection");
        panel.add(buttonStartGit,
                  new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                                                   null, new Dimension(500, 500), null, 0, false));
        textPaneConsole = new JTextPane();
        textPaneConsole.setEditable(false);
        scrollPane1.setViewportView(textPaneConsole);
        final JLabel label1 = new JLabel();
        label1.setText("Console Log");
        panel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                                              GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
                                              null, null, 0, false));
        buttonShowGitSettings = new JButton();
        buttonShowGitSettings.setText("Open Git Settings");
        panel.add(buttonShowGitSettings,
                  new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonStartAntidote = new JButton();
        buttonStartAntidote.setEnabled(true);
        buttonStartAntidote.setText("Start Antidote");
        panel.add(buttonStartAntidote,
                  new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCreateDockerfile = new JButton();
        buttonCreateDockerfile.setText("Create Dockerfile");
        panel.add(buttonCreateDockerfile,
                  new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonBuildBenchmarkImages = new JButton();
        buttonBuildBenchmarkImages.setText("Build Benchmark Images");
        panel.add(buttonBuildBenchmarkImages,
                  new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return panel;
    }
}
