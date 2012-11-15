/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.cms7.channelmanager.channels.util.rest.mappers.exceptions.util.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.rest.ChannelService;
import org.onehippo.cms7.channelmanager.channels.util.rest.mappers.exceptions.ResponseToChannelExceptionMapper;

/**
 * A factory class to encapsulate any additional logic for creating REST client proxies
 */
public class RestClientProxyFactory implements Serializable {

    private IRestProxyService restProxyService;
    private List<Object> additionalProviders;

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
            return this.restProxyService.createRestProxy(restServiceApiClass, this.additionalProviders);
        }

        return this.restProxyService.createRestProxy(restServiceApiClass);
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

    protected void init() {
        this.additionalProviders.add(new ResponseToChannelExceptionMapper());
    }

}
