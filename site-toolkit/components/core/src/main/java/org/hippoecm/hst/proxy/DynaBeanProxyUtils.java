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
package org.hippoecm.hst.proxy;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.proxy.Invoker;

/**
 * DynaBeanProxyUtils
 * 
 * @version $Id$
 */
public class DynaBeanProxyUtils
{
    private DynaBeanProxyUtils()
    {
    }
    
    public static Object createDynaBeanProxy(final DynaBean dynaBean, Class<?> ... proxyInterfaces) {
        ProxyFactory factory = new ProxyFactory();
        Invoker invoker = new DynaBeanMethodInvoker(dynaBean);
        return factory.createInvokerProxy(invoker, proxyInterfaces);
    }
    
}
