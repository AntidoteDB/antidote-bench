package adbm.settings;

import adbm.antidote.AntidoteUtil;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MapDBManager
{
    private static final Logger log = LogManager.getLogger(MapDBManager.class);
    
    private static String location = "./keyStoreDatabase";

    private static DB mapDB;

    private static HTreeMap<String, String> keyTypeMapDB;

    private static HTreeMap<String, String> appSettings;

    private static HTreeMap.KeySet<String> benchmarkCommits;

    public static void startMapDB()
    {
        mapDB = DBMaker.fileDB(location).closeOnJvmShutdown().transactionEnable().make();
        keyTypeMapDB = mapDB
                .hashMap("keyTypeMapDB", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        appSettings = mapDB
                .hashMap("appSettings", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        benchmarkCommits = mapDB.hashSet("benchmarkCommits", Serializer.STRING).createOrOpen();
        populateTypeKeyMap();
    }

    public static Map<String, AntidotePB.CRDT_type> getAllKeys()
    {
        Map<String, AntidotePB.CRDT_type> res = new HashMap<>();
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            res.put(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
        return res;
    }

    public static AntidotePB.CRDT_type getTypeOfKey(String name)
    {
        return AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
    }

    public static void addKey(String name, AntidotePB.CRDT_type type)
    {
        keyTypeMapDB.put(name, type.name());
        AntidoteUtil.addKey(name, type);
        mapDB.commit();
    }

    public static void removeKey(String name)
    {
        AntidotePB.CRDT_type type = AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
        keyTypeMapDB.remove(name);
        AntidoteUtil.removeKey(name, type);
        mapDB.commit();
    }

    private static void populateTypeKeyMap()
    {
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            AntidoteUtil.addKey(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
    }

    public static void resetKeyTypeSettings() {
        keyTypeMapDB.clear();
        mapDB.commit();
    }

    public static final String GitRepoLocationSetting = "GitRepo";

    public static final String ConfigLocSetting = "Config";

    public static final String LastBuildImageCommit = "ImageCommit";

    public static String getAppSetting(String setting)
    {
        return appSettings.getOrDefault(setting, "");
    }

    public static void setAppSetting(String setting, String value)
    {
        appSettings.put(setting, value);
        mapDB.commit();
    }

    public static void resetAppSettings() {
        appSettings.clear();
        mapDB.commit();
    }

    public static boolean isReady() {
        if (mapDB != null && keyTypeMapDB != null && appSettings != null) return true;
        log.error("ERROR: The settings are not initialized!\nPlease restart the application!");
        return false;
    }

    public static boolean isReadyNoText() {
        if (mapDB != null && keyTypeMapDB != null && appSettings != null) return true;
        return false;
    }


    public static HashSet<String> getBenchmarkCommits() {
        return new HashSet<>(benchmarkCommits);
    }
    //TODO decide on length
    public static void addBenchmarkCommit(String commitHash) {
        benchmarkCommits.add(commitHash);
        mapDB.commit();
    }

    public static void removeBenchmarkCommit(String commitHash) {
        benchmarkCommits.remove(commitHash);
        mapDB.commit();
    }

    public static void resetBenchmarkCommits() {
        benchmarkCommits.clear();
        mapDB.commit();
    }
}
