/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.inject;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.onehippo.cms7.essentials.dashboard.event.listeners.InstructionsEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.LoggingPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.ValidationEventListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.google.common.eventbus.EventBus;


/**
 * Guice module for injecting event bus instance
 *
 * @version "$Id$"
 */

@Configuration
@ComponentScan(value = {"org.onehippo.cms7.essentials", "org.onehippo.cms7.essentials.dashboard.rest"})
public class ApplicationModule {


    @SuppressWarnings("StaticVariableOfConcreteClass")


    private final transient EventBus eventBus = new EventBus("Essentials Event Bus");


    @Inject
    private LoggingPluginEventListener loggingPluginEventListener;
    @Inject
    private MemoryPluginEventListener memoryPluginEventListener;
    @Inject
    private ValidationEventListener validationEventListener;
    @Inject
    private InstructionsEventListener instructionsEventListener;

    private volatile boolean initialized = false;

    @Inject
    private ApplicationContext applicationContext;

    @Bean(name = "eventBus")
    @Singleton
    public EventBus getEventBus() {
        if (!initialized) {
            eventBus.register(loggingPluginEventListener);
            eventBus.register(memoryPluginEventListener);
            eventBus.register(validationEventListener);
            eventBus.register(instructionsEventListener);
            eventBus.register(instructionsEventListener);
            initialized = true;
        }
        return eventBus;
    }


}