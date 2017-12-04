package adbm.main;

import adbm.antidote.AntidoteClientWrapper;
import adbm.gui.AntidoteView;
import adbm.util.DockerConnection;
import adbm.util.MapDBConnection;

import java.util.ArrayList;
import java.util.List;

public class Main {



    public static List<AntidoteClientWrapper> clientList = new ArrayList<>();

    public static void startAntidoteClient(String name) {
        if (DockerConnection.runContainer(name)) clientList.add(new AntidoteClientWrapper(name));
    }

    public static void stopAntidoteClient(String name) {
        DockerConnection.stopContainer(name);
        clientList.remove(name);
    }

    public static void removeAntidoteClient(String name) {
        DockerConnection.removeContainer(name);
        clientList.remove(name);
    }

    public static void main(String[] args) {
        DockerConnection.startDocker();
        MapDBConnection.startMapDB();
        System.out.println("Ready!");
        List<String> runningContainers = DockerConnection.getRunningContainers();
        List<String> notRunningContainers = DockerConnection.getNotRunningContainers();
        for (String container : runningContainers) {
            clientList.add(new AntidoteClientWrapper(container));
        }
        if (clientList.isEmpty()) {
            if (!notRunningContainers.isEmpty()) {
                DockerConnection.startContainer(notRunningContainers.get(0));
            }
        }
        if (clientList.isEmpty()) {
            startAntidoteClient("defaultAntidoteDC");
        }
        System.out.println("Done!");
        AntidoteView gui = new AntidoteView(clientList.get(0));
    }












}
