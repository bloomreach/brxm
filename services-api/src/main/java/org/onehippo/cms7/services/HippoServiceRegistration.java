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
package org.onehippo.cms7.services;

/**
 * The Service Registration represents a service with its context.
 * Services that are obtained in this way, e.g. because they were registered as extensions with the whiteboard pattern,
 * must be invoked with the context classloader set to the classloader in this registration object.
 */
public class HippoServiceRegistration {

    private final ClassLoader classLoader;
    private final Object service;

    HippoServiceRegistration(final ClassLoader classLoader, final Object service) {
        this.classLoader = classLoader;
        this.service = service;
    }

    public Object getService() {
        return service;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
