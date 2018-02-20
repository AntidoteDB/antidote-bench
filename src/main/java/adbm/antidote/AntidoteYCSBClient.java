/**
 * Class for benchmarking the antidote with YCSB
 */

package adbm.antidote;

import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.main.Main;
import adbm.settings.MapDBManager;
import com.yahoo.ycsb.*;

import java.util.*;
import java.util.stream.Collectors;

public class AntidoteYCSBClient extends DB {

    /**
     * Any argument-based initialization should start with init() in YCSB
     *
     * @throws DBException
     */
    @Override
    public void init() throws DBException {
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

    /**
     * Read of YCSB_Client
     *
     * @param table  The name of the table
     * @param key    The record key of the record to read.
     * @param fields The list of fields to read, or null for all of them
     * @param result A HashMap of field/value pairs for the result
     * @return
     */
    @Override
    public Status read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result) {
        List<String> field = new ArrayList<>();
        for (String name : fields) {
            field.add(name);
        }

        List<String> results = Main.client.getKeyValues(field);
        if (result != null) {
            result = new HashMap<String, ByteIterator>();
        }
        for (int i = 0; i < field.size(); i++) {
            result.put(field.get(i), new ByteArrayByteIterator(results.get(i).getBytes()));
        }
        return Status.OK;
    }

    /**
     * @param table       The name of the table
     * @param startkey    The record key of the first record to read.
     * @param recordcount The number of records to read
     * @param fields      The list of fields to read, or null for all of them
     * @param result      A Vector of HashMaps, where each HashMap is a set field/value pairs for one record
     * @return
     */
    @Override
    public Status scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        return Status.NOT_IMPLEMENTED;
    }

    /**
     * Update of YCSB
     *
     * @param table  The name of the table
     * @param key    The record key of the record to write.
     * @param values A HashMap of field/value pairs to update in the record
     * @return
     */
    @Override
    public Status update(String table, String key, HashMap<String, ByteIterator> values) {

        //Using the hasmap creating an Operation(keyName,OperationName,value) and passing it as a list of Operation

        Main.client.addKeyValues(values.entrySet().stream().map((s) -> new Operation(s.getKey(), AntidoteUtil.getDefaultOperation(s.getKey()), s.getValue())).collect(Collectors.toList()));
        return Status.OK;
    }

    /**
     * Insert in YCSB
     *
     * @param table  The name of the table
     * @param key    The record key of the record to insert.
     * @param values A HashMap of field/value pairs to insert in the record
     * @return
     */
    @Override
    public Status insert(String table, String key, HashMap<String, ByteIterator> values) {
        update(table, key, values);
        return Status.OK;
    }

    /**
     * Delete in YCSB
     *
     * @param table The name of the table
     * @param key   The record key of the record to delete.
     * @return
     */
    @Override
    public Status delete(String table, String key) {
        return Status.NOT_IMPLEMENTED;
    }
}
