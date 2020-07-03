/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.xpages;


import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class XPageFixtureIT extends AbstractTestConfigurations {

    private Session session;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void xpage_fixture_test() throws Exception {

        // just for the purpose of testing the 'hst:qualifier' property being auto created, first remove the hst:qualifier
        // properties from the bootstrapped example and then copy the node and confirm copy has new hst:qualifier props

        // reason why the bootstrap contains the 'hst:qualifier' property is because the 'xpage1' is used in the
        // hst-unittestcontent.yaml and relies on the present 'hst:qualifier' property of 'xpage1' to be stable

        final Node xpages = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:xpages");

        final Node mainOrigin = xpages.getNode("xpage1/main");
        mainOrigin.getNode("container1").getProperty("hst:qualifier").remove();
        mainOrigin.getNode("container2").getProperty("hst:qualifier").remove();

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage1",
                "/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage1-copy");

        session.save();

        final Node xpageCopy = xpages.getNode("xpage1-copy");
        assertFalse("property 'hst:qualifier' is not expected on xpage", xpageCopy.hasProperty("hst:qualifier"));

        final Node main = xpageCopy.getNode("main");
        assertFalse("main is of type hst:component which should not get an hst:qualifier", main.hasProperty("hst:qualifier"));

        final Node container1 = main.getNode("container1");
        assertTrue("property hst:qualifier is expected to be autocreated", container1.hasProperty("hst:qualifier"));
        validateTag(container1.getProperty("hst:qualifier").getString());

        final Node banner = container1.getNode("banner");
        assertFalse("property hst:qualifier not expected on the banner component", banner.hasProperty("hst:qualifier"));


        final Node container2 = main.getNode("container2");
        assertTrue("property hst:qualifier is expected to be autocreated", container2.hasProperty("hst:qualifier"));
        validateTag(container2.getProperty("hst:qualifier").getString());

    }

    private void validateTag(final String expectedUUID) {
        try {
            UUID.fromString(expectedUUID);
        } catch (IllegalArgumentException e) {
            fail("Expected uuid format");
        }
    }
}
