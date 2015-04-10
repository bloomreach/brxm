/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.jaxrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.message.Message;

public class CXFRepositoryJaxrsEndpoint extends RepositoryJaxrsEndpoint {

    private JAXRSInvoker invoker;
    private List<Interceptor<? extends Message>> inInterceptors;
    private List<Interceptor<? extends Message>> inFaultInterceptors;
    private List<Interceptor<? extends Message>> outInterceptors;
    private List<Interceptor<? extends Message>> outFaultInterceptors;

    public CXFRepositoryJaxrsEndpoint invoker(JAXRSInvoker invoker) {
        this.invoker = invoker;
        return this;
    }

    public JAXRSInvoker getInvoker() {
        return invoker;
    }

    public CXFRepositoryJaxrsEndpoint(final String address) {
        super(address);
    }

    public CXFRepositoryJaxrsEndpoint inInterceptor(Interceptor<? extends Message> interceptor) {
        if (inInterceptors == null) {
            inInterceptors = new ArrayList<>();
        }
        inInterceptors.add(interceptor);
        return this;
    }

    public List<Interceptor<? extends Message>> getInInterceptors() {
        return inInterceptors != null ? inInterceptors : Collections.EMPTY_LIST;
    }

    public CXFRepositoryJaxrsEndpoint inFaultInterceptor(Interceptor<? extends Message> interceptor) {
        if (inFaultInterceptors == null) {
            inFaultInterceptors = new ArrayList<>();
        }
        inFaultInterceptors.add(interceptor);
        return this;
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return inFaultInterceptors != null ? inFaultInterceptors : Collections.EMPTY_LIST;
    }

    public CXFRepositoryJaxrsEndpoint outInterceptor(Interceptor<? extends Message> interceptor) {
        if (outInterceptors == null) {
            outInterceptors = new ArrayList<>();
        }
        outInterceptors.add(interceptor);
        return this;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return outInterceptors != null ? outInterceptors : Collections.EMPTY_LIST;
    }

    public CXFRepositoryJaxrsEndpoint outFaultInterceptor(Interceptor<? extends Message> interceptor) {
        if (outFaultInterceptors == null) {
            outFaultInterceptors = new ArrayList<>();
        }
        outFaultInterceptors.add(interceptor);
        return this;
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return outFaultInterceptors != null ? outFaultInterceptors : Collections.EMPTY_LIST;
    }

    public void preCreate(JAXRSServerFactoryBean endpointFactory) {
    }

    public void postCreate(Server server) {
        server.getEndpoint().getInInterceptors().addAll(getInInterceptors());
        server.getEndpoint().getInFaultInterceptors().addAll(getInFaultInterceptors());
        server.getEndpoint().getOutInterceptors().addAll(getOutInterceptors());
        server.getEndpoint().getOutFaultInterceptors().addAll(getOutFaultInterceptors());
    }
}
