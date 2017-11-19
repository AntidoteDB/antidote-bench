import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.*;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.antidotedb.client.Key.*;

public class Main {

    public static boolean resetContainer = false;

    public static void main(String[] args) {
        System.out.println("Checking docker images...\n");
        String image = executeCommand("docker image ls");
        if (!image.contains("antidotedb/antidote")) {
            System.out.println("Standard Antidote Image was not found.");
            System.out.println("Pulling antidotedb/antidote...\n");
            executeCommand("docker pull antidotedb/antidote");
        }
        /*
        System.out.println("Checking docker networks...\n");
        String network = executeCommand("docker network ls");
        if (!network.contains("default_ntwk")) {
            System.out.println("Standard Antidote Network was not found.");
            System.out.println("Creating default_ntwk...\n");
            executeCommand("docker network create --driver bridge default_ntwk");
        }*/
        System.out.println("Checking docker containers...\n");
        String container = executeCommand("docker container ls");
        if (!container.contains("antidotetest")) {
            System.out.println("Standard Antidote Container was not found.");
            System.out.println("Running antidotetest...\n");
            executeCommand("docker run -t -d -p 127.0.0.1:8087:8087 --name antidotetest antidotedb/antidote");
        } else {
            if (resetContainer) {
                System.out.println("Stopping antidotetest...\n");
                executeCommand("docker stop antidotetest");
                System.out.println("Removing antidotetest...\n");
                executeCommand("docker container rm antidotetest");
                System.out.println("Running antidotetest...\n");
                executeCommand("docker run -t -d -p 127.0.0.1:8087:8087 --name antidotetest antidotedb/antidote");
            } else {
                System.out.println("Starting antidotetest...\n");
                executeCommand("docker start antidotetest");
            }
        }

        System.out.println("Ready Ready Standing By");

        antidote = new AntidoteClient(new InetSocketAddress("localhost", 8087));

        bucket = Bucket.bucket("bucket");

        createKeyMap();

        AntidoteGUI gui = new AntidoteGUI();
    }

    private static AntidoteClient antidote;

    private static Bucket bucket;

