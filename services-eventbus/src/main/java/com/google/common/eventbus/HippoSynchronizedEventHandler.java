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
package com.google.common.eventbus;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Event handler that sets up the contex classloader when dispatching events.
 */
public class HippoSynchronizedEventHandler extends SynchronizedEventHandler {

    private final Annotation[] annotations;
    private final ClassLoader classLoader;

    public HippoSynchronizedEventHandler(final Object target, final Method method, ClassLoader contextClassLoader) {
        super(target, method);
        this.annotations = method.getAnnotations();
        this.classLoader = contextClassLoader;
    }

    public Annotation[] getAnnotations() {
        if (annotations == null) {
            return new Annotation[0];
        }
        return annotations.clone();
    }

    @Override
    public synchronized void handleEvent(final Object event) throws InvocationTargetException {
        ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            super.handleEvent(event);
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
    }
}
