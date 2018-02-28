package adbm.settings;

import adbm.util.IStartStop;
import eu.antidotedb.antidotepb.AntidotePB;

import java.util.List;
import java.util.Map;

/**
 *
 */
public interface IAntidoteKeyStoreManager extends IStartStop
{
    /**
     *
     * @return
     */
    Map<String, AntidotePB.CRDT_type> getMapKeyNameKeyType();

    /**
     *
     * @param keyName
     * @return
     */
    AntidotePB.CRDT_type getTypeOfKey(String keyName);

    /**
     *
     * @param keyName
     * @param keyType
     * @return
     */
    boolean addKey(String keyName, AntidotePB.CRDT_type keyType);

    /**
     *
     * @param keyName
     * @return
     */
    boolean removeKey(String keyName);

    /**
     *
     * @return
     */
    Map<AntidotePB.CRDT_type, List<String>> getKeyTypeKeyNamesMap();

    /**
     *
     * @return
     */
    boolean resetKeyTypeSettings();
}
