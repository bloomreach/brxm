/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.components;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HstComponentConfigurationIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void localParameters_in_case_of_multiple_variants_on_component() throws Exception {

        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");

        contactPage.setProperty("hst:parameternames", new String[]{"foo","foo"});
        contactPage.setProperty("hst:parametervalues", new String[]{"bar", "lux"});
        contactPage.setProperty("hst:parameternameprefixes", new String[]{"", "professional"});
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstComponentConfiguration contactPageConfig = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/contactpage");
        assertNotNull(contactPageConfig);

        final String prefixedParameterName = ConfigurationUtils.createPrefixedParameterName("professional", "foo");

        final Map<String, String> parameters = contactPageConfig.getParameters();
        assertTrue(parameters.containsKey("foo"));
        assertTrue(parameters.containsKey(prefixedParameterName));
        assertEquals("bar", parameters.get("foo"));
        assertEquals("lux", parameters.get(prefixedParameterName));

        final Map<String, String> localParameters = contactPageConfig.getLocalParameters();
        assertTrue(localParameters.containsKey("foo"));
        assertTrue(localParameters.containsKey(prefixedParameterName));
        assertEquals("bar", localParameters.get("foo"));
        assertEquals("lux", localParameters.get(prefixedParameterName));

    }
}
