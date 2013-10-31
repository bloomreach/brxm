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

import org.onehippo.cms7.essentials.dashboard.event.listeners.InstructionsEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.LoggingPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.event.listeners.ValidationEventListener;
import org.onehippo.cms7.essentials.dashboard.instruction.executors.PluginInstructionExecutor;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionExecutor;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Guice module for injecting event bus instance
 *
 * @version "$Id$"
 */
public class EventBusModule extends AbstractModule {

    @SuppressWarnings("StaticVariableOfConcreteClass")
    private static final EventBusModule instance = new EventBusModule();

    private final transient EventBus eventBus = new EventBus("Essentials Event Bus");
    private final transient LoggingPluginEventListener loggingPluginEventListener = new LoggingPluginEventListener();
    private final transient MemoryPluginEventListener memoryPluginEventListener = new MemoryPluginEventListener();
    private final transient ValidationEventListener validationEventListener = new ValidationEventListener();
    private final transient InstructionsEventListener instructionsEventListener = new InstructionsEventListener();
    private final transient InstructionExecutor instructionExecutor = new PluginInstructionExecutor();

    public void cleanup() {
        eventBus.unregister(loggingPluginEventListener);
        eventBus.unregister(memoryPluginEventListener);
        eventBus.unregister(validationEventListener);
        eventBus.unregister(instructionsEventListener);
    }

    @Override
    protected void configure() {
        bind(EventBus.class).toInstance(eventBus);

        bindListener(Matchers.any(), new TypeListener() {
            public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
                typeEncounter.register(new InjectionListener<I>() {
                    public void afterInjection(I i) {
                        eventBus.register(i);
                    }
                });

            }
        });
        bind(LoggingPluginEventListener.class).toInstance(loggingPluginEventListener);
        bind(MemoryPluginEventListener.class).toInstance(memoryPluginEventListener);
        bind(ValidationEventListener.class).toInstance(validationEventListener);
        bind(InstructionsEventListener.class).toInstance(instructionsEventListener);
        bind(InstructionExecutor.class).toInstance(instructionExecutor);

    }

    public static EventBusModule getInstance() {
        return instance;
    }

    private EventBusModule() {
    }
}