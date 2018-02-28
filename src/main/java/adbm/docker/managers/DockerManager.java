package adbm.docker.managers;

import adbm.docker.IDockerManager;
import adbm.docker.util.DockerUtil;
import adbm.docker.util.DockerfileBuilder;
import adbm.main.Main;
import adbm.util.AdbmConstants;
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

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static adbm.util.helpers.FormatUtil.format;

public class DockerManager implements IDockerManager {

    private static final Logger log = LogManager.getLogger(DockerManager.class);

    //TODO test container name is action
    //TODO check safety and if multi threading works

    //TODO update doc
    //TODO automatic container removal when container is not reusable
    //TODO container already running!
    //TODO clean up bad containers that are not created or exited

    //TODO Change how this method works (just one image and change build inside container)
    //TODO test local build!
    //TODO different IP possible not just local host
    // TODO Containers are not allowed to have the same name! Therefore the returned list must have only one element!

    /**
     * The Docker Client.
     * Is set to null when {@link #stop()} is called.
     */
    private DockerClient docker;

    /**
     * Is set to true when a image is currently building.
     * The building process takes some time and the docker client is unavailable during that time.
     */
    private boolean isBuildingImage;

    /**
     * The instance of the class.
     * There can only be a single instance of this class.
     */
    private static DockerManager instance = new DockerManager();

    /**
     * Returns the instance of this class.
     *
     * @return the instance of this class.
     */
    public static synchronized DockerManager getInstance() {
        return instance;
    }

    /**
     * Private constructor to prevent creation of other instances.
     */
    private DockerManager() {

    }

    @Override
    public boolean start() {
        return start(null);
    }

