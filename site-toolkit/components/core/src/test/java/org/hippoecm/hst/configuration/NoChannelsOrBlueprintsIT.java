/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;


import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NoChannelsOrBlueprintsIT extends AbstractTestConfigurations {

    private HstManager hstSitesManager;
    private  Session session;
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        this.hstSitesManager = getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }


    @Test
    public void testModelWithoutChannels() throws Exception {
        removeChannelNodes(session);
        session.save();
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
        assertTrue(vhosts.getChannels("dev-localhost").isEmpty());
    }

    @Test
    public void testModelWithoutBlueprints() throws Exception {
        session.getNode("/hst:hst/hst:blueprints").remove();
        session.save();
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
        assertTrue(vhosts.getBlueprints().isEmpty());
    }

    @Test
    public void testModelWithoutChannelsOrBlueprints() throws Exception {
        removeChannelNodes(session);
        session.getNode("/hst:hst/hst:blueprints").remove();
        session.save();
        VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
        assertTrue(vhosts.getChannels("dev-localhost").isEmpty());
        assertTrue(vhosts.getBlueprints().isEmpty());
    }

    private void removeChannelNodes(final Session session) throws RepositoryException {
        QueryResult result = session.getWorkspace().getQueryManager().createQuery("/jcr:root/hst:hst//element(*,hst:channel)", "xpath").execute();
        for (Node node : new NodeIterable(result.getNodes())) {
            node.remove();
        }
    }



}
