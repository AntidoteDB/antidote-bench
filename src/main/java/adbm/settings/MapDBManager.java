package adbm.settings;

import adbm.antidote.AntidoteUtil;
import adbm.main.Main;
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

    private static DB mapDB;

    private static HTreeMap<String, String> keyTypeMapDB;

    private static HTreeMap<String, String> appSettings;

    private static HTreeMap.KeySet<String> benchmarkCommits;

    private static final String GitRepoLocationSetting = "GitRepo";


    public static String getGitRepoLocation() {
        if (isReadyNoText()) {
            String gitRepo = getAppSetting(GitRepoLocationSetting);
            if (gitRepo.isEmpty()) {
                return Main.defaultAntidotePath;
            }
            else {
                return gitRepo;
            }
        }
        else {
            return Main.defaultAntidotePath;
        }
    }

    public static void setGitRepoLocation(String path) {
        if (isReadyNoText()) {
            setAppSetting(GitRepoLocationSetting, path);
        }
    }

    public static final String ConfigLocSetting = "Config";

    private static boolean useSettings() {
        if (!Main.isGuiMode()) {
            log.warn("Settings can't be used when the application is not in GUI mode!");
            return false;
        }
        return true;
    }

    public static boolean isReady() {
        if (!useSettings()) return false;
        if (mapDB != null && keyTypeMapDB != null && appSettings != null) return true;
        log.error("The settings are not initialized!\nPlease restart the application!");
        return false;
    }

    public static boolean isReadyNoText() {
        if (!useSettings()) return false;
        if (mapDB != null && keyTypeMapDB != null && appSettings != null) return true;
        return false;
    }

    public static void startMapDB()
    {
        if (!useSettings()) return;
        mapDB = DBMaker.fileDB(Main.appSettingsPath).closeOnJvmShutdown().transactionEnable().make();
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
        if (!useSettings()) return new HashMap<>();
        Map<String, AntidotePB.CRDT_type> res = new HashMap<>();
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            res.put(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
        return res;
    }

    public static AntidotePB.CRDT_type getTypeOfKey(String name)
    {
        if (!useSettings() || !keyTypeMapDB.containsKey(name)) return Main.usedKeyType;
            return AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
    }

    public static void addKey(String name, AntidotePB.CRDT_type type)
    {
        if (!useSettings()) return;
        keyTypeMapDB.put(name, type.name());
        AntidoteUtil.addKey(name, type);
        mapDB.commit();
    }

    public static void removeKey(String name)
    {
        if (!useSettings()) return;
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
        if (!useSettings()) return;
        keyTypeMapDB.clear();
        mapDB.commit();
    }

    public static String getAppSetting(String setting)
    {
        if (!useSettings()) return "";
        return appSettings.getOrDefault(setting, "");
    }

    public static void setAppSetting(String setting, String value)
    {
        if (!useSettings()) return;
        appSettings.put(setting, value);
        mapDB.commit();
    }

    public static void resetAppSettings() {
        if (!useSettings()) return;
        appSettings.clear();
        mapDB.commit();
    }




    public static HashSet<String> getBenchmarkCommits() {
        if (!useSettings()) return new HashSet<>();
        return new HashSet<>(benchmarkCommits);
    }
    //TODO decide on length
    public static void addBenchmarkCommit(String commitHash) {
        if (!useSettings()) return;
        benchmarkCommits.add(commitHash);
        mapDB.commit();
    }

    public static void removeBenchmarkCommit(String commitHash) {
        if (!useSettings()) return;
        benchmarkCommits.remove(commitHash);
        mapDB.commit();
    }

    public static void resetBenchmarkCommits() {
        if (!useSettings()) return;
        benchmarkCommits.clear();
        mapDB.commit();
    }
}
