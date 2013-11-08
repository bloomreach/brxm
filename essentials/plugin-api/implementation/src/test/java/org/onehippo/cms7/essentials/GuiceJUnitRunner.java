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

package org.onehippo.cms7.essentials;

/**
 * @version "$Id$"
 */

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;


public class GuiceJUnitRunner extends BlockJUnit4ClassRunner {
    private Injector injector;

    public GuiceJUnitRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        Class<?>[] classes = getModulesFor(clazz);
        injector = parseModules(classes);
    }

    @Override
    public Object createTest() throws Exception {
        Object obj = super.createTest();
        injector.injectMembers(obj);
        return obj;
    }

    private Injector parseModules(Class<?>[] classes) throws InitializationError {
        Module[] modules = new Module[classes.length];
        for (int i = 0; i < classes.length; i++) {
            try {

                final Class<?> myClass = classes[i];
                if (myClass.equals(EventBusModule.class)) {
                    modules[i] = EventBusModule.getInstance();
                    continue;
                }


                modules[i] = (Module) myClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new InitializationError(e);
            }
        }
        return Guice.createInjector(modules);
    }

    private Class<?>[] getModulesFor(Class<?> clazz) throws InitializationError {
        GuiceJUnitModules annotation = clazz.getAnnotation(GuiceJUnitModules.class);
        if (annotation == null) {
            throw new InitializationError("Missing @GuiceModules annotation for unit test '" + clazz.getName() + '\'');
        }
        return annotation.value();
    }


}