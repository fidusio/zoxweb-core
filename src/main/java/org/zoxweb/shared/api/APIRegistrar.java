package org.zoxweb.shared.api;

import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.RegistrarMap;

import java.util.LinkedHashMap;

public class APIRegistrar
        extends RegistrarMap<String, APIServiceProvider<?, ?>, APIRegistrar> {
    public static final APIRegistrar SINGLETON = new APIRegistrar();
    private volatile APIServiceProvider<?, ?> asDefault = null;

    private APIRegistrar() {
        super(new LinkedHashMap<>());
    }

    public synchronized APIRegistrar register(APIServiceProvider<?, ?> apiServiceProvider) {
        register(apiServiceProvider.getName(), apiServiceProvider);
        return this;
    }

    public <V> V getDefault() {
        return (V) asDefault;
    }

    public <V> APIRegistrar setDefault(V asDefault) {
        this.asDefault = (APIServiceProvider<?, ?>) asDefault;
        return this;
    }


    public NVGenericMap stats(boolean detailed) {

        NVGenericMap ret = null;
        if (asDefault != null)
            ret = asDefault.ping(detailed);

        if (ret == null)
            ret = new NVGenericMap();

        ret.setName("api-registrar");
        return ret;
    }
}
