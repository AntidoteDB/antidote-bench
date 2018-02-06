package adbm.antidote;

import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.docker.DockerManager;
import adbm.settings.MapDBManager;
import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static adbm.settings.MapDBManager.getTypeOfKey;
import static eu.antidotedb.client.Key.create;

/**
 * Bridge between Antidote and YCSB
 * Fetch information from Antidote
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
        hostPort = DockerManager.getHostPortFromContainer(name);

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
     * To write to Antidote. Executes all the operations of antidote
     * With Notransaction
     *
     * @param operation
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
            //bucket.update(antidote.noTransaction(), update);
            return update;
        }
        return null;
    }

    /**
     * To write to Antidote. Executes all the operations of antidote
     * With Notransaction
     *
     * @param operations
     */
    public void ExecuteKeyOperations(List<Operation> operations) {

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
     * To write to Antidote. Executes all the operations of antidote
     * With Notransaction
     *
     * @param operation
     */
    public void ExecuteSingleKeyOperation(Operation operation) {

        if (running) {
            InteractiveTransaction tx = antidote.startTransaction();
            bucket.update(tx, getKeyUpdate(operation));
            tx.commitTransaction();

        }
    }

    /**
     * @param keys
     * @param operations
     */
    public void ExecuteKeyRW(List<String> keys, List<Operation> operations) {
        // TODO different data structure
        if (running) {
            InteractiveTransaction tx = antidote.startTransaction();
            for (int i = 0; i < keys.size(); i++) {
                Operation op = operations.get(i);
                if (op == null) {
                    bucket.read(tx, createKey(keys.get(i)));
                } else {
                    bucket.update(tx, getKeyUpdate(op));
                }
            }
            tx.commitTransaction();
        }
    }

    class Operation {
        public final String keyName;
        public final String operationName;
        public final Object value;

        public Operation(String keyName, String operationName, Object value) {
            this.keyName = keyName;
            this.operationName = operationName;
            this.value = value;
        }
    }


    /**
     * To read from Antidote. Execute all operations
     * With no transaction
     *
     * @param name
     * @return
     */
    public String getKeyValueNoTransaction(String name) {
        if (running) {
            Key key = createKey(name);

            //Assuming that every CRDT type returns String
            return bucket.read(antidote.noTransaction(), createKey(name)).toString();

           /*
            if (key.getType().equals(AntidotePB.CRDT_type.LWWREG)) {
            } else if (key.getType().equals(AntidotePB.CRDT_type.MVREG)) {
            } else if (key.getType().equals(AntidotePB.CRDT_type.COUNTER) || key.getType().equals(AntidotePB.CRDT_type.FATCOUNTER) || key.getType().equals(AntidotePB.CRDT_type.INTEGER)) {
                return bucket.read(antidote.noTransaction(), createKey(name)).toString();
            } else if ((key.getType().equals(AntidotePB.CRDT_type.GMAP)) || (key.getType().equals(AntidotePB.CRDT_type.AWMAP)) || (key.getType().equals(AntidotePB.CRDT_type.RRMAP))) {
                // return bucket.read(antidote.noTransaction(),)
            } else if ((key.getType().equals(AntidotePB.CRDT_type.ORSET)) || (key.getType().equals(AntidotePB.CRDT_type.RWSET))) {
            }*/
        } else
            return null;
    }

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
