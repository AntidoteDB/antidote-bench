package adbm.antidote;

import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.docker.DockerManager;
import adbm.settings.MapDBManager;
import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.*;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;

import static adbm.settings.MapDBManager.getTypeOfKey;
import static eu.antidotedb.client.Key.create;
import static eu.antidotedb.client.Key.map_g;

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

    public void isReady() {

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

    public void stop() {
        if (running) {
            this.firePropertyChange(AntidoteController.DCListChanged, "", "");
            running = false;
        }
    }

    public void start() {
        if (!running) {
            this.firePropertyChange(AntidoteController.DCListChanged, "", "");
            running = false;
        }
    }

    public void AddKey(String name, AntidotePB.CRDT_type type) {
        if (running) {
            AntidoteStaticTransaction tx = antidote.createStaticTransaction();
            bucket.update(tx, create(type, ByteString.copyFromUtf8(name)).reset());
            tx.commitTransaction();
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    public void RemoveKey(String name) {
        if (running) {
            MapDBManager.removeKey(name);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    /**
     * To write to Antidote. Executes all the operations of antidote
     *
     * @param name
     * @param operation
     * @param value
     */
    public void ExecuteKeyOperation(String name, String operation, Object value) {

        if (running) {
            Key key = create(getTypeOfKey(name), ByteString.copyFromUtf8(name));
            UpdateOp update = null;
            Long longObject = null;

            if (key.getType().equals(AntidotePB.CRDT_type.LWWREG)) {
                if (operation.equals("assign")) {
                    RegisterKey lwwregKey = (RegisterKey) key;
                    update = lwwregKey.assign(value);
                }

            } else if (key.getType().equals(AntidotePB.CRDT_type.MVREG)) {
                MVRegisterKey mvregKey = (MVRegisterKey) key;
                if (operation.equals("assign")) {
                    update = mvregKey.assign(value);
                } else if (operation.equals("reset")) {
                    update = mvregKey.reset();
                }
            } else if ((key.getType().equals(AntidotePB.CRDT_type.COUNTER)) || (key.getType().equals(AntidotePB.CRDT_type.FATCOUNTER))) {

                CounterKey counterKey = (CounterKey) key;
                longObject = Long.parseLong(value.toString());
                if (operation.equals("increment")) {
                    update = counterKey.increment(longObject);
                } else if (operation.equals("decrement")) {
                    update = counterKey.increment(-longObject);
                } else if (operation.equals("reset")) {
                    update = counterKey.reset();
                }
            } else if (key.getType().equals(AntidotePB.CRDT_type.INTEGER)) {
                IntegerKey integerKey = (IntegerKey) key;
                longObject = Long.parseLong(value.toString());
                if (operation.equals("increment")) {
                    update = integerKey.increment(longObject);
                } else if (operation.equals("decrement")) {
                    update = integerKey.increment(-longObject);
                } else if (operation.equals("reset")) {
                    update = integerKey.reset();
                } else if (operation.equals("set")) {
                    operation = "assign";
                    update = integerKey.assign(longObject);
                }
            } else if ((key.getType().equals(AntidotePB.CRDT_type.GMAP)) || (key.getType().equals(AntidotePB.CRDT_type.AWMAP)) || (key.getType().equals(AntidotePB.CRDT_type.RRMAP))) {
                MapKey gmapKey = (MapKey) key;

                if (operation.equals("update")) {
                    update = gmapKey.update(); // Check with Kevin
                } else if (operation.equals("removeKey")) {
                    update = gmapKey.removeKey(key);

                } else if (operation.equals("removeKeys")) {
                    // update = gmapKey.removeKeys(getMapKeyValue(gmapKey))

                } else if (operation.equals("reset")) {
                    update = gmapKey.reset();
                }
            } else if ((key.getType().equals(AntidotePB.CRDT_type.ORSET)) || (key.getType().equals(AntidotePB.CRDT_type.RWSET))) {

                SetKey rwsetSetKey = (SetKey) key;

                if (operation.equals("add")) {
                    update = rwsetSetKey.add(value);

                } else if (operation.equals("addAll")) {
                    //update=rwsetSetKey.addAll()

                } else if (operation.equals("remove")) {
                    update = rwsetSetKey.remove(value);

                } else if (operation.equals("removeAll")) {

                } else if (operation.equals("reset")) {
                    update = rwsetSetKey.reset();
                }
            }

        }
    }

    /**
     * To read from Antidote. Execute all operations
     *
     * @param name
     * @return
     */
    // read
    public String getKeyValue(String name) {
        if (running) {
            Key key = createKey(name);
            if (key.getType().equals(AntidotePB.CRDT_type.COUNTER) || key.getType().equals(AntidotePB.CRDT_type.FATCOUNTER) || key.getType().equals(AntidotePB.CRDT_type.INTEGER)) {
                return bucket.read(antidote.noTransaction(), createKey(name)).toString();
            } else if ((key.getType().equals(AntidotePB.CRDT_type.GMAP)) || (key.getType().equals(AntidotePB.CRDT_type.AWMAP)) || (key.getType().equals(AntidotePB.CRDT_type.RRMAP))) {
                // return bucket.read(antidote.noTransaction(),)
            } else if ((key.getType().equals(AntidotePB.CRDT_type.ORSET)) || (key.getType().equals(AntidotePB.CRDT_type.RWSET))) {
            }
        }
        return null;
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
        }
        return null;
    }
}
