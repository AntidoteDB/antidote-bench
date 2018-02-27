package adbm.antidote.wrappers;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.main.Main;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.Bucket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static adbm.util.helpers.GeneralUtil.anyNullOrPredicate;
import static adbm.util.helpers.GeneralUtil.isNullOrEmpty;

public class AntidoteClientWrapperChecks implements IAntidoteClientWrapper
{
    private static final Logger log = LogManager.getLogger(AntidoteClientWrapperChecks.class);

    private AntidoteClientWrapper wrapper;

    public AntidoteClientWrapperChecks(String name) {
        wrapper = new AntidoteClientWrapper(name);
    }

    public AntidoteClientWrapperChecks(AntidoteClientWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public boolean start()
    {
        return wrapper.start();
    }

    @Override
    public boolean start(String address, int port)
    {
        return wrapper.start(address, port);
    }

    @Override
    public boolean stop()
    {
        return wrapper.stop();
    }

    @Override
    public boolean isReady()
    {
        return wrapper.isReady() && Main.getDockerManager().isContainerRunning(getName()); //TODO checks
    }

    @Override
    public AntidoteClient getAntidoteClient()
    {
        return wrapper.getAntidoteClient();
    }

    @Override
    public Bucket getBucket()
    {
        return wrapper.getBucket();
    }

    @Override
    public String getName()
    {
        return wrapper.getName();
    }

    @Override
    public int getHostPort()
    {
        return wrapper.getHostPort();
    }

    @Override
    public Object readKeyValue(String keyName)
    {
        return readKeyValue(keyName, Main.getUsedTransactionType());
    }

    /**
     *
     * @param keyName The name of the key.
     * @param txType The transaction type.
     * @return
     */
    @Override
    public Object readKeyValue(String keyName, TransactionType txType)
    {
        if (isNullOrEmpty(keyName)) {
            log.error("The key name cannot be null or empty!");
            return "";
        }
        return wrapper.readKeyValue(keyName, txType);
    }

    /**
     *
     * @param keyNames An iterable of key names.
     * @return
     */
    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames)
    {
        return readKeyValues(keyNames, Main.getUsedTransactionType());
    }

    /**
     *
     * @param keyNames An iterable of key names.
     * @param txType The transaction type.
     * @return
     */
    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames, TransactionType txType)
    {
        if (keyNames == null) {
            log.error("The list of key names was null!");
            return new ArrayList<>();
        }
        if (anyNullOrPredicate(
                StreamSupport.stream(keyNames.spliterator(), false), s -> s.trim().isEmpty())) {
            log.error("The list of key name had invalid values!");
            return new ArrayList<>();
        }
        return wrapper.readKeyValues(keyNames, txType);
    }

    /**
     *
     * @param operation The update operation that is performed.
     */
    @Override
    public void updateKey(UpdateOperation operation)
    {
        updateKey(operation, Main.getUsedTransactionType());
    }

    /**
     *
     * @param operation The update operation that is performed.
     * @param txType The transaction type.
     */
    @Override
    public void updateKey(UpdateOperation operation, TransactionType txType)
    {
        if (operation == null || !operation.isValidUpdateOperation()) {
            log.error("The operation cannot be null or invalid!{}", operation == null ? "\nOperation: NULL" : operation);
            return;
        }
        wrapper.updateKey(operation, txType);
    }

    /**
     *
     * @param operations An iterable of update operations that are performed.
     */
    @Override
    public void updateKeys(Iterable<UpdateOperation> operations)
    {
        updateKeys(operations, Main.getUsedTransactionType());
    }

    /**
     *
     * @param operations An iterable of update operations that are performed.
     * @param txType The transaction type.
     */
    @Override
    public void updateKeys(Iterable<UpdateOperation> operations, TransactionType txType)
    {
        if (operations == null) {
            log.error("The list of operation was null!");
            return;
        }
        if (anyNullOrPredicate(
                StreamSupport.stream(operations.spliterator(), false), Operation::isValidUpdateOperation)) {
            log.error("The list of operation had invalid values!");
            return;
        }
        wrapper.updateKeys(operations, txType);
    }

    /**
     *
     * @param operations An iterable of operations that are performed.
     * @return
     */
    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations)
    {
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
                                             TransactionType txType)
    {
        if (operations == null) {
            log.error("The list of operation was null!");
            return new ArrayList<>();
        }
        if (anyNullOrPredicate(
                StreamSupport.stream(operations.spliterator(), false), o -> o.isValidReadOperation() || o.isValidUpdateOperation())) {
            log.error("The list of operation had invalid values!");
            return new ArrayList<>();
        }
        return wrapper.performKeyOperations(operations, txType);
    }

}
