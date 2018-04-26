package adbm.antidote.wrappers;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.main.Main;
import adbm.util.EverythingIsNonnullByDefault;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.Bucket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@EverythingIsNonnullByDefault
public class AntidoteClientWrapperChecks implements IAntidoteClientWrapper
{
    private static final Logger log = LogManager.getLogger(AntidoteClientWrapperChecks.class);

    private AntidoteClientWrapper wrapper;

    public AntidoteClientWrapperChecks(String name, String containerName)
    {
        wrapper = new AntidoteClientWrapper(name, containerName);
    }

    public AntidoteClientWrapperChecks(AntidoteClientWrapper wrapper)
    {
        this.wrapper = wrapper;
    }

    @Override
    public boolean start()
    {
        return wrapper.start();
    }

    @Override
    public boolean start(@Nullable String address, int port)
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

    @Nullable
    @Override
    public AntidoteClient getAntidoteClient()
    {
        return wrapper.getAntidoteClient();
    }

    @Nullable
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
    public String getContainerName()
    {
        return wrapper.getContainerName();
    }

    @Override
    public int getHostPort()
    {
        return wrapper.getHostPort();
    }

    @Override
    public Object readKeyValue(String keyName)
    {
        return readKeyValue(keyName, Main.getAntidoteYCSBConfiguration().getUsedTransactionType());
    }

    @Override
    public Object readKeyValue(String keyName, TransactionType txType)
    {
        if (keyName.trim().isEmpty()) {
            log.error("The key name cannot be null or empty!");
            return "";
        }
        return wrapper.readKeyValue(keyName, txType);
    }

    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames)
    {
        return readKeyValues(keyNames, Main.getAntidoteYCSBConfiguration().getUsedTransactionType());
    }

    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames, TransactionType txType)
    {
        if (StreamSupport.stream(keyNames.spliterator(), false).anyMatch(s -> s.trim().isEmpty())) {
            log.error("The list of key name had invalid values!");
            return new ArrayList<>();
        }
        return wrapper.readKeyValues(keyNames, txType);
    }

    @Override
    public void updateKey(UpdateOperation operation)
    {
        updateKey(operation, Main.getAntidoteYCSBConfiguration().getUsedTransactionType());
    }

    @Override
    public void updateKey(UpdateOperation operation, TransactionType txType)
    {
        if (!operation.isValidUpdateOperation()) {
            log.error("The operation cannot be null or invalid!{}", operation);
            return;
        }
        wrapper.updateKey(operation, txType);
    }

    @Override
    public void updateKeys(Iterable<UpdateOperation> operations)
    {
        updateKeys(operations, Main.getAntidoteYCSBConfiguration().getUsedTransactionType());
    }

    @Override
    public void updateKeys(Iterable<UpdateOperation> operations, TransactionType txType)
    {
        if (StreamSupport.stream(operations.spliterator(), false).anyMatch(Operation::isValidUpdateOperation))
        {
            log.error("The list of operation had invalid values!");
            return;
        }
        wrapper.updateKeys(operations, txType);
    }

    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations)
    {
        return performKeyOperations(operations, Main.getAntidoteYCSBConfiguration().getUsedTransactionType());
    }

    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations,
                                             TransactionType txType)
    {
        if (StreamSupport.stream(operations.spliterator(), false).anyMatch(o -> o.isValidReadOperation() || o.isValidUpdateOperation()))
        {
            log.error("The list of operation had invalid values!");
            return new ArrayList<>();
        }
        return wrapper.performKeyOperations(operations, txType);
    }

}
