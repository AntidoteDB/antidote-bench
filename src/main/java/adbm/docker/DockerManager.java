package adbm.docker;

import adbm.git.GitManager;
import adbm.main.Main;
import adbm.settings.MapDBManager;
import adbm.util.SimpleProgressHandler;
import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static adbm.docker.DockerUtil.normalizeName;

public class DockerManager
{

    private static final Logger log = LogManager.getLogger(DockerManager.class);

    //TODO test container name is action
    //TODO check safety and if multi threading works

    //TODO update doc
    //TODO automatic container removal when container is not reusable
    //TODO container already running!
    //TODO clean up bad containers that are not created or exited

    //TODO Change how this method works (just one image and change build inside container)
    //TODO test local build!
    private static DockerClient docker;

    /**
     * This image is required to build the Antidote Benchmark image.
     */
    private static final String requiredImage = "erlang:19";

    /**
     * This is the network that is used for the antidote containers.
     */
    private static final String antidoteDockerNetworkName = "antidote_ntwk";

    /**
     * This image is very similar to the normal Antidote image.
     * It keeps the git repository so that other commits can be checked out.
     */
    private static final String antidoteDockerImageName = "antidotedb/benchmark";

    private static final int standardClientPort = 8087;

    private static final int secondsToWaitBeforeKilling = 10;

    private static boolean isBuildingImage = false;

    private static String ipAddress;

    public static boolean isBuildingImage()
    {
        return isBuildingImage;
    }

    //TODO different IP possible not just local host

    //TODO unlimited amount (find available ports automatically)
    private static final List<Integer> hostPortList = new ArrayList<>(
            Arrays.asList(8087, 8088, 8089, 8090, 8091, 8092));

    // TODO Containers are not allowed to have the same name! Therefore the returned list must have only one element!

