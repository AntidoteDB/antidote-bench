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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MapDBManager implements ISettingsManager, IAntidoteKeyStoreManager
{
    private static final Logger log = LogManager.getLogger(MapDBManager.class);

    private DB mapDB;

    private HTreeMap<String, String> keyTypeMapDB;

    private HTreeMap<String, String> appSettings;

    private HTreeMap.KeySet<String> benchmarkCommits;

    private static MapDBManager instance = new MapDBManager();

    public static synchronized MapDBManager getInstance() {
        return instance;
    }

    private MapDBManager() {

    }

    @Override
    public boolean start()
    {
        mapDB = DBMaker.fileDB(AdbmConstants.appSettingsPath).closeOnJvmShutdown().transactionEnable().make();
        keyTypeMapDB = mapDB
                .hashMap("keyTypeMapDB", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        appSettings = mapDB
                .hashMap("appSettings", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        benchmarkCommits = mapDB.hashSet("benchmarkCommits", Serializer.STRING).createOrOpen();
        populateTypeKeyMap();
        return true;
    }

    @Override
    public boolean stop()
    {
        return true; //TODO
    }

    @Override
    public boolean isReady()
    {
        return mapDB != null && keyTypeMapDB != null && appSettings != null && benchmarkCommits != null;
    }

    @Override
    public String getAppSetting(String setting)
    {
        return appSettings.getOrDefault(setting, "");
    }

    @Override
    public boolean setAppSetting(String setting, String value)
    {
        appSettings.put(setting, value);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean resetAppSettings()
    {
        appSettings.clear();
        mapDB.commit();
        return true;
    }

    @Override
    public String getGitRepoLocation()
    {
        if (isReady()) {
            String gitRepo = getAppSetting(GitRepoLocationSetting);
            if (gitRepo.isEmpty()) {
                return AdbmConstants.defaultAntidotePath;
            }
            else {
                return gitRepo;
            }
        }
        else {
            return AdbmConstants.defaultAntidotePath;
        }
    }

    @Override
    public void setGitRepoLocation(String path)
    {
        if (isReady()) {
            setAppSetting(GitRepoLocationSetting, path);
        }
    }

    @Override
    public HashSet<String> getBenchmarkCommits()
    {
        return new HashSet<>(benchmarkCommits);
    }

    //TODO decide on length
    @Override
    public boolean addBenchmarkCommit(String commitHash)
    {
        benchmarkCommits.add(commitHash);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean removeBenchmarkCommit(String commitHash)
    {
        benchmarkCommits.remove(commitHash);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean resetBenchmarkCommits()
    {
        benchmarkCommits.clear();
        mapDB.commit();
        return true;
    }

    @Override
    public Map<String, AntidotePB.CRDT_type> getAllKeys()
    {
        Map<String, AntidotePB.CRDT_type> res = new HashMap<>();
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            res.put(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
        return res;
    }

    @Override
    public AntidotePB.CRDT_type getTypeOfKey(String name)
    {
        if (!isReady()) return Main.getUsedKeyType();
        if (!keyTypeMapDB.containsKey(name)) return Main.getUsedKeyType();
        return AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
    }

    @Override
    public boolean addKey(String name, AntidotePB.CRDT_type type)
    {
        keyTypeMapDB.put(name, type.name());
        AntidoteUtil.addKey(name, type);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean removeKey(String name)
    {
        AntidotePB.CRDT_type type = AntidotePB.CRDT_type.valueOf(keyTypeMapDB.get(name));
        keyTypeMapDB.remove(name);
        AntidoteUtil.removeKey(name, type);
        mapDB.commit();
        return true;
    }

    @Override
    public boolean populateTypeKeyMap()
    {
        for (Map.Entry<String, String> entry : keyTypeMapDB.getEntries()) {
            AntidoteUtil.addKey(entry.getKey(), AntidotePB.CRDT_type.valueOf(entry.getValue()));
        }
        return true;
    }

    @Override
    public boolean resetKeyTypeSettings()
    {
        keyTypeMapDB.clear();
        mapDB.commit();
        return true;
    }

}
