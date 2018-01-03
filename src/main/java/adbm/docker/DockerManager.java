package adbm.docker;

import adbm.git.GitManager;
import adbm.settings.MapDBManager;
import adbm.util.SimpleProgressHandler;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import com.spotify.docker.client.messages.Container;
import org.eclipse.jgit.lib.ObjectId;

import java.io.File;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    public static boolean buildBenchmarkImages(boolean local)
    {
        if (!isReady()) return false;
        try {
            for (String commit : MapDBManager.getBenchmarkCommits()) {
                System.out.println("Checking if an Image for the Commit " + commit + " exists...");
                String imageTag = ":" + ObjectId.fromString(commit).abbreviate(commitHashLength).name();
                List<Image> images = docker
                        .listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName + imageTag));
                if (images.isEmpty()) {
                    System.out.println("Image " + antidoteDockerImageName + imageTag + " does not exist and must be built.");
                    if (local) GitManager.checkoutCommit(commit);
                    DockerfileBuilder.createDockerfile(local, commit);
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    //TODO automatic container removal
    public static boolean runContainer(String name)
    {
        List<String> containerList = getRunningContainers();
        if (containerList.contains(name)) {
            return false;
        }
        containerList = getNotRunningContainers();
        if (containerList.contains(name)) {
            startContainer(name);
            return true;
        }
        List<Integer> portList = getUsedHostPorts();
        if (portList.containsAll(hostPortList)) return false;
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
                System.out.println("Creating Container!");
                ContainerConfig containerConfig = ContainerConfig.builder()
                                                                 .image(antidoteDockerImageName)
                                                                 .hostConfig(hostConfig)
                                                                 .exposedPorts(Integer.toString(standardClientPort))
                                                                 .env("SHORT_NAME=true", "NODE_NAME=antidote@" + name)
                                                                 .tty(true)
                                                                 .openStdin(true)
                                                                 .build();
                ContainerCreation createdContainer = docker.createContainer(containerConfig, name);
                System.out.println("Container created!");
                docker.startContainer(createdContainer.id());
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void removeContainer(String name)
    {
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                    docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                    docker.removeContainer(container.id());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopContainer(String name)
    {
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                    docker.stopContainer(container.id(), secondsToWaitBeforeKilling);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startContainer(String name)
    {
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
                    docker.startContainer(container.id());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getRunningContainers()
    {
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.withStatusRunning())) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0).substring(1));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Running Containers:\n" + containerSet);
        return new ArrayList<>(containerSet);
    }

    public static List<String> getNotRunningContainers()
    {
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.withStatusExited()
                        )
                        ) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0).substring(1));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers(),
                                        DockerClient.ListContainersParam.withStatusCreated())) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0).substring(1));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
                //TODO Think about other States
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Not Running Containers:\n" + containerSet);
        return new ArrayList<>(containerSet);
    }

    public static List<String> getAllContainers()
    {
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.allContainers())) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0).substring(1));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(containerSet);
    }

    public static int getHostPortFromContainer(String name)
    {
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", "/" + name))) {
                    List<Integer> portList = new ArrayList<>();
                    for (Container.PortMapping port : container.ports()) {
                        if (port.privatePort() == standardClientPort) {
                            portList.add(port.publicPort());
                        }
                    }
                    if (portList.size() == 1) {
                        return portList.get(0);
                    }
                    else {
                        // TODO NOT ALLOWED!
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
                    for (Container.PortMapping port : container.ports()) {
                        if (port.privatePort() == standardClientPort) {
                            singlePortList.add(port.publicPort());
                        }
                    }
                    if (portList.size() == 1) {
                        portList.add(singlePortList.get(0));
                    }
                    else {
                        // TODO NOT ALLOWED!
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return portList;
    }


}
