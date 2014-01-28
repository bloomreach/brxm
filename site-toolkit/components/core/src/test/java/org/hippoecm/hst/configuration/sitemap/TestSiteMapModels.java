/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSiteMapModels extends AbstractTestConfigurations {

    private HstManager hstManager;
    private EventPathsInvalidator invalidator;
    private HippoSession session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
        invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }


    protected HippoSession createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @Test
    public void test_sitemap_inheritance_without_workspace_sitemap() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/subsite");
        final HstSite hstSite = mount.getMount().getHstSite();

        //hstSite.getSiteMap()

    }
}
