package adbm.antidote.operations;

public class UpdateOperation <T> extends Operation <T>
{
    public UpdateOperation(String keyName, String operationName, T value)
    {
        super(keyName, operationName, value);
    }
}
