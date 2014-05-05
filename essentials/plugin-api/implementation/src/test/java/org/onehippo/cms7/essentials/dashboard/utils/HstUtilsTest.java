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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertEquals;

public class HstUtilsTest extends BaseRepositoryTest{




    @Test
    public void testGetHstMounts() throws Exception {
        final Set<Node> hstMounts = HstUtils.getHstMounts(getContext());
        assertEquals("expected 3 mounts, hst:root and 2 added by us", 3, hstMounts.size());
    }



    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }





    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createHstRootConfig();
        final Session session = getContext().createSession();
        final Node hstRoot = session.getNode("/hst:hst");
        final Node virtualHost = hstRoot.addNode("hst:hosts", "hst:virtualhosts");
        final Node hostGroup = virtualHost.addNode("localhost-group", "hst:virtualhostgroup");
        final Node localhost = hostGroup.addNode("localhost", "hst:virtualhost");
        final Node root = localhost.addNode("hst:root", "hst:mount");
        root.addNode("restone", "hst:mount");
        root.addNode("resttwo", "hst:mount");
        session.save();
    }


}