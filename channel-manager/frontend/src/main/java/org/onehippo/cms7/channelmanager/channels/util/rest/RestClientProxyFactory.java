/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.channelmanager.channels.util.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.rest.ChannelService;
import org.onehippo.cms7.channelmanager.channels.util.rest.mappers.exceptions.ResponseToChannelExceptionMapper;

/**
 * A factory class to encapsulate any additional logic for creating REST client proxies
 */
public class RestClientProxyFactory implements IRestProxyService, Serializable {

    private IRestProxyService restProxyService;
    private List<Object> additionalProviders;

    @Override
    public String getContextPath() {
        return restProxyService.getContextPath();
    }

    /**
     * Creates a new instance of {@link RestClientProxyFactory}
     *
     * @param restProxyService The {@link IRestProxyService} to which {@link RestClientProxyFactory} delegates methods
     *                         calls
     * @throws IllegalArgumentException If <CODE>restProxyService</CODE> is <CODE>null</CODE>
     */
    public RestClientProxyFactory(IRestProxyService restProxyService) {
        if (restProxyService == null) {
            throw new IllegalArgumentException("REST proxy service can not be 'null'");
        }

        this.restProxyService = restProxyService;
        this.additionalProviders = new ArrayList<Object>(1);
        init();
    }

    /**
     * Creates a proxy to a REST service based on the provided class
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T>                 the generic type of the REST service API class.
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    public <T> T createRestProxy(final Class<T> restServiceApiClass) {
        // For now we only need the additional providers when {@link ChannelService} is used
        // These rules are hardcoded yes, but that is for now only
        if (restServiceApiClass.equals(ChannelService.class)) {
            return this.restProxyService.createSecureRestProxy(restServiceApiClass, this.additionalProviders);
        }

        return this.restProxyService.createSecureRestProxy(restServiceApiClass);
    }

    /**
     * Creates a proxy to a REST service based on the provided class and security {@link javax.security.auth.Subject} A
     * security {@link javax.security.auth.Subject} which indicates that the caller wants a security context to be
     * propagated with the REST call
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T>                 the generic type of the REST service API class.
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    public <T> T createSecureRestProxy(final Class<T> restServiceApiClass) {
        // For now we only need the additional providers when {@link ChannelService} is used
        // These rules are hardcoded yes, but that is for now only
        if (restServiceApiClass.equals(ChannelService.class)) {
            return this.restProxyService.createSecureRestProxy(restServiceApiClass, this.additionalProviders);
        }

        return this.restProxyService.createSecureRestProxy(restServiceApiClass);
    }

    /**
     * Creates a proxy to a REST service based on the provided class
     * <p/>
     * <p/>
     * This version takes addition list of providers to configure the client proxy with </P>
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T>                 the generic type of the REST service API class.
     * @param additionalProviders {@link java.util.List} of additional providers to configure client proxies with
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    @Override
    public <T> T createRestProxy(final Class<T> restServiceApiClass, final List<Object> additionalProviders) {
        throw new UnsupportedOperationException("This method should not be called");
    }

    /**
     * Creates a proxy to a REST service based on the provided class and security {@link javax.security.auth.Subject} A
     * security {@link javax.security.auth.Subject} which indicates that the caller wants a security context to be
     * propagated with the REST call
     * <p/>
     * <p/>
     * This version takes addition list of providers to configure the client proxy with </P>
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T>                 the generic type of the REST service API class.
     * @param additionalProviders {@link java.util.List} of additional providers to configure client proxies with
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    @Override
    public <T> T createSecureRestProxy(final Class<T> restServiceApiClass, final List<Object> additionalProviders) {
        throw new UnsupportedOperationException("This method should not be called");
    }

    protected void init() {
        this.additionalProviders.add(new ResponseToChannelExceptionMapper());
    }

}
