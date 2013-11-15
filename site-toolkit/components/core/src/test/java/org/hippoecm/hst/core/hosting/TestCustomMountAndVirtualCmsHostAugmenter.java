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
package org.hippoecm.hst.core.hosting;

import java.util.List;

import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link CustomMountAndVirtualHostAugmenter}.
 */
public class TestCustomMountAndVirtualCmsHostAugmenter extends AbstractTestConfigurations {

    @Test
    public void reverse() {
        String[] abc = {"a", "b", "c"};
        CustomMountAndVirtualCmsHostAugmenter.reverse(abc);
        assertArrayEquals("Array should be reversed", new String[] { "c", "b", "a" }, abc);

        String[] a = {"a"};
        CustomMountAndVirtualCmsHostAugmenter.reverse(a);
        assertArrayEquals("Reversed array of one element should be identical", new String[] { "a" }, a);

        String[] empty = {};
        CustomMountAndVirtualCmsHostAugmenter.reverse(empty);
        assertArrayEquals("Empty array should still be empty", new String[0], empty);
    }

    @Test
    public void testCMSHostAugmentation() throws Exception {
        /*
         *   Since the unit test content contains three hostgroups with cmslocations
         * 1) http://localhost:8080/cms
         * 2) http://testgroup.example.com:8080/cms
         * 3) http://globalandsubset.example.com:8080/cms
         *
         * we expect after the host augmentation is finished, these three hosts all should match
         */
        HstManager hstSitesManager = getComponent(HstManager.class.getName());
        final List<String> hostGroupNames = hstSitesManager.getVirtualHosts().getHostGroupNames();

        assertTrue(hostGroupNames.size() == 3);

        CustomMountAndVirtualCmsHostAugmenter augmenter = new CustomMountAndVirtualCmsHostAugmenter();
        augmenter.setMountName("_rp");
        augmenter.setMountType("composer");
        augmenter.setMountNamedPipeline("ComposerPipeline");

        augmenter.augment((MutableVirtualHosts)hstSitesManager.getVirtualHosts());

        final List<String> hostGroupNamesAfterAugmentation = hstSitesManager.getVirtualHosts().getHostGroupNames();
        // there are three cms hosts, but localhost is already present. Hence, only hostgroups for testgroup.example.com and
        // globalandsubset.example.com should be added

        assertTrue(hostGroupNamesAfterAugmentation.size() == 5);

        final ResolvedMount localHostComposerMount = hstSitesManager.getVirtualHosts().matchMount("localhost", "", "_rp");
        assertNotNull(localHostComposerMount);
        assertEquals(localHostComposerMount.getResolvedVirtualHost().getVirtualHost().getHostGroupName(), "dev-localhost");

        final ResolvedMount testGroupHostComposerMount = hstSitesManager.getVirtualHosts().matchMount("testgroup.example.com", "", "_rp");
        assertNotNull(testGroupHostComposerMount);
        assertTrue(testGroupHostComposerMount.getResolvedVirtualHost().getVirtualHost().getHostGroupName().startsWith("org.hippoecm.hst.core.hosting.CustomMountAndVirtualCmsHostAugmenter-"));

        final ResolvedMount globalAndSubsetComposerMount = hstSitesManager.getVirtualHosts().matchMount("globalandsubset.example.com", "", "_rp");

        assertNotNull(globalAndSubsetComposerMount);
        assertTrue(globalAndSubsetComposerMount.getResolvedVirtualHost().getVirtualHost().getHostGroupName().startsWith("org.hippoecm.hst.core.hosting.CustomMountAndVirtualCmsHostAugmenter-"));

    }

}
