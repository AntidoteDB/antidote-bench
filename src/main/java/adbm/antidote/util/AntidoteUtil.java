package adbm.antidote.util;

import adbm.main.Main;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import eu.antidotedb.antidotepb.AntidotePB;
import eu.antidotedb.client.Key;

import java.util.*;

/**
 *
 */
public class AntidoteUtil {



    public static final EnumMap<AntidotePB.CRDT_type, String> typeGUIMap = createTypeGUIMap();

    public static final Map<String, AntidotePB.CRDT_type> guiTypeMap = createGUITypeMap();

    public static final EnumMap<AntidotePB.CRDT_type, String[]> typeOperationMap = createTypeOperationMap();

    public static final EnumMap<AntidotePB.CRDT_type, List<String>> typeKeyMap = createTypeKeyMap();

    public static Key createKeyFromMapDB(String name)
    {
        return Key.create(Main.getKeyManager().getTypeOfKey(name), ByteString.copyFromUtf8(name));
    }

    public static Key createKey(String name)
    {
        if (!Main.isGuiMode()) { //TODO maybe performance optimization!
            return createKey(name, Main.getBenchmarkConfig().getUsedKeyType());
        }
        return createKeyFromMapDB(name);
    }

    public static Key createKey(String name, AntidotePB.CRDT_type type)
    {
        return Key.create(type, ByteString.copyFromUtf8(name));
    }

    public static List<String> getKeysForType(AntidotePB.CRDT_type type) {
        return new ArrayList<>(typeKeyMap.get(type));
    }

    public static void addKey(String key, AntidotePB.CRDT_type type) {
        typeKeyMap.get(type).add(key);
    }

    public static void removeKey(String key, AntidotePB.CRDT_type type) {
        typeKeyMap.get(type).remove(key);
    }

    public static String getDefaultOperation(String keyName) {
        return typeOperationMap.get(Main.getKeyManager().getTypeOfKey(keyName))[0];
    }

    private static Map<String, AntidotePB.CRDT_type> createGUITypeMap() {
        Map<String, AntidotePB.CRDT_type> map = new LinkedHashMap<>();
        map.put("Last-writer-wins Register (LWW-Register)", AntidotePB.CRDT_type.LWWREG);
        map.put("Multi-value Register (MV-Register)", AntidotePB.CRDT_type.MVREG);
        map.put("Counter", AntidotePB.CRDT_type.COUNTER);
        map.put("Fat Counter", AntidotePB.CRDT_type.FATCOUNTER);
        map.put("Integer", AntidotePB.CRDT_type.INTEGER);
        // No Flags
        map.put("Grow-only Map (G-Map)", AntidotePB.CRDT_type.GMAP);
        map.put("Add-wins Map (AW-Map)", AntidotePB.CRDT_type.AWMAP);
        map.put("Remove-Resets Map (RR-Map)", AntidotePB.CRDT_type.RRMAP);
        // No Grow-only Set (G-Set)
        map.put("Add-wins Set (AW-Set / OR-Set)", AntidotePB.CRDT_type.ORSET);
        map.put("Remove-wins Set (RW-Set)", AntidotePB.CRDT_type.RWSET);
        return map;
    }

    private static EnumMap<AntidotePB.CRDT_type, String> createTypeGUIMap() {
        EnumMap<AntidotePB.CRDT_type, String> map = new EnumMap<>(AntidotePB.CRDT_type.class);
        map.put(AntidotePB.CRDT_type.LWWREG, "Last-writer-wins Register (LWW-Register)");
        map.put(AntidotePB.CRDT_type.MVREG, "Multi-value Register (MV-Register)");
        map.put(AntidotePB.CRDT_type.COUNTER, "Counter");
        map.put(AntidotePB.CRDT_type.FATCOUNTER, "Fat Counter");
        map.put(AntidotePB.CRDT_type.INTEGER, "Integer");
        // No Flags
        map.put(AntidotePB.CRDT_type.GMAP, "Grow-only Map (G-Map)");
        map.put(AntidotePB.CRDT_type.AWMAP, "Add-wins Map (AW-Map)");
        map.put(AntidotePB.CRDT_type.RRMAP, "Remove-Resets Map (RR-Map)");
        // No Grow-only Set (G-Set)
        map.put(AntidotePB.CRDT_type.ORSET, "Add-wins Set (AW-Set / OR-Set)");
        map.put(AntidotePB.CRDT_type.RWSET, "Remove-wins Set (RW-Set)");
        return map;
    }

    private static EnumMap<AntidotePB.CRDT_type, String[]> createTypeOperationMap() {
        EnumMap<AntidotePB.CRDT_type, String[]> map = new EnumMap<>(AntidotePB.CRDT_type.class);
        map.put(AntidotePB.CRDT_type.LWWREG, new String[]{"assign"});
        map.put(AntidotePB.CRDT_type.MVREG, new String[]{"assign", "reset"});
        map.put(AntidotePB.CRDT_type.COUNTER, new String[]{"increment", "decrement"});
        map.put(AntidotePB.CRDT_type.FATCOUNTER, new String[]{"increment", "decrement", "reset"});
        map.put(AntidotePB.CRDT_type.INTEGER, new String[]{"increment", "decrement", "set", "reset"});
        // No Flags
        map.put(AntidotePB.CRDT_type.GMAP, new String[]{"update", "removeKey", "removeKeys", "reset"});
        map.put(AntidotePB.CRDT_type.AWMAP, new String[]{"update", "removeKey", "removeKeys", "reset"});
        map.put(AntidotePB.CRDT_type.RRMAP, new String[]{"update", "removeKey", "removeKeys", "reset"});
        // No Grow-only Set (G-Set)
        map.put(AntidotePB.CRDT_type.ORSET, new String[]{"add", "addAll", "remove", "removeAll", "reset"});
        map.put(AntidotePB.CRDT_type.RWSET, new String[]{"add", "addAll", "remove", "removeAll", "reset"});
        return map;
    }

    private static final HashSet<String> allOperations = new HashSet<>(Arrays.asList("assign", "increment", "decrement", "reset", "set", "update", "removeKey", "removeKeys", "add", "addAll", "remove", "removeAll"));

    public static boolean isValidOperation(String operation) {
        return allOperations.contains(operation);
    }

    public static final Map<String, AntidotePB.CRDT_type> STRING_CRDT_TYPE_MAP = createStringEnumMap(AntidotePB.CRDT_type.values());

    private static <T extends Enum<T>> Map<String, T> createStringEnumMap(T[] values) {
        Map<String, T> map = Maps.newHashMapWithExpectedSize(values.length);
        for (T value : values) {
            map.put(value.name().toUpperCase(), value);
        }
        return map;
    }

    private static EnumMap<AntidotePB.CRDT_type, List<String>> createTypeKeyMap() {
        EnumMap<AntidotePB.CRDT_type, List<String>> map = new EnumMap<>(AntidotePB.CRDT_type.class);
        map.put(AntidotePB.CRDT_type.LWWREG, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.MVREG, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.COUNTER, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.FATCOUNTER, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.INTEGER, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.GMAP, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.AWMAP, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.RRMAP, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.ORSET, new ArrayList<>());
        map.put(AntidotePB.CRDT_type.RWSET, new ArrayList<>());
        return map;
    }

}
