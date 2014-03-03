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

import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PrototypePagesRepresentation;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MountResourcePrototypesTest extends AbstractMountResourceTest {

    @Test
    public void test_no_prototype_pages() throws Exception {
        // delete existing prototype page first
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:prototypepages").remove();
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        final Response response = mountResource.getPrototypePages();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        PrototypePagesRepresentation representation = (PrototypePagesRepresentation)((ExtResponseRepresentation) response.getEntity()).getData();
        assertEquals(0, representation.getPages().size());
    }

    @Test
    public void test_prototype_pages() throws Exception {
        // "/hst:hst/hst:configurations/unittestproject/hst:prototypepages" contains 1 prototype 'singlerow-page'

        mockNewRequest(session, "localhost", "/home");
        PrototypePagesRepresentation representation = (PrototypePagesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();
        assertEquals(1, representation.getPages().size());
        assertEquals("singlerow-page", representation.getPages().get(0).getName());
    }

    @Test
    public void test_prototype_pages_not_from_inherited_config() throws Exception {
        // make a common config page prototype : inherited config pages should not be available as prototype
        session.move("/hst:hst/hst:configurations/unittestproject/hst:prototypepages/singlerow-page",
                "/hst:hst/hst:configurations/unittestcommon/hst:prototypepages/singlerow-page");
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PrototypePagesRepresentation representation = (PrototypePagesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();
        assertEquals(0, representation.getPages().size());
    }

    @Test
    public void test_prototype_pages_are_sorted() throws Exception {

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/singlerow-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/aaa-page");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/singlerow-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/ccc-page");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/singlerow-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/bbb-page");
        session.save();

        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PrototypePagesRepresentation representation = (PrototypePagesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();

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
