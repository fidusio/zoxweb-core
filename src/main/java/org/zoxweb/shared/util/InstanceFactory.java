package org.zoxweb.shared.util;

/**
 * Factory interfaces for creating object instances.
 * Provides three levels of parameterization:
 * <ul>
 *     <li>{@link Creator} - no-arg factory</li>
 *     <li>{@link ParamCreator} - single typed parameter factory</li>
 *     <li>{@link ParamsCreator} - varargs factory</li>
 * </ul>
 * All interfaces are functional (SAM) and can be used as lambda targets.
 */
public interface InstanceFactory {

    /**
     * Factory that creates instances of type {@code T} from a variable number of parameters.
     *
     * @param <T> the type of object to create
     */
    interface ParamsCreator<T> {

        /**
         * Creates a new instance of type {@code T}.
         *
         * @param params the construction parameters
         * @return new instance of {@code T}
         */
        T newInstance(Object... params);
    }

    /**
     * Factory that creates instances of type {@code T} from a single typed input parameter.
     * <p>This is a functional interface suitable for use with lambdas, e.g.:
     * <pre>{@code
     * ParamCreator<NVBase<?>, NVConfig> creator = (nvc) -> new NVInt(nvc.getName(), 0);
     * }</pre>
     *
     * @param <T> the type of object to create
     * @param <I> the input parameter type
     */
    interface ParamCreator<T, I> {

        /**
         * Creates a new instance of type {@code T} from the given input.
         *
         * @param input the construction parameter
         * @return new instance of {@code T}
         */
        T newInstance(I input);
    }

    /**
     * Factory that creates instances of type {@code T} with no parameters.
     *
     * @param <T> the type of object to create
     */
    interface Creator<T> {

        /**
         * Creates a new instance of type {@code T}.
         *
         * @return new instance of {@code T}
         */
        T newInstance();
    }
}