    @Override
    public boolean start(String uri, String... args) {
        log.trace("Starting DockerManager!");
        if (isReady()) {
            log.debug("The DockerManager was already ready and will be restarted!");
        }
        String certPath = null;
        if (args.length > 0) {
            certPath = args[1];
        }
        try {
            if (uri == null || certPath == null)
                docker = DefaultDockerClient.fromEnv().readTimeoutMillis(3600000).build();
            else return false;
            //else docker = DefaultDockerClient.builder().uri(uri).dockerCertificates(new DockerCertificates(Paths.get(certPath))).build(); //TODO testing
            log.debug("Checking that image {} is available...", AdbmConstants.requiredImage);
            if (docker.listImages(DockerClient.ListImagesParam.byName(AdbmConstants.requiredImage)).isEmpty()) {
                log.info("Image {} is not available and must be pulled.", AdbmConstants.requiredImage);
                //TODO add confirm
                boolean confirm = true;
                if (Main.isGuiMode()) confirm = JOptionPane.showConfirmDialog(null,
                        "The image " + AdbmConstants.requiredImage + " is not available in Docker and must be pulled before the " + AdbmConstants.appName + " application can be used.\nPressing \"Cancel\" this will terminate the application.",
                        "Image need to be pulled",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION;
                if (confirm) docker.pull(AdbmConstants.requiredImage, new SimpleProgressHandler("Image"));
                else return false;
            } else {
                log.debug(AdbmConstants.requiredImage + " is available.");
            }
            log.debug("Checking that Network {} exists...", AdbmConstants.antidoteDockerNetworkName);
            boolean containsNetwork = false;
            for (Network network : docker.listNetworks()) {
                if (network.name().equals(AdbmConstants.antidoteDockerNetworkName)) {
                    containsNetwork = true;
                    break;
                }
            }
            if (!containsNetwork) {
                log.info("Network {} does not exist and will be created.", AdbmConstants.antidoteDockerNetworkName);
                docker.createNetwork(NetworkConfig.builder().name(AdbmConstants.antidoteDockerNetworkName).driver("bridge").build());
            }
            log.info("Docker initialized!");
            Runtime.getRuntime().addShutdownHook(
                    new Thread(Main::closeApp));
            if (!getNamesOfRunningContainers().isEmpty() && Main.stopContainers) {
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
            log.error("An error occurred while starting Docker.", e);
        }
        return false;
    }

    @Override
    public boolean stop() {
        log.trace("Stopping DockerManager!");
        if (isBuildingImage) {
            //TODO maybe wait or completely fail
        }
        if (docker != null)
        {
            docker.close();
        }
        docker = null;
        return true;
    }

    @Override
    public boolean isReady() {
        boolean isReady = docker != null && !isBuildingImage;
        if (!isReady) log.trace("DockerManager was not ready!");
        return isReady;
    }

    @Override
    public boolean isReadyInfo() {
        if (docker != null && !isBuildingImage) return true;
        if (isBuildingImage)
            log.info("Docker cannot be used because an image is building currently!");
        else
            log.info("The Docker Connection is not started or was stopped!");
        return false;
    }

    @Override
    public boolean isBuildingImage() {
        return isBuildingImage;
    }

    @Override
    public boolean antidoteBenchmarkImageExists() {
        if (!isReady()) return false;
        log.debug("Checking if the {} image already exists...", AdbmConstants.appName);
        List<Image> images;
        try {
            images = docker.listImages(DockerClient.ListImagesParam.byName(AdbmConstants.antidoteDockerImageName),
                    DockerClient.ListImagesParam.allImages());
            log.debug("Existing Images: ", images);
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while checking if an image exists!", e);
            return false;
        }
        return !images.isEmpty();
    }

    @Override
    public synchronized boolean buildAntidoteBenchmarkImage(boolean local) {
        if (!isReady()) return false;
        try {
            isBuildingImage = true;
            log.debug("Checking if an image already exists...");
            List<Image> images = docker
                    .listImages(DockerClient.ListImagesParam.byName(AdbmConstants.antidoteDockerImageName));
            if (images.isEmpty()) {
                log.debug("Image {} does not exist and will be built.", AdbmConstants.antidoteDockerImageName);
            } else {
                log.debug("Image {} already exists and will be rebuilt.", AdbmConstants.antidoteDockerImageName);
                removeAllContainers();
            }
            if (local) Main.getGitManager().checkoutBranch("master");
            DockerfileBuilder.createDockerfile(local);
            File folder = new File(AdbmConstants.dockerfilePath);
            String path = folder.getCanonicalPath();
            if (local) {
                folder = new File(Main.getSettingsManager().getGitRepoLocation()).getParentFile();
                if (folder != null) path = folder.getCanonicalPath();
            }
            log.info("Building Image {}...", AdbmConstants.antidoteDockerImageName);
            docker.build(Paths.get(path), AdbmConstants.antidoteDockerImageName, new SimpleProgressHandler("Image"));
            log.info("Image {} was successfully built.", AdbmConstants.antidoteDockerImageName);
            return true;
        } catch (DockerException | InterruptedException | IOException e) {
            log.error("An error occurred while building an image!", e);
        } finally {
            isBuildingImage = false;
        }
        return false;
    }

    @Override
    public String getCommitOfContainer(String containerName) {
        if (!isReady()) return "";
        return ""; //TODO
    }

    @Override
    public boolean rebuildAntidoteInContainer(String containerName, String commit) {
        //TODO checks and logs correctly
        if (!isReady()) return false;
        String containerId = getContainerId(containerName, true);
        if (containerId.isEmpty()) {
            log.warn("The container with the name {} could not be rebuild because no id was found!");
            return false;
        }
        //TODO if (container.names().contains(name)) {
        log.debug("Rebuilding Antidote in the container (id) {} ({}) with the commit {}", containerName, containerId, commit);
        try {
            String[] stopAntidote = {"/opt/antidote/bin/env", "stop"};
            String execId1 = docker.execCreate(containerId, stopAntidote).id();
            try (LogStream stream = docker.execStart(execId1)) {
                log.debug(stream.readFully());
            }

            String[] pullAntidote = {"bash", "-c", "\"cd /usr/src/antidote && git pull\""};
            String execId2 = docker.execCreate(containerId, pullAntidote).id();
            try (LogStream stream = docker.execStart(execId2)) {
                log.debug(stream.readFully());
            }

            String[] checkoutCommit = {"bash", "-c", format("\"cd /usr/src/antidote && git checkout {}\"", commit)};
            String execId3 = docker.execCreate(containerId, checkoutCommit).id();
            try (LogStream stream = docker.execStart(execId3)) {
                log.debug(stream.readFully());
            }

            String[] makeRel = {"bash", "-c", "\"cd /usr/src/antidote && make rel\""};
            String execId4 = docker.execCreate(containerId, makeRel).id();
            try (LogStream stream = docker.execStart(execId4)) {
                log.debug(stream.readFully());
            }

            String[] copyAntidote = {"cp", "-R", "/usr/src/antidote/_build/default/rel/antidote", "/opt/"};
            String execId5 = docker.execCreate(containerId, copyAntidote).id();
            try (LogStream stream = docker.execStart(execId5)) {
                log.debug(stream.readFully());
            }

            //TODO necessary?
            String[] setUpPorts = {"sed", "-e", "'$i,{kernel, [{inet_dist_listen_min, 9100}, {inet_dist_listen_max, 9100}]}'", "/usr/src/antidote/_build/default/rel/antidote/releases/0.0.1/sys.config", ">", "/opt/antidote/releases/0.0.1/sys.config"};
            String execId6 = docker.execCreate(containerId, setUpPorts).id();
            try (LogStream stream = docker.execStart(execId6)) {
                log.debug(stream.readFully());
            }

            String[] startAndAttach = {"bash", "-c", "/opt/antidote/start_and_attach.sh"};
            String execId7 = docker.execCreate(containerId, startAndAttach).id();
            try (LogStream stream = docker.execStart(execId7)) {
                log.debug(stream.readFully());
            }
            log.info("Container was rebuild and is now restarted!");
            //return true;
            return stopContainer(containerName) && startContainer(containerName);
            //Thread.sleep(500);
            //waitUntilContainerIsReady(containerName);
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while rebuilding Antidote in a container!", e);
            return false;
        }
    }

    @Override
    public boolean runContainer(String containerName) {
        if (!isReady()) return false;
        boolean containerExists = getAllContainersAlsoNonRelevant().stream()
                .anyMatch(container -> container
                        .names() != null && (Objects
                        .requireNonNull(container.names())
                        .contains(containerName) || Objects
                        .requireNonNull(container.names())
                        .contains("/" + containerName)));
        if (containerExists) {
            String containerId = getContainerId(containerName, true);
            if (!containerId.isEmpty()) {
                log.debug("The container {} is already running!", containerName);
                return true;
            } else {
                containerId = getContainerId(containerName, false);
                if (containerId.isEmpty()) {
                    log.error(
                            "The name {} cannot be used to create a new container because another container (that was not built from the {} image) has that name!",
                            containerName,
                            AdbmConstants.appName);
                    return false;
                } else {
                    log.debug("Starting the existing container {}", containerName);
                    return startContainer(containerName);
                }
            }
        } else {
            List<Integer> portList = getUsedHostPorts();
            Optional<Integer> hostPortOptional = AdbmConstants.hostPortList.stream().filter(port -> !portList.contains(port))
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
            portBindings.put(Integer.toString(AdbmConstants.standardClientPort), hostPorts);
            final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings)
                    .networkMode(AdbmConstants.antidoteDockerNetworkName).build();
            log.debug("Creating a new container {}...", containerName);
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(AdbmConstants.antidoteDockerImageName)
                    .hostConfig(hostConfig)
                    .exposedPorts(Integer.toString(AdbmConstants.standardClientPort))
                    .env("SHORT_NAME=true", "NODE_NAME=antidote@" + containerName)
                    .tty(true)
                    .build();
            try {
                docker.createContainer(containerConfig, containerName);
            } catch (DockerException | InterruptedException e) {
                log.error("An error has occurred while running a container!", e);
                return false;
            }
            return startContainer(containerName);
        }
    }

    @Override
    public boolean startContainer(String containerName) {
        if (!isReady()) return false;
        log.info("Starting the container {}...", containerName);
        String containerId = getContainerId(containerName, false);
        try {
            docker.startContainer(containerId);
            log.info("State of the started container (id: {}): {}", containerId,
                    docker.inspectContainer(containerId).state());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while starting a container!", e);
            return false;
        }
        log.info("Started the container {}", containerName);
        waitUntilContainerIsReady(containerName); //TODO better
        return true;
    }

    @Override
    public boolean stopContainer(String containerName) {
        if (!isReady()) return false;
        log.info("Stopping the container {}...", containerName);
        String containerId = getContainerId(containerName, true);
        try {
            docker.stopContainer(containerId, AdbmConstants.secondsToWaitBeforeKilling);
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while stopping a container!", e);
            return false;
        }
        log.info("Stopped the container {}", containerName);
        return true;
    }

    @Override
    public boolean removeContainer(String containerName) {
        if (!isReady()) return false;
        log.debug("Removing the container {}...", containerName);
        String containerId = getContainerId(containerName, false);
        try {
            docker.stopContainer(containerId, AdbmConstants.secondsToWaitBeforeKilling);
            docker.removeContainer(containerId);
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while removing a container!", e);
            return false;
        }
        log.debug("Removed the container {}", containerName);
        return true;
    }

    @Override
    public boolean stopAllContainers() {
        if (!isReady()) return false;
        log.debug("Stopping all containers that were created from the image {}!", AdbmConstants.antidoteDockerImageName);
        try {
            for (Container container : getRunningContainers()) {
                docker.stopContainer(container.id(), AdbmConstants.secondsToWaitBeforeKilling);
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while stopping all containers!", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean removeAllContainers() {
        if (!isReady()) return false;
        log.debug("Removing all containers that were created from the image {}!", AdbmConstants.antidoteDockerImageName);
        try {
            for (Container container : getAllContainers()) {
                docker.stopContainer(container.id(), AdbmConstants.secondsToWaitBeforeKilling);
                docker.removeContainer(container.id());
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while removing all containers!", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean connectContainers(String containerName1, String containerName2) {
        if (!isReady()) return false;
        return false; //TODO
    }

    @Override
    public boolean isAntidoteReady(String containerName) {
        if (!isReady()) return false;
        return false;//TODO
    }

    @Override
    public boolean isContainerRunning(String containerName) {
        return getContainerId(containerName, true).equals("");
    }

    @Override
    public List<String> getNamesOfRunningContainers() {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting running containers...");
        Set<String> containerSet = new HashSet<>();
        for (Container container : getRunningContainers()) {
            String firstName = DockerUtil.getFirstNameOfContainer(container.names(), container.id());
            if (!firstName.isEmpty())
                containerSet.add(firstName);
        }
        log.debug("Running containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Override
    public List<String> getNamesOfNotRunningContainers() {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting exited and created containers...");
        Set<String> containerSet = new HashSet<>();
        List<Container> notRunningContainers = getAllContainers();
        notRunningContainers.removeAll(getRunningContainers());
        for (Container container : notRunningContainers) {
            String firstName = DockerUtil.getFirstNameOfContainer(container.names(), container.id());
            if (!firstName.isEmpty())
                containerSet.add(firstName);
        }
        log.debug("Exited and created containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Override
    public List<String> getNamesOfAllContainers() {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting all containers...");
        Set<String> containerSet = new HashSet<>();
        for (Container container : getAllContainers()) {
            String firstName = DockerUtil.getFirstNameOfContainer(container.names(), container.id());
            if (!firstName.isEmpty())
                containerSet.add(firstName);
        }
        log.debug("All containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Override
    public List<Integer> getHostPortsFromContainer(String containerName) {
        if (!isReady()) return new ArrayList<>();
        String containerId = getContainerId(containerName, false);
        if (containerId.isEmpty()) {
            return new ArrayList<>();
        }
        log.debug("Getting host port from container (id: {}) with the name {}...", containerId, containerName);
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
                if (portBindingMap.containsKey(Integer.toString(AdbmConstants.standardClientPort))) {
                    List<PortBinding> portBindingList = portBindingMap.get(Integer.toString(AdbmConstants.standardClientPort));
                    if (!portBindingList.isEmpty()) {
                        //TODO different IP possible but later
                        List<Integer> portList = portBindingList.stream().map(portBinding -> Integer
                                .parseInt(portBinding.hostPort())).collect(
                                Collectors
                                        .toList()); //TODO Parse Exceptions when Port is not an Integer (Not sure if this is allowed)
                        if (portList.size() > 1) {
                            log.debug(
                                    "The container {} has multiple host ports: {}", containerName, portList);
                        }

                        return portList;
                    }
                }
            }
        }
        log.error("Failed to retrieve the host port of the container {}!", containerName);
        return new ArrayList<>();
    }

    private List<Container> getRunningContainers() {
        if (!isReady()) return new ArrayList<>();
        try {
            return docker.listContainers(DockerClient.ListContainersParam.filter("ancestor", AdbmConstants.antidoteDockerImageName),
                    DockerClient.ListContainersParam.allContainers(),
                    DockerClient.ListContainersParam.withStatusRunning());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while getting all running containers (internal)!", e);
            return new ArrayList<>();
        }
    }

    private List<Container> getAllContainers() {
        if (!isReady()) return new ArrayList<>();
        try {
            return docker.listContainers(DockerClient.ListContainersParam.filter("ancestor", AdbmConstants.antidoteDockerImageName),
                    DockerClient.ListContainersParam.allContainers());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while getting all containers (internal)!", e);
            return new ArrayList<>();
        }
    }

    private List<Container> getAllContainersAlsoNonRelevant() {
        if (!isReady()) return new ArrayList<>();
        try {
            return docker.listContainers(DockerClient.ListContainersParam.allContainers());
        } catch (DockerException | InterruptedException e) {
            log.error(
                    "An error has occurred while getting all containers including those that are not relevant (internal)!",
                    e);
            return new ArrayList<>();
        }
    }

    private void waitUntilContainerIsReady(String name) {
        if (!isReady()) return;
        try {
            //TODO more efficient and rework
            String containerId = getContainerId(name, true);
            if (containerId.isEmpty()) {
                return; //TODO logs
            }
            Thread.sleep(500);
            String logs;
            try (LogStream stream = docker
                    .logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                logs = stream.readFully();
            }
            //log.trace("\n{}", logs);
            int start = logs.lastIndexOf("NODE_NAME");
            while (!logs.substring(start).contains("Application antidote started on node")) {
                Thread.sleep(500);
                log.debug("The Antidote database is still not fully started!");
                try (LogStream stream = docker
                        .logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                    logs = stream.readFully();
                }
                start = logs.lastIndexOf("NODE_NAME");
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while waiting for the container!", e);
        }
    }

    /**
     * Gets the list of used host ports.
     * This is used to make sure that every container has a unique host port.
     *
     * @return The list of used host ports.
     */
    private List<Integer> getUsedHostPorts() {
        List<Integer> portList = new ArrayList<>();
        if (!isReady()) return portList;
        for (Container container : getAllContainers()) {
            portList.addAll(getHostPortsFromContainer(DockerUtil.getFirstNameOfContainer(container.names(), container.id())));
        }
        if (new HashSet<>(portList).size() < portList.size()) {
            log.error("The list of used host ports contains duplicates which can lead to errors!");
        }
        log.debug("Full list of host ports: {}", portList);
        return portList;
    }

    private String getContainerId(String name, boolean mustBeRunning) {
        if (!isReady()) return "";
        try {
            List<Container> adbmContainersWithName;
            if (mustBeRunning) {
                adbmContainersWithName = docker.listContainers(DockerClient.ListContainersParam.filter("name", name),
                        DockerClient.ListContainersParam
                                .filter("ancestor", AdbmConstants.antidoteDockerImageName),
                        DockerClient.ListContainersParam.allContainers(),
                        DockerClient.ListContainersParam.withStatusRunning());
            } else {
                adbmContainersWithName = docker.listContainers(DockerClient.ListContainersParam.filter("name", name),
                        DockerClient.ListContainersParam
                                .filter("ancestor", AdbmConstants.antidoteDockerImageName),
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

}
