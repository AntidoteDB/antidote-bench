package adbm.main.ui;

import adbm.antidote.AntidoteClientWrapper;
import adbm.antidote.ui.AntidoteView;
import adbm.docker.DockerfileBuilder;
import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.git.ui.SelectBranchDialog;
import adbm.settings.MapDBManager;
import adbm.settings.ui.SettingsDialog;
import adbm.util.TextPaneAppender;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.text.*;

public class MainWindow
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
    private Document document;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public MainWindow()
    {
        JFrame frame = new JFrame("ConsoleLog");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                int i = JOptionPane.showConfirmDialog(null, "Do you want to close the application?");
                if (i == 0) {
                    DockerManager.stopAllContainers();
                    System.exit(0);
                }

            }
        });
        TextPaneAppender.addTextPane(textPaneConsole);
        buttonSettings.addActionListener(e -> {
            if (MapDBManager.isReady())
                new SettingsDialog();
        });
        buttonStartGit.addActionListener(e -> {
            if (!GitManager.isReadyNoText())
                executorService.execute(GitManager::startGit);
            else log.info("Git is already started!");
        });
        buttonStartDocker.addActionListener(e -> {
            if (!DockerManager.isReadyNoText())
                executorService.execute(DockerManager::startDocker);
            else log.info("Docker is already started!");
        });
        buttonShowGitSettings.addActionListener(e -> {
            if (GitManager.isReady())
                new SelectBranchDialog();
        });
        buttonStartAntidote.addActionListener(e -> {
            if (DockerManager.isReady())
                new AntidoteView(new AntidoteClientWrapper("TestAntidote"));
            //TODO
        });
        buttonCreateDockerfile.addActionListener(e -> {
            if (GitManager.isReady())
                DockerfileBuilder.createDockerfile(false);
        });
        buttonBuildBenchmarkImages.addActionListener(e -> {
            if (DockerManager.isReady()) {
                int confirm = JOptionPane.showConfirmDialog(
                        frame,
                        "Are you sure you want to build the Image of the Antidote Benchmark?\n" +
                                "This will take about 5 minutes and shows progress every 10 seconds.",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                if (DockerManager.antidoteBenchmarkImageExists()) {
                    confirm = JOptionPane.showConfirmDialog(
                            frame,
                            "A previously built Image of the Antidote Benchmark exists.\n" +
                                    "Do you want to rebuilt the image and remove the existing image?\n" +
                                    "If you choose \"Yes\" then all containers that were created from the existing image will be stopped and removed.",
                            "Confirmation",
                            JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;
                }
                executorService.execute(() -> {
                    DockerManager.buildAntidoteBenchmarkImage(false);
                });
                buttonBuildBenchmarkImages.setEnabled(false);
                Executors.newSingleThreadExecutor().execute(() -> {
                    StopWatch watch = new StopWatch();
                    watch.start();
                    //TODO race condition
                    while (DockerManager.isBuildingImage()) {
                        log.info("Image is building... Elapsed time: {}s", watch.getTime() / 1000);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
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
