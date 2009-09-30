/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.configuration;

import static org.junit.Assert.assertNotNull;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestConfigurationWithoutRepository extends AbstractSpringTestCase {

    protected static final String TESTPROJECT_NAME = "testproject";

    private HstSites hstSites;
    private HstSiteMapMatcher siteMapMatcher;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.hstSites = getComponent(HstSites.class.getName());
        this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
    }

    //@Test
    public void testConfiguration() {
        HstSite hstSite = hstSites.getSite(TESTPROJECT_NAME);
        assertNotNull(hstSite);
        
        String pathInfo = "/news/2008/08";
        
        // TODO: fix this by editing the spring configuration. set sites info correctly.
//        ResolvedSiteMapItem resolvedSiteMapItem = this.siteMapMatcher.match(pathInfo, hstSite);
//        assertNotNull(resolvedSiteMapItem);
    }

}
