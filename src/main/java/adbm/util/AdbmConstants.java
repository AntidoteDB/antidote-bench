package adbm.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static adbm.util.helpers.FormatUtil.format;

/**
 * Holds all constants that are used by the application.
 * They are mostly file paths and names.
 * Some of the constants may change in time.
 */
public class AdbmConstants
{

    /**
     * The name of the application. Used for logging and gui.
     */
    public static final String appName = "Antidote Benchmark";

    public static final String benchmarkContainerName = "AntidoteBenchmarkContainer";

    /**
     * This is the path to the resources folder where all resource except some properties are stored.
     */
    public static final String resourcesPath = "resources";

    /**
     * This is the path to the workloads that can be used for benchmarking with YCSB.
     */
    public static final String ycsbWorkloadsPath = format("{}/YCSB/Workloads", resourcesPath);

    /**
     * This is the path to the application settings.
     */
    public static final String appSettingsPath = format("{}/Settings/AppSettings", resourcesPath);

    /**
     * This is the path to the log4j2.xml configuration file.
     */
    public static final String logSettingsPath = format("{}/log4j2.xml", resourcesPath);

    /**
     * This is the path to the Dockerfile that is used to build the benchmark image.
     */
    public static final String dockerfilePath = format("{}/Dockerfile", resourcesPath);

    /**
     * This is the path to the images that are used in the gui.
     */
    public static final String imagesPath = format("{}/Images", resourcesPath);

    /**
     * This is the default path to the folder of the application logs.
     */
    public static final String defaultLogPath = "Logs";

    /**
     * This is the default path to the folder of the Antidote git repository.
     */
    public static final String defaultAntidotePath = "AntidoteGitRepo";

    /**
     * This is the git url to the Antidote git repository.
     */
    public static final String gitUrl = "https://github.com/SyncFree/antidote.git";

    /**
     * This image is required to build the Antidote benchmark image.
     */
    public static final String requiredImage = "erlang:19";

    /**
     * This is the network that is used for the antidote containers.
     */
    public static final String antidoteDockerNetworkName = "antidote_ntwk";

    /**
     * This is the name of the antidote benchmark image.
     * It is similar to the regular Antidote image.
     * However it keeps the Antidote git repository intact so that other commits can be checked out.
     */
    public static final String antidoteDockerImageName = "antidotedb/benchmark";

    /**
     * The standart client port used by Antidote containers.
     */
    public static final int standardClientPort = 8087;

    /**
     * The time to wait before killing a container that is being stopped.
     */
    public static final int secondsToWaitBeforeKilling = 10;

    //TODO unlimited amount (find available ports automatically)
    /**
     * The list of available host ports that can be used by Antidote containers.
     */
    public static final List<Integer> hostPortList = new ArrayList<>(
            Arrays.asList(8087, 8088, 8089, 8090, 8091, 8092));

    //TODO change the way this works!
    /**
     * The standard Antidote host that is used to connect to the Antidote container.
     */
    public static final String antidoteHost = "localhost";

    /**
     * The number of attempts to start the git repository before cancelling the start.
     */
    public static final int numberOfAttemptsToStartGit = 3;

}
