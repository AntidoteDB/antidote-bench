package adbm.antidote.wrappers;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.antidote.ui.AntidoteController;
import adbm.antidote.ui.AntidoteModel;
import adbm.settings.MapDBManager;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.Bucket;

import java.util.List;

public class AntidoteClientWrapperGui extends AntidoteModel implements IAntidoteClientWrapper
{


    private AntidoteClientWrapperChecks wrapper;

    private boolean running;

    public boolean isReady() {
        return running;
    }

    /**
     * Constructor of the class
     *
     * @param name
     */
    public AntidoteClientWrapperGui(String name)
    {
        wrapper = new AntidoteClientWrapperChecks(name);
        running = true;
    }

    /**
     * Constructor of the class
     *
     */
    public AntidoteClientWrapperGui(AntidoteClientWrapperChecks wrapper)
    {
        this.wrapper = wrapper;
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

    public void AddKey(String name, AntidotePB.CRDT_type type)
    {
        if (running) {
            MapDBManager.addKey(name, type);
            this.firePropertyChange(AntidoteController.KeyListChanged, "", "");
        }
    }

    public void RemoveKey(String name)
    {
        if (running) {
            MapDBManager.removeKey(name);
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
