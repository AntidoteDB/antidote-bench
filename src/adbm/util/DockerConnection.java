package adbm.util;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import com.spotify.docker.client.messages.Container;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class DockerConnection
{

    private static DockerClient docker;

    private static String antidoteDockerNetworkName = "antidote_ntwk";

    private static String antidoteDockerImageName = "antidotedb/antidotebm";

    private static int standardClientPort = 8087;

    private static int secondsToWaitBeforeKilling = 10;

    private static List<Integer> hostPortList = new ArrayList<>(Arrays.asList(8087, 8088, 8089, 8090, 8091, 8092));

    // Containers are not allowed to have the same name! Therefore the returned list must have only one element!

    public static void startDocker()
    {
        try {
            docker = DefaultDockerClient.fromEnv().readTimeoutMillis(10000).build();
            if (docker.listImages(DockerClient.ListImagesParam.byName("antidotedb/antidote")).isEmpty()) {
                System.out.println("Please run the following command in your commandline:\ndocker pull antidotedb/antidote");
                JDialog dialog = new JDialog();
                dialog.setTitle("Pull the antidote image first!");
                dialog.setSize(400,100);
                dialog.setModal(true);
                dialog.setLayout(new FlowLayout());
                dialog.add(new JLabel("Please run the following command in your commandline:"));
                dialog.add(new JTextField("docker pull antidotedb/antidote"));
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Runtime.getRuntime().exit(0);
            }
            if (docker.listImages(DockerClient.ListImagesParam.byName(antidoteDockerImageName)).isEmpty()) {
                docker.build(Paths.get("./Dockerfile/"), antidoteDockerImageName);
            }
            boolean containsNetwork = false;
            for (Network network : docker.listNetworks()) {
                if (network.name().equals(antidoteDockerNetworkName)) {
                    containsNetwork = true;
                    break;
                }
            }
            if (!containsNetwork) {
                docker.createNetwork(NetworkConfig.builder().name(antidoteDockerNetworkName).driver("bridge").build());
            }
        } catch (DockerCertificateException e) {
            e.printStackTrace();
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
                                        DockerClient.ListContainersParam.withStatusRunning())) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
            }
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(containerSet);
    }

    public static List<String> getNotRunningContainers()
    {
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.withStatusExited())) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName),
                                        DockerClient.ListContainersParam.withStatusCreated())) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
                //TODO Think about other States
            }
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(containerSet);
    }

    public static List<String> getAllContainers()
    {
        Set<String> containerSet = new HashSet<>();
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("ancestor", antidoteDockerImageName))) {
                    if (container.names().size() == 1) {
                        containerSet.add(container.names().get(0));
                    }
                    else {
                        // TODO NOT ALLOWED
                    }
                }
            }
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(containerSet);
    }

    public static int getHostPortFromContainer(String name)
    {
        try {
            if (docker != null) {
                for (Container container : docker
                        .listContainers(DockerClient.ListContainersParam.filter("name", name))) {
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
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return portList;
    }




}
