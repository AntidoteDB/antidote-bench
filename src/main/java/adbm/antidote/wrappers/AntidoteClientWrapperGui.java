package adbm.antidote.wrappers;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.main.Main;
import adbm.util.EverythingIsNonnullByDefault;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.Bucket;

import javax.annotation.Nullable;
import java.util.List;

@EverythingIsNonnullByDefault
public class AntidoteClientWrapperGui extends AntidoteModel implements IAntidoteClientWrapper
{

    private AntidoteClientWrapperChecks wrapper;

    public AntidoteClientWrapperGui(String name, String containerName)
    {
        wrapper = new AntidoteClientWrapperChecks(name, containerName);
    }

    public AntidoteClientWrapperGui(AntidoteClientWrapperChecks wrapper)
    {
        this.wrapper = wrapper;
    }

    public void AddKey(String name, AntidotePB.CRDT_type type)
    {
        if (isReady()) {
            Main.getKeyManager().addKey(name, type);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    public void RemoveKey(String name)
    {
        if (isReady()) {
            Main.getKeyManager().removeKey(name);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    @Override
    public boolean start()
    {
        boolean success = wrapper.start();
        this.firePropertyChange(AntidoteController.DCListChanged, "", "");
        return success;
    }

    @Override
    public boolean start(@Nullable String address, int port)
    {
        boolean success = wrapper.start(address, port);
        this.firePropertyChange(AntidoteController.DCListChanged, "", "");
        return success;
    }

    @Override
    public boolean stop()
    {
        boolean success = wrapper.stop();
        this.firePropertyChange(AntidoteController.DCListChanged, "", "");
        return success;
    }

    @Override
    public boolean isReady()
    {
        return wrapper.isReady();
    }

    // TODO implement isReadyInfo()

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
        return wrapper.readKeyValue(keyName);
    }

    @Override
    public Object readKeyValue(String keyName, TransactionType txType)
    {
        return wrapper.readKeyValue(keyName, txType);
    }

    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames)
    {
        return wrapper.readKeyValues(keyNames);
    }

    @Override
    public List<Object> readKeyValues(Iterable<String> keyNames, TransactionType txType)
    {
        return wrapper.readKeyValues(keyNames, txType);
    }

    @Override
    public void updateKey(UpdateOperation operation)
    {
        wrapper.updateKey(operation);
    }

    @Override
    public void updateKey(UpdateOperation operation, TransactionType txType)
    {
        wrapper.updateKey(operation, txType);
    }

    @Override
    public void updateKeys(Iterable<UpdateOperation> operations)
    {
        wrapper.updateKeys(operations);
    }

    @Override
    public void updateKeys(Iterable<UpdateOperation> operations, TransactionType txType)
    {
        wrapper.updateKeys(operations, txType);
    }

    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations)
    {
        return wrapper.performKeyOperations(operations);
    }

    @Override
    public List<Object> performKeyOperations(Iterable<Operation> operations,
                                             TransactionType txType)
    {
        return wrapper.performKeyOperations(operations, txType);
    }

}