    /**
     * Checks if the DockerManager (Connection to Docker) is ready and the methods can be used.
     * Prints a message if the DockerManager is not ready.
     *
     * @return true if DockerManager is ready otherwise false.
     */
    public static boolean isReady()
    {
        if (docker != null && !isBuildingImage) return true;
        if (isBuildingImage)
            log.info("Docker cannot be used because an image is building currently!");
        else
            log.info("The connection to Docker was not started yet or was reset to the initial state!");
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

    @Nonnull
    private static String getContainerId(String name, boolean mustBeRunning)
    {
        try {
            List<Container> adbmContainersWithName;
            if (mustBeRunning) {
                adbmContainersWithName = docker.listContainers(DockerClient.ListContainersParam.filter("name", name),
                                                               DockerClient.ListContainersParam
                                                                       .filter("ancestor", antidoteDockerImageName),
                                                               DockerClient.ListContainersParam.allContainers(),
                                                               DockerClient.ListContainersParam.withStatusRunning());
            }
            else {
                adbmContainersWithName = docker.listContainers(DockerClient.ListContainersParam.filter("name", name),
                                                               DockerClient.ListContainersParam
                                                                       .filter("ancestor", antidoteDockerImageName),
                                                               DockerClient.ListContainersParam.allContainers());
            }
            if (adbmContainersWithName.isEmpty()) {
                log.error(
                        "No container with the name {} was found! An empty id (\"\") will be returned!",
                        name);
                return "";
            }
            if (adbmContainersWithName.size() > 1) {
                log.warn("Multiple Antidote Benchmark containers have the same name!");
                log.info("The id of the first container will be returned!");
                log.debug("List of containers (id) with the name {}: {}", name,
                          adbmContainersWithName.stream().map(Container::id).collect(
                                  Collectors.toList()));
            }
            return adbmContainersWithName.get(0).id();
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while getting the container id from a name.", e);
            return "";
        }
    }

    /**
     * TODO Change that remote Docker can be used instead of local
     * Starts the connection to Docker.
     * It checks that the required Image erlang:19 is available and pulls it if it is not available.
     * It checks that the Docker Network that is used for in this application exists and creates it if it does not exist.
     *
     * @return true if the connection to Docker was successfully started otherwise false.
     */
    public static boolean startDocker(String uri, String certPath)
    {
        // TODO if (!GitManager.isReady()) return false;
        if (isReadyNoText()) {
            log.debug("The DockerManager was already ready and will be restarted!");
        }
        try {
            if (uri == null || certPath == null)
                docker = DefaultDockerClient.fromEnv().readTimeoutMillis(3600000).build();
            else return false;
            //else docker = DefaultDockerClient.builder().uri(uri).dockerCertificates(new DockerCertificates(Paths.get(certPath))).build(); //TODO testing
            log.debug("Checking that image {} is available...", requiredImage);
            if (docker.listImages(DockerClient.ListImagesParam.byName(requiredImage)).isEmpty()) {
                log.info("Image {} is not available and must be pulled.", requiredImage);
                //TODO add confirm
                boolean confirm = true;
                if (Main.isGuiMode()) confirm = JOptionPane.showConfirmDialog(null,
                                                                              "The image " + requiredImage + " is not available in Docker and must be pulled before the " + Main.appName + " application can be used.\nPressing \"Cancel\" this will terminate the application.",
                                                                              "Image need to be pulled", JOptionPane.OK_CANCEL_OPTION,
                                                                              JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION;
                if (confirm) docker.pull(requiredImage, new SimpleProgressHandler("Image"));
                else return false;
            }
            else {
                log.debug(requiredImage + " is available.");
            }
            log.debug("Checking that Network {} exists...", antidoteDockerNetworkName);
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
                    new Thread(() -> getNamesOfRunningContainers().forEach(DockerManager::stopContainer)));
            if (!getNamesOfRunningContainers().isEmpty()) {
                log.error("The Antidote Benchmark containers cannot be running when the DockerManager starts!" +
                                  "\nPlease restart Docker manually!");
                //JOptionPane.showMessageDialog(null,"");TODO
                return false;
            }
            if (!antidoteBenchmarkImageExists()) {
                buildAntidoteBenchmarkImage(false);
            }
            return true;
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean rebuildAntidoteInContainer(String name, String commit)
    {
        //TODO checks and logs correctly
        String containerId = getContainerId(name, true);
        if (containerId.isEmpty()) {
            log.warn("The container with the name {} could not be rebuild because no id was found!");
            return false;
        }
        //TODO if (container.names().contains(name)) {
        log.debug("Rebuilding Antidote in the container (id) {} ({}) with the commit {}", name, containerId, commit);
        try {
            String stopAntidote = "/opt/antidote/bin/env stop";
            final String execId = docker
                    .execCreate(containerId, new String[]{stopAntidote}, DockerClient.ExecCreateParam.tty(),
                                DockerClient.ExecCreateParam.attachStdout()).id();
            try (final LogStream stream = docker.execStart(execId)) {
                log.debug(stream.readFully());
            }
            final String execIdt = docker
                    .execCreate(containerId, new String[]{"bash -c \"cd /usr/src/antidote && git pull\""})
                    .id();
            try (final LogStream stream = docker.execStart(execIdt)) {
                Thread.sleep(5000); //TODO testing
                log.debug(stream.readFully());
            }
            final String execId1 = docker.execCreate(containerId,
                                                     new String[]{"bash -c \"cd /usr/src/antidote && git checkout " + commit + "\""})
                                         .id();
            try (final LogStream stream = docker.execStart(execId1)) {
                log.debug(stream.readFully());
            }

            final String execId3 = docker
                    .execCreate(containerId, new String[]{"bash -c \"cd /usr/src/antidote && make rel\""})
                    .id();
            try (final LogStream stream = docker.execStart(execId3)) {
                log.debug(stream.readFully());
            }
            final String execId4 = docker.execCreate(containerId,
                                                     new String[]{"cp -R /usr/src/antidote/_build/default/rel/antidote /opt/"})
                                         .id();
            try (final LogStream stream = docker.execStart(execId4)) {
                log.debug(stream.readFully());
            }
            //TODO necessary?
            final String execId5 = docker.execCreate(containerId,
                                                     new String[]{"sed -e '$i,{kernel, [{inet_dist_listen_min, 9100}, {inet_dist_listen_max, 9100}]}' /usr/src/antidote/_build/default/rel/antidote/releases/0.0.1/sys.config > /opt/antidote/releases/0.0.1/sys.config"})
                                         .id();
            try (final LogStream stream = docker.execStart(execId5)) {
                log.debug(stream.readFully());
            }
            final String execId7 = docker
                    .execCreate(containerId, new String[]{"bash -c /opt/antidote/start_and_attach.sh"})
                    .id();
            try (final LogStream stream = docker.execStart(execId7)) {
                log.debug(stream.readFully());
            }
            return true;
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while rebuilding Antidote in a container!", e);
            return false;
        }
    }

    public static boolean antidoteBenchmarkImageExists()
    {
        if (!isReady()) return false;
        log.debug("Checking if the {} image already exists...", Main.appName);
        List<Image> images;
        try {
            images = docker.listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName), DockerClient.ListImagesParam.allImages());
            log.debug("Existing Images: ", images);
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while checking if an image exists!", e);
            return false;
        }
        return !images.isEmpty();
    }

    public static synchronized boolean buildAntidoteBenchmarkImage(boolean local)
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
                folder = new File(MapDBManager.getGitRepoLocation()).getParentFile();
                if (folder != null) path = folder.getCanonicalPath();
            }
            log.info("Building Image {}...", antidoteDockerImageName);
            docker.build(Paths.get(path), antidoteDockerImageName, new SimpleProgressHandler("Image"));
            log.info("Image {} was successfully built.", antidoteDockerImageName);
            return true;
        } catch (DockerException | InterruptedException | IOException e) {
            log.error("An error occurred while building an image!", e);
        } finally {
            isBuildingImage = false;
        }
        return false;
    }

    public static boolean removeAllContainers()
    {
        if (!isReady()) return false;
        log.debug("Removing all containers that were created from the image {}!", antidoteDockerImageName);
        try {
            for (Container container : getAllContainers()) {
                docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                docker.removeContainer(container.id());
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while removing all containers!", e);
            return false;
        }
        return true;
    }

    public static boolean stopAllContainers()
    {
        if (!isReady()) return false;
        log.debug("Removing all containers that were created from the image {}!", antidoteDockerImageName);
        try {
            for (Container container : getRunningContainers()) {
                docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while stopping all containers!", e);
            return false;
        }
        return true;
    }

    /**
     * Runs a container with the specified name.
     * If the container exists and is running nothing is done.
     * If the container exists and is not running it will be started.
     * If the container does not exist it will be created and then started.
     * If a new container is created it will use a unique host port that is used to connect containers via the Antidote Docker Network (for replication)
     *
     * @param name The name of the container.
     * @return true if the container is running otherwise false.
     */
    public static boolean runContainer(String name)
    {
        if (!isReady()) return false;
        boolean containerExists = getAllContainersAlsoNonRelevant().stream()
                                                                   .anyMatch(container -> container
                                                                           .names() != null && (Objects
                                                                           .requireNonNull(container.names())
                                                                           .contains(name) || Objects
                                                                           .requireNonNull(container.names())
                                                                           .contains("/" + name)));
        if (containerExists) {
            String containerId = getContainerId(name, true);
            if (!containerId.isEmpty()) {
                log.debug("The container {} is already running!", name);
                return true;
            }
            else {
                containerId = getContainerId(name, false);
                if (containerId.isEmpty()) {
                    log.error(
                            "The name {} cannot be used to create a new container because another container (that was not built from the {} image) has that name!",
                            name,
                            Main.appName);
                    return false;
                }
                else {
                    log.debug("Starting the existing container {}", name);
                    startContainer(name);
                    return true;
                }
            }
        }
        else {
            List<Integer> portList = getUsedHostPorts();
            Optional<Integer> hostPortOptional = hostPortList.stream().filter(port -> !portList.contains(port))
                                                             .findFirst();
            if (!hostPortOptional.isPresent()) {
                log.error("Port list contains all ports from the host port list!\n" +
                                  "No new containers can be started!\n" +
                                  "You have to extend the list of allowed host ports to run more containers!");
                return false;
            }
            int hostPort = hostPortOptional.get();
            final Map<String, List<PortBinding>> portBindings = new HashMap<>();
            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", hostPort));
            portBindings.put(Integer.toString(standardClientPort), hostPorts);
            final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings)
                                                    .networkMode(antidoteDockerNetworkName).build();
            log.debug("Creating a new container {}...", name);
            ContainerConfig containerConfig = ContainerConfig.builder()
                                                             .image(antidoteDockerImageName)
                                                             .hostConfig(hostConfig)
                                                             .exposedPorts(Integer.toString(standardClientPort))
                                                             .env("SHORT_NAME=true", "NODE_NAME=antidote@" + name)
                                                             .tty(true)
                                                             .build();
            try {
                docker.createContainer(containerConfig, name);
            } catch (DockerException | InterruptedException e) {
                log.error("An error has occurred while running a container!", e);
                return false;
            }
            startContainer(name);
            return true;
        }
    }

    @Nonnull
    private static List<Container> getRunningContainers()
    {
        try {
            return docker.listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                         DockerClient.ListContainersParam.allContainers(),
                                         DockerClient.ListContainersParam.withStatusRunning());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while getting all running containers (internal)!", e);
            return new ArrayList<>();
        }
    }

    @Nonnull
    private static List<Container> getAllContainers()
    {
        try {
            return docker.listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                         DockerClient.ListContainersParam.allContainers());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while getting all containers (internal)!", e);
            return new ArrayList<>();
        }
    }

    @Nonnull
    private static List<Container> getAllContainersAlsoNonRelevant()
    {
        try {
            return docker.listContainers(DockerClient.ListContainersParam.allContainers());
        } catch (DockerException | InterruptedException e) {
            log.error(
                    "An error has occurred while getting all containers including those that are not relevant (internal)!",
                    e);
            return new ArrayList<>();
        }
    }

    /**
     * Stops and then removes the container with the specified name.
     * Can cause issues if the container is used.
     *
     * @param name The name of the container.
     * @return true if the container was successfully removed otherwise false.
     */
    public static boolean removeContainer(String name)
    {
        if (!isReady()) return false;
        log.debug("Removing the container {}...", name);
        String containerId = getContainerId(name, false);
        try {
            docker.stopContainer(containerId, secondsToWaitBeforeKilling);
            docker.removeContainer(containerId);
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while removing a container!", e);
            return false;
        }
        log.debug("Removed the container {}", name);
        return true;
    }

    /**
     * Stops the container with the specified name.
     * Can cause issues if the container is used.
     *
     * @param name The name of the container.
     * @return true if the container was successfully removed otherwise false.
     */
    public static boolean stopContainer(String name)
    {
        if (!isReady()) return false;
        log.info("Stopping the container {}...", name);
        String containerId = getContainerId(name, true);
        try {
            docker.stopContainer(containerId, secondsToWaitBeforeKilling);
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while stopping a container!", e);
            return false;
        }
        log.info("Stopped the container {}", name);
        return true;
    }

    /**
     * Starts the container with the specified name.
     *
     * @param name The name of the container.
     * @return true if the container was successfully removed otherwise false.
     */
    public static boolean startContainer(String name)
    {
        if (!isReady()) return false;
        log.info("Starting the container {}...", name);
        String containerId = getContainerId(name, false);
        try {
            docker.startContainer(containerId);
            log.info("State of the started container (id: {}): {}", containerId,
                     docker.inspectContainer(containerId).state());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while starting a container!", e);
            return false;
        }
        log.info("Started the container {}", name);
        waitUntilContainerIsReady(name); //TODO better
        return true;
    }

    public static void waitUntilContainerIsReady(String name)
    {
        try {
            //TODO more efficient and rework
            String containerId = getContainerId(name, true);
            if (containerId.isEmpty()) {
                return; //TODO logs
            }
            Thread.sleep(500);
            String logs;
            try (LogStream stream = docker
                    .logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr()))
            {
                logs = stream.readFully();
            }
            int start = logs.lastIndexOf("NODE_NAME");
            while (!logs.substring(start).contains("Application antidote started on node")) {
                Thread.sleep(500);
                log.debug("Container is not ready yet!");
                try (LogStream stream = docker
                        .logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr()))
                {
                    logs = stream.readFully();
                }
                start = logs.lastIndexOf("NODE_NAME");
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while waiting for the container!", e);
        }
    }

    /**
     * Returns a list of the names of all running containers that are built from the antidote benchmark image.
     *
     * @return A list of the names of all running containers that are built from the antidote benchmark image.
     */
    @Nonnull
    public static List<String> getNamesOfRunningContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting running containers...");
        Set<String> containerSet = new HashSet<>();
        for (Container container : getRunningContainers()) {
            String firstName = getFirstNameOfContainer(container);
            if (!firstName.isEmpty())
                containerSet.add(firstName);
        }
        log.debug("Running containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    /**
     * Returns a list of the names of all containers that not running (created or exited) that are built from the antidote benchmark image.
     *
     * @return A list of the names of all containers that not running (created or exited) that are built from the antidote benchmark image.
     */
    @Nonnull
    public static List<String> getNamesOfNotRunningContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting exited and created containers...");
        Set<String> containerSet = new HashSet<>();
        List<Container> notRunningContainers = getAllContainers();
        notRunningContainers.removeAll(getRunningContainers());
        for (Container container : notRunningContainers) {
            String firstName = getFirstNameOfContainer(container);
            if (!firstName.isEmpty())
                containerSet.add(firstName);
        }
        log.debug("Exited and created containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    /**
     * Returns a list of the names of all containers that are built from the antidote benchmark image.
     *
     * @return A list of the names of all containers that are built from the antidote benchmark image.
     */
    @Nonnull
    public static List<String> getNamesOfAllContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting all containers...");
        Set<String> containerSet = new HashSet<>();
        for (Container container : getAllContainers()) {
            String firstName = getFirstNameOfContainer(container);
            if (!firstName.isEmpty())
                containerSet.add(firstName);
        }
        log.debug("All containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Nonnull
    private static String getFirstNameOfContainer(Container container)
    {
        List<String> nameList = container.names();
        if (nameList != null && nameList.size() > 0) {
            log.trace("Container (id: {}) first name before normalization: {}", container.id(), nameList.get(0));
            String firstName = normalizeName(nameList.get(0));
            if (nameList.size() > 1) {
                StringBuilder nameString = new StringBuilder();
                for (String name : nameList) {
                    nameString.append(normalizeName(name));
                    nameString.append(", ");
                }
                String names = nameString.toString();
                if (names.length() > 2) {
                    log.debug(
                            "The container (id: {}) has multiple names: {}", container.id(), names
                                    .substring(0, names.length() - 3));
                }
            }
            log.trace("Container (id: {}) first name after normalization: {}", container.id(), firstName);
            return firstName;
        }
        else {
            log.error("The container (id: {}) does not have a name! This should not be possible!", container.id());
            return "";
        }
    }

    /**
     * Returns the host port of the container with the specified name.
     * This port is used when an antidote client connects to the container or if containers a connected via the Antidote Docker Network for replication.
     * Returns -1 if something goes wrong.
     *
     * @param name The name of the container.
     * @return The host port of the container.
     */
    @Nonnull
    public static List<Integer> getHostPortsFromContainer(String name)
    {
        if (!isReady()) return new ArrayList<>();
        String containerId = getContainerId(name, false);
        if (containerId.isEmpty()) {
            return new ArrayList<>();
        }
        log.debug("Getting host port from container (id: {}) with the name {}...", containerId, name);
        ContainerInfo containerInfo;
        try {
            containerInfo = docker.inspectContainer(containerId);
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while retrieving the host port!", e);
            return new ArrayList<>();
        }
        if (containerInfo != null) {
            HostConfig config = containerInfo.hostConfig();
            ImmutableMap<String, List<PortBinding>> portBindingMap = config != null ? config.portBindings() : null;
            if (portBindingMap != null) {
                if (portBindingMap.containsKey(Integer.toString(standardClientPort))) {
                    List<PortBinding> portBindingList = portBindingMap.get(Integer.toString(standardClientPort));
                    if (!portBindingList.isEmpty()) {
                        //TODO different IP possible but later
                        List<Integer> portList = portBindingList.stream().map(portBinding -> Integer
                                .parseInt(portBinding.hostPort())).collect(
                                Collectors
                                        .toList()); //TODO Parse Exceptions when Port is not an Integer (Not sure if this is allowed)
                        if (portList.size() > 1) {
                            log.debug(
                                    "The container {} has multiple host ports: {}", name, portList);
                        }

                        return portList;
                    }
                }
            }
        }
        log.error("Failed to retrieve the host port of the container {}!", name);
        return new ArrayList<>();
    }

    /**
     * Gets the list of used host ports.
     * This is used to make sure that every container has a unique host port.
     *
     * @return The list of used host ports.
     */
    @Nonnull
    private static List<Integer> getUsedHostPorts()
    {
        List<Integer> portList = new ArrayList<>();
        if (!isReady()) return portList;
        for (Container container : getAllContainers()) {
            portList.addAll(getHostPortsFromContainer(getFirstNameOfContainer(container)));
        }
        if (new HashSet<>(portList).size() < portList.size()) {
            log.error("The list of used host ports contains duplicates which can lead to errors!");
        }
        log.debug("Full list of host ports: {}", portList);
        return portList;
    }

}
