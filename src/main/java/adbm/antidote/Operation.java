package adbm.antidote;

public class Operation
{
    //TODO maybe do getters
    public final String keyName;
    public final String operationName;
    public final Object value;

    public Operation(String keyName, String operationName, Object value) {
        this.keyName = keyName;
        this.operationName = operationName;
        this.value = value;
    }
}
