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

    // Antidote = AD
    // Benchmark = BM
    // Antidote Benchmark = ADBM

    /**
     * The name of the application. Used for logging and gui.
     */
    public static final String APP_NAME = "Antidote Benchmark";

    public static final String DATE_FORMAT_TIME = "HHmmssXX";

    public static final String DATE_FORMAT_DATE = "yyyyMMdd";

    public static final String ADBM_CONTAINER_NAME = "AntidoteBenchmarkContainer";

    public static final String YCSB_DB_CLASS_NAME = "adbm.ycsb.AntidoteYCSBClient";

    public static final String DATE_FORMAT = "yyyyMMdd'T'HHmmssXX";

    public static final String RESULT_FILE_NAME_START = "Result";

    public static final int NUMBER_COMMIT_ABBREVIATION = 10; //Should be plenty

    /**
     * This is the path to the resources folder where all resource except some properties are stored.
     */
    public static final String RESOURCES_PATH = "resources";

    public static final String YCSB_PATH = format("{}/YCSB", RESOURCES_PATH);

    /**
     * This is the path to the workloads that can be used for benchmarking with YCSB.
     */
    public static final String YCSB_WORKLOADS_PATH = format("{}/Workloads", YCSB_PATH);

    public static final String YCSB_SAMPLE_WORKLOAD_NAME = "SampleWorkload";

    public static String getWorkloadPath(String workloadName) {
        return format("{}/{}.txt", YCSB_WORKLOADS_PATH, workloadName);
    }

    /**
     * This is the path to the YCSB benchmark results.
     */
    public static final String YCSB_RESULTS_PATH = format("{}/Results", YCSB_PATH);

    public static final String YCSB_SAMPLE_RESULT_PATH = format("{}/SampleResult.csv", YCSB_RESULTS_PATH);

    public static final String SETTINGS_PATH = format("{}/Settings", RESOURCES_PATH);

    /**
     * This is the path to the application settings.
     */
    public static final String APP_SETTINGS_PATH = format("{}/AppSettings", SETTINGS_PATH);

    /**
     * This is the path to the log4j2.xml configuration file.
     */
    public static final String LOG_SETTINGS_PATH = "log4j2.xml";

    /**
     * This is the path to the Dockerfile that is used to build the benchmark image.
     */
    public static final String DOCKERFILE_RESOURCES_PATH = format("{}/Dockerfile", RESOURCES_PATH);

    public static final String DOCKERFILE_PATH = format("{}/Dockerfile", DOCKERFILE_RESOURCES_PATH);

    /**
     * This is the path to the images that are used in the gui.
     */
    public static final String IMAGES_PATH = format("{}/Images", RESOURCES_PATH);

    /**
     * This is the default path to the folder of the application logs.
     */
    public static final String DEFAULT_LOG_PATH = "Logs";

    /**
     * This is the default path to the folder of the Antidote git repository.
     */
    public static final String DEFAULT_AD_GIT_REPO_PATH = "AntidoteGitRepo";

    /**
     * This is the git url to the Antidote git repository.
     */
    public static final String AD_GIT_REPO_URL = "https://github.com/SyncFree/antidote.git";

    /**
     * This image is required to build the Antidote benchmark image.
     */
    public static final String REQUIRED_IMAGE = "erlang:19";

    /**
     * This is the network that is used for the antidote containers.
     */
    public static final String ADBM_DOCKER_NETWORK_NAME = "antidote_ntwk";

    /**
     * This is the name of the antidote benchmark image.
     * It is similar to the regular Antidote image.
     * However it keeps the Antidote git repository intact so that other commits can be checked out.
     */
    public static final String ADBM_DOCKER_IMAGE_NAME = "antidotedb/benchmark";

    /**
     * The standart client port used by Antidote containers.
     */
    public static final int STANDARD_ADBM_CLIENT_PORT = 8087;

    /**
     * The time to wait before killing a container that is being stopped.
     */
    public static final int SECONDS_TO_WAIT_BEFORE_KILLING = 10;

    //TODO unlimited amount (find available ports automatically)
    /**
     * The list of available host ports that can be used by Antidote containers.
     */
    public static final List<Integer> ADBM_HOST_PORT_LIST = new ArrayList<>(
            Arrays.asList(8087, 8088, 8089, 8090, 8091, 8092));

    //TODO change the way this works!
    /**
     * The standard Antidote host that is used to connect to the Antidote container.
     */
    public static final String ADBM_CLIENT_HOST = "localhost";

    /**
     * The number of attempts to start the git repository before cancelling the start.
     */
    public static final int NUMBER_OF_ATTEMPTS_TO_START_GIT = 3;

}
