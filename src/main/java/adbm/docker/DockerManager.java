package adbm.docker;

import adbm.git.GitManager;
import adbm.util.SimpleProgressHandler;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;
import com.spotify.docker.client.messages.Container;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class DockerManager
{

    private static DockerClient docker;

    private static final String requiredImage = "erlang:19";

    private static final String antidoteDockerNetworkName = "antidote_ntwk";

    private static final String antidoteDockerImageName = "antidotedb/benchmark";

    private static final int standardClientPort = 8087;

    private static final int secondsToWaitBeforeKilling = 10;

    private static final int commitHashLength = 7;

    public static boolean isReady()
    {
        if (docker != null) return true;
        System.out.println("The connection to Docker is not ready!");
        System.out.println("Please start the Docker connection again!");
        return false;
    }

    public static boolean isReadyNoText()
    {
        return docker != null;
    }

    private static final List<Integer> hostPortList = new ArrayList<>(
            Arrays.asList(8087, 8088, 8089, 8090, 8091, 8092));

    // Containers are not allowed to have the same name! Therefore the returned list must have only one element!

    //TODO change that remote Docker can be used instead of local
    public static boolean startDocker()
    {
        if (!GitManager.isReady()) return false;
        try {
            docker = DefaultDockerClient.fromEnv().readTimeoutMillis(3600000).build();
            System.out.println("Checking that Image " + requiredImage + " is available...");
            if (docker.listImages(DockerClient.ListImagesParam.byName(requiredImage)).isEmpty()) {
                System.out.println("Image " + requiredImage + " is not available and will be pulled.");
                docker.pull(requiredImage, new SimpleProgressHandler("Image"));
            }
            else {
                System.out.println(requiredImage + " is available.");
            }
            System.out.println("Checking that Network " + antidoteDockerNetworkName + " exists...");
            boolean containsNetwork = false;
            for (Network network : docker.listNetworks()) {
                if (network.name().equals(antidoteDockerNetworkName)) {
                    containsNetwork = true;
                    break;
                }
            }
            if (!containsNetwork) {
                System.out.println("Network " + antidoteDockerNetworkName + " does not exist and will be created.");
                docker.createNetwork(NetworkConfig.builder().name(antidoteDockerNetworkName).driver("bridge").build());
            }
            System.out.println("Docker initialized!");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean IsBuildingImage = false;

    public static boolean buildBenchmarkImages(boolean local)
    {
        if (!isReady()) return false;
        try {
            System.out.println("Checking if an Image for the master branch exists...");
            List<Image> images = docker
                    .listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName));
            if (images.isEmpty()) {
                System.out.println("Image " + antidoteDockerImageName + " does not exist and must be built.");
                DockerfileBuilder.createDockerfile(false);
                File folder = new File("Dockerfile");
                String path = folder.getCanonicalPath();
                System.out.println("Building Image " + antidoteDockerImageName + " ...");
                docker.build(Paths.get(path), antidoteDockerImageName,
                             new SimpleProgressHandler("Image"));
                System.out.println("Image " + antidoteDockerImageName + " was successfully built.");
            }
            else {
                System.out
                        .println("Image " + antidoteDockerImageName + " already exists and does not have to be built.");
            }
            /*
            for (String commit : MapDBManager.getBenchmarkCommits()) {
                System.out.println("Checking if an Image for the Commit " + commit + " exists...");
                String imageTag = ":" + ObjectId.fromString(commit).abbreviate(commitHashLength).name();
                List<Image> images = docker
                        .listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName + imageTag));
                if (images.isEmpty()) {
                    System.out.println("Image " + antidoteDockerImageName + imageTag + " does not exist and must be built.");
                    if (local) GitManager.checkoutCommit(commit);
                    DockerfileBuilder.createDockerfile(local);
                    File folder = new File("Dockerfile");
                    String path = folder.getCanonicalPath();
                    if (local) {
                        folder = new File(MapDBManager.getAppSetting(MapDBManager.GitRepoLocationSetting)).getParentFile();
                        if (folder != null) path = folder.getCanonicalPath();
                    }
                    System.out.println("Building Image " + antidoteDockerImageName + imageTag + " ...");
                    docker.build(Paths.get(path), antidoteDockerImageName + imageTag,
                                 new SimpleProgressHandler("Image"));
                    System.out.println("Image " + antidoteDockerImageName + imageTag + " was successfully built.");
                }
                else {
                    System.out.println("Image " + antidoteDockerImageName + imageTag + " already exists and does not have to be built.");
                }
            }
            */
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String normalizeName(String containerNameFromDocker)
    {
        if (containerNameFromDocker.startsWith("/")) return containerNameFromDocker.substring(1);
        else return containerNameFromDocker;
    }

    //TODO automatic container removal
    public static boolean runContainer(String name)
    {
        System.out.println("Running the container \"" + name + "\"...");
        List<String> containerList = getRunningContainers();
        if (containerList.contains(name)) {
            System.out.println("The container \"" + name + "\" is already running!");
            System.out.println("The container \"" + name + "\" is restarted now!");
            stopContainer(name);
        }
        containerList = getNotRunningContainers();
        if (containerList.contains(name)) {
            startContainer(name);
            return true;
        }
        List<Integer> portList = getUsedHostPorts();
        if (portList.containsAll(hostPortList)) {
            System.out.println("Port list contains all ports from the host port list!\n" +
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
            if (docker != null) {
                System.out.println("Creating a new container \"" + name + "\"...");
                ContainerConfig containerConfig = ContainerConfig.builder()
                                                                 .image(antidoteDockerImageName)
                                                                 .hostConfig(hostConfig)
                                                                 .exposedPorts(Integer.toString(standardClientPort))
                                                                 .env("SHORT_NAME=true", "NODE_NAME=antidote@" + name)
                                                                 .tty(true)
                                                                 .build();
                ContainerCreation createdContainer = docker.createContainer(containerConfig, name);
                System.out.println("Created new container \"" + name + "\"!");
                System.out.println("Starting the new container \"" + name + "\"...");
                docker.startContainer(createdContainer.id());
                System.out.println("Started the new container \"" + name + "\"!");
                System.out.println("Container ID: " + createdContainer.id());
                System.out.println("State: " + docker.inspectContainer(createdContainer.id()).state());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void removeContainer(String name)
    {
        System.out.println("Removing the container \"" + name + "\"...");
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                    docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                    docker.removeContainer(container.id());
                    System.out.println("Removed the container \"" + name + "\"!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopContainer(String name)
    {
        System.out.println("Stopping the container \"" + name + "\"...");
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                    docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                    System.out.println("Stopped the container \"" + name + "\"!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startContainer(String name)
    {
        System.out.println("Starting the container \"" + name + "\"...");
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.filter("name", name))) {
                    docker.startContainer(container.id());
                    System.out.println("Started the container \"" + name + "\"!");
                    System.out.println("Container ID: " + container.id());
                    System.out.println("State: " + docker.inspectContainer(container.id()).state());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getRunningContainers()
    {
        System.out.println("Getting running containers...");
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.withStatusRunning())) {
                    List<String> nameList = container.names();
                    if (nameList == null) {
                        System.out.println("ERROR: The container has no name!");
                    }
                    else {
                        if (nameList.size() > 0) {
                            String firstName = normalizeName(nameList.get(0));
                            if (nameList.size() > 1) {
                                StringBuilder nameString = new StringBuilder();
                                for (String name : nameList) {
                                    nameString.append(normalizeName(name) + ", ");
                                }
                                String names = nameString.toString();
                                if (names.length() > 2) {
                                    System.out.println(
                                            "A container has multiple names: " + names
                                                    .substring(0, names.length() - 3));
                                    System.out.println("Using the first name: " + firstName);
                                }
                            }
                            containerSet.add(firstName);
                        }
                        else {
                            System.out.println("ERROR: The container has no name!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Running containers:\n" + containerSet);
        return new ArrayList<>(containerSet);
    }

    public static List<String> getNotRunningContainers()
    {
        System.out.println("Getting exited and created containers...");
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.withStatusExited()
                        )
                        ) {
                    List<String> nameList = container.names();
                    if (nameList == null) {
                        System.out.println("ERROR: The container has no name!");
                    }
                    else {
                        if (nameList.size() > 0) {
                            String firstName = normalizeName(nameList.get(0));
                            if (nameList.size() > 1) {
                                StringBuilder nameString = new StringBuilder();
                                for (String name : nameList) {
                                    nameString.append(normalizeName(name) + ", ");
                                }
                                String names = nameString.toString();
                                if (names.length() > 2) {
                                    System.out.println(
                                            "A container has multiple names: " + names
                                                    .substring(0, names.length() - 3));
                                    System.out.println("Using the first name: " + firstName);
                                }
                            }
                            containerSet.add(firstName);
                        }
                        else {
                            System.out.println("ERROR: The container has no name!");
                        }
                    }
                }
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.withStatusCreated())) {
                    List<String> nameList = container.names();
                    if (nameList == null) {
                        System.out.println("ERROR: The container has no name!");
                    }
                    else {
                        if (nameList.size() > 0) {
                            String firstName = normalizeName(nameList.get(0));
                            if (container.names().size() > 1) {
                                StringBuilder nameString = new StringBuilder();
                                for (String name : nameList) {
                                    nameString.append(normalizeName(name) + ", ");
                                }
                                String names = nameString.toString();
                                if (names.length() > 2) {
                                    System.out.println(
                                            "A container has multiple names: " + names
                                                    .substring(0, names.length() - 3));
                                    System.out.println("Using the first name: " + firstName);
                                }
                            }
                            containerSet.add(firstName);
                        }
                        else {
                            System.out.println("ERROR: The container has no name!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Exited and created containers:\n" + containerSet);
        return new ArrayList<>(containerSet);
    }

    public static List<String> getAllContainers()
    {
        System.out.println("Getting all containers...");
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers())) {
                    List<String> nameList = container.names();
                    if (nameList == null) {
                        System.out.println("ERROR: The container has no name!");
                    }
                    else {
                        if (nameList.size() > 0) {
                            String firstName = normalizeName(nameList.get(0));
                            if (nameList.size() > 1) {
                                StringBuilder nameString = new StringBuilder();
                                for (String name : nameList) {
                                    nameString.append(normalizeName(name) + ", ");
                                }
                                String names = nameString.toString();
                                if (names.length() > 2) {
                                    System.out.println(
                                            "A container has multiple names: " + names
                                                    .substring(0, names.length() - 3));
                                    System.out.println("Using the first name: " + firstName);
                                }
                            }
                            containerSet.add(firstName);
                        }
                        else {
                            System.out.println("ERROR: The container has no name!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("All containers:\n" + containerSet);
        return new ArrayList<>(containerSet);
    }

    public static int getHostPortFromContainer(String name)
    {
        System.out.println("Getting host port form container \"" + name + "\"...");
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                    List<Integer> portList = new ArrayList<>();
                    List<Container.PortMapping> portMappingList = container.ports();
                    if (portMappingList == null) {
                        System.out.println("ERROR: The container has no host port!");
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
                                    nameString.append(port + ", ");
                                }
                                String names = nameString.toString();
                                if (names.length() > 2) {
                                    System.out.println(
                                            "The container \"" + name + "\" has multiple host ports: " + names
                                                    .substring(0, names.length() - 3));
                                    System.out.println("Using the first host port: " + firstPort);
                                }
                            }
                            System.out.println("The host port of the container \"" + name + "\" is " + firstPort);
                            return firstPort;
                        }
                        else {
                            System.out.println("ERROR: The container has no host port!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Integer> getUsedHostPorts()
    {
        List<Integer> portList = new ArrayList<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName))) {
                    List<Integer> singlePortList = new ArrayList<>();
                    List<Container.PortMapping> portMappingList = container.ports();
                    if (portMappingList == null) {
                        System.out.println("ERROR: The container has no host port!");
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
                                    nameString.append(port + ", ");
                                }
                                String names = nameString.toString();
                                if (names.length() > 2) {
                                    System.out.println(
                                            "The container has multiple host ports: " + names
                                                    .substring(0, names.length() - 3));
                                    System.out.println("Using the first host port: " + firstPort);
                                }
                            }
                            portList.add(firstPort);
                        }
                        else {
                            System.out.println("ERROR: The container has no host port!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return portList;
    }


}
