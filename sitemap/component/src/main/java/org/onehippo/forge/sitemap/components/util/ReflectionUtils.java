/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.util;

import org.hippoecm.hst.core.component.HstComponentException;

/**
 */
public final class ReflectionUtils {
    private ReflectionUtils() {} // Hide utility class constructor

    public static <T> T obtainInstanceForClass(Class<T> returnClass, String className) {
        Class clazz;
        try {
            clazz = Class.forName(className);
            if (!returnClass.isAssignableFrom(clazz)) {
                throw new HstComponentException("Specified custom UrlInformationProvider does not implement the"
                        + "UrlInformationProvider interface");
            }
        } catch (ClassNotFoundException e) {
            throw new HstComponentException("Cannot find the class for the custom UrlInformationProvider", e);
        }

        Object instance;
        try {
            instance = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new HstComponentException("Cannot instantiate custom UrlInformationProvider class", e);
        } catch (IllegalAccessException e) {
            throw new HstComponentException("Cannot instantiate custom UrlInformationProvider class", e);
        }

        if (returnClass.isAssignableFrom(instance.getClass())) {
            return (T) instance;
        } else {
            throw new HstComponentException("Instantiated class is not a " + returnClass.getName());
        }
    }

}
