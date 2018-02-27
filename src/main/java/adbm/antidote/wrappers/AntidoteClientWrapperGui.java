package adbm.antidote.wrappers;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.main.Main;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.Bucket;

import java.util.List;

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

    public boolean start()
    {
        boolean success = wrapper.start();
        this.firePropertyChange(AntidoteController.DCListChanged, "", "");
        return success;
    }

    /**
     *
     * @param address The address.
     * @param port The port.
     * @return
     */
    public boolean start(String address, int port)
    {
        boolean success = wrapper.start(address, port);
        this.firePropertyChange(AntidoteController.DCListChanged, "", "");
        return success;
    }

    public boolean stop()
    {
        boolean success = wrapper.stop();
        this.firePropertyChange(AntidoteController.DCListChanged, "", "");
        return success;
    }

    public boolean isReady()
    {
        return wrapper.isReady();
    }

    // TODO implement isReadyInfo()

    /**
     *
     * @param name
     * @param type
     */
    public void AddKey(String name, AntidotePB.CRDT_type type)
    {
        if (isReady()) {
            Main.getKeyManager().addKey(name, type);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    /**
     *
     * @param name
     */
    public void RemoveKey(String name)
    {
        if (isReady()) {
            Main.getKeyManager().removeKey(name);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
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
    public String getContainerName()
    {
        return wrapper.getContainerName();
    }

    @Override
    public int getHostPort()
    {
        return wrapper.getHostPort();
    }

    /**
     *
     * @param keyName The name of the key.
     * @return
     */
    @Override
    public Object readKeyValue(String keyName)
    {
        return wrapper.readKeyValue(keyName);
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
        return wrapper.readKeyValues(keyNames);
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
        return wrapper.readKeyValues(keyNames, txType);
    }

    /**
     *
     * @param operation The update operation that is performed.
     */
    @Override
    public void updateKey(UpdateOperation operation)
    {
        wrapper.updateKey(operation);
    }

    /**
     *
     * @param operation The update operation that is performed.
     * @param txType The transaction type.
     */
    @Override
    public void updateKey(UpdateOperation operation, TransactionType txType)
    {
        wrapper.updateKey(operation, txType);
    }

    /**
     *
     * @param operations An iterable of update operations that are performed.
     */
    @Override
    public void updateKeys(Iterable<UpdateOperation> operations)
    {
        wrapper.updateKeys(operations);
    }

    /**
     *
     * @param operations An iterable of update operations that are performed.
     * @param txType The transaction type.
     */
    @Override
    public void updateKeys(Iterable<UpdateOperation> operations, TransactionType txType)
    {
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
        return wrapper.performKeyOperations(operations);
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
        return wrapper.performKeyOperations(operations, txType);
    }

}
