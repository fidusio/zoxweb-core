package org.zoxweb.shared.util;

public interface IDGenerator<I, N>
        extends GetName, DataEncoder<N, I>, DataDecoder<I, N>, Validator<I> {

    /**
     * Generate a unique random ID.
     * @return a new ID.
     */
    I generateID();

    N generateNativeID();

    default boolean isValid(I toValidate) {
        try {
            return decode(toValidate) != null;
        } catch (Exception e) {
            // Validation failure - return false as expected
        }
        return false;
    }


}
