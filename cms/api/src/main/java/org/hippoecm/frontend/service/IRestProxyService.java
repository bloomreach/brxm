package org.hippoecm.frontend.service;

import java.util.List;

import javax.security.auth.Subject;

import org.apache.wicket.IClusterable;

/**
 * Creates proxies to talk to REST services. Proxies are generated from classes that specify the API of
 * the REST service to use.
 */
public interface IRestProxyService extends IClusterable {

    /**
     * Creates a proxy to a REST service based on the provided class
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T> the generic type of the REST service API class.
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    <T> T createRestProxy(Class<T> restServiceApiClass);

    /**
     * Creates a proxy to a REST service based on the provided class and security {@link Subject}
     * A security {@link Subject} which indicates that the caller wants a security context to be propagated with the REST call
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T> the generic type of the REST service API class.
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    <T> T createSecureRestProxy(Class<T> restServiceApiClass);

    /**
     * Creates a proxy to a REST service based on the provided class
     *
     * <P>
     * This version takes addition list of providers to configure the client proxy with
     * </P>
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T> the generic type of the REST service API class.
     * @param additionalProviders {@link List} of additional providers to configure client proxies with
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    <T> T createRestProxy(Class<T> restServiceApiClass, List<Object> additionalProviders);

    /**
     * Creates a proxy to a REST service based on the provided class and security {@link Subject}
     * A security {@link Subject} which indicates that the caller wants a security context to be propagated with the REST call
     *
     * <P>
     * This version takes addition list of providers to configure the client proxy with
     * </P>
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T> the generic type of the REST service API class.
     * @param additionalProviders {@link List} of additional providers to configure client proxies with
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    <T> T createSecureRestProxy(Class<T> restServiceApiClass, List<Object> additionalProviders);

}
