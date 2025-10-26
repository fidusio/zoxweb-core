package org.zoxweb.server.util.cache;

import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.io.Serializable;


@SuppressWarnings("serial")
public class JCacheExpiryPolicy
        implements ExpiryPolicy, Serializable {

    private Duration creation;
    private Duration access;
    private Duration update;

    private JCacheExpiryPolicy(Duration... durations) {
        if (durations != null && durations.length > 0) {
            int index = 0;
            creation = durations[index++];
            if (durations.length > index) {
                access = durations[index++];
                if (durations.length > index) {
                    update = durations[index++];
                }
            }
        }
    }


    public static Factory<ExpiryPolicy> factoryOf(Duration... durations) {
        return new FactoryBuilder.SingletonFactory<ExpiryPolicy>(
                new JCacheExpiryPolicy(durations));
    }

    @Override
    public Duration getExpiryForCreation() {
        // TODO Auto-generated method stub
        return creation;
    }

    @Override
    public Duration getExpiryForAccess() {
        // TODO Auto-generated method stub
        return access;
    }

    @Override
    public Duration getExpiryForUpdate() {
        // TODO Auto-generated method stub
        return update;
    }

}
