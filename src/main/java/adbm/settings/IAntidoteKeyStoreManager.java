package adbm.settings;

import adbm.util.IStartStop;
import eu.antidotedb.antidotepb.AntidotePB;

import java.util.Map;

public interface IAntidoteKeyStoreManager extends IStartStop
{
    Map<String, AntidotePB.CRDT_type> getAllKeys();

    AntidotePB.CRDT_type getTypeOfKey(String name);

    boolean addKey(String name, AntidotePB.CRDT_type type);

    boolean removeKey(String name);

    boolean populateTypeKeyMap();

    boolean resetKeyTypeSettings();
}
