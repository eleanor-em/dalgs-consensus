package consensus.util;

/**
 * Represents a value that should be assigned to only once.
 */
public class OnceAssignable<T> {
    private T value;
    private boolean hasValue = false;

    /**
     * Sets the value, if it hasn't been set already.
     */
    public void setIfEmpty(T value) {
        if (!hasValue) {
            hasValue = true;
            this.value = value;
        }
    }

    /**
     * Sets the value, panicking if it has already been set.
     */
    public void set(T value) {
        if (hasValue) {
            throw new RuntimeException("Attempted to assign to OnceAssignable with existing value: " + value);
        } else {
            hasValue = true;
            this.value = value;
        }
    }

    /**
     * Gets the value, panicking if there is no value.
     */
    public T get() {
        if (!hasValue) {
            throw new RuntimeException("Attempted to get value of OnceAssignable with no value");
        } else {
            return value;
        }
    }
}
