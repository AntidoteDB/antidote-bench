/**
 * Class for benchmarking the antidote with YCSB
 */

package adbm.ycsb;

import adbm.antidote.util.AntidoteUtil;
import adbm.antidote.operations.UpdateOperation;
import adbm.main.Main;
import com.yahoo.ycsb.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class AntidoteYCSBClient extends DB
{

    private static final Logger log = LogManager.getLogger(AntidoteYCSBClient.class);

    /**
     * Any argument-based initialization should start with init() in YCSB
     *
     * @throws DBException
     */
    @Override
    public void init() throws DBException
    {
        Main.initializeBenchmarkClient();
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
    public Status read(String table, String key, Set<String> fields, HashMap<String, ByteIterator> result)
    {
        if (fields == null) {
            log.warn("Read operation was not performed because the set of fields to read was null!");
            return Status.BAD_REQUEST;
        }
        if (fields.size() == 0) {
            log.info("Read operation was not performed because the set of fields to read was empty!");
            return Status.OK;
        }
        if (result == null) {
            log.warn("Read operation was not performed because the map of results was null!");
            return Status.BAD_REQUEST;
        }
        if (result.size() != 0) {
            log.warn("The map of results was not empty before the read!");
        }
        // Keep order
        List<String> orderedFields = new ArrayList<>(fields);
        List<Object> orderedResults = Main.getBenchmarkClient().readKeyValues(orderedFields);
        for (int i = 0; i < orderedResults.size(); i++) {
            result.put(orderedFields.get(i), new ByteArrayByteIterator(orderedResults.get(i).toString().getBytes()));
        }//TODO think about the result
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
    public Status scan(String table, String startkey, int recordcount, Set<String> fields,
                       Vector<HashMap<String, ByteIterator>> result)
    {
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
    public Status update(String table, String key, HashMap<String, ByteIterator> values)
    {

        //Using the hashmap creating an Operation(keyName,OperationName,value) and passing it as a list of Operation
        if (values == null) {
            log.warn("Update operation was not performed because the map of values was null!");
            return Status.BAD_REQUEST;
        }
        if (values.size() == 0) {
            log.info("Update operation was not performed because the map of values was empty!");
            return Status.OK;
        }
        Main.getBenchmarkClient()
            .updateKeys(values.entrySet().stream().map((s) -> new UpdateOperation<>(s.getKey(), AntidoteUtil
                    .getDefaultOperation(s.getKey()), s.getValue())).collect(Collectors.toList()));
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
    public Status insert(String table, String key, HashMap<String, ByteIterator> values)
    {
        return update(table, key, values);
    }

    /**
     * Delete in YCSB
     *
     * @param table The name of the table
     * @param key   The record key of the record to delete.
     * @return
     */
    @Override
    public Status delete(String table, String key)
    {
        return Status.NOT_IMPLEMENTED;
    }
}
