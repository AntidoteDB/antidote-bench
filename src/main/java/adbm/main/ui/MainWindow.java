package adbm.main.ui;

import adbm.antidote.ui.AntidoteView;
import adbm.antidote.wrappers.AntidoteClientWrapperGui;
import adbm.git.ui.GitDialog;
import adbm.main.Main;
import adbm.settings.ui.SettingsDialog;
import adbm.util.AdbmConstants;
import adbm.util.TextPaneAppender;
import adbm.util.helpers.FileUtil;
import adbm.ycsb.ui.AntidoteYCSBConfigurationDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
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
    //private JButton buttonStartDocker;
    private JButton buttonStartGit;
    private JButton buttonShowGitSettings;
    //private JButton buttonStartAntidote;
    private JButton buttonOpenBenchmarkDialog;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private DefaultComboBoxModel<String> comboBoxWorkloadModel = new DefaultComboBoxModel<>();
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
        setTitle(AdbmConstants.APP_NAME);
        setIconImage(new ImageIcon(format("{}/AntidoteIcon.PNG", AdbmConstants.IMAGES_PATH)).getImage());
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
        for (String fileName : FileUtil.getAllFileNamesInFolder(AdbmConstants.YCSB_WORKLOADS_PATH))
            comboBoxWorkloadModel.addElement(fileName);
        comboBoxWorkloadModel.setSelectedItem(Main.getAntidoteYCSBConfiguration().getUsedWorkLoad());
    }

}
