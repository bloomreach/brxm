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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.Test;

import junit.framework.Assert;
import static junit.framework.Assert.assertNull;
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
         * Since the unit test content has:
         * 1) for 'dev-localhost' hostgroup cmsLocation = http://localhost:8080/cms, http://com.localhost:8080/cms
         * 2) for 'testgroup' hostgroup cmsLocation = http://testgroup.example.com:8080/cms, http://cms.unit.test, http://sub.cms.unit.test
         * 3) for 'globalAndSubSetGroupEnvironment' hostgroup cmsLocation = http://globalandsubset.example.com:8080/cms
         *
         * we expect the cms hosts to be added to the hostgroups they belong to
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

        assertEquals(3, hostGroupNamesAfterAugmentation.size());


        String[] localhostCmsHosts = new String[]{"localhost:8080","com.localhost:8080"};
        for (String localhostCmsHost : localhostCmsHosts) {
            final ResolvedMount localHostComposerMount = hstSitesManager.getVirtualHosts().matchMount(localhostCmsHost, "", "_rp");
            assertNotNull(localHostComposerMount);
            assertEquals(localHostComposerMount.getMount().getVirtualHost().getHostGroupName(), "dev-localhost");
        }

        String[] testGroupCmsHosts = new String[]{"testgroup.example.com:8080","cms.unit.test", "sub.cms.unit.test"};
        for (String testGroupCmsHost : testGroupCmsHosts) {
            final ResolvedMount testGroupComposerMount = hstSitesManager.getVirtualHosts().matchMount(testGroupCmsHost, "", "_rp");
            assertNotNull(testGroupComposerMount);
            assertEquals(testGroupComposerMount.getMount().getVirtualHost().getHostGroupName(), "testgroup");
        }


        String[] globalAndSubSetGroupEnvironmentCmsHosts = new String[]{"globalandsubset.example.com"};
        for (String globalAndSubSetGroupEnvironmentCmsHost : globalAndSubSetGroupEnvironmentCmsHosts) {
            final ResolvedMount globalAndSubSetGroupEnvironmentComposerMount = hstSitesManager.getVirtualHosts().matchMount(globalAndSubSetGroupEnvironmentCmsHost, "", "_rp");
            assertNotNull(globalAndSubSetGroupEnvironmentComposerMount);
            assertEquals(globalAndSubSetGroupEnvironmentComposerMount.getMount().getVirtualHost().getHostGroupName(), "globalAndSubSetGroupEnvironment");
        }

    }

    @Test
    public void test_cannot_add_cms_location_for_host_that_exists_in_other_hostgroup_already() throws Exception {
        // the hostgroup 'testgroup' contains alreay a host 'www.unit.test' : This makes that host impossible to be used as CMS host for another host group as
        // that would result in exact same hosts in multiple host groups. This unit test confirms that
        // for this, we add to dev-localhost hostgroup a cmslocation = http://www.unit.test : This host
        // should not become available as cms host for dev-localhost
        Session session = createSession();
        try {
            createHstConfigBackup(session);
            session.getNode("/hst:hst/hst:hosts/dev-localhost")
                    .setProperty(HstNodeTypes.VIRTUALHOSTGROUP_PROPERTY_CMS_LOCATION, "http://www.unit.test");
            session.save();
            Thread.sleep(100);

            HstManager hstSitesManager = getComponent(HstManager.class.getName());
            final List<String> hostGroupNames = hstSitesManager.getVirtualHosts().getHostGroupNames();

            assertTrue(hostGroupNames.size() == 3);

            CustomMountAndVirtualCmsHostAugmenter augmenter = new CustomMountAndVirtualCmsHostAugmenter();
            augmenter.setMountName("_rp");
            augmenter.setMountType("composer");
            augmenter.setMountNamedPipeline("ComposerPipeline");

            augmenter.augment((MutableVirtualHosts)hstSitesManager.getVirtualHosts());

            final ResolvedMount justTheWwwUnitTestRootMount = hstSitesManager.getVirtualHosts().matchMount("www.unit.test", "/site", "_rp");
            assertNotNull(justTheWwwUnitTestRootMount);
            assertEquals("",justTheWwwUnitTestRootMount.getResolvedMountPath());
            assertEquals("hst:root",justTheWwwUnitTestRootMount.getMount().getName());

        } finally {
            restoreHstConfigBackup(session);
        }
    }



    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

}
