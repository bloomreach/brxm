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

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@ComponentScan("org.onehippo.cms7.essentials")
public class ApplicationModule {
    //implements } BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ApplicationModule.class);
    @SuppressWarnings("StaticVariableOfConcreteClass")

    private static final ApplicationModule instance = new ApplicationModule();

    private final transient EventBus eventBus = new EventBus("Essentials Event Bus");

    @Autowired
    private ApplicationContext applicationContext;

    @Bean(name = "eventBus")
    @Singleton
    public EventBus getEventBus() {
        return eventBus;
    }


    public static ApplicationModule getInstance() {
        return instance;
    }


}