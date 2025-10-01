package org.zoxweb.shared.util;

public interface InstanceFactory {


    interface ParamsCreator<T> {

        /**
         * Create a new instance based on the type T
         * @param params to be set
         * @return new instance of T
         */
        T newInstance(Object... params);
    }

    interface Creator<T> {
        /**
         * Create a new instance based on the type T
         * @return  new instance of T
         */
        T newInstance();
    }
}
