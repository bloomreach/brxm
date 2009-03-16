/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.jcr.pool;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;

public class ReadOnlyPooledSessionDecoratorProxyFactoryImpl extends PooledSessionDecoratorProxyFactoryImpl {

    protected Map<String, Boolean> readOnlyMethodMap;
    
    public ReadOnlyPooledSessionDecoratorProxyFactoryImpl() {
        this.readOnlyMethodMap = new HashMap<String, Boolean>();
        this.readOnlyMethodMap.put("move", Boolean.TRUE);
        this.readOnlyMethodMap.put("save", Boolean.TRUE);
        this.readOnlyMethodMap.put("removeLockToken", Boolean.TRUE);
        this.readOnlyMethodMap.put("setNamespacePrefix", Boolean.TRUE);
    }

    protected Interceptor getInterceptor() {
        return new ReadOnlyPooledSessionInterceptor();
    }

    protected class ReadOnlyPooledSessionInterceptor extends PooledSessionInterceptor {
        public Object intercept(Invocation invocation) throws Throwable {
            Object ret = null;
            String methodName = invocation.getMethod().getName();

            if (readOnlyMethodMap.containsKey(methodName)) {
                throw new UnsupportedOperationException("Read-only session does not support this operation: " + methodName);
            } else {
                ret = super.intercept(invocation);
            }

            return ret;
        }
    }
}
