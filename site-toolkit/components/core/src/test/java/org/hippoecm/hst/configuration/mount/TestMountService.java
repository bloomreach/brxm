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
package org.hippoecm.hst.configuration.mount;


import java.util.List;

import com.google.common.collect.ImmutableList;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class TestMountService extends AbstractTestConfigurations {

    private HstManager hstManager;
    @Before
    public void setUp() throws Exception {
        super.setUp();
        hstManager = getComponent(HstManager.class.getName());
    }

    @Test
    public void test_mount_properties_return_immutable_list() throws Exception {
        final ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount("www.unit.test", "/site", "/");
        final List<String> propertyNames = resolvedMount.getMount().getPropertyNames();
        assertTrue(propertyNames instanceof ImmutableList);
    }

    @Test
    public void test_mount_properties_dont_return_inherited() throws Exception {

        /*
            + hst:root
                - hst:alias = root
                - hst:mountpoint = /hst:hst/hst:sites/unittestproject
                + custompipeline
                     - hst:homepage = home
                     - hst:namedpipeline = CustomPipeline
         */

        final ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount("www.unit.test", "/site", "/");
        final ResolvedMount subResolvedMount = hstManager.getVirtualHosts().matchMount("www.unit.test", "/site", "/custompipeline");
        // subResolvedMount inherits mountpoint from parent
        assertEquals(resolvedMount.getMount().getMountPoint(), subResolvedMount.getMount().getMountPoint());

        final List<String> propertyNames = resolvedMount.getMount().getPropertyNames();
        assertTrue(propertyNames.contains("hst:alias"));
        assertTrue(propertyNames.contains("hst:mountpoint"));
        assertEquals("root", resolvedMount.getMount().getProperty("hst:alias"));
        assertEquals("/hst:hst/hst:sites/unittestproject",resolvedMount.getMount().getProperty("hst:mountpoint"));

        final List<String> subPropertyNames = subResolvedMount.getMount().getPropertyNames();

        assertTrue(subPropertyNames.contains("hst:homepage"));
        assertTrue(subPropertyNames.contains("hst:namedpipeline"));
        assertFalse(subPropertyNames.contains("hst:alias"));
        assertFalse(subPropertyNames.contains("hst:mountpoint"));

        assertEquals("home", subResolvedMount.getMount().getProperty("hst:homepage"));
        assertEquals("CustomPipeline", subResolvedMount.getMount().getProperty("hst:namedpipeline"));



    }

}
