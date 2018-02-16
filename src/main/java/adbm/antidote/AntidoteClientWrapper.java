package adbm.antidote;

import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.docker.DockerManager;
import adbm.settings.MapDBManager;
import com.google.protobuf.ByteString;
import com.yahoo.ycsb.ByteIterator;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.*;

import java.net.InetSocketAddress;
import java.util.*;

import static adbm.settings.MapDBManager.getTypeOfKey;
import static eu.antidotedb.client.Key.create;

/**
 * Bridge between Antidote and YCSB
 * Fetch the neccessary information from Antidote
 */
public class AntidoteClientWrapper extends AntidoteModel {

    private final AntidoteClient antidote;

    private final Bucket bucket;

    public final int hostPort;

    public final String name;

    private boolean running;

    public boolean isReady() {
        return true;
    }

    /**
     * Constructor of the class
     *
     * @param name
     */
    public AntidoteClientWrapper(String name) {
        //docker run -i -t -d --name antidote1 -p 8087:8087 --network antidote_ntwk -e SHORT_NAME=true -e NODE_NAME=antidote@antidote1 antidotedb/antidote
        DockerManager.runContainer(name);
        hostPort = DockerManager.getHostPortsFromContainer(name).get(0); //TODO exception

        antidote = new AntidoteClient(new InetSocketAddress("localhost", hostPort));

        bucket = Bucket.bucket(name + "bucket");

        this.name = name;

        running = true;
    }

    /**
     *
     */
    public void stop() {
        if (running) {
            this.firePropertyChange(AntidoteController.DCListChanged, "", "");
            running = false;
        }
    }

    /**
     *
     */
    public void start() {
        if (!running) {
            this.firePropertyChange(AntidoteController.DCListChanged, "", "");
            running = false;
        }
    }

