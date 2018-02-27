package adbm.antidote.wrappers;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.antidote.util.AntidoteUtil;
import adbm.main.Main;
import adbm.util.AdbmConstants;
import eu.antidotedb.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static adbm.util.helpers.GeneralUtil.isNullOrEmpty;

/**
 * Bridge between Antidote and benchmarking tools
 * This implementation of IAntidoteClientWrapper has the least amount of overhead.
 * It only performs the operations without validating the connection or parameters.
 * It is used by other implementations to perform the basic operations and add extra functionality.
 * For benchmarking this class should be used because it avoids most expensive checks.
 * Of course you need to be sure that the input parameters are always valid otherwise there will be unhandled exceptions.
 * If you can't be sure the input parameter will always be valid then use the AntidoteClientWrapperChecks class.
 */
public class AntidoteClientWrapper implements IAntidoteClientWrapper {

    //TODO currently only one AntidoteClientWrapper is allowed per container because they share the same name

    private static final Logger log = LogManager.getLogger(AntidoteClientWrapper.class);

    /**
     * The AntidoteClient that is used to create transactions.
     */
    private AntidoteClient antidoteClient;

    /**
     * The bucket that is used for performing database operations.
     */
    private Bucket bucket;

    /**
     * The name of this wrapper which is also the name of the corresponding container.
     */
    private final String name; //TODO check if final

    /**
     * The host port which is important when connecting Antidote containers with each other (replication)
     */
    private int hostPort;

    /**
     * Constructor of the AntidoteClientWrapper.
     * Makes sure the corresponding container is running.
     * Stores the host port of the container.
     * Creates a new AntidoteClient. (Currently only localhost connections are implemented)
     * Creates a bucket.
     *
     * @param name The name of the container where the AntidoteClient will be running.
     */
    public AntidoteClientWrapper(String name) {
        if (isNullOrEmpty(name)) {
            name = "BAD_NAME";
        }
        this.name = name;
    }

    public boolean start() {
        return start(null, 0);
    }

    /**
     * @param address The address.
     * @param port    The port.
     * @return
     */
    public boolean start(String address, int port) {
        boolean success = Main.getDockerManager().runContainer(name);
        if (!success) return false;
        if (port <= 0) {
            hostPort = Main.getDockerManager().getHostPortsFromContainer(name).get(0);
            if (hostPort <= 0) return false;
        }
        if (isNullOrEmpty(address)) {
            address = AdbmConstants.antidoteHost;
        }
        try {
            antidoteClient = new AntidoteClient(new InetSocketAddress(address, hostPort));
            bucket = Bucket.bucket(name + "bucket");
            return true;
        } catch (Exception e) {
            log.error("An error occurred while creating a Antidote Client!", e);
        }
        return false;
    }

    public boolean stop() {
        boolean success = Main.getDockerManager().stopContainer(name);
        antidoteClient = null;
        bucket = null;
        hostPort = 0;
        return success;
    }

    public boolean isReady() {
        return hostPort <= 0 && antidoteClient != null && bucket != null;
    }

    @Override
    public AntidoteClient getAntidoteClient() {
        return antidoteClient;
    }

    @Override
    public Bucket getBucket() {
        return bucket;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getHostPort() {
        return hostPort;
    }

    /**
     *
     * @param keyName The name of the key.
     * @return
     */
    @Override
    public Object readKeyValue(String keyName) {
        return readKeyValue(keyName, Main.getUsedTransactionType());
    }

    /**
     *
     * @param keyName The name of the key.
     * @param txType The transaction type.
     * @return
     */
    @Override
    public Object readKeyValue(String keyName, TransactionType txType) {
        switch (txType) {
            case NoTransaction:
                return bucket.read(antidoteClient.noTransaction(), AntidoteUtil.createKey(keyName));
            case StaticTransaction:
            case InteractiveTransaction:
            default:
                Object returnObject;
                try (InteractiveTransaction tx = antidoteClient.startTransaction()) {
                    returnObject = bucket.read(tx, AntidoteUtil.createKey(keyName));
                    tx.commitTransaction();
                }
                return returnObject;
        }
    }

    /**
     *
     * @param keyNames An iterable of key names.
     * @return
     */
    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames) {
        return readKeyValues(keyNames, Main.getUsedTransactionType());
    }

