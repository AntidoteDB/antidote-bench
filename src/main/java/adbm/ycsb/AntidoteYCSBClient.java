/**
 * Class for benchmarking the antidote with YCSB
 */

package adbm.ycsb;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.UpdateOperation;
import adbm.antidote.util.AntidoteUtil;
import adbm.antidote.wrappers.AntidoteClientWrapper;
import adbm.util.AdbmConstants;
import com.yahoo.ycsb.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static adbm.util.helpers.FormatUtil.format;

public class AntidoteYCSBClient extends DB
{

    private static final Logger log = LogManager.getLogger(AntidoteYCSBClient.class);

    private IAntidoteClientWrapper antidoteClient;

    private static AtomicInteger idCounter = new AtomicInteger(1);

    /**
     * Any argument-based initialization should start with init() in YCSB
     *
     * @throws DBException
     */
    @Override
    public void init() throws DBException
    {
        antidoteClient = new AntidoteClientWrapper(format("AntidoteClient-{}", idCounter.getAndIncrement()),
                                                   AdbmConstants.ADBM_CONTAINER_NAME);
        if (!antidoteClient.start()) {
            log.error("The Antidote Client could not be started!");
            throw new DBException("The Antidote Client could not be started!");
        }
    }

    @Override
    public void cleanup() throws DBException
    {
        antidoteClient.stop();
        antidoteClient = null;
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
            log.warn("The Antidote Database does not support reading all fields!");
            return Status.NOT_IMPLEMENTED;
        }
        if (result == null) {
            log.warn("Read operation was not performed because the map of results was null!");
            return Status.BAD_REQUEST;
        }
        if (result.size() != 0) {
            log.warn("The map of results was not empty before the read!");
        }
        int size = fields.size();
        switch (size) {
            case 0:
                log.debug("No field is read!");
                return Status.OK;
            case 1:
                log.debug("Single field is read!");
                String field = fields.iterator().next();
                result.put(field, new ByteArrayByteIterator(antidoteClient.readKeyValue(field).toString().getBytes()));
                return Status.OK;
            default:
                log.debug("Multiple fields ({}) are read!", size);
                // Keep order
                List<String> orderedFields = new ArrayList<>(fields);
                List<Object> orderedResults = antidoteClient.readKeyValues(orderedFields);
                for (int i = 0; i < orderedResults.size(); i++) {
                    result.put(orderedFields.get(i),
                               new ByteArrayByteIterator(orderedResults.get(i).toString().getBytes()));
                }//TODO think about the result
                return Status.OK;
        }

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
        int size = values.size();
        switch (size) {
            case 0:
                log.debug("No update operation is performed.");
                return Status.OK;
            case 1:
                log.debug("Single update operation is performed.");
                Map.Entry<String, ByteIterator> entry = values.entrySet().iterator().next();
                antidoteClient.updateKey(new UpdateOperation<>(entry.getKey(), AntidoteUtil.getOperation(), entry.getValue()));
                return Status.OK;
            default:
                log.debug("Multiple update operations ({}) are performed", size);
                antidoteClient
                        .updateKeys(values.entrySet().stream().map((s) -> new UpdateOperation<>(s.getKey(), AntidoteUtil
                                .getOperation(), s.getValue())).collect(Collectors.toList()));
                return Status.OK;
        }
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
