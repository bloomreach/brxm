/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertEquals;

public class HstUtilsTest extends BaseRepositoryTest{

    @Test
    public void testGetHstMounts() throws Exception {
        final Set<Node> hstMounts = HstUtils.getHstMounts(jcrService);
        assertEquals("expected 3 mounts, hst:root and 2 added by us", 3, hstMounts.size());
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Session session = jcrService.createSession();
        final Node root = session.getNode("/hst:hst")
                .addNode("hst:hosts", "hst:virtualhosts")
                .addNode("localhost-group", "hst:virtualhostgroup")
                .addNode("localhost", "hst:virtualhost")
                .addNode("hst:root", "hst:mount");
        root.addNode("restone", "hst:mount");
        root.addNode("resttwo", "hst:mount");
        session.save();
        jcrService.destroySession(session);
    }
}