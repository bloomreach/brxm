/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class ProxyUtils {
    public static <T> T createProxy(final ClassLoader objectClassLoader, final Class<T> proxyInterface, final T object) {
        return  (T) Proxy.newProxyInstance(objectClassLoader, new Class[]{proxyInterface}, (proxy, method, args) -> {
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            if (currentContextClassLoader == objectClassLoader) {
                try {
                    return method.invoke(object, args);
                } catch (InvocationTargetException ite) {
                    throw (ite.getCause() != null) ? ite.getCause() : ite;
                }
            }
            Thread.currentThread().setContextClassLoader(objectClassLoader);
            try {
                return method.invoke(object, args);
            } catch (InvocationTargetException ite) {
                throw (ite.getCause() != null) ? ite.getCause() : ite;
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
        });
    }
}
