/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
 *
 */
package org.hippoecm.frontend.util;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.mock.core.container.MockComponentManager;
import org.hippoecm.hst.mock.core.container.MockContainerConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InterpolationUtilsTest {

    private static final Map<String, String> PLATFORM_PROPS = Collections
            .unmodifiableMap(MapUtils.putAll(new LinkedHashMap<String, String>(),
                    new String[] {
                            "public.brx.smEndpoint", "https://core.dxpapi.com/api/v1/core/",
                            "public.brx.smAccountId", "1234"
                            }));

    private static final String EXAMPLE_FRONTEND_CONFIG = "{\r\n  \"sm.widgets.url\": \"http://core.dxpapi.com/api/v1/merchant/widgets?account_id=${public.brx.smAccountId}\"\r\n}";

    private ComponentManager oldComponentManager;

    @Before
    public void setUp() throws Exception {
        final MockContainerConfiguration containerConfiguration = new MockContainerConfiguration();
        PLATFORM_PROPS.forEach((key, value) -> {
            containerConfiguration.setProperty(String.class, key, value);
        });

        final MockComponentManager componentManager = new MockComponentManager() {
            @Override
            public ContainerConfiguration getContainerConfiguration() {
                return containerConfiguration;
            }
        };

        this.oldComponentManager = HstServices.getComponentManager();
        HstServices.setComponentManager(componentManager);
    }

    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(oldComponentManager);
    }

    @Test
    public void testInterpolation() throws Exception {
        // null safe
        assertEquals(null, InterpolationUtils.interpolate(null));
        // with no expressions
        assertEquals("Hello, World!", InterpolationUtils.interpolate("Hello, World!"));
        assertEquals("The endpoint is https://core.dxpapi.com/api/v1/core/.",
                InterpolationUtils.interpolate("The endpoint is ${public.brx.smEndpoint}."));
        assertEquals("The account ID is 1234.",
                InterpolationUtils.interpolate("The account ID is ${public.brx.smAccountId}."));
        assertEquals("Invocation URL: https://core.dxpapi.com/api/v1/core/?account_id=1234&foo=bar", InterpolationUtils
                .interpolate("Invocation URL: ${public.brx.smEndpoint}?account_id=${public.brx.smAccountId}&foo=bar"));
        // Unresolved variable references should remain as given.
        assertEquals("Account ID: 1234, Domain Key: ${public.brx.smDomainKey}", InterpolationUtils
                .interpolate("Account ID: ${public.brx.smAccountId}, Domain Key: ${public.brx.smDomainKey}"));
        assertEquals(
                "{\r\n  \"sm.widgets.url\": \"http://core.dxpapi.com/api/v1/merchant/widgets?account_id=1234\"\r\n}",
                InterpolationUtils.interpolate(EXAMPLE_FRONTEND_CONFIG));
    }
}
