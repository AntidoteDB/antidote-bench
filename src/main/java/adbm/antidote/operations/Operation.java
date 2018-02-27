package adbm.antidote.operations;

import adbm.antidote.util.AntidoteUtil;

import static adbm.util.helpers.FormatUtil.format;

public class Operation<T> {

    public final boolean read;
    public final String keyName;
    public final String operationName;
    public final T value;

    /**
     * Defining the Operation used in the wrappers
     *
     * @param keyName
     * @param operationName
     * @param value
     */
    public Operation(String keyName, String operationName, T value) {
        this.read = false;
        this.keyName = keyName;
        this.operationName = operationName;
        this.value = value;
    }

    /**
     *
     * @param keyName
     */
    public Operation(String keyName) {
        this.read = true;
        this.keyName = keyName;
        this.operationName = null;
        this.value = null;
    }

    public boolean isValid() {
        return keyName != null && !keyName.isEmpty();
    }

    public boolean isValidReadOperation() {
        return read && isValid();
    }

    public boolean isValidUpdateOperation() {
        return !read && isValid() && AntidoteUtil.isValidOperation(operationName) && value != null;
    }

    private static final String notAllowed = " (NOT ALLOWED)";

    private static final String nullNotAllowed = "NULL" + notAllowed;

    private static final String emptyStringNotAllowed = "NULL" + notAllowed;

    @Override
    public String toString() {
        String key = keyName == null ? nullNotAllowed : keyName
                .isEmpty() ? emptyStringNotAllowed : keyName;
        String operation = operationName == null ? nullNotAllowed : AntidoteUtil
                .isValidOperation(operationName) ? operationName : operationName + notAllowed;
        String valueString = value == null ? nullNotAllowed : value.toString();
        return format("\nOperation String:\nread: {}\nkeyName: {}\noperationName: {}\nvalue: {}", read, key, operation,
                valueString);
    }
}
