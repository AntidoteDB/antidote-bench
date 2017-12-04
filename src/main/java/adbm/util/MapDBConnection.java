package adbm.util;

import eu.antidotedb.antidotepb.AntidotePB;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.HashMap;
import java.util.Map;

public class MapDBConnection
{

    private static DB mapDB;

    private static HTreeMap<String, String> keyTypeMapDB;

    public static void startMapDB() {
        mapDB = DBMaker.fileDB("./out/keyStoreDatabase").closeOnJvmShutdown().transactionEnable().make();
        keyTypeMapDB = mapDB
                .hashMap("keyTypeMapDB", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        populateTypeKeyMap();
    }

    public static Map<String, AntidotePB.CRDT_type> getAllKeys() {
        Map<String, AntidotePB.CRDT_type> res = new HashMap<>();
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            res.put(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
        return res;
    }

    public static AntidotePB.CRDT_type getTypeOfKey(String name) {
        return AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
    }

    public static void addKey(String name, AntidotePB.CRDT_type type) {
        keyTypeMapDB.put(name, type.name());
        AntidoteUtil.addKey(name, type);
    }

    public static void removeKey(String name) {
        AntidotePB.CRDT_type type = AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
        keyTypeMapDB.remove(name);
        AntidoteUtil.removeKey(name, type);
    }

    private static void populateTypeKeyMap() {
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            AntidoteUtil.addKey(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
    }


}
