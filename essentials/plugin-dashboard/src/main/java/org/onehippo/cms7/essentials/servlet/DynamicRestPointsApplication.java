/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.servlet;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */

@ApplicationPath("/dynamic")
public class DynamicRestPointsApplication extends Application {

    @Singleton
    @Inject
    private EventBus eventBus;


    @Singleton
    @Inject
    private AutowireCapableBeanFactory injector;

    @Singleton
    @Inject
    private ApplicationContext applicationContext;


    private final Set<Class<?>> classes = new HashSet<>();

    @Override
    public Set<Object> getSingletons() {
        final Set<Object> singletons = new HashSet<>();
        singletons.add(eventBus);
        singletons.add(injector);
        singletons.add(applicationContext);
        return singletons;
    }
    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    public void addClass(final Class<?> clazz) {
        classes.add(clazz);
    }

}
