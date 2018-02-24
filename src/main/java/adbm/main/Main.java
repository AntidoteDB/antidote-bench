package adbm.main;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.wrappers.AntidoteClientWrapper;
import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.main.ui.MainWindow;
import adbm.settings.MapDBManager;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static adbm.util.FormatUtil.format;

public class Main
{

    private static final Logger log = LogManager.getLogger(Main.class);

    public static final Map<String, IAntidoteClientWrapper> clientList = new HashMap<>();

    public static AntidoteClientWrapper benchmarkClient;

    public static final String appName = "Antidote Benchmark";

    public static final String ycsbWorkloadsPath = format("{}/YCSB/workloads", getResourcesPath());

    public static final String appSettingsPath = format("{}/Settings/AppSettings", getResourcesPath());

    public static final String logSettingsPath = format("{}/Settings/LogSettings/log4j2.xml", getResourcesPath());

    public static final String defaultLogPath = "Logs";

    public static final String defaultAntidotePath = "AntidoteGitRepo";

    public static final String dockerfilePath = format("{}/Dockerfile", getResourcesPath());

    public static final String imagesPath = format("{}/Images", getResourcesPath());

    public static String getResourcesPath()
    {
        return "resources";
    }

    private static boolean guiMode = false;

    public static boolean isGuiMode()
    {
        return guiMode;
    }

    public static AntidotePB.CRDT_type usedKeyType = AntidotePB.CRDT_type.COUNTER;

    public static String usedOperation;

    public static IAntidoteClientWrapper.TransactionType usedTransactionType = IAntidoteClientWrapper.TransactionType.InteractiveTransaction;

    public static final List<String> benchmarkCommits = new ArrayList<>();

    public static void closeApp()
    {
        //TODO Docker Windows general problems
        DockerManager.stopAllContainers();
    }

    public static IAntidoteClientWrapper startAntidoteClient(String name)
    {
        if (clientList.containsKey(name)) return clientList.get(name);
        if (DockerManager.runContainer(name)) {
            AntidoteClientWrapper clientWrapper = new AntidoteClientWrapper(name);
            clientList.put(name, clientWrapper);
            return clientWrapper;
        }
        return null;
    }

    public static void stopAntidoteClient(String name)
    {
        DockerManager.stopContainer(name);
    }

    public static void removeAntidoteClient(String name)
    {
        DockerManager.removeContainer(name);
        clientList.remove(name);
    }



    public static void main(String[] args)
    {
        Handler handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(handler);

        //Validation that Docker can be used!
        if (!DockerManager.startDocker(null, null)) System.exit(1);
        if (!DockerManager.runContainer("TestDockerContainer")) {
            log.error("Docker is a bad state! Please restart Docker before using this application!");
            System.exit(1);
        }
        //Client.main(new String[]{"-db","adbm.antidote.AntidoteYCSBClient", "-P", format("{}/YCSB/workloads/workloada", getResourcesPath()), "-s"});
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
                            if (GitManager.isCommitId(value)) {
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
                            //Client.main(new String[]{"-db","adbm.antidote.AntidoteYCSBClient", "-P", "workloads/workloada", "-s"});
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
        MapDBManager.startMapDB();
        if (guiMode) {
            MainWindow.showMainWindow();
            log.info("Using the Application:" +
                             "\nFirst click on Application Settings and select Folder for the Antidote Repository." +
                             "\nThe Folder must be empty or contain only an existing Antidote Repository (it must be in a clean state)." +
                             "\n\nNow start the Git Connection and then the selected directory is evaluated and if it is empty the current Antidote Repository is pulled. (about 20MB)" +
                             "\n\nOnce the Git Connection is initialized the other functionality that is enabled can be used." +
                             "\n\nThe Git Settings are there to select Commits for which an Image shall be built." +
                             "\nDockerfiles are created automatically when Images are built but a generic Dockerfile can be created with the corresponding button." +
                             "\n\nDo not use the Image building yet! It works but takes 5-10 minutes for each selected Benchmark Commit selected in the Git Settings." +
                             "\nThe Image building is an asynchronous process that will build an Image for all selected Benchmark Commits." +
                             "\n\nThe building time for an Image is several minutes and may fail completely because Docker is not in the right state (Before building Images restart Docker to be safe)." +
                             "\n");
        }
    }

    public static void runBenchmark()
    {

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
