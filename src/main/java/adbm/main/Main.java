package adbm.main;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.wrappers.AntidoteClientWrapperGui;
import adbm.docker.IDockerManager;
import adbm.docker.managers.DockerManager;
import adbm.git.IGitManager;
import adbm.git.managers.GitManager;
import adbm.main.ui.MainWindow;
import adbm.settings.IAntidoteKeyStoreManager;
import adbm.settings.ISettingsManager;
import adbm.settings.managers.MapDBManager;
import adbm.util.AdbmConstants;
import adbm.ycsb.AntidoteYCSBConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static adbm.util.helpers.FormatUtil.format;

public class Main
{

    private static final Logger log = LogManager.getLogger(Main.class);

    private static final Map<String, AntidoteClientWrapperGui> clientList = new HashMap<>();

    public static Map<String, AntidoteClientWrapperGui> getClientList()
    {
        return clientList;
    }

    private static boolean guiMode = false;

    public static boolean isGuiMode()
    {
        return guiMode;
    }

    public static void setGuiMode(boolean setGuiMode)
    {
        guiMode = setGuiMode;
    }

    private static IDockerManager dockerManager = DockerManager.getInstance();

    public static IDockerManager getDockerManager()
    {
        return dockerManager;
    }

    private static ISettingsManager settingsManager = MapDBManager.getInstance();

    public static ISettingsManager getSettingsManager()
    {
        return settingsManager;
    }

    private static IAntidoteKeyStoreManager keyManager = MapDBManager.getInstance();

    public static IAntidoteKeyStoreManager getKeyManager()
    {
        return keyManager;
    }

    private static IGitManager gitManager = GitManager.getInstance();

    public static IGitManager getGitManager()
    {
        return gitManager;
    }

    private static AntidoteYCSBConfiguration antidoteYCSBConfiguration = new AntidoteYCSBConfiguration();

    public static AntidoteYCSBConfiguration getAntidoteYCSBConfiguration()
    {
        return antidoteYCSBConfiguration;
    }

    public static boolean stopContainers = false;

    public static void closeApp()
    {
        if (stopContainers) dockerManager.stopAllContainers();
        dockerManager.stop();
        gitManager.stop();
        settingsManager.stop();
        keyManager.stop();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        System.exit(0);
    }

    public static AntidoteClientWrapperGui startAntidoteClient(String name, String containerName)
    {
        if (clientList.containsKey(name)) {
            AntidoteClientWrapperGui wrapper = clientList.get(name);
            wrapper.start();
            return wrapper;
        }
        if (dockerManager.runContainer(containerName)) {
            AntidoteClientWrapperGui clientWrapper = new AntidoteClientWrapperGui(name, containerName);
            clientList.put(name, clientWrapper);
            return clientWrapper;
        }
        return null;
    }

    public static void stopAntidoteClient(String name)
    {
        if (clientList.containsKey(name)) {
            IAntidoteClientWrapper wrapper = clientList.get(name);
            wrapper.stop();
        }

    }

    public static void removeAntidoteClient(String name)
    {
        dockerManager.removeContainer(name);
        clientList.remove(name);
    }

    public static boolean startBenchmarkContainer()
    {
        if (!dockerManager.isReady()) {
            if (!dockerManager.start()) {
                log.error("Docker could not be started!");
                return false;
            }
            if (!dockerManager.runContainer(AdbmConstants.ADBM_CONTAINER_NAME)) {
                log.error("Docker is a bad state! Please restart Docker before using this application!");
                return false;
            }
        }
        return true;
    }

    public static boolean isDockerRunning;

    private static Thread shutdownHook = new Thread(Main::closeApp);

    public static void main(String[] args)
    {
        Handler handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("An error occurred while setting the LookAndFeel!" , e);
        }

        isDockerRunning = startBenchmarkContainer();
        if (!isDockerRunning) closeApp();

        guiMode = true;
        settingsManager.start();
        MainWindow.showMainWindow();
            /*log.info("Using the Application:" +
                             "\nFirst click on Application Settings and select Folder for the Antidote Repository." +
                             "\nThe Folder must be empty or contain only an existing Antidote Repository (it must be in a clean state)." +
                             "\n\nNow start the Git Connection and then the selected directory is evaluated and if it is empty the current Antidote Repository is pulled. (about 20MB)" +
                             "\n\nOnce the Git Connection is initialized the other functionality that is enabled can be used." +
                             "\n\nThe Git Settings are there to select Commits for which an Image shall be built." +
                             "\nDockerfiles are created automatically when Images are built but a generic Dockerfile can be created with the corresponding button." +
                             "\n\nDo not use the Image building yet! It works but takes 5-10 minutes for each selected Benchmark Commit selected in the Git Settings." +
                             "\nThe Image building is an asynchronous process that will build an Image for all selected Benchmark Commits." +
                             "\n\nThe building time for an Image is several minutes and may fail completely because Docker is not in the right state (Before building Images restart Docker to be safe)." +
                             "\n");*/
    }

    private static class Handler implements Thread.UncaughtExceptionHandler
    {
        public void uncaughtException(Thread t, Throwable e)
        {
            log.error("An uncaught exception has occurred in the Thread: " + t, e);
            log.warn("Such exceptions are usually caused by coding mistakes!");
        }
    }
}
