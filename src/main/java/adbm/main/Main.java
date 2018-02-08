package adbm.main;

import adbm.antidote.AntidoteClientWrapper;
import adbm.antidote.Operation;
import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.main.ui.MainWindow;
import adbm.settings.MapDBManager;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class Main
{

    private static final Logger log = LogManager.getLogger(Main.class);

    public static Map<String, AntidoteClientWrapper> clientList = new HashMap<>();

    public static AntidoteClientWrapper client;

    public static final String appName = "Antidote Benchmark";

    public static boolean guiMode = true;

    public static boolean useSettings = true;

    public static void closeApp()
    {
        //TODO Docker Windows general problems
        DockerManager.stopAllContainers();
    }

    /**
     * @param name
     * @return
     */
    public static AntidoteClientWrapper startAntidoteClient(String name)
    {
        if (clientList.containsKey(name)) return clientList.get(name);
        if (DockerManager.runContainer(name)) {
            AntidoteClientWrapper clientWrapper = new AntidoteClientWrapper(name);
            clientList.put(name, clientWrapper);
            return clientWrapper;
        }
        return null;
    }

    /**
     * @param name
     */
    public static void stopAntidoteClient(String name)
    {
        DockerManager.stopContainer(name);
    }

    /**
     * @param name
     */
    public static void removeAntidoteClient(String name)
    {
        DockerManager.removeContainer(name);
        clientList.remove(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Handler handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Option help = new Option( "help", "show help" );
        Option gui = new Option( "gui", "activate gui mode" );
        Option version = new Option( "version", "print the version information" );
        Option debug = new Option( "debug", "print debugging information" );
        Option emacs = new Option( "emacs",
                                   "produce logging information without adornments" );
        Option logfile   = Option.builder().argName( "logfile" )
                                        .hasArg()
                                        .desc(  "use given file for log" )
                                        .longOpt("logfile")
                                        .build();

        Option antidoterepofolder  = Option.builder().argName( "adrepo" )
                                        .hasArg()
                                        .desc( "add an instance of class as ")
                                           .longOpt("antidoterepofolder")
                                        .build();
        //MapDBManager.startMapDB();
        //GitManager.startGit();
        //DockerManager.startDocker();
        log.warn("Ready!");
        //AntidoteClientWrapper f1 = startAntidoteClient("antidote1");
        //AntidoteClientWrapper f2 = startAntidoteClient("antidote2");
        //log.warn("Rebuilding!");
        //f1.AddKey("Test", AntidotePB.CRDT_type.INTEGER);
        //log.info(f1.getKeyValueNoTransaction("Test"));
        //f1.getKeyUpdate(new Operation("Test", "increment", 3));
        //log.info(f1.getKeyValueNoTransaction("Test"));
        //f1.AddKey("Test1", AntidotePB.CRDT_type.INTEGER);
        //log.info(f.getKeyValue("Test"));
        //DockerManager.rebuildAntidoteInContainer("TestAntidote", "2742f58a2e28d64dbfcf226b1a7b4d53303cd6b6");
        //TODO set up
        //client = new AntidoteClientWrapper("TestAntidote");
        //log.info("Ok!");
        //client.AddKey("Test", AntidotePB.CRDT_type.COUNTER);
        //log.info(client.getKeyValue("Test"));
        /*List<String> runningContainers = DockerManager.getNamesOfRunningContainers();
        List<String> notRunningContainers = DockerManager.getNamesOfNotRunningContainers();
        for (String container : runningContainers) {
            clientList.add(new AntidoteClientWrapper(container));
        }
        if (clientList.isEmpty()) {
            if (!notRunningContainers.isEmpty()) {
                DockerManager.startContainer(notRunningContainers.get(0));
            }
        }
        if (clientList.isEmpty()) {
            startAntidoteClient("defaultAntidoteDC");
        }
        log.info("Done!");
        AntidoteView gui = new AntidoteView(clientList.get(0));*/
        new MainWindow();
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
        log.debug("Testing Debug!");
        log.trace("Testing Trace!");
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
