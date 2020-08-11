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
package org.onehippo.cms7.crisp.hst.module;

import java.lang.reflect.Method;

import org.hippoecm.hst.core.container.ComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal utility to access the operations of <code>org.hippoecm.hst.site.HstServices</code> in hst-commons JAR,
 * which can be located in either SITE or CMS war while CRISP API and configuration can optionally shared between
 * two wars.
 */
final class HstServicesAccess {

    private static Logger log = LoggerFactory.getLogger(HstServicesAccess.class);

    private static final String HST_SERVICES_CLASS_NAME = "org.hippoecm.hst.site.HstServices";
    private static final String IS_AVAILABLE_METHOD_NAME = "isAvailable";
    private static final String GET_COMPONENT_MANAGER_METHOD_NAME = "getComponentManager";

    private static final Class<?>[] EMPTY_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_ARGS = new Object[0];

    private HstServicesAccess() {
    }

    static boolean isAvailable() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            final Class<?> hstServicesClazz = contextClassLoader.loadClass(HST_SERVICES_CLASS_NAME);
            final Method isAvailableMethod = hstServicesClazz.getMethod(IS_AVAILABLE_METHOD_NAME, EMPTY_ARG_TYPES);
            final Boolean available = (Boolean) isAvailableMethod.invoke(hstServicesClazz, EMPTY_ARGS);
            return (available != null && available.booleanValue());
        } catch (Throwable th) {
            log.warn("Cannot access {}#{}().", HST_SERVICES_CLASS_NAME, IS_AVAILABLE_METHOD_NAME, th.toString());
        }

        return false;
    }

    static ComponentManager getComponentManager() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            final Class<?> hstServicesClazz = contextClassLoader.loadClass(HST_SERVICES_CLASS_NAME);
            final Method getComponentManagerMethod = hstServicesClazz.getMethod(GET_COMPONENT_MANAGER_METHOD_NAME,
                    EMPTY_ARG_TYPES);
            return (ComponentManager) getComponentManagerMethod.invoke(hstServicesClazz, EMPTY_ARGS);
        } catch (Throwable th) {
            log.warn("Cannot access {}#{}().", HST_SERVICES_CLASS_NAME, GET_COMPONENT_MANAGER_METHOD_NAME,
                    th.toString());
        }

        return null;
    }
}
