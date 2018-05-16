package adbm.docker.managers;

import adbm.docker.IDockerManager;
import adbm.docker.util.DockerUtil;
import adbm.docker.util.DockerfileBuilder;
import adbm.main.Main;
import adbm.util.AdbmConstants;
import adbm.util.EverythingIsNonnullByDefault;
import adbm.util.SimpleProgressHandler;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static adbm.util.helpers.FormatUtil.format;

@EverythingIsNonnullByDefault
public class DockerManager implements IDockerManager
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
    //TODO different IP possible not just local host
    // TODO Containers are not allowed to have the same name! Therefore the returned list must have only one element!

    /**
     * The Docker Client.
     * Is set to null when {@link #stop()} is called.
     */
    @Nullable
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
    public static DockerManager getInstance()
    {
        return instance;
    }

    /**
     * Private constructor to prevent creation of other instances.
     */
    private DockerManager()
    {

    }

    @Override
    public boolean start()
    {
        return start(null);
    }

    @Override
    public boolean start(@Nullable String uri, String... args)
    {
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
            log.debug("Checking that image {} is available...", AdbmConstants.REQUIRED_IMAGE);
            if (docker.listImages(DockerClient.ListImagesParam.byName(AdbmConstants.REQUIRED_IMAGE)).isEmpty()) {
                log.info("Image {} is not available and must be pulled.", AdbmConstants.REQUIRED_IMAGE);
                //TODO add confirm
                boolean confirm = true;
                if (Main.isGuiMode()) confirm = JOptionPane.showConfirmDialog(null,
                                                                              "The image " + AdbmConstants.REQUIRED_IMAGE + " is not available in Docker and must be pulled before the " + AdbmConstants.APP_NAME + " application can be used.\nPressing \"Cancel\" this will terminate the application.",
                                                                              "Image needs to be pulled",
                                                                              JOptionPane.OK_CANCEL_OPTION,
                                                                              JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION;
                if (confirm) docker.pull(AdbmConstants.REQUIRED_IMAGE, new SimpleProgressHandler("Image"));
                else return false;
            }
            else {
                log.debug(AdbmConstants.REQUIRED_IMAGE + " is available.");
            }
            log.debug("Checking that image {} is available...", AdbmConstants.AD_DEFAULT_IMAGE_NAME);
            if (docker.listImages(DockerClient.ListImagesParam.byName(AdbmConstants.AD_DEFAULT_IMAGE_NAME)).isEmpty()) {
                log.info("Image {} is not available and must be pulled.", AdbmConstants.AD_DEFAULT_IMAGE_NAME);
                //TODO add confirm
                boolean confirm = true;
                if (Main.isGuiMode()) confirm = JOptionPane.showConfirmDialog(null,
                                                                              "The image " + AdbmConstants.AD_DEFAULT_IMAGE_NAME + " is not available in Docker and must be pulled before the " + AdbmConstants.APP_NAME + " application can be used.\nPressing \"Cancel\" this will terminate the application.",
                                                                              "Image needs to be pulled",
                                                                              JOptionPane.OK_CANCEL_OPTION,
                                                                              JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION;
                if (confirm) docker.pull(AdbmConstants.AD_DEFAULT_IMAGE_NAME, new SimpleProgressHandler("Image"));
                else return false;
            }
            else {
                log.debug(AdbmConstants.AD_DEFAULT_IMAGE_NAME + " is available.");
            }
            log.debug("Checking that Network {} exists...", AdbmConstants.ADBM_DOCKER_NETWORK_NAME);
            boolean containsNetwork = false;
            for (Network network : docker.listNetworks()) {
                if (network.name().equals(AdbmConstants.ADBM_DOCKER_NETWORK_NAME)) {
                    containsNetwork = true;
                    break;
                }
            }
            if (!containsNetwork) {
                log.info("Network {} does not exist and will be created.", AdbmConstants.ADBM_DOCKER_NETWORK_NAME);
                docker.createNetwork(
                        NetworkConfig.builder().name(AdbmConstants.ADBM_DOCKER_NETWORK_NAME).driver("bridge").build());
            }
            log.info("Docker initialized!");

            if (!getNamesOfRunningContainers().isEmpty() && Main.stopContainers) {
                log.error("The Antidote Benchmark containers cannot be running when the DockerManager starts!" +
                                  "\nPlease restart Docker manually!");
                //JOptionPane.showMessageDialog(null,"");TODO
                return false;
            }
            if (!antidoteBenchmarkImageExists()) {
                buildAntidoteBenchmarkImage();
            }
            return true;
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            log.error("An error occurred while starting Docker.", e);
        }
        return false;
    }

    @Override
    public boolean stop()
    {
        log.trace("Stopping DockerManager!");
        if (isBuildingImage) {
            //TODO maybe wait or completely fail
        }
        if (docker != null) {
            docker.close();
        }
        docker = null;
        return true;
    }

    @Override
    public boolean isReady()
    {
        boolean isReady = docker != null && !isBuildingImage;
        if (!isReady) log.trace("DockerManager was not ready!");
        return isReady;
    }

    @Override
    public boolean isReadyInfo()
    {
        if (docker != null && !isBuildingImage) return true;
        if (isBuildingImage)
            log.info("Docker cannot be used because an image is building currently!");
        else
            log.info("The Docker Connection is not started or was stopped!");
        return false;
    }

    @Override
    public boolean isBuildingImage()
    {
        return isBuildingImage;
    }

    @Override
    public boolean antidoteBenchmarkImageExists()
    {
        if (!isReady()) return false;
        log.debug("Checking if the {} image already exists...", AdbmConstants.APP_NAME);
        List<Image> images;
        try {
            images = docker.listImages(DockerClient.ListImagesParam.byName(AdbmConstants.ADBM_DOCKER_IMAGE_NAME),
                                       DockerClient.ListImagesParam.allImages());
            log.debug("Existing Images: ", images);
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while checking if an image exists!", e);
            return false;
        }
        return !images.isEmpty();
    }

    @Override
    public boolean buildAntidoteBenchmarkImage()
    {
        if (AdbmConstants.AD_DEFAULT_IMAGE_NAME.equals(AdbmConstants.ADBM_DOCKER_IMAGE_NAME)) return false;
        if (!isReady()) return false;
        try {
            isBuildingImage = true;
            log.debug("Checking if an image already exists...");
            List<Image> images = docker
                    .listImages(DockerClient.ListImagesParam.byName(AdbmConstants.ADBM_DOCKER_IMAGE_NAME));
            if (images.isEmpty()) {
                log.debug("Image {} does not exist and will be built.", AdbmConstants.ADBM_DOCKER_IMAGE_NAME);
            }
            else {
                log.debug("Image {} already exists and will be rebuilt.", AdbmConstants.ADBM_DOCKER_IMAGE_NAME);
                removeAllContainers();
            }
            if (!DockerfileBuilder.createDockerfile()) {
                return false;
            }
            File folder = new File(AdbmConstants.DOCKER_FOLDER_PATH);
            log.info("Building Image {} from {}...", AdbmConstants.ADBM_DOCKER_IMAGE_NAME, folder.getCanonicalPath());
            docker.build(folder.toPath(), AdbmConstants.ADBM_DOCKER_IMAGE_NAME, new SimpleProgressHandler("Image"));
            log.info("Image {} was successfully built.", AdbmConstants.ADBM_DOCKER_IMAGE_NAME);
            return true;
        } catch (DockerException | InterruptedException | IOException e) {
            log.error("An error occurred while building an image!", e);
            return false;
        } finally {
            isBuildingImage = false;
        }
    }

    @Override
    public List<String> performExec(String containerName, boolean attachOutput, String[]... args)
    {
        List<String> outputs = new ArrayList<>();
        if (!isReady()) return outputs;
        String containerId = getContainerId(containerName, true);
        if (containerId.isEmpty()) {
            log.warn(
                    "An exec could not be performed on the container with the name {} because no container id was found!");
            return outputs;
        }
        try {
            for (String[] arg : args) {
                String logCommand = "";
                for (String a : arg) {
                    logCommand = logCommand + " " + a;
                }
                log.trace("Running command: {}", logCommand);
                String execId;
                if (attachOutput) {
                    execId = docker.execCreate(containerId, arg, DockerClient.ExecCreateParam.attachStdout(),
                                               DockerClient.ExecCreateParam.attachStderr()).id();
                }
                else {
                    execId = docker.execCreate(containerId, arg).id();
                }
                try (LogStream stream = docker.execStart(execId)) {
                    String output = stream.readFully();
                    if (output.isEmpty() && attachOutput) {
                        log.trace("No Output!");
                    }
                    else {
                        log.trace(output);
                    }
                    outputs.add(output);
                }
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while performing an exec on a container!", e);
        }

        return outputs;
    }

    @Override
    public boolean rebuildAntidoteInContainer(String containerName, String commit)
    {
        if (!isReady()) return false;
        log.debug("Rebuilding Antidote in the container {} with the commit {}", containerName,
                  commit);
        String[] stopAntidote = {"/opt/antidote/bin/env", "stop"};
        //String[] pullAntidote = {"bash", "-c", "cd /usr/src/antidote; git pull"};
        String[] checkoutCommit = {"bash", "-c", format("cd /usr/src/antidote; git checkout {}", commit)};
        String[] makeRel = {"bash", "-c", "cd /usr/src/antidote; make rel"};
        //String[] magicStop = {"bash", "-c", "ps aw o pid,command | awk '$2 ~ /^\\/opt\\/antidote\\// {print $1}' | xargs kill -9"};
        String[] deleteAntidoteData = {"bash", "-c", "rm -rf /opt/antidote/data/*"};
        String[] copyAntidote = {"bash", "-c", "cp -R /usr/src/antidote/_build/default/rel/antidote /opt/"};
        String[] setUpPorts = {"bash", "-c", "sed -e '$i,{kernel, [{inet_dist_listen_min, 9100}, {inet_dist_listen_max, 9100}]}' /usr/src/antidote/_build/default/rel/antidote/releases/0.0.1/sys.config > /opt/antidote/releases/0.0.1/sys.config"};
        String[] startAndAttach = {"bash", "-c", "/opt/antidote/start_and_attach.sh"};
        performExec(containerName, true, stopAntidote, /*pullAntidote,*/ checkoutCommit, makeRel, /*magicStop,*/
                    deleteAntidoteData, copyAntidote,
                    setUpPorts);
        performExec(containerName, false, startAndAttach);
        log.info("Container was rebuild and is now restarted!");
        return waitUntilContainerIsReady(containerName, true);
        //return true;
        //return stopContainer(containerName) && startContainer(containerName);
        //Thread.sleep(500);
        //waitUntilContainerIsReady(containerName);
    }

    @Override
    public boolean runContainer(String containerName)
    {
        if (!isReady()) return false;
        log.trace("Trying to run Container {}", containerName);
        boolean containerExists = false;
        String altContainerName = "/" + containerName;
        for (Container container : getAllContainersAlsoNonRelevant()) {
            List<String> containerNames = container.names();
            if (containerNames != null && (containerNames.contains(containerName) || containerNames.contains(altContainerName))) {
                containerExists = true;
            }
        }
        if (containerExists) {
            String containerId = getContainerId(containerName, true);
            if (!containerId.isEmpty()) {
                log.debug("The container {} is already running!", containerName);
                return true;
            }
            else {
                containerId = getContainerId(containerName, false);
                if (containerId.isEmpty()) {
                    log.error(
                            "The name {} cannot be used to create a new container because another container (that was not built from the {} image) has that name!",
                            containerName,
                            AdbmConstants.APP_NAME);
                    return false;
                }
                else {
                    log.debug("Starting the existing container {}", containerName);
                    return startContainer(containerName);
                }
            }
        }
        else {
            List<Integer> portList = getUsedHostPorts();
            Optional<Integer> hostPortOptional = AdbmConstants.ADBM_HOST_PORT_LIST.stream().filter(port -> !portList
                    .contains(port))
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
            portBindings.put(Integer.toString(AdbmConstants.STANDARD_ADBM_CLIENT_PORT), hostPorts);
            final HostConfig hostConfig = HostConfig.builder().portBindings(portBindings)
                                                    .networkMode(AdbmConstants.ADBM_DOCKER_NETWORK_NAME).build();
            log.debug("Creating a new container {}...", containerName);
            ContainerConfig containerConfig = ContainerConfig.builder()
                                                             .image(AdbmConstants.ADBM_DOCKER_IMAGE_NAME)
                                                             .hostConfig(hostConfig)
                                                             .exposedPorts(Integer.toString(
                                                                     AdbmConstants.STANDARD_ADBM_CLIENT_PORT))
                                                             .env("SHORT_NAME=true",
                                                                  "NODE_NAME=antidote@" + containerName)
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
    public boolean startContainer(String containerName)
    {
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
        waitUntilContainerIsReady(containerName, false); //TODO better
        return true;
    }

    @Override
    public boolean stopContainer(String containerName)
    {
        if (!isReady()) return false;
        log.info("Stopping the container {}...", containerName);
        String containerId = getContainerId(containerName, true);
        try {
            docker.stopContainer(containerId, AdbmConstants.SECONDS_TO_WAIT_BEFORE_KILLING);
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while stopping a container!", e);
            return false;
        }
        log.info("Stopped the container {}", containerName);
        return true;
    }

    @Override
    public boolean removeContainer(String containerName)
    {
        if (!isReady()) return false;
        log.debug("Removing the container {}...", containerName);
        String containerId = getContainerId(containerName, false);
        try {
            docker.stopContainer(containerId, AdbmConstants.SECONDS_TO_WAIT_BEFORE_KILLING);
            docker.removeContainer(containerId);
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while removing a container!", e);
            return false;
        }
        log.debug("Removed the container {}", containerName);
        return true;
    }

    @Override
    public boolean stopAllContainers()
    {
        if (!isReady()) return false;
        log.debug("Stopping all containers that were created from the image {}!", AdbmConstants.ADBM_DOCKER_IMAGE_NAME);
        try {
            for (Container container : getRunningContainers()) {
                docker.stopContainer(container.id(), AdbmConstants.SECONDS_TO_WAIT_BEFORE_KILLING);
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while stopping all containers!", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean removeAllContainers()
    {
        if (!isReady()) return false;
        log.debug("Removing all containers that were created from the image {}!", AdbmConstants.ADBM_DOCKER_IMAGE_NAME);
        try {
            for (Container container : getAllContainers()) {
                docker.stopContainer(container.id(), AdbmConstants.SECONDS_TO_WAIT_BEFORE_KILLING);
                docker.removeContainer(container.id());
            }
        } catch (DockerException | InterruptedException e) {
            log.error("An error occurred while removing all containers!", e);
            return false;
        }
        return true;
    }

    @Override
    public String getCommitOfContainer(String containerName)
    {
        if (!isReady()) return "";
        String[] checkCommit = {"bash", "-c", "cd /usr/src/antidote; git rev-parse HEAD"};
        List<String> commitId = performExec(containerName, true, checkCommit);
        int size = commitId.size();
        if (size == 1) {
            return commitId.get(0);
        }
        else if (size == 0) {
            log.warn("The container has no valid commit!");
            return "";
        }
        else {
            log.warn("The container has multiple commits!");
            return commitId.get(0);
        }
    }

    @Override
    public boolean connectContainers(String containerName1, String containerName2)
    {
        if (!isReady()) return false;
        //String[] connectContainer = {"bash", "-c", "git rev-parse HEAD"};
        //performExec(containerName1, connectContainer);
        return false; //TODO
    }

    @Override
    public boolean isAntidoteReady(String containerName)
    {
        if (!isReady()) return false;
        return false;//TODO
    }

    @Override
    public boolean isContainerRunning(String containerName)
    {
        return !getContainerId(containerName, true).equals("");
    }

    @Override
    public List<String> getNamesOfRunningContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting running containers...");
        Set<String> containerSet = new HashSet<>();
        for (Container container : getRunningContainers()) {
            List<String> containerNames = container.names();
            if (containerNames != null) {
                String firstName = DockerUtil.getFirstNameOfContainer(containerNames, container.id());
                if (!firstName.isEmpty())
                    containerSet.add(firstName);
            }
        }
        log.debug("Running containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Override
    public List<String> getNamesOfNotRunningContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting exited and created containers...");
        Set<String> containerSet = new HashSet<>();
        List<Container> notRunningContainers = getAllContainers();
        notRunningContainers.removeAll(getRunningContainers());
        for (Container container : notRunningContainers) {
            List<String> containerNames = container.names();
            if (containerNames != null) {
                String firstName = DockerUtil.getFirstNameOfContainer(containerNames, container.id());
                if (!firstName.isEmpty())
                    containerSet.add(firstName);
            }
        }
        log.debug("Exited and created containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Override
    public List<String> getNamesOfAllContainers()
    {
        if (!isReady()) return new ArrayList<>();
        log.debug("Getting all containers...");
        Set<String> containerSet = new HashSet<>();
        for (Container container : getAllContainers()) {
            List<String> containerNames = container.names();
            if (containerNames != null) {
                String firstName = DockerUtil.getFirstNameOfContainer(containerNames, container.id());
                if (!firstName.isEmpty())
                    containerSet.add(firstName);
            }
        }
        log.debug("All containers: {}", containerSet);
        return new ArrayList<>(containerSet);
    }

    @Override
    public List<Integer> getHostPortsFromContainer(String containerName)
    {
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
            Map<String, List<PortBinding>> portBindingMap = config != null ? config.portBindings() : null;
            if (portBindingMap != null) {
                if (portBindingMap.containsKey(Integer.toString(AdbmConstants.STANDARD_ADBM_CLIENT_PORT))) {
                    List<PortBinding> portBindingList = portBindingMap
                            .get(Integer.toString(AdbmConstants.STANDARD_ADBM_CLIENT_PORT));
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

    private List<Container> getRunningContainers()
    {
        try {
            return docker.listContainers(
                    DockerClient.ListContainersParam.filter("ancestor", AdbmConstants.ADBM_DOCKER_IMAGE_NAME),
                    DockerClient.ListContainersParam.allContainers(),
                    DockerClient.ListContainersParam.withStatusRunning());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while getting all running containers (internal)!", e);
            return new ArrayList<>();
        }
    }

    private List<Container> getAllContainers()
    {
        try {
            return docker.listContainers(
                    DockerClient.ListContainersParam.filter("ancestor", AdbmConstants.ADBM_DOCKER_IMAGE_NAME),
                    DockerClient.ListContainersParam.allContainers());
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while getting all containers (internal)!", e);
            return new ArrayList<>();
        }
    }

    private List<Container> getAllContainersAlsoNonRelevant()
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

    private boolean waitUntilContainerIsReady(String name, boolean rebuilt)
    {
        try {
            //TODO more efficient and rework
            String containerId = getContainerId(name, true);
            if (containerId.isEmpty()) {
                return false; //TODO logs
            }
            Thread.sleep(500);
            String logs;
            try (LogStream stream = docker
                    .logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr()))
            {
                logs = stream.readFully();
            }
            //log.trace("\n{}", logs);
            int start;
            if (rebuilt) {
                start = logs.lastIndexOf("Erlang closed the connection");
            }
            else {
                start = logs.lastIndexOf("NODE_NAME");
            }
            while (!logs.substring(start).contains("Application antidote started on node")) {
                Thread.sleep(1000);
                log.debug("The Antidote database is starting...");
                try (LogStream stream = docker
                        .logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr()))
                {
                    logs = stream.readFully();
                }
                if (rebuilt) {
                    start = logs.lastIndexOf("Erlang closed the connection");
                }
                else {
                    start = logs.lastIndexOf("NODE_NAME");
                }
            }
            return true;
        } catch (DockerException | InterruptedException e) {
            log.error("An error has occurred while waiting for the container!", e);
            return false;
        }
    }

    /**
     * Gets the list of used host ports.
     * This is used to make sure that every container has a unique host port.
     *
     * @return The list of used host ports.
     */
    private List<Integer> getUsedHostPorts()
    {
        List<Integer> portList = new ArrayList<>();
        //if (!isReady()) return portList;
        for (Container container : getAllContainers()) {
            List<String> containerNames = container.names();
            if (containerNames != null)
                portList.addAll(
                        getHostPortsFromContainer(DockerUtil.getFirstNameOfContainer(containerNames, container.id())));
        }
        if (new HashSet<>(portList).size() < portList.size()) {
            log.error("The list of used host ports contains duplicates which can lead to errors!");
        }
        log.debug("Full list of host ports: {}", portList);
        return portList;
    }

    private String getContainerId(String name, boolean mustBeRunning)
    {
        //if (!isReady()) return "";
        try {
            List<Container> adbmContainersWithName;
            if (mustBeRunning) {
                adbmContainersWithName = docker.listContainers(DockerClient.ListContainersParam.filter("name", name),
                                                               DockerClient.ListContainersParam
                                                                       .filter("name", "/" + name),
                                                               DockerClient.ListContainersParam
                                                                       .filter("ancestor",
                                                                               AdbmConstants.ADBM_DOCKER_IMAGE_NAME),
                                                               DockerClient.ListContainersParam.allContainers(),
                                                               DockerClient.ListContainersParam.withStatusRunning());
            }
            else {
                adbmContainersWithName = docker.listContainers(DockerClient.ListContainersParam.filter("name", name),
                                                               DockerClient.ListContainersParam
                                                                       .filter("name", "/" + name),
                                                               DockerClient.ListContainersParam
                                                                       .filter("ancestor",
                                                                               AdbmConstants.ADBM_DOCKER_IMAGE_NAME),
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
