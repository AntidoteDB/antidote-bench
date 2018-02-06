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
import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
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
                if (DockerManager.imageExists()) {
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
                    DockerManager.buildBenchmarkImage(false);
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

}
