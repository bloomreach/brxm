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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests;

import javax.jcr.Node;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PagePrototypesRepresentation;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MountResourcePrototypesTest extends AbstractMountResourceTest {

    @Test
    public void test_no_prototype_pages() throws Exception {
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        final Response response = mountResource.getPagePrototypes();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        PagePrototypesRepresentation representation = (PagePrototypesRepresentation)((ExtResponseRepresentation) response.getEntity()).getData();
        assertEquals(0, representation.getPages().size());
    }

    @Test
    public void test_prototype_pages() throws Exception {
        // make a page prototype
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/proto");

        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PagePrototypesRepresentation representation = (PagePrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPagePrototypes().getEntity()).getData();
        assertEquals(1, representation.getPages().size());
        assertEquals("proto", representation.getPages().get(0).getName());
    }

    @Test
    public void test_prototype_pages_not_from_inherited_config() throws Exception {
        // make a common config page prototype : inherited config pages should not be available as prototype
        // TODO discuss whether we want to support inherited config pages as prototype
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/standarddetail")
                .setProperty(HstNodeTypes.COMPONENT_PROPERTY_PROTOTYPE, true);
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PagePrototypesRepresentation representation = (PagePrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPagePrototypes().getEntity()).getData();
        assertEquals(0, representation.getPages().size());
    }

    @Test
    public void test_prototype_pages_are_sorted() throws Exception {
        movePagesFromCommonToUnitTestProject();
        // make a all pages prototype
        for (Node page : new NodeIterable(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:pages").getNodes())) {
            page.setProperty(HstNodeTypes.COMPONENT_PROPERTY_PROTOTYPE, true);
        }
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PagePrototypesRepresentation representation = (PagePrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPagePrototypes().getEntity()).getData();

        ComponentRepresentation prev = null;
        for (ComponentRepresentation pageRepresentation : representation.getPages()) {
            if (prev == null) {
                prev = pageRepresentation;
                continue;
            }
            assertTrue(pageRepresentation.getName().compareTo(prev.getName()) >= 0);
            prev = pageRepresentation;
        }

    }

}
