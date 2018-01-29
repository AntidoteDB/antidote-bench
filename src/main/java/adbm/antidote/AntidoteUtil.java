package adbm.antidote;

import adbm.settings.MapDBManager;
import eu.antidotedb.antidotepb.AntidotePB;

import java.util.*;

public class AntidoteUtil {

    public static final EnumMap<AntidotePB.CRDT_type, String> typeGUIMap = createTypeGUIMap();

    public static final Map<String, AntidotePB.CRDT_type> guiTypeMap = createGUITypeMap();

    public static final EnumMap<AntidotePB.CRDT_type, String[]> typeOperationMap = createTypeOperationMap();

    public static final EnumMap<AntidotePB.CRDT_type, List<String>> typeKeyMap = createTypeKeyMap();

    public static List<String> getKeysForType(AntidotePB.CRDT_type type) {
        List<String> list = new ArrayList<>();
        for (String key : typeKeyMap.get(type)) {
            list.add(key);
        }
        return list;
    }

    public static void addKey(String key, AntidotePB.CRDT_type type) {
        typeKeyMap.get(type).add(key);
    }

    public static void removeKey(String key, AntidotePB.CRDT_type type) {
        typeKeyMap.get(type).remove(key);
    }

    public static String getDefaultOperation(String keyName) {
        return typeOperationMap.get(MapDBManager.getTypeOfKey(keyName))[0];
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