    /**
     *
     * @param keyNames An iterable of key names.
     * @param txType The transaction type.
     * @return
     */
    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames, TransactionType txType) {
        List<Object> results = new ArrayList<>();
        BatchRead batchRead = antidoteClient.newBatchRead();
        List<BatchReadResult> batchReadResults = new ArrayList<>();
        for (String name : keyNames) {
            batchReadResults.add(bucket.read(batchRead, AntidoteUtil.createKey(name)));
        }
        switch (txType) {
            case NoTransaction:
                batchRead.commit(antidoteClient.noTransaction());
                break;
            case StaticTransaction:
            case InteractiveTransaction:
            default:
                try (InteractiveTransaction tx = antidoteClient.startTransaction()) {
                    batchRead.commit(tx);
                    tx.commitTransaction();
                }
                break;
        }
        for (BatchReadResult result : batchReadResults) {
            results.add(result.get());
        }
        return results;
    }

    /**
     *
     * @param operation The update operation that is performed.
     */
    @Override
    public void updateKey(UpdateOperation operation) {
        updateKey(operation, Main.getUsedTransactionType());
    }

    /**
     *
     * @param operation The update operation that is performed.
     * @param txType The transaction type.
     */
    @Override
    public void updateKey(UpdateOperation operation, TransactionType txType) {
        switch (txType) {
            case NoTransaction:
                bucket.update(antidoteClient.noTransaction(), getKeyUpdateOp(operation));
                break;
            case StaticTransaction:
                AntidoteStaticTransaction tx = antidoteClient.createStaticTransaction();
                bucket.update(tx, getKeyUpdateOp(operation));
                tx.commitTransaction();
                break;
            case InteractiveTransaction:
            default:
                try (InteractiveTransaction txi = antidoteClient.startTransaction()) {
                    bucket.update(txi, getKeyUpdateOp(operation));
                    txi.commitTransaction();
                }
                break;
        }
    }

    /**
     *
     * @param operations An iterable of update operations that are performed.
     */
    @Override
    public void updateKeys(Iterable<UpdateOperation> operations) {
        updateKeys(operations, Main.getUsedTransactionType());
    }

    /**
     *
     * @param operations An iterable of update operations that are performed.
     * @param txType The transaction type.
     */
    @Override
    public void updateKeys(Iterable<UpdateOperation> operations, TransactionType txType) {
        List<UpdateOp> updates = new ArrayList<>();

        for (UpdateOperation operation : operations) {
            updates.add(getKeyUpdateOp(operation));
        }
        switch (txType) {
            case NoTransaction:
                bucket.updates(antidoteClient.noTransaction(), updates);
                break;
            case StaticTransaction:
                AntidoteStaticTransaction tx = antidoteClient.createStaticTransaction();
                bucket.updates(tx, updates);
                tx.commitTransaction();
                break;
            case InteractiveTransaction:
            default:
                try (InteractiveTransaction itx = antidoteClient.startTransaction()) {
                    bucket.updates(itx, updates);
                    itx.commitTransaction();
                }
                break;
        }
    }

    /**
     *
     * @param operations An iterable of operations that are performed.
     * @return
     */
    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations) {
        return performKeyOperations(operations, Main.getUsedTransactionType());
    }

    /**
     *
     * @param operations An iterable of operations that are performed.
     * @param txType The transaction type.
     * @return
     */
    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations,
                                             TransactionType txType) {
        List<Object> returnList = new ArrayList<>();
        switch (txType) {
            case NoTransaction:
                for (Operation op : operations) {
                    if (op.read) {
                        returnList.add(bucket.read(antidoteClient.noTransaction(), AntidoteUtil.createKey(op.keyName)));
                    } else {
                        bucket.update(antidoteClient.noTransaction(), getKeyUpdateOp((UpdateOperation) op));
                    }
                }
                break;
            case StaticTransaction:
            case InteractiveTransaction:
            default:
                try (InteractiveTransaction tx = antidoteClient.startTransaction()) {
                    for (Operation op : operations) {
                        if (op.read) {
                            returnList.add(bucket.read(tx, AntidoteUtil.createKey(op.keyName)));
                        } else {
                            bucket.update(tx, getKeyUpdateOp((UpdateOperation) op));
                        }
                    }
                    tx.commitTransaction();
                }
                break;
        }
        return returnList;
    }

    /**
     * Returns the UpdateOp for a UpdateOperation.
     * It does not interact with the database.
     *
     * @param operation The operation.
     * @return The UpdateOp.
     */
    private UpdateOp getKeyUpdateOp(UpdateOperation operation) {
        String name = operation.keyName;
        String operationName = operation.operationName;
        Object value = operation.value;
        Key key = AntidoteUtil.createKey(name);
        long longObject;
        try {
            switch (key.getType()) {
                case LWWREG:
                    RegisterKey<String> lwwregKey = (RegisterKey<String>) key;
                    switch (operationName) {
                        case "assign":
                            return lwwregKey.assign(value.toString());
                        default:
                            return operationDoesNotExist(key, operation);
                    }
                case MVREG:
                    MVRegisterKey<String> mvregKey = (MVRegisterKey<String>) key;
                    switch (operationName) {
                        case "assign":
                            return mvregKey.assign(value.toString());
                        case "reset":
                            return mvregKey.reset();
                        default:
                            return operationDoesNotExist(key, operation);
                    }
                case COUNTER:
                case FATCOUNTER:
                    try {
                        longObject = Long.parseLong(value.toString());
                    } catch (NumberFormatException e) {
                        longObject = value.toString().length();
                    }
                    CounterKey counterKey = (CounterKey) key;
                    switch (operationName) {
                        case "increment":
                            return counterKey.increment(longObject);
                        case "decrement":
                            return counterKey.increment(-longObject);
                        case "reset":
                            return counterKey.reset();
                        default:
                            return operationDoesNotExist(key, operation);
                    }
                case INTEGER:
                    try {
                        longObject = Long.parseLong(value.toString());
                    } catch (NumberFormatException e) {
                        longObject = value.toString().length();
                    }
                    IntegerKey integerKey = (IntegerKey) key;
                    switch (operationName) {
                        case "increment":
                            return integerKey.increment(longObject);
                        case "decrement":
                            return integerKey.increment(-longObject);
                        case "reset":
                            return integerKey.reset();
                        case "set":
                            return integerKey.assign(longObject);
                        default:
                            return operationDoesNotExist(key, operation);
                    }
                case GMAP:
                case AWMAP:
                case RRMAP:
                    MapKey gmapKey = (MapKey) key;
                    switch (operationName) {
                        case "update":
                            return returnFailure(key, operation); //TODO not implemented
                        case "removeKey":
                            return returnFailure(key, operation); //TODO not implemented
                        case "removeKeys":
                            return returnFailure(key, operation); //TODO not implemented
                        case "reset":
                            return gmapKey.reset();
                        default:
                            return operationDoesNotExist(key, operation);
                    }
                case ORSET:
                case RWSET:
                    SetKey<String> rwsetSetKey = (SetKey<String>) key;
                    switch (operationName) {
                        case "add":
                            return rwsetSetKey.add(value.toString());
                        case "addAll":
                            if (value instanceof Iterable) {
                                List<String> all = new ArrayList<>();
                                for (Object part : (Iterable) value) {
                                    all.add(part.toString());
                                }
                                return rwsetSetKey.addAll(all);
                            }
                            return rwsetSetKey.addAll(value.toString());
                        case "remove":
                            return rwsetSetKey.remove(value.toString());
                        case "removeAll":
                            if (value instanceof Iterable) {
                                List<String> all = new ArrayList<>();
                                for (Object part : (Iterable) value) {
                                    all.add(part.toString());
                                }
                                return rwsetSetKey.removeAll(all);
                            }
                            return rwsetSetKey.removeAll(value.toString());
                        case "reset":
                            return rwsetSetKey.reset();
                        default:
                            return operationDoesNotExist(key, operation);
                    }
                default:
                    log.error("The key type {} does not match any known key types!", key.getType());
                    return returnFailure(key, operation);
            }
        } catch (Exception e) {
            log.error("An error occurred while creating a key update!", e);
            return returnFailure(key, operation);
        }
    }

    /**
     *
     * @param key
     * @param operation
     * @return
     */
    private UpdateOp operationDoesNotExist(Key key, UpdateOperation operation) {
        log.error("The key type {} does not have the operation {}!", AntidoteUtil.typeGUIMap.get(key.getType()),
                operation.operationName);
        return returnFailure(key, operation);
    }

    /**
     *
     * @param key
     * @param operation
     * @return
     */
    private UpdateOp returnFailure(Key key, UpdateOperation operation) {
        log.error("Operation on Key failed!\nKey: {}{}", key, operation);
        return null;
    }

}
