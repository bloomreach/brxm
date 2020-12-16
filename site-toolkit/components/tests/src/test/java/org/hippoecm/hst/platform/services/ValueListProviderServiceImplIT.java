/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.platform.services;

import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.platform.api.ValueListProviderService;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueListProviderServiceImplIT extends AbstractTestConfigurations {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void test_value_list_provider_service() {
        ValueListProviderService valueListProviderService = componentManager.getComponent(
            "ValueListProviderService", "org.hippoecm.hst.platform");

        ValueListProvider testValueListProvider = valueListProviderService.getProvider("test-provider");

        assertEquals("test-provider has 3 values",3, testValueListProvider.getValues().size());
        assertEquals("first value in test-provider is test1","test1", testValueListProvider.getValues().get(0));
        assertEquals("first value in test-provider is test2","test2", testValueListProvider.getValues().get(1));
        assertEquals("first value in test-provider is test3","test3", testValueListProvider.getValues().get(2));

        ValueListProvider testValueListProvider2 = valueListProviderService.getProvider("test-provider");
        assertTrue("The same instance of test-provider is used",testValueListProvider == testValueListProvider2);
    }
}
