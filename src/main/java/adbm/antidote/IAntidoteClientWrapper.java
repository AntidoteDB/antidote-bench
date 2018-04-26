package adbm.antidote;

import adbm.antidote.operations.Operation;
import adbm.antidote.operations.UpdateOperation;
import adbm.util.EverythingIsNonnullByDefault;
import adbm.util.IStartStop;
import eu.antidotedb.client.AntidoteClient;
import eu.antidotedb.client.Bucket;

import javax.annotation.Nullable;
import java.util.List;

@EverythingIsNonnullByDefault
public interface IAntidoteClientWrapper extends IStartStop
{

    /**
     * An enum that contains all available transactions types in the AntidoteClient.
     * They are used to configure database operations and to benchmarks different types of transactions.
     */
    enum TransactionType {
        NoTransaction,
        StaticTransaction,
        InteractiveTransaction
    }

    /**
     * Returns the AntidoteClient that is used to create transactions.
     * For further extensions.
     * @return The AntidoteClient.
     */
    @Nullable
    AntidoteClient getAntidoteClient();

    /**
     * Returns the bucket that is used to perform database operations.
     * For further extensions.
     * @return The Bucket.
     */
    @Nullable
    Bucket getBucket();

    /**
     * Returns the name of the AntidoteClientWrapper which is also the name of the container where Antidote database is running.
     * @return The name.
     */
    String getName();

    /**
     * Returns the name of the AntidoteClientWrapper which is also the name of the container where Antidote database is running.
     * @return The name.
     */
    String getContainerName();

    /**
     * Returns the host port of the AntidoteClientWrapper's container which can be used to connect different Antidote containers.
     * @return The host port.
     */
    int getHostPort();

    /**
     * Performs the operation with the transaction type that is set in the Main class.
     * Static transactions are performed as interactive transactions.
     * Reads the value of the key in the Antidote database that is found under the given key name.
     * Returns the value as an object which dependent on the CRDT type of the key.
     * @param keyName The name of the key.
     * @return The value of the key.
     */
    Object readKeyValue(String keyName);

    /**
     * Performs the operation with the given transaction type.
     * Static transactions are performed as interactive transactions.
     * Reads the value of the key in the Antidote database that is found under the given key name.
     * Returns the value as an object which dependent on the CRDT type of the key.
     * @param keyName The name of the key.
     * @param txType The transaction type.
     * @return The value of the key.
     */
    Object readKeyValue(String keyName, TransactionType txType);

    /**
     * Performs a BatchRead with the transaction type that is set in the Main class.
     * Static transactions are performed as interactive transactions.
     * Reads the values of all keys as part of a batch operation and returns a list of the key values.
     * Returns the values as objects which are dependent on the CRDT type of the keys.
     * @param keyNames An iterable of key names.
     * @return The list of values of the given keys.
     */
    List<Object> readKeyValues(Iterable<String> keyNames);

    /**
     * Performs a BatchRead with the given transaction type.
     * Static transactions are performed as interactive transactions.
     * Reads the values of all keys as part of a batch operation and returns a list of the key values.
     * Returns the values as objects which are dependent on the CRDT type of the keys.
     * @param keyNames An iterable of key names.
     * @param txType The transaction type.
     * @return The list of values of the given keys.
     */
    List<Object> readKeyValues(Iterable<String> keyNames, TransactionType txType);

    /**
     * Performs a key update with the transaction type that is set in the Main class.
     * The operation contains all necessary information to perform the key update.
     * @param operation The update operation that is performed.
     */
    void updateKey(UpdateOperation operation);

    /**
     * Performs a key update with the given transaction type.
     * The operation contains all necessary information to perform the key update.
     * @param operation The update operation that is performed.
     * @param txType The transaction type.
     */
    void updateKey(UpdateOperation operation, TransactionType txType);

    /**
     * Performs multiple key updates with the transaction type that is set in the Main class.
     * The operations contain all necessary information to perform the key updates.
     * @param operations An iterable of update operations that are performed.
     */
    void updateKeys(Iterable<UpdateOperation> operations);

    /**
     * Performs multiple key updates with the given transaction type.
     * The operations contain all necessary information to perform the key updates.
     * @param operations An iterable of update operations that are performed.
     * @param txType The transaction type.
     */
    void updateKeys(Iterable<UpdateOperation> operations, TransactionType txType);

    /**
     * Performs combined read and update operations on keys with the transaction type that is set in the Main class.
     * Static transactions are performed as interactive transactions.
     * The operations contain all necessary information to perform the key reads/updates.
     * Returns the key values of all read operations.
     * @param operations An iterable of operations that are performed.
     * @return The key values of all read operations.
     */
    List<Object> performKeyOperations(Iterable<Operation> operations);

    /**
     * Performs combined read and update operations on keys with the given transaction type.
     * Static transactions are performed as interactive transactions.
     * The operations contain all necessary information to perform the key reads/updates.
     * Returns the key values of all read operations.
     * @param operations An iterable of operations that are performed.
     * @param txType The transaction type.
     * @return The key values of all read operations.
     */
    List<Object> performKeyOperations(Iterable<Operation> operations, TransactionType txType);

}