    /**
     * To
     *
     * @param name
     * @param type
     */
    public void AddKey(String name, AntidotePB.CRDT_type type) {
        if (running) {
            InteractiveTransaction tx = antidote.startTransaction();
            bucket.update(tx, create(type, ByteString.copyFromUtf8(name)).reset());
            tx.commitTransaction();
            MapDBManager.addKey(name, type);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    /**
     * @param name
     */
    public void RemoveKey(String name) {
        if (running) {
            MapDBManager.removeKey(name);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    /**
     * Single read with noTransaction - Case 1
     *
     * @param name
     * @return - the read string
     */
    public String getKeyValueNoTx(String name) {
        if (running) {
            Key key = createKey(name);

            //Assuming that every CRDT type returns String
            return bucket.read(antidote.noTransaction(), createKey(name)).toString();
        } else
            return null;
    }

    /**
     * Single read with Transaction - Case 2
     *
     * @param name * @return - the read string
     */
    public String getKeyValueTx(String name) {
        if (running) {
            String returnString;
            InteractiveTransaction tx = antidote.startTransaction();
            returnString = bucket.read(tx, createKey(name)).toString();
            tx.commitTransaction();
            return returnString;
        } else
            return null;
    }

    /**
     * Single write with noTransaction - Case 3
     *
     * @param operation operations (key, operation, value)
     */
    public void addKeyNoTx(Operation operation) {
        if (running) {
            bucket.update(antidote.noTransaction(), getKeyUpdate(operation));
        }
    }

    /**
     * Single write with Transaction - Case 4
     *
     * @param operation operations (key, operation, value)
     */
    public void addKeyTx(Operation operation) {
        if (running) {
            InteractiveTransaction tx = antidote.startTransaction();
            bucket.update(tx, getKeyUpdate(operation));
            tx.commitTransaction();
        }
    }

    /**
     * Multiple read and write as a single Transaction - Case 5
     *
     * @param keys
     * @param operations operations (key, operation, value)
     */
    public List<String> executeKeyRW(List<String> keys, List<Operation> operations) {
        // TODO different data structure

        List<String> returnList = new ArrayList<>();
        if (running) {

            InteractiveTransaction tx = antidote.startTransaction();
            for (int i = 0; i < keys.size(); i++) {
                Operation op = operations.get(i);
                //Assuming that if the operation is null then its a read else a write
                if (op == null) {
                    returnList.add(bucket.read(tx, createKey(keys.get(i))).toString());
                } else {
                    bucket.update(tx, getKeyUpdate(op));
                }
            }
            tx.commitTransaction();
        }
        return returnList;
    }

    /**
     * For write
     * Executing all operations
     *
     * @param operation - operations (key, operation,value)
     * @return - update operation in write
     */
    public UpdateOp getKeyUpdate(Operation operation) {

        if (running) {
            String name = operation.keyName;
            String operationName = operation.operationName;
            Object value = operation.value;
            Key key = create(getTypeOfKey(name), ByteString.copyFromUtf8(name));
            UpdateOp update = null;
            Long longObject = null;

            if (key.getType().equals(AntidotePB.CRDT_type.LWWREG)) {
                if (operationName.equals("assign")) {
                    RegisterKey lwwregKey = (RegisterKey) key;
                    update = lwwregKey.assign(value);
                }

            } else if (key.getType().equals(AntidotePB.CRDT_type.MVREG)) {
                MVRegisterKey mvregKey = (MVRegisterKey) key;
                if (operationName.equals("assign")) {
                    update = mvregKey.assign(value);
                } else if (operationName.equals("reset")) {
                    update = mvregKey.reset();
                }
            } else if ((key.getType().equals(AntidotePB.CRDT_type.COUNTER)) || (key.getType().equals(AntidotePB.CRDT_type.FATCOUNTER))) {

                CounterKey counterKey = (CounterKey) key;
                longObject = Long.parseLong(value.toString());
                if (operationName.equals("increment")) {
                    update = counterKey.increment(longObject);
                } else if (operationName.equals("decrement")) {
                    update = counterKey.increment(-longObject);
                } else if (operationName.equals("reset")) {
                    update = counterKey.reset();
                }
            } else if (key.getType().equals(AntidotePB.CRDT_type.INTEGER)) {
                IntegerKey integerKey = (IntegerKey) key;
                longObject = Long.parseLong(value.toString());
                if (operationName.equals("increment")) {
                    update = integerKey.increment(longObject);
                } else if (operationName.equals("decrement")) {
                    update = integerKey.increment(-longObject);
                } else if (operationName.equals("reset")) {
                    update = integerKey.reset();
                } else if (operationName.equals("set")) {
                    operationName = "assign";
                    update = integerKey.assign(longObject);
                }
            } else if ((key.getType().equals(AntidotePB.CRDT_type.GMAP)) || (key.getType().equals(AntidotePB.CRDT_type.AWMAP)) || (key.getType().equals(AntidotePB.CRDT_type.RRMAP))) {
                MapKey gmapKey = (MapKey) key;

                if (operationName.equals("update")) {
                    update = gmapKey.update(); // Check with Kevin
                } else if (operationName.equals("removeKey")) {
                    update = gmapKey.removeKey(key);

                } else if (operationName.equals("removeKeys")) {
                    // update = gmapKey.removeKeys(getMapKeyValue(gmapKey))

                } else if (operationName.equals("reset")) {
                    update = gmapKey.reset();
                }
            } else if ((key.getType().equals(AntidotePB.CRDT_type.ORSET)) || (key.getType().equals(AntidotePB.CRDT_type.RWSET))) {

                SetKey rwsetSetKey = (SetKey) key;

                if (operationName.equals("add")) {
                    update = rwsetSetKey.add(value);

                } else if (operationName.equals("addAll")) {
                    //update=rwsetSetKey.addAll()

                } else if (operationName.equals("remove")) {
                    update = rwsetSetKey.remove(value);

                } else if (operationName.equals("removeAll")) {

                } else if (operationName.equals("reset")) {
                    update = rwsetSetKey.reset();
                }
            }
            return update;
        }
        return null;
    }

    /**
     * For multiple writes
     *
     * @param operations - List of operations (key, operation,value) which are write)
     */
    public void addKeyValues(List<Operation> operations) {
        if (running) {
            List<UpdateOp> updates = new ArrayList<>();

            for (Operation operation : operations) {
                updates.add(getKeyUpdate(operation));
            }

            InteractiveTransaction tx = antidote.startTransaction();
            bucket.updates(tx, updates);
            tx.commitTransaction();

        }
    }

    /**
     * For multiple reads
     *
     * @param keyNames - A list of key names which are read
     * @return - String of reads
     */
    public List<String> getKeyValues(List<String> keyNames) {
        List<String> results = new ArrayList<>();
        if (running) {
            List<Key> keyList = new ArrayList<>();
            for (String name : keyNames) {
                keyList.add(createKey(name));
            }
            BatchRead batchRead = antidote.newBatchRead();
            List<BatchReadResult> res = new ArrayList<>();
            for (Key key : keyList) {
                res.add(bucket.read(batchRead, key));
            }
            batchRead.commit(antidote.noTransaction());
            for (BatchReadResult result : res) {
                results.add(result.get().toString());
            }
        }
        return results;
    }


    /**
     * To get the map keys
     *
     * @param key
     * @return
     */
    public Map<String, Long> getMapKeyValue(Key<MapKey.MapReadResult> key) {
        return bucket.read(antidote.noTransaction(), key).asJavaMap(ResponseDecoder.integer());
    }

    /**
     * @param name
     * @return
     */
    private Key createKey(String name) {
        if (running) {
            return create(getTypeOfKey(name), ByteString.copyFromUtf8(name));
        } else
            return null;
    }
}
