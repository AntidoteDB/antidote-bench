package adbm.settings.managers;

import adbm.antidote.util.AntidoteUtil;
import adbm.main.Main;
import adbm.settings.IAntidoteKeyStoreManager;
import adbm.settings.ISettingsManager;
import adbm.util.AdbmConstants;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.*;

import static adbm.util.helpers.FormatUtil.format;

public class MapDBManager implements ISettingsManager, IAntidoteKeyStoreManager
{
    private static final Logger log = LogManager.getLogger(MapDBManager.class);

    private DB mapDB;

    private HTreeMap<String, String> keyTypeMapDB;

    private HTreeMap<String, String> ycsbSettings;

    private HTreeMap<String, String> appSettings;

    private HTreeMap.KeySet<String> benchmarkCommits;

    private static MapDBManager instance = new MapDBManager();

    public static synchronized MapDBManager getInstance()
    {
        return instance;
    }

    private MapDBManager()
    {

    }

    @Override
    public boolean start()
    {
        log.trace("Starting MapDBManager!");
        mapDB = DBMaker.fileDB(AdbmConstants.APP_SETTINGS_PATH).closeOnJvmShutdown().transactionEnable().make();
        keyTypeMapDB = mapDB
                .hashMap("keyTypeMapDB", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        ycsbSettings = mapDB
                .hashMap("ycsbSettings", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        appSettings = mapDB
                .hashMap("appSettings", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        benchmarkCommits = mapDB.hashSet("benchmarkCommits", Serializer.STRING).createOrOpen();
        getKeyTypeKeyNamesMap();
        return true;
    }

    @Override
    public boolean stop()
    {
        log.trace("Stopping MapDBManager!");
        if (keyTypeMapDB != null && !keyTypeMapDB.isClosed())
            keyTypeMapDB.close();
        if (appSettings != null && !appSettings.isClosed())
            appSettings.close();
        //TODO benchmark commits
        if (mapDB != null && !mapDB.isClosed()) {
            mapDB.close();
        }
        mapDB = null;
        keyTypeMapDB = null;
        ycsbSettings = null;
        appSettings = null;
        benchmarkCommits = null;
        return true;
    }

    @Override
    public boolean isReady()
    {
        boolean isReady = mapDB != null && keyTypeMapDB != null && ycsbSettings != null && appSettings != null && benchmarkCommits != null;
        if (!isReady) log.trace("MapDBManager was not ready!");
        return isReady;
    }

    @Override
    public boolean resetAllSettings() {
        if (!isReady()) return false;
        ycsbSettings.clear();
        appSettings.clear();
        benchmarkCommits.clear();
        mapDB.commit();
        return true;
    }

    @Override
    public String getAllSettings() {
        //TODO Update with keytype
        if (!isReady()) return "";
        StringBuilder s = new StringBuilder();
        s.append("App Settings:\n\n");
        appSettings.forEach((key, value) -> s.append(format("{} = {}\n", key, value)));

        s.append("\nYCSB Settings:\n\n");
        ycsbSettings.forEach((key, value) -> s.append(format("{} = {}\n", key, value)));

        s.append("\nBenchmark Commits:\n\n");
        benchmarkCommits.forEach(commit -> s.append(format("{}\n", commit)));
        return s.toString();
    }

    @Override
    public String getYCSBSetting(String settingName)
    {
        if (!isReady()) return "";
        return ycsbSettings.getOrDefault(settingName, "");
    }

    @Override
    public boolean setYCSBSetting(String settingName, String value)
    {
        if (!isReady()) return false;
        ycsbSettings.put(settingName, value);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean resetYCSBSettings()
    {
        if (!isReady()) return false;
        ycsbSettings.clear();
        mapDB.commit();
        return true;
    }

    @Override
    public String getAppSetting(String settingName)
    {
        if (!isReady()) return checkDefaultSetting(settingName, "");
        return checkDefaultSetting(settingName, appSettings.getOrDefault(settingName, ""));
    }

    @Override
    public boolean setAppSetting(String settingName, String value)
    {
        if (!isReady()) return false;
        appSettings.put(settingName, value);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean resetAppSettings()
    {
        if (!isReady()) return false;
        appSettings.clear();
        mapDB.commit();
        return true;
    }

    @Override
    public HashSet<String> getBenchmarkCommits()
    {
        if (!isReady()) return new HashSet<>();
        return new HashSet<>(benchmarkCommits);
    }

    @Override
    public boolean addBenchmarkCommit(String commitId)
    {
        if (!isReady()) return false;
        benchmarkCommits.add(commitId);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean removeBenchmarkCommit(String commitId)
    {
        if (!isReady()) return false;
        benchmarkCommits.remove(commitId);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean resetBenchmarkCommits()
    {
        if (!isReady()) return false;
        benchmarkCommits.clear();
        mapDB.commit();
        return true;
    }

    @Override
    public Map<String, AntidotePB.CRDT_type> getMapKeyNameKeyType()
    {
        Map<String, AntidotePB.CRDT_type> map = new HashMap<>();
        if (!isReady()) return map;
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            map.put(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
        return map;
    }

    @Override
    public AntidotePB.CRDT_type getTypeOfKey(String keyName)
    {
        if (!isReady()) return Main.getAntidoteYCSBConfiguration().getUsedKeyType();
        if (!keyTypeMapDB.containsKey(keyName)) return Main.getAntidoteYCSBConfiguration().getUsedKeyType();
        return AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(keyName));
    }

    @Override
    public boolean addKey(String keyName, AntidotePB.CRDT_type keyType)
    {
        if (!isReady()) return false;
        keyTypeMapDB.put(keyName, keyType.name());
        AntidoteUtil.addKey(keyName, keyType);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean removeKey(String keyName)
    {
        if (!isReady()) return false;
        AntidotePB.CRDT_type type = AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(keyName));
        keyTypeMapDB.remove(keyName);
        AntidoteUtil.removeKey(keyName, type);
        mapDB.commit();
        return true;
    }

    @Override
    public Map<AntidotePB.CRDT_type, List<String>> getKeyTypeKeyNamesMap()
    {
        Map<AntidotePB.CRDT_type, List<String>> map = new HashMap<>();
        if (!isReady()) return map;
        for (AntidotePB.CRDT_type type : AntidotePB.CRDT_type.values()) {
            map.put(type, new ArrayList<>());
        }
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            String keyName = entry.getKey();
            String keyType = entry.getValue().toUpperCase();
            if (AntidoteUtil.STRING_CRDT_TYPE_MAP.containsKey(keyType)) {
                map.get(AntidoteUtil.STRING_CRDT_TYPE_MAP.get(keyType)).add(keyName);
            }
            else {
                log.warn("The key {} did not have a valid CRDT type! CRDT type: {}", keyName, keyType);
            }
        }
        return map;
    }

    @Override
    public boolean resetKeyTypeSettings()
    {
        if (!isReady()) return false;
        keyTypeMapDB.clear();
        mapDB.commit();
        return true;
    }

}