    // write
    public static void setKeyData(String keyName, String command, Object commandValue) {
        Key key = keyMap.get(keyName);
        UpdateOp update = null;
        if (!command.equals("reset")) {
            if (command.equals("increment") || command.equals("decrement") || command.equals("set")) {
                commandValue = Long.parseLong(commandValue.toString());
                long value = (long) commandValue;
                if (command.equals("decrement")) {
                    command = "increment";
                    value = -value;
                }
                if (command.equals("set")) {
                    command = "assign";
                }
                Class[] par = new Class[1];
                par[0] = long.class;
                try {
                    Method method = key.getClass().getMethod(command, par);
                    update = (UpdateOp) method.invoke(key, value);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                if (command.equals("assign")) {
                    Class[] par = new Class[1];
                    par[0] = Object.class;
                    try {
                        Method method = key.getClass().getMethod(command, par);
                        update = (UpdateOp) method.invoke(key, commandValue);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            try {
                Method method = key.getClass().getMethod(command, null);
                update = (UpdateOp) method.invoke(key, null);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        if (update != null) {
            AntidoteStaticTransaction tx = antidote.createStaticTransaction();
            bucket.update(tx, update);
            tx.commitTransaction();
        }
    }

    // read
    public static String getKeyValue(String keyName) {
        return bucket.read(antidote.noTransaction(), keyMap.get(keyName)).toString();
    }

    public static List<String> getKeysForType(AntidotePB.CRDT_type type) {
        List<String> list = new ArrayList<>();
        for (Key key : typeKeyMap.get(type)) {
            list.add(key.getKey().toStringUtf8());
        }
        return list;
    }

    public static Map<String, Key> keyMap = new LinkedHashMap<>();

    public static EnumMap<AntidotePB.CRDT_type, List<Key>> typeKeyMap = new EnumMap<>(AntidotePB.CRDT_type.class);

    public static EnumMap<AntidotePB.CRDT_type, String> typeGUIMap = createTypeGUIMap();

    public static Map<String, AntidotePB.CRDT_type> guiTypeMap = createGUITypeMap();

    public static EnumMap<AntidotePB.CRDT_type, String[]> typeCommandMap = createTypeCommandMap();

    private static String executeCommand(String command) {

        System.out.println("Executing Docker Command:\n" + command + "\n");
        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Docker Output:\n" + output.toString() + "\n");

        return output.toString();

    }

    private static void addKey(String name, AntidotePB.CRDT_type type) {
        keyMap.put(name, create(type, ByteString.copyFromUtf8(name)));
        typeKeyMap.get(type).add(keyMap.get(name));
        AntidoteStaticTransaction tx = antidote.createStaticTransaction();
        bucket.update(tx, keyMap.get(name).reset());
        tx.commitTransaction();
    }

    private static void addKeys(Map<String, AntidotePB.CRDT_type> keyList) {
        List<UpdateOp> transactionKeyList = new ArrayList<>();
        for (Map.Entry<String, AntidotePB.CRDT_type> key : keyList.entrySet()) {
            keyMap.put(key.getKey(), create(key.getValue(), ByteString.copyFromUtf8(key.getKey())));
            typeKeyMap.get(key.getValue()).add(keyMap.get(key.getKey()));
            if (key.getValue().equals(AntidotePB.CRDT_type.LWWREG))
                transactionKeyList.add(((RegisterKey) keyMap.get(key.getKey())).assign("0"));
            else if (key.getValue().equals(AntidotePB.CRDT_type.COUNTER))
                transactionKeyList.add(((CounterKey) keyMap.get(key.getKey())).increment(0));
            else
                transactionKeyList.add(keyMap.get(key.getKey()).reset());
        }
        AntidoteStaticTransaction tx = antidote.createStaticTransaction();
        bucket.updates(tx, transactionKeyList);
        tx.commitTransaction();
    }

    private static void createKeyMap() {
        typeKeyMap.put(AntidotePB.CRDT_type.LWWREG, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.MVREG, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.COUNTER, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.FATCOUNTER, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.INTEGER, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.GMAP, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.AWMAP, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.RRMAP, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.ORSET, new ArrayList<>());
        typeKeyMap.put(AntidotePB.CRDT_type.RWSET, new ArrayList<>());

        Map<String, AntidotePB.CRDT_type> map = new LinkedHashMap<>();
        map.put("register1", AntidotePB.CRDT_type.LWWREG);
        map.put("register2", AntidotePB.CRDT_type.LWWREG);
        map.put("register3", AntidotePB.CRDT_type.LWWREG);
        map.put("multiregister1", AntidotePB.CRDT_type.MVREG);
        map.put("multiregister2", AntidotePB.CRDT_type.MVREG);
        map.put("multiregister3", AntidotePB.CRDT_type.MVREG);
        map.put("counter1", AntidotePB.CRDT_type.COUNTER);
        map.put("counter2", AntidotePB.CRDT_type.COUNTER);
        map.put("counter3", AntidotePB.CRDT_type.COUNTER);
        map.put("fatcounter1", AntidotePB.CRDT_type.FATCOUNTER);
        map.put("fatcounter2", AntidotePB.CRDT_type.FATCOUNTER);
        map.put("fatcounter3", AntidotePB.CRDT_type.FATCOUNTER);
        map.put("integer1", AntidotePB.CRDT_type.INTEGER);
        map.put("integer2", AntidotePB.CRDT_type.INTEGER);
        map.put("integer3", AntidotePB.CRDT_type.INTEGER);
        map.put("gmap1", AntidotePB.CRDT_type.GMAP);
        map.put("gmap2", AntidotePB.CRDT_type.GMAP);
        map.put("gmap3", AntidotePB.CRDT_type.GMAP);
        map.put("awmap1", AntidotePB.CRDT_type.AWMAP);
        map.put("awmap2", AntidotePB.CRDT_type.AWMAP);
        map.put("awmap3", AntidotePB.CRDT_type.AWMAP);
        map.put("rrmap1", AntidotePB.CRDT_type.RRMAP);
        map.put("rrmap2", AntidotePB.CRDT_type.RRMAP);
        map.put("rrmap3", AntidotePB.CRDT_type.RRMAP);
        map.put("set1", AntidotePB.CRDT_type.ORSET);
        map.put("set2", AntidotePB.CRDT_type.ORSET);
        map.put("set3", AntidotePB.CRDT_type.ORSET);
        map.put("removeset1", AntidotePB.CRDT_type.RWSET);
        map.put("removeset2", AntidotePB.CRDT_type.RWSET);
        map.put("removeset3", AntidotePB.CRDT_type.RWSET);
        addKeys(map);
    }

    private static Map<String, AntidotePB.CRDT_type> createGUITypeMap() {
        Map<String, AntidotePB.CRDT_type> map = new LinkedHashMap<>();
        map.put("Last-writer-wins Register (LWW-Register)", AntidotePB.CRDT_type.LWWREG);
        map.put("Multi-value Register (MV-Register)", AntidotePB.CRDT_type.MVREG);
        map.put("Counter", AntidotePB.CRDT_type.COUNTER);
        map.put("Fat Counter", AntidotePB.CRDT_type.FATCOUNTER);
        map.put("Integer", AntidotePB.CRDT_type.INTEGER);
        // No Flags
        map.put("Grow-only Map (G-Map)", AntidotePB.CRDT_type.GMAP);
        map.put("Add-wins Map (AW-Map)", AntidotePB.CRDT_type.AWMAP);
        map.put("Remove-Resets Map (RR-Map)", AntidotePB.CRDT_type.RRMAP);
        // No Grow-only Set (G-Set)
        map.put("Add-wins Set (AW-Set / OR-Set)", AntidotePB.CRDT_type.ORSET);
        map.put("Remove-wins Set (RW-Set)", AntidotePB.CRDT_type.RWSET);
        return map;
    }

    private static EnumMap<AntidotePB.CRDT_type, String> createTypeGUIMap() {
        EnumMap<AntidotePB.CRDT_type, String> map = new EnumMap<>(AntidotePB.CRDT_type.class);
        map.put(AntidotePB.CRDT_type.LWWREG, "Last-writer-wins Register (LWW-Register)");
        map.put(AntidotePB.CRDT_type.MVREG, "Multi-value Register (MV-Register)");
        map.put(AntidotePB.CRDT_type.COUNTER, "Counter");
        map.put(AntidotePB.CRDT_type.FATCOUNTER, "Fat Counter");
        map.put(AntidotePB.CRDT_type.INTEGER, "Integer");
        // No Flags
        map.put(AntidotePB.CRDT_type.GMAP, "Grow-only Map (G-Map)");
        map.put(AntidotePB.CRDT_type.AWMAP, "Add-wins Map (AW-Map)");
        map.put(AntidotePB.CRDT_type.RRMAP, "Remove-Resets Map (RR-Map)");
        // No Grow-only Set (G-Set)
        map.put(AntidotePB.CRDT_type.ORSET, "Add-wins Set (AW-Set / OR-Set)");
        map.put(AntidotePB.CRDT_type.RWSET, "Remove-wins Set (RW-Set)");
        return map;
    }

    private static EnumMap<AntidotePB.CRDT_type, String[]> createTypeCommandMap() {
        EnumMap<AntidotePB.CRDT_type, String[]> map = new EnumMap<>(AntidotePB.CRDT_type.class);
        map.put(AntidotePB.CRDT_type.LWWREG, new String[]{"assign"});
        map.put(AntidotePB.CRDT_type.MVREG, new String[]{"assign", "reset"});
        map.put(AntidotePB.CRDT_type.COUNTER, new String[]{"increment", "decrement"});
        map.put(AntidotePB.CRDT_type.FATCOUNTER, new String[]{"increment", "decrement", "reset"});
        map.put(AntidotePB.CRDT_type.INTEGER, new String[]{"increment", "decrement", "set", "reset"});
        // No Flags
        map.put(AntidotePB.CRDT_type.GMAP, new String[]{"update", "removeKey", "removeKeys", "reset"});
        map.put(AntidotePB.CRDT_type.AWMAP, new String[]{"update", "removeKey", "removeKeys", "reset"});
        map.put(AntidotePB.CRDT_type.RRMAP, new String[]{"update", "removeKey", "removeKeys", "reset"});
        // No Grow-only Set (G-Set)
        map.put(AntidotePB.CRDT_type.ORSET, new String[]{"add", "addAll", "remove", "removeAll", "reset"});
        map.put(AntidotePB.CRDT_type.RWSET, new String[]{"add", "addAll", "remove", "removeAll", "reset"});
        return map;
    }
}
