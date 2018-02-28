package adbm.main;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.wrappers.AntidoteClientWrapper;
import adbm.antidote.wrappers.AntidoteClientWrapperGui;
import adbm.docker.IDockerManager;
import adbm.docker.managers.DockerManagerSpotify;
import adbm.git.IGitManager;
import adbm.git.managers.GitManager;
import adbm.main.ui.MainWindow;
import adbm.resultsVisualization.VisualizationMain;
import adbm.settings.IAntidoteKeyStoreManager;
import adbm.settings.ISettingsManager;
import adbm.settings.managers.MapDBManager;
import adbm.util.AdbmConstants;
import com.yahoo.ycsb.Client;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private static IDockerManager dockerManager = DockerManagerSpotify.getInstance();

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

    public static boolean stopContainers = false;

    public static void closeApp()
    {
        //TODO Docker Windows general problems
        if (stopContainers) dockerManager.stopAllContainers();
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


    private static boolean useTransactions = true;

    public static void setUseTransactions(boolean bool)
    {
        useTransactions = bool;
    }

    public static boolean getUseTransactions()
    {
        return useTransactions;
    }

    private static String usedWorkload = "workloada";

    public static String getUsedWorkLoad()
    {
        return usedWorkload;
    }

    public static void setUsedWorkload(String workload)
    {
        usedWorkload = workload;
    }

    public static void resultsTest() {
        VisualizationMain test = new VisualizationMain();
    }

    private static int numberOfThreads = 1;

    public static int getNumberOfThreads()
    {
        return numberOfThreads;
    }

    public static void setNumberOfThreads(int number)
    {
        if (number > 0)
            numberOfThreads = number;
    }

    private static int targetNumber = 0;

    public static int getTargetNumber()
    {
        return targetNumber;
    }

    public static void setTargetNumber(int number)
    {
        if (number >= 0)
            targetNumber = number;
    }

    public static void addIfNotEmpty(List<String> list, String[]... elements) {
        for (String[] element : elements)
        {
            for (String e : element)
            if (!e.isEmpty()) {
                list.add(e);
            }
        }
    }



    public static void benchmarkTest()
    {
        //initializeBenchmarkClient();
        String usedDB = "adbm.ycsb.AntidoteYCSBClient";
        boolean showStatus = true;
        String[] threadsArg = numberOfThreads <= 1 ? new String[0] : new String[]{"-threads", format("{}", numberOfThreads)};
        String[] targetArg = targetNumber <= 0 ? new String[0] : new String[]{"-target", format("{}", targetNumber)};
        String[] transactionArg = useTransactions ? new String[]{"-t"} : new String[0];
        String[] dbArg = {"-db" ,format("{}", usedDB)};
        String[] workloadArg = {"-P", format("{}/{}", AdbmConstants.ycsbWorkloadsPath, usedWorkload)};
        String[] statusArg = showStatus ? new String[]{"-s"} : new String[0];

        List<String> argList = new ArrayList<>();
        addIfNotEmpty(argList, threadsArg, targetArg, transactionArg, dbArg, workloadArg, statusArg);




        String[] ycsbArgs = argList.toArray(new String[0]);
        log.info("YCSB Args:");
        for(String arg : ycsbArgs) {
            log.info(arg);
        }
        Client.main(ycsbArgs);
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

    public static void main(String[] args)
    {
        Handler handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        if (!startBenchmarkContainer()) {
            System.exit(1);
        }
        //benchmarkTest();
        resultsTest();
        //System.exit(0);
        //DockerManagerJava test = new DockerManagerJava();
        //test.start();
        //System.exit(0);//TODO!

        //Validation that Docker can be used!
        /*
        if (!dockerManager.start()) System.exit(1);
        if (!dockerManager.runContainer("AntidoteBenchmarkClient")) {
            log.error("Docker is a bad state! Please restart Docker before using this application!");
            System.exit(1);
        }
        String firstCommit = "d087ea62cb694dcc10bb09791a61adc819892fff";
        String secondCommit = "2e539e227ee7edd7058cb32fc966006ca6c75caf";
        initializeBenchmarkClient();
        String[] ycsbArgs = new String[]{"-db", "adbm.ycsb.AntidoteYCSBClient", "-P", format(
                "{}/YCSB/Workloads/workloada", AdbmConstants.resourcesPath), "-s"};
        Client.main(ycsbArgs);
        boolean rebuildSuccess = dockerManager.rebuildAntidoteInContainer("AntidoteBenchmarkClient", firstCommit);
        if (!rebuildSuccess) {
            System.exit(1);
        }
        Client.main(ycsbArgs);
        rebuildSuccess = dockerManager.rebuildAntidoteInContainer("AntidoteBenchmarkClient", secondCommit);
        if (!rebuildSuccess) {
            System.exit(1);
        }
        Client.main(ycsbArgs);*/

        if (args != null && args.length > 0) {
            Option gui = new Option("gui", "activate gui mode");
            Option debug = new Option("debug", "print debugging information");
            Option config = Option.builder().argName("config")
                                  .hasArg()
                                  .desc("set the used configuration file")
                                  .longOpt("config")
                                  .build();
            Option commits = Option.builder().argName("commits")
                                   .hasArgs()
                                   .desc("set the commits you want to benchmark and compare")
                                   .longOpt("commits")
                                   .build();
            Options options = new Options();
            options.addOption(gui);
            options.addOption(debug);
            options.addOption(config);
            options.addOption(commits);

            CommandLineParser parser = new DefaultParser();
            try {
                // parse the command line arguments
                CommandLine line = parser.parse(options, args);
                if (line.hasOption("debug")) {

                    //TODO print debug information
                }
                if (line.hasOption("gui")) {
                    guiMode = true;
                    //TODO open GUI and ignore other commands
                }
                else {
                    guiMode = false;
                    if (!line.hasOption("config")) {

                        //TODO return error
                    }
                    else {

                    }
                    if (line.hasOption("commits")) {

                        //TODO parsing
                        for (String value : line.getOptionValues("commits")) {
                            if (gitManager.isCommitId(value)) {
                                benchmarkCommits.add(value);
                            }
                            else {
                                log.warn(
                                        "The commit id {} was not found in the repository and cannot be added benchmark!",
                                        value);
                            }
                        }

                        //TODO return error
                    }
                    if (!benchmarkCommits.isEmpty()) {
                        for (String commit : benchmarkCommits) {
                            //Client.main(new String[]{"-db","adbm.antidote.AntidoteYCSBClient", "-P", "Workloads/workloada", "-s"});
                            //TODO start a client
                            //Client.main(new String[0]);
                        }
                        //Client.main(new String[0]);
                    }
                }
                //TODO config
            } catch (ParseException exp) {
                // oops, something went wrong
                log.error("Parsing failed.  Reason: " + exp.getMessage());
            }
        }
        //MapDBManager.startMapDB();
        //GitManager.startGit();
        //DockerManager.startDocker();
        guiMode = true; //TODO
        settingsManager.start();
        //keyManager.start();//TODO check
        if (guiMode) {
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

    public static void initializeBenchmarkClient()
    {
        if (benchmarkClient == null)
            benchmarkClient = new AntidoteClientWrapper("Test", AdbmConstants.benchmarkContainerName);
        if (!benchmarkClient.isReady())
            benchmarkClient.start();
        //TODO
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
