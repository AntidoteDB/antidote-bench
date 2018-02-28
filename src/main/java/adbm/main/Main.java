package adbm.main;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.wrappers.AntidoteClientWrapper;
import adbm.antidote.wrappers.AntidoteClientWrapperGui;
import adbm.docker.IDockerManager;
import adbm.docker.managers.DockerManager;
import adbm.git.IGitManager;
import adbm.git.managers.GitManager;
import adbm.main.ui.MainWindow;
import adbm.resultsVisualization.VisualizationMain;
import adbm.settings.IAntidoteKeyStoreManager;
import adbm.settings.ISettingsManager;
import adbm.settings.managers.MapDBManager;
import adbm.util.AdbmConstants;
import adbm.util.helpers.GeneralUtil;
import com.yahoo.ycsb.Client;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.*;

import static adbm.util.helpers.FormatUtil.format;

public class Main
{

    private static final Logger log = LogManager.getLogger(Main.class);

    private static final Map<String, AntidoteClientWrapperGui> clientList = new HashMap<>();

    public static Map<String, AntidoteClientWrapperGui> getClientList()
    {
        return clientList;
    }

    private static IAntidoteClientWrapper benchmarkClient;

    public static IAntidoteClientWrapper getBenchmarkClient()
    {
        return benchmarkClient;
    }

    private static boolean guiMode = false;

    public static boolean isGuiMode()
    {
        return guiMode;
    }

    private static AntidotePB.CRDT_type usedKeyType = AntidotePB.CRDT_type.COUNTER;

    public static AntidotePB.CRDT_type getUsedKeyType()
    {
        return usedKeyType;
    }

    private static String usedOperation;

    public static String getUsedOperation()
    {
        return usedOperation;
    }

    private static IAntidoteClientWrapper.TransactionType usedTransactionType = IAntidoteClientWrapper.TransactionType.InteractiveTransaction;

    public static IAntidoteClientWrapper.TransactionType getUsedTransactionType()
    {
        return usedTransactionType;
    }

    //TODO think about this
    private static final List<String> benchmarkCommits = new ArrayList<>();

    public static List<String> getBenchmarkCommits()
    {
        return benchmarkCommits;
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

    private static BenchmarkConfig benchmarkConfig = new BenchmarkConfig();

    public static BenchmarkConfig getBenchmarkConfig() {
        return benchmarkConfig;
    }

    public static boolean stopContainers = false;

    public static void closeApp()
    {
        if (stopContainers) dockerManager.stopAllContainers();
        dockerManager.stop();
        gitManager.stop();
        settingsManager.stop();
        keyManager.stop();
    }

    public static IAntidoteClientWrapper startAntidoteClient(String name, String containerName)
    {
        if (clientList.containsKey(name)) {
            IAntidoteClientWrapper wrapper = clientList.get(name);
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

    public static void resultsTest()
    {
        VisualizationMain test = new VisualizationMain();
    }

    private static boolean startBenchmarkContainer()
    {
        if (!dockerManager.isReady()) {
            if (!dockerManager.start()) {
                log.error("Docker could not be started!");
                return false;
            }
            if (!dockerManager.runContainer(AdbmConstants.benchmarkContainerName)) {
                log.error("Docker is a bad state! Please restart Docker before using this application!");
                return false;
            }
        }
        return true;
    }

    public static boolean isDockerRunning;

    public static void main(String[] args) {
        Handler handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        isDockerRunning = startBenchmarkContainer();
        //resultsTest();
        /*boolean rebuildSuccess = dockerManager.rebuildAntidoteInContainer("AntidoteBenchmarkClient", secondCommit);
        if (!rebuildSuccess) {
            System.exit(1);
        }
        Client.main(ycsbArgs);*/

        //For Command Line
        if (args != null && args.length > 0) {
            //Declaring the options
            Option gui = new Option("gui", "activate gui mode");
            Option commits = Option.builder().argName("commits")
                    .hasArgs()
                    .desc("set the commits you want to benchmark and compare")
                    .longOpt("commits")
                    .build();

            Options options = new Options();

            //Adding the options
            options.addOption(gui);
            options.addOption(commits);

            //Parsing through the command line arguments
            CommandLineParser parser = new DefaultParser();

            try {
                CommandLine line = parser.parse(options, args);

                //Option to activate gui
                if (line.hasOption("gui")) {
                    guiMode = true;
                    settingsManager.start();
                    MainWindow.showMainWindow();
                } else if (line.hasOption("commits")) {
                    guiMode = false;
                    for (String value : line.getOptionValues("commits")) {
                        if (value.equals(null)) {
                            log.warn(
                                    "The commit id {} was not found in the repository and cannot be added benchmark!",
                                    value);
                        } else {
                            benchmarkCommits.add(value);
                        }
                    }
                    if (!benchmarkCommits.isEmpty()) {
                        startBenchmarkContainer();
                        for (String commit : benchmarkCommits) {
                            boolean rebuildSuccess = dockerManager.rebuildAntidoteInContainer(AdbmConstants.benchmarkContainerName, commit);
                            if (!rebuildSuccess) {
                                System.exit(1);
                            } else {
                                //Calling the benchmark
                                benchmarkConfig.runBenchmark();
                            }
                        }
                    } else {
                        log.warn("No commit id () was found !");
                    }
                }
            } catch (ParseException exp) {
                // oops, something went wrong
                log.error("Parsing failed.  Reason: " + exp.getMessage());
            }
        } else {
            //MapDBManager.startMapDB();
            //GitManager.startGit();
            //DockerManager.startDocker();
            guiMode = true;
            settingsManager.start();
            //keyManager.start();//TODO check
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
    }

    //TODO test
    private static class Handler implements Thread.UncaughtExceptionHandler
    {
        public void uncaughtException(Thread t, Throwable e)
        {
            log.error("An uncaught exception has occurred in the Thread: " + t, e);
            log.warn("Such exceptions are usually caused by coding mistakes!");
        }
    }
}
