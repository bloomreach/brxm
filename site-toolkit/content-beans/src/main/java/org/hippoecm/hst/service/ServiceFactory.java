/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.service;

import javax.jcr.Node;

import org.hippoecm.hst.proxy.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory util class to create lightweight JCR Node mapped POJO.
 * 
 * @version $Id$
 */
public class ServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(ServiceFactory.class);

    /**
     * Create and returns a lightweight JCR Node mapped POJO.
     * <P>
     * If the <CODE>proxyInterfacesOrDelegateeClass</CODE> argument is array of interfaces, then
     * this method will create a proxy instance implementing those interfaces after setting
     * a underlying {@link org.hippoecm.hst.service.Service} object for the proxy.
     * </P>
     * <P>
     * If the <CODE>proxyInterfacesOrDelegateeClass</CODE> argument is one-length array and
     * its own element is an instantiable delegatee class, then this method will instantiate the class after setting
     * a underlying {@link org.hippoecm.hst.service.Service} object.
     * </P> 
     * 
     * @param <T>
     * @param node JCR Node
     * @param proxyInterfacesOrDelegateeClass interfaces should implement or delegatee class which may implement interface(s).
     * @return proxy object or delegatee object
     * @throws Exception
     */
    public static <T> T create(Node node, Class ... proxyInterfacesOrDelegateeClass) throws Exception {
        T proxy = null;
        
        Service service = new AbstractJCRService(node) {
            private static final long serialVersionUID = 1L;

            public Service[] getChildServices() {
                return null;
            }
        };

        if (proxyInterfacesOrDelegateeClass.length == 1 && !proxyInterfacesOrDelegateeClass[0].isInterface()) {
            proxy = (T) proxyInterfacesOrDelegateeClass[0].newInstance();
        } else {
            log.warn("ServiceFactory#create support for proxyInterfacesOrDelegateeClass argument(s) which is" +
                    "a interface(s) is not supported any more. Only classes.");
            proxy = (T) ProxyUtils.createBeanAccessProviderProxy(new ServiceBeanAccessProviderImpl(service), proxyInterfacesOrDelegateeClass);
        }
        
        if (proxy instanceof UnderlyingServiceAware) {
            ((UnderlyingServiceAware) proxy).setUnderlyingService(service);
        }
        
        return proxy;
    }
    
}
