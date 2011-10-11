package org.hippoecm.frontend.service;

import org.apache.wicket.IClusterable;

/**
 * Creates proxies to talk to REST services. Proxies are generated from classes that specify the API of
 * the REST service to use.
 */
public interface IRestProxyService extends IClusterable {

    /**
     * Creates a proxy to a REST service based on the provided class.
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T> the generic type of the REST service API class.
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    <T> T createRestProxy(Class<T> restServiceApiClass);

}
