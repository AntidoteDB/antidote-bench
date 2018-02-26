package adbm.docker.managers;

import adbm.docker.IDockerManager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DockerManagerJava implements IDockerManager
{
    private static final Logger log = LogManager.getLogger(DockerManagerJava.class);

    @Override
    public boolean start(String uri, String... args)
    {
        DockerClient dockerClient = DockerClientBuilder.getInstance("tcp://localhost:2375").build();
        log.info(dockerClient.listContainersCmd().withShowAll(true).exec());
        return false;
    }

    @Override
    public boolean start()
    {
        return false;
    }

    @Override
    public boolean stop()
    {
        return false;
    }

    @Override
    public boolean isReady()
    {
        return false;
    }

    @Override
    public boolean isBuildingImage()
    {
        return false;
    }

    @Override
    public boolean antidoteBenchmarkImageExists()
    {
        return false;
    }

    @Override
    public boolean buildAntidoteBenchmarkImage(boolean local)
    {
        return false;
    }

    @Override
    public String getCommitOfContainer(String containerName)
    {
        return null;
    }

    @Override
    public boolean rebuildAntidoteInContainer(String containerName, String commit)
    {
        return false;
    }

    @Override
    public boolean runContainer(String containerName)
    {
        return false;
    }

    @Override
    public boolean startContainer(String containerName)
    {
        return false;
    }

    @Override
    public boolean stopContainer(String containerName)
    {
        return false;
    }

    @Override
    public boolean removeContainer(String containerName)
    {
        return false;
    }

    @Override
    public boolean stopAllContainers()
    {
        return false;
    }

    @Override
    public boolean removeAllContainers()
    {
        return false;
    }

    @Override
    public boolean connectContainers(String containerName1, String containerName2)
    {
        return false;
    }

    @Override
    public boolean isAntidoteReady(String containerName)
    {
        return false;
    }

    @Override
    public boolean isContainerRunning(String containerName)
    {
        return false;
    }

    @Override
    public List<String> getNamesOfRunningContainers()
    {
        return null;
    }

    @Override
    public List<String> getNamesOfNotRunningContainers()
    {
        return null;
    }

    @Override
    public List<String> getNamesOfAllContainers()
    {
        return null;
    }

    @Override
    public List<Integer> getHostPortsFromContainer(String containerName)
    {
        return null;
    }
}
