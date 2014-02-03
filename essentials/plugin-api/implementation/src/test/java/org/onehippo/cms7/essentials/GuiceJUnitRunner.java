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



public class GuiceJUnitRunner extends BlockJUnit4ClassRunner {

    public GuiceJUnitRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        Class<?>[] classes = getModulesFor(clazz);

    }

    @Override
    public Object createTest() throws Exception {
        Object obj = super.createTest();
        return obj;
    }


    private Class<?>[] getModulesFor(Class<?> clazz) throws InitializationError {
        GuiceJUnitModules annotation = clazz.getAnnotation(GuiceJUnitModules.class);
        if (annotation == null) {
            throw new InitializationError("Missing @GuiceModules annotation for unit test '" + clazz.getName() + '\'');
        }
        return annotation.value();
    }


}