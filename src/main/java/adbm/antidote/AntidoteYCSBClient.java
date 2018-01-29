package adbm.antidote;

import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.main.Main;
import adbm.settings.MapDBManager;
import com.yahoo.ycsb.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class AntidoteYCSBClient extends DB
{

    @Override
    public void init() throws DBException
    {
        if (!MapDBManager.isReadyNoText()) {
            MapDBManager.startMapDB();
        }
        if (!GitManager.isReadyNoText()) {
            GitManager.startGit();
        }
        if (!DockerManager.isReadyNoText()) {
            DockerManager.startDocker();
        }
        if (Main.client == null) {
            Main.client = new AntidoteClientWrapper("TestAntidote");
        }
    }

    @Override
    public Status read(String s, String s1, Set<String> set, HashMap<String, ByteIterator> hashMap)
    {
        if (set != null) {
            for (String field : set) {
                hashMap.put(field, new ByteArrayByteIterator(Main.client.getKeyValue(field).getBytes()));
            }
        }
        else {
            for (String key : MapDBManager.getAllKeys().keySet())
            hashMap.put(key, new ByteArrayByteIterator(Main.client.getKeyValue(key).getBytes()));
        }
        return Status.OK;
    }

    @Override
    public Status scan(String s, String s1, int i, Set<String> set, Vector<HashMap<String, ByteIterator>> vector)
    {
        return Status.NOT_IMPLEMENTED;
    }

    @Override
    public Status update(String table, String key, HashMap<String, ByteIterator> values)
    {
        for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
            Main.client.ExecuteKeyOperation(entry.getKey(), AntidoteUtil.getDefaultOperation(entry.getKey()), entry.getValue()); //TODO Value may be bad
        }
        return Status.OK;
    }

    @Override
    public Status insert(String table, String key, HashMap<String, ByteIterator> values)
    {
        update(table, key, values);
        return Status.OK;
    }

    @Override
    public Status delete(String table, String key)
    {
        return Status.NOT_IMPLEMENTED;
    }
}
