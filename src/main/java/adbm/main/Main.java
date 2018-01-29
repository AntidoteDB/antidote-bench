package adbm.main;

import adbm.antidote.AntidoteClientWrapper;
import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.main.ui.MainWindow;
import adbm.settings.MapDBManager;
import eu.antidotedb.antidotepb.AntidotePB;

import java.util.ArrayList;
import java.util.List;

public class Main
{

    public static List<AntidoteClientWrapper> clientList = new ArrayList<>();

    public static AntidoteClientWrapper client;

    public static AntidoteClientWrapper startAntidoteClient(String name)
    {

        if (DockerManager.runContainer(name)) {
            AntidoteClientWrapper clientWrapper = new AntidoteClientWrapper(name);
            clientList.add(clientWrapper);
            return clientWrapper;
        }
        return null;
    }

    public static void stopAntidoteClient(String name)
    {
        DockerManager.stopContainer(name);
        clientList.remove(name);
    }

    public static void removeAntidoteClient(String name)
    {
        DockerManager.removeContainer(name);
        clientList.remove(name);
    }

    public static void main(String[] args)
    {
        MapDBManager.startMapDB();
        GitManager.startGit();
        DockerManager.startDocker();
        System.out.println("Ready!");
        //client = new AntidoteClientWrapper("TestAntidote");
        //System.out.println("Ok!");
        //client.AddKey("Test", AntidotePB.CRDT_type.COUNTER);
        //System.out.println(client.getKeyValue("Test"));
        /*List<String> runningContainers = DockerManager.getRunningContainers();
        List<String> notRunningContainers = DockerManager.getNotRunningContainers();
        for (String container : runningContainers) {
            clientList.add(new AntidoteClientWrapper(container));
        }
        if (clientList.isEmpty()) {
            if (!notRunningContainers.isEmpty()) {
                DockerManager.startContainer(notRunningContainers.get(0));
            }
        }
        if (clientList.isEmpty()) {
            startAntidoteClient("defaultAntidoteDC");
        }
        System.out.println("Done!");
        AntidoteView gui = new AntidoteView(clientList.get(0));*/
        new MainWindow();
        System.out.println("Using the Application:" +
                                   "\nFirst click on Application Settings and select Folder for the Antidote Repository." +
                                   "\nThe Folder must be empty or contain only an existing Antidote Repository (it must be in a clean state)." +
                                   "\n\nNow start the Git Connection and then the selected directory is evaluated and if it is empty the current Antidote Repository is pulled. (about 20MB)" +
                                   "\n\nOnce the Git Connection is initialized the other functionality that is enabled can be used." +
                                   "\n\nThe Git Settings are there to select Commits for which an Image shall be built." +
                                   "\nDockerfiles are created automatically when Images are built but a generic Dockerfile can be created with the corresponding button." +
                                   "\n\nDo not use the Image building yet! It works but takes 5-10 minutes for each selected Benchmark Commit selected in the Git Settings." +
                                   "\nThe Image building is an asynchronous process that will build an Image for all selected Benchmark Commits." +
                                   "\n\nThe building time for an Image is several minutes and may fail completely because Docker is not in the right state (Before building Images restart Docker to be safe)." +
                                   "\n");
    }


}
