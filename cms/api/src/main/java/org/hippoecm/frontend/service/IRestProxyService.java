/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.service;

import java.util.List;

import javax.security.auth.Subject;

import org.apache.wicket.util.io.IClusterable;

/**
 * Creates proxies to talk to REST services. Proxies are generated from classes that specify the API of
 * the REST service to use.
 */
public interface IRestProxyService extends IClusterable {

    /**
     * The context path for this {@link IRestProxyService}. The context path for ROOT.war is an empty String. Other allowed
     * values must start with a '/' and are not allowed to have another '/'. If not configured, <code>null</code> is returned
     * @return the context path for which this rest proxy service is available, optionally <code>null</code> when not configured
     */
    String getContextPath();

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
