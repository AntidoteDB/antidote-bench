import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.*;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.antidotedb.client.Key.*;

public class Main {


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
            executeCommand("docker run -d -p 127.0.0.1:8087:8087 --name antidotetest antidotedb/antidote");
        } else {
            System.out.println("Stopping antidotetest...\n");
            executeCommand("docker stop antidotetest");
            System.out.println("Removing antidotetest...\n");
            executeCommand("docker container rm antidotetest");
            System.out.println("Running antidotetest...\n");
            executeCommand("docker run -d -p 127.0.0.1:8087:8087 --name antidotetest antidotedb/antidote");
        }

        System.out.println("Ready Ready Standing By");

        antidote = new AntidoteClient(new InetSocketAddress("localhost", 8087));

        // List of Things in the database
        // counter1
        // counter2
        bucket = Bucket.bucket("bucket");


        AntidoteGUI gui = new AntidoteGUI();




        /*

        CounterKey c1 = Key.counter("c1");
        CounterKey c2 = Key.counter("c2");
        BatchRead batchRead = antidote.newBatchRead();
        BatchReadResult<Integer> c1val = bucket.read(batchRead, c1);
        BatchReadResult<Integer> c2val = bucket.read(batchRead, c2);
        batchRead.commit(antidote.noTransaction());
        int sum = c1val.get() + c2val.get();
        List<Integer> values = bucket.readAll(antidote.noTransaction(), Arrays.asList(c1, c2));

        bucket.update(antidote.noTransaction(), Key.set("users").add("Hans Wurst"));


        MapKey testmap = Key.map_aw("testmap2");

        AntidoteStaticTransaction tx = antidote.createStaticTransaction();
        bucket.update(tx,
                testmap.update(
                        Key.counter("a").increment(5),
                        Key.register("b").assign("Hello")
                ));

        ValueCoder<Integer> intCoder = ValueCoder.stringCoder(Object::toString, Integer::valueOf);
        CounterKey c = Key.counter("my_example_counter");
        SetKey<Integer> numberSet = Key.set("set_of_numbers", intCoder);
        try (InteractiveTransaction tx2 = antidote.startTransaction()) {
            int val = bucket.read(tx2, c);
            bucket.update(tx2, numberSet.add(val));
        }*/
    }

    private static AntidoteClient antidote;

    private static Bucket bucket;

    public static Map<String, Key> keyMap = createKeyMap();

    private static Map<String, Key> createKeyMap()
    {
        Map<String,Key> myMap = new HashMap<>();
        myMap.put("counter1", counter("counter1"));
        myMap.put("counter2", counter("counter2"));
        return myMap;
    }

    public static void setKeyData(String keyName, String command, String commandValue) {
        Key key = keyMap.get(keyName);
        AntidoteStaticTransaction tx = antidote.createStaticTransaction();
        if (key.getType().equals(AntidotePB.CRDT_type.COUNTER)) {
            CounterKey counterKey = (CounterKey) key;
            switch (command) {
                case "increment":
                    bucket.update(tx, counterKey.increment(5));
                    break;
                case "decrement":
                    bucket.update(tx, Key.counter("a").increment(5));
                    break;
            }
        }
    }

    public static String getKeyData(String keyName) {
    return "";
    }

    public static List<String> types = Stream.of(AntidotePB.CRDT_type.values())
            .map(AntidotePB.CRDT_type::name)
            .collect(Collectors.toList());

    public static Map<String, List<String>> typeCommandMap = createTypeCommandMap();

    private static Map<String, List<String>> createTypeCommandMap()
    {
        Map<String,List<String>> myMap = new HashMap<>();
        myMap.put(AntidotePB.CRDT_type.COUNTER.name(), new ArrayList<>());
        myMap.put(AntidotePB.CRDT_type.FATCOUNTER.name(), new ArrayList<>());
        myMap.get(AntidotePB.CRDT_type.COUNTER.name()).add("increment");
        myMap.get(AntidotePB.CRDT_type.COUNTER.name()).add("decrement");
        return myMap;
    }

    public static List<String> getKeys(String type){
        List<String> keyList = new ArrayList<>();
        switch (AntidotePB.CRDT_type.valueOf(type)) {
            case COUNTER:
                keyMap.keySet().forEach(key -> {
                    if (keyMap.get(key).getType().equals(AntidotePB.CRDT_type.COUNTER)) {
                        keyList.add(key);
                    }
                });
                break;
        }

        return keyList;
    }

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
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Docker Output:\n" + output.toString() + "\n");

        return output.toString();

    }
}
