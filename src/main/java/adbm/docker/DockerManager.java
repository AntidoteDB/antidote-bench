package adbm.docker;

import adbm.git.GitManager;
import adbm.settings.MapDBManager;
import adbm.util.SimpleProgressHandler;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.*;
import com.spotify.docker.client.messages.Container;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class DockerManager
{

    private static final Logger log = LogManager.getLogger(DockerManager.class);

    //TODO test container name is action
    //TODO check safety and if multi threading works

    private static DockerClient docker;

    private static final String requiredImage = "erlang:19";

    private static final String antidoteDockerNetworkName = "antidote_ntwk";

    private static final String antidoteDockerImageName = "antidotedb/benchmark";

    private static final int standardClientPort = 8087;

    private static final int secondsToWaitBeforeKilling = 10;

    private static boolean isBuildingImage = false;

    public static boolean isBuildingImage()
    {
        return isBuildingImage;
    }



    private static final List<Integer> hostPortList = new ArrayList<>(
            Arrays.asList(8087, 8088, 8089, 8090, 8091, 8092));

    // Containers are not allowed to have the same name! Therefore the returned list must have only one element!

    /**
     * Checks if the DockerManager (Connection to Docker) is ready and the methods can be used.
     * Prints a message if the DockerManager is not ready.
     *
     * @return true if DockerManager is ready otherwise false.
     */
    public static boolean isReady()
    {
        if (docker != null && !isBuildingImage) return true;
        log.info("The DockerManager is not ready!");
        if (!isBuildingImage)
            log.info("Please restart the DockerManager!");
        else
            log.info("An image is being built!");
        return false;
    }

    /**
     * Checks if the DockerManager (Connection to Docker) is ready and the methods can be used.
     * Does not print any messages.
     *
     * @return true if DockerManager is ready otherwise false.
     */
    public static boolean isReadyNoText()
    {
        return docker != null && !isBuildingImage;
    }


    /**
     * TODO Change that remote Docker can be used instead of local
     * Starts the connection to Docker.
     * It checks that the required Image erlang:19 is available and pulls it if it is not available.
     * It checks that the Docker Network that is used for in this application exists and creates it if it does not exist.
     *
     * @return true if the connection to Docker was successfully started otherwise false.
     */
    public static boolean startDocker()
    {
        if (!GitManager.isReady()) return false;
        try {
            docker = DefaultDockerClient.fromEnv().readTimeoutMillis(3600000).build();
            log.info("Checking that image {} is available...", requiredImage);
            if (docker.listImages(DockerClient.ListImagesParam.byName(requiredImage)).isEmpty()) {
                log.info("Image {} is not available and will be pulled.", requiredImage);
                docker.pull(requiredImage, new SimpleProgressHandler("Image"));
            }
            else {
                log.info(requiredImage + " is available.");
            }
            log.info("Checking that Network {} exists...", antidoteDockerNetworkName);
            boolean containsNetwork = false;
            for (Network network : docker.listNetworks()) {
                if (network.name().equals(antidoteDockerNetworkName)) {
                    containsNetwork = true;
                    break;
                }
            }
            if (!containsNetwork) {
                log.info("Network {} does not exist and will be created.", antidoteDockerNetworkName);
                docker.createNetwork(NetworkConfig.builder().name(antidoteDockerNetworkName).driver("bridge").build());
            }
            log.info("Docker initialized!");
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> getRunningContainers().forEach(DockerManager::stopContainer)));
            if (!getRunningContainers().isEmpty()) {
                log.error("ERROR: The Antidote Benchmark containers cannot be running when the DockerManager starts!" +
                                  "\nPlease restart Docker manually!");
                //JOptionPane.showMessageDialog(null,"");
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean rebuildAntidoteInContainer(String name, String commit)
    {
        //TODO checks and logs correctly
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                    DockerClient.ListContainersParam.allContainers(),
                                    DockerClient.ListContainersParam.withStatusRunning())) {
                //TODO if (container.names().contains(name)) {
                log.info("Rebuilding Antidote in the container \"{}\" with the commit \"{}\"", name, commit);
                final String execId = docker.execCreate(container.id(), new String[]{"/opt/antidote/bin/env", "stop"})
                                            .id();
                try (final LogStream stream = docker.execStart(execId)) {
                    log.debug(stream.readFully());
                }
                final String execIdt = docker
                        .execCreate(container.id(), new String[]{"bash", "-c", "\"cd /usr/src/antidote && git pull\""})
                        .id();
                try (final LogStream stream = docker.execStart(execIdt)) {
                    log.debug(stream.readFully());
                }
                final String execId1 = docker.execCreate(container.id(),
                                                         new String[]{"bash", "-c", "\"cd /usr/src/antidote && git checkout " + commit + "\""})
                                             .id();
                try (final LogStream stream = docker.execStart(execId1)) {
                    log.debug(stream.readFully());
                }

                final String execId3 = docker
                        .execCreate(container.id(), new String[]{"bash", "-c", "\"cd /usr/src/antidote && make rel\""})
                        .id();
                try (final LogStream stream = docker.execStart(execId3)) {
                    log.debug(stream.readFully());
                }
                final String execId4 = docker.execCreate(container.id(),
                                                         new String[]{"cp", "-R", "/usr/src/antidote/_build/default/rel/antidote", "/opt/"})
                                             .id();
                try (final LogStream stream = docker.execStart(execId4)) {
                    log.debug(stream.readFully());
                }
                //TODO necessary?
                final String execId5 = docker.execCreate(container.id(),
                                                         new String[]{"sed", "-e", "'$i,{kernel, [{inet_dist_listen_min, 9100}, {inet_dist_listen_max, 9100}]}'", "/usr/src/antidote/_build/default/rel/antidote/releases/0.0.1/sys.config > /opt/antidote/releases/0.0.1/sys.config"})
                                             .id();
                try (final LogStream stream = docker.execStart(execId5)) {
                    log.debug(stream.readFully());
                }
                final String execId7 = docker
                        .execCreate(container.id(), new String[]{"bash", "-c", "/opt/antidote/start_and_attach.sh"})
                        .id();
                try (final LogStream stream = docker.execStart(execId7)) {
                    log.debug(stream.readFully());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean imageExists() {
        if (!isReady()) return false;
        try {
            log.debug("Checking if an image already exists...");
            List<Image> images = docker.listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName));
            return images.isEmpty();
        } catch (Exception e) {
            log.error("An error occurred while checking if an image exists!", e);
        }
        return false;
    }

    /**
     * TODO Change how this method works (just one image and change build inside container)
     * TODO test local!
     * Builds the
     *
     * @param local
     * @return
     */
    public static synchronized boolean buildBenchmarkImage(boolean local)
    {
        if (!isReady()) return false;
        try {
            isBuildingImage = true;
            log.debug("Checking if an image already exists...");
            List<Image> images = docker
                    .listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName));
            if (images.isEmpty()) {
                log.debug("Image {} does not exist and will be built.", antidoteDockerImageName);
            }
            else {
                log.debug("Image {} already exists and will be rebuilt.", antidoteDockerImageName);
                removeAllContainers();
            }
            if (local) GitManager.checkoutBranch("master");
            DockerfileBuilder.createDockerfile(local);
            File folder = new File("Dockerfile");
            String path = folder.getCanonicalPath();
            if (local) {
                folder = new File(MapDBManager.getAppSetting(MapDBManager.GitRepoLocationSetting)).getParentFile();
                if (folder != null) path = folder.getCanonicalPath();
            }
            log.info("Building Image {}...", antidoteDockerImageName);
            docker.build(Paths.get(path), antidoteDockerImageName, new SimpleProgressHandler("Image"));
            log.info("Image {} was successfully built.", antidoteDockerImageName);
            return true;
        } catch (Exception e) {
            log.error("An error occurred while building an image!", e);
        } finally {
            isBuildingImage = false;
        }
        return false;
    }

    /**
     * If the first character of the name is '/' it is removed and then the name is returned.
     * This character is sometimes added by Docker for unknown reasons.
     *
     * @param containerNameFromDocker The name of the container.
     * @return The name where the first character is removed if it was a '/'.
     */
    private static String normalizeName(String containerNameFromDocker)
    {
        if (containerNameFromDocker.startsWith("/")) return containerNameFromDocker.substring(1);
        else return containerNameFromDocker;
    }

    public static void removeAllContainers() {
        if (!isReady()) return;
        log.info("Removing all containers that were created from the image {}!", antidoteDockerImageName);
        for (String container : getAllContainers()) {
            removeContainer(container);
        }
    }


    /**
     * TODO automatic container removal when container is not reusable
     * Runs a container with the specified name.
     * If the container exists and is running it is restarted. TODO This will be changed and is done to prevent bugs!
     * If the container exists and is not running it will be started. TODO Check that the container is usable!
     * If the container does not exist it will be created and then started.
     * If a new container is created it will use a unique host port that is used to connect containers via the Antidote Docker Network (for replication)
     *
     * @param name The name of the container.
     * @return true if the container is running otherwise false.
     */
    public static boolean runContainer(String name)
    {
        if (!isReady()) return false;
        log.info("Running the container {}...", name);
        List<String> containerList = getRunningContainers();
        if (containerList.contains(name)) {
            log.info("The container {} is already running!", name);
            //TODO possible problems no matter what is done
            // TODO add restart functionality in GUI
            //log.info("The container {} is restarted now!", name);
            //stopContainer(name); //TODO
            return true;
        }
        containerList = getNotRunningContainers();
        if (containerList.contains(name)) {
            startContainer(name);
            return true;
        }
        List<Integer> portList = getUsedHostPorts();
        if (portList.containsAll(hostPortList)) {
            log.info("Port list contains all ports from the host port list!\n" +
                             "No new containers can be started!\n" +
                             "You have to extend the list of allowed host ports to run more containers!");
            return false;
        }
        int hostPort = 0;
        for (int port : hostPortList) {
            if (!portList.contains(port)) {
                hostPort = port;
                break;
            }
        }
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        List<PortBinding> hostPorts = new ArrayList<>();
        hostPorts.add(PortBinding.of("0.0.0.0", hostPort));
        portBindings.put(Integer.toString(standardClientPort), hostPorts);
        final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings)
                                                .networkMode(antidoteDockerNetworkName).build();
        try {
            log.info("Creating a new container {}...", name);
            ContainerConfig containerConfig = ContainerConfig.builder()
                                                             .image(antidoteDockerImageName)
                                                             .hostConfig(hostConfig)
                                                             .exposedPorts(Integer.toString(standardClientPort))
                                                             .env("SHORT_NAME=true", "NODE_NAME=antidote@" + name)
                                                             .tty(true)
                                                             .build();
            docker.createContainer(containerConfig, name);
            startContainer(name);
            return true;
        } catch (Exception e) {
            log.error("An error has occured while running a container!", e);
        }
        return false;
    }

    /**
     * Stops and then removes the container with the specified name.
     * Can cause issues if the container is used.
     *
     * @param name The name of the container.
     */
    public static void removeContainer(String name)
    {
        if (!isReady()) return;
        log.info("Removing the container {}...", name);
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                docker.removeContainer(container.id());
                log.info("Removed the container {}", name);
            }
        } catch (Exception e) {
            log.error("An error has occured while removing a container!", e);
        }
    }

    /**
     * Stops the container with the specified name.
     * Can cause issues if the container is used.
     *
     * @param name The name of the container.
     */
    public static void stopContainer(String name)
    {
        if (!isReady()) return;
        log.info("Stopping the container {}...", name);
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                log.info("Stopped the container {}", name);
            }
        } catch (Exception e) {
            log.error("An error has occured while stopping a container!", e);
        }
    }

    /**
     * Starts the container with the specified name.
     *
     * @param name The name of the container.
     */
    public static void startContainer(String name)
    {
        if (!isReady()) return;
        log.info("Starting the container {}...", name);
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.allContainers(),
                                    DockerClient.ListContainersParam.filter("name", name))) {
                docker.startContainer(container.id());
                log.info("Started the container {}", name);
                log.info("Container ID: {}", container.id());
                log.info("State: {}", docker.inspectContainer(container.id()).state());
                //TODO more efficient
                Thread.sleep(500);
                String logs;
                try (LogStream stream = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                    logs = stream.readFully();
                }
                int start = logs.lastIndexOf("NODE_NAME");
                while (!logs.substring(start).contains("Application antidote started on node")) {
                    Thread.sleep(500);
                    log.info("Container is not ready yet!");
                    try (LogStream stream = docker.logs(container.id(), DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                        logs = stream.readFully();
                    }
                    start = logs.lastIndexOf("NODE_NAME");
                }
            }
        } catch (Exception e) {
            log.error("An error has occurred while starting a container!", e);
        }
    }

    /**
     * Returns a list of the names of all running containers that are built from the antidote benchmark image.
     *
     * @return A list of the names of all running containers that are built from the antidote benchmark image.
     */
    public static List<String> getRunningContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.info("Getting running containers...");
        Set<String> containerSet = new HashSet<>();
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                    DockerClient.ListContainersParam.allContainers(),
                                    DockerClient.ListContainersParam.withStatusRunning())) {
                List<String> nameList = container.names();
                if (nameList == null) {
                    log.info("ERROR: The container has no name!");
                }
                else {
                    if (nameList.size() > 0) {
                        String firstName = normalizeName(nameList.get(0));
                        if (nameList.size() > 1) {
                            StringBuilder nameString = new StringBuilder();
                            for (String name : nameList) {
                                nameString.append(normalizeName(name));
                                nameString.append(", ");
                            }
                            String names = nameString.toString();
                            if (names.length() > 2) {
                                log.info(
                                        "A container has multiple names: {}", names
                                                .substring(0, names.length() - 3));
                                log.info("Using the first name: {}", firstName);
                            }
                        }
                        containerSet.add(firstName);
                    }
                    else {
                        log.info("ERROR: The container has no name!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Running containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    /**
     * Returns a list of the names of all containers that not running (created or exited) that are built from the antidote benchmark image.
     *
     * @return A list of the names of all containers that not running (created or exited) that are built from the antidote benchmark image.
     */
    public static List<String> getNotRunningContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.info("Getting exited and created containers...");
        Set<String> containerSet = new HashSet<>();
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                    DockerClient.ListContainersParam.allContainers(),
                                    DockerClient.ListContainersParam.withStatusExited()
                    )
                    ) {
                List<String> nameList = container.names();
                if (nameList == null) {
                    log.info("ERROR: The container has no name!");
                }
                else {
                    if (nameList.size() > 0) {
                        String firstName = normalizeName(nameList.get(0));
                        if (nameList.size() > 1) {
                            StringBuilder nameString = new StringBuilder();
                            for (String name : nameList) {
                                nameString.append(normalizeName(name));
                                nameString.append(", ");
                            }
                            String names = nameString.toString();
                            if (names.length() > 2) {
                                log.info(
                                        "A container has multiple names: {}", names
                                                .substring(0, names.length() - 3));
                                log.info("Using the first name: {}", firstName);
                            }
                        }
                        containerSet.add(firstName);
                    }
                    else {
                        log.info("ERROR: The container has no name!");
                    }
                }
            }
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                    DockerClient.ListContainersParam.allContainers(),
                                    DockerClient.ListContainersParam.withStatusCreated())) {
                List<String> nameList = container.names();
                if (nameList == null) {
                    log.info("ERROR: The container has no name!");
                }
                else {
                    if (nameList.size() > 0) {
                        String firstName = normalizeName(nameList.get(0));
                        if (container.names().size() > 1) {
                            StringBuilder nameString = new StringBuilder();
                            for (String name : nameList) {
                                nameString.append(normalizeName(name));
                                nameString.append(", ");
                            }
                            String names = nameString.toString();
                            if (names.length() > 2) {
                                log.info(
                                        "A container has multiple names: {}", names
                                                .substring(0, names.length() - 3));
                                log.info("Using the first name: {}", firstName);
                            }
                        }
                        containerSet.add(firstName);
                    }
                    else {
                        log.info("ERROR: The container has no name!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Exited and created containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    /**
     * Returns a list of the names of all containers that are built from the antidote benchmark image.
     *
     * @return A list of the names of all containers that are built from the antidote benchmark image.
     */
    public static List<String> getAllContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.info("Getting all containers...");
        Set<String> containerSet = new HashSet<>();
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                    DockerClient.ListContainersParam.allContainers())) {
                List<String> nameList = container.names();
                if (nameList == null) {
                    log.info("ERROR: The container has no name!");
                }
                else {
                    if (nameList.size() > 0) {
                        String firstName = normalizeName(nameList.get(0));
                        if (nameList.size() > 1) {
                            StringBuilder nameString = new StringBuilder();
                            for (String name : nameList) {
                                nameString.append(normalizeName(name));
                                nameString.append(", ");
                            }
                            String names = nameString.toString();
                            if (names.length() > 2) {
                                log.info(
                                        "A container has multiple names: {}", names
                                                .substring(0, names.length() - 3));
                                log.info("Using the first name: {}", firstName);
                            }
                        }
                        containerSet.add(firstName);
                    }
                    else {
                        log.info("ERROR: The container has no name!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("All containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    /**
     * Returns the host port of the container with the specified name.
     * This port is used when an antidote client connects to the container or if containers a connected via the Antidote Docker Network for replication.
     * Returns -1 if something goes wrong.
     *
     * @param name The name of the container.
     * @return The host port of the container.
     */
    public static int getHostPortFromContainer(String name)
    {
        if (!isReady()) return -1;
        log.info("Getting host port form container {}...", name);
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("name", name),
                                    DockerClient.ListContainersParam.allContainers())) {
                List<Integer> portList = new ArrayList<>();
                List<Container.PortMapping> portMappingList = container.ports();
                if (portMappingList == null) {
                    log.info("ERROR: The container has no host port!");
                }
                else {
                    for (Container.PortMapping port : portMappingList) {
                        if (port.privatePort() == standardClientPort) {
                            portList.add(port.publicPort());
                        }
                    }
                    if (portList.size() > 0) {
                        int firstPort = portList.get(0);
                        if (portList.size() > 1) {
                            StringBuilder nameString = new StringBuilder();
                            for (int port : portList) {
                                nameString.append(port);
                                nameString.append(", ");
                            }
                            String names = nameString.toString();
                            if (names.length() > 2) {
                                log.info(
                                        "The container {} has multiple host ports: {}", name, names
                                                .substring(0, names.length() - 3));
                                log.info("Using the first host port: {}", firstPort);
                            }
                        }
                        log.info("The host port of the container {} is {}", name, firstPort);
                        return firstPort;
                    }
                    else {
                        log.info("ERROR: The container has no host port!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * TODO limitless host ports
     * Gets the list of used host ports.
     * This is used to make sure that every container has a unique host port.
     *
     * @return The list of used host ports.
     */
    public static List<Integer> getUsedHostPorts()
    {
        List<Integer> portList = new ArrayList<>();
        if (!isReady()) return portList;
        try {
            for (Container container : docker
                    .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                    DockerClient.ListContainersParam.allContainers())) {
                List<Integer> singlePortList = new ArrayList<>();
                List<Container.PortMapping> portMappingList = container.ports();
                if (portMappingList == null) {
                    log.info("ERROR: The container has no host port!");
                }
                else {
                    for (Container.PortMapping port : portMappingList) {
                        if (port.privatePort() == standardClientPort) {
                            singlePortList.add(port.publicPort());
                        }
                    }
                    if (singlePortList.size() > 0) {
                        int firstPort = singlePortList.get(0);
                        if (singlePortList.size() > 1) {
                            StringBuilder nameString = new StringBuilder();
                            for (int port : singlePortList) {
                                nameString.append(port);
                                nameString.append(", ");
                            }
                            String names = nameString.toString();
                            if (names.length() > 2) {
                                log.info("The container has multiple host ports: {}", names.substring(0, names.length() - 3));
                                log.info("Using the first host port: {}", firstPort);
                            }
                        }
                        portList.add(firstPort);
                    }
                    else {
                        log.info("ERROR: The container has no host port!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return portList;
    }


}
