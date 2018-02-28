package adbm.docker;

import adbm.util.IStartStop;

import java.util.List;

/**
 * This interface is used for documentation of the relevant methods and to add other DockerManagers that use different APIs.
 * There are at least two Docker Java APIs available and it is possible to simply use the command line to interact with docker.
 * https://github.com/spotify/docker-client
 * https://github.com/docker-java/docker-java
 */
public interface IDockerManager extends IStartStop
{

    /**
     * Returns true if an image is currently building.
     * Otherwise false.
     * @return true if an image is currently building. Otherwise false.
     */
    boolean isBuildingImage();

    /**
     * Returns true if the Antidote benchmark image already exists and does not have to be built.
     * Otherwise false.
     * @return true if the Antidote benchmark image already exists and does not have to be built. Otherwise false.
     */
    boolean antidoteBenchmarkImageExists();

    /**
     * Builds the Antidote benchmark image.
     * It creates a new Dockerfile and uses it to build the Antidote benchmark image.
     * The image can be built using a local Antidote git repository which is copied inside the container.
     * Otherwise the image is built by pulling the Antidote git repository remotely.
     * Will override an existing Antidote benchmark image (and remove all its containers).
     * Usually takes a few minutes to complete and should be called with another thread to avoid blocking the main thread.
     * @param local If true the image is built by copying a local Antidote git repository otherwise the git repository is pulled remotely.
     * @return true if the image building process succeeded. Otherwise false.
     */
    boolean buildAntidoteBenchmarkImage(boolean local);

    /**
     * Returns the commit id of the Antidote database that is currently running in the container.
     * @param containerName The name of the container.
     * @return the commit id of the Antidote database that is currently running in the container.
     */
    String getCommitOfContainer(String containerName);

    /**
     *
     * @param containerName
     * @param commit
     * @return
     */
    boolean rebuildAntidoteInContainer(String containerName, String commit);

    /**
     * Runs a container with the specified name.
     * If the container exists and is running nothing is done.
     * If the container exists and is not running it will be started.
     * If the container does not exist it will be created and then started.
     * If a new container is created it will use a unique host port that is used to connect containers via the Antidote Docker Network (for replication)
     *
     * @param containerName The name of the container.
     * @return true if the container is running otherwise false.
     */
    boolean runContainer(String containerName);

    /**
     * Starts the container with the specified name.
     *
     * @param containerName The name of the container.
     * @return true if the container was successfully removed otherwise false.
     */
    boolean startContainer(String containerName);

    /**
     * Stops the container with the specified name.
     * Can cause issues if the container is used.
     *
     * @param containerName The name of the container.
     * @return true if the container was successfully removed otherwise false.
     */
    boolean stopContainer(String containerName);

    /**
     * Stops and then removes the container with the specified name.
     * Can cause issues if the container is used.
     *
     * @param containerName The name of the container.
     * @return true if the container was successfully removed otherwise false.
     */
    boolean removeContainer(String containerName);

    /**
     *
     * @return
     */
    boolean stopAllContainers();

    /**
     *
     * @return
     */
    boolean removeAllContainers();

    /**
     *
     * @param containerName1
     * @param containerName2
     * @return
     */
    boolean connectContainers(String containerName1, String containerName2);

    /**
     *
     * @param containerName
     * @return
     */
    boolean isAntidoteReady(String containerName);

    /**
     *
     * @param containerName
     * @return
     */
    boolean isContainerRunning(String containerName);

    /**
     * Returns a list of the names of all running containers that are built from the antidote benchmark image.
     *
     * @return A list of the names of all running containers that are built from the antidote benchmark image.
     */
    List<String> getNamesOfRunningContainers();

    /**
     * Returns a list of the names of all containers that not running (created or exited) that are built from the antidote benchmark image.
     *
     * @return A list of the names of all containers that not running (created or exited) that are built from the antidote benchmark image.
     */
    List<String> getNamesOfNotRunningContainers();

    /**
     * Returns a list of the names of all containers that are built from the antidote benchmark image.
     *
     * @return A list of the names of all containers that are built from the antidote benchmark image.
     */
    List<String> getNamesOfAllContainers();

    /**
     * Returns the host port of the container with the specified name.
     * This port is used when an antidote client connects to the container or if containers a connected via the Antidote Docker Network for replication.
     * Returns -1 if something goes wrong.
     *
     * @param containerName The name of the container.
     * @return The host port of the container.
     */
    List<Integer> getHostPortsFromContainer(String containerName);



}
