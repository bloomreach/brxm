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
import org.hippoecm.hst.pagecomposer.jaxrs.model.PrototypeRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PrototypesRepresentation;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
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
        PrototypesRepresentation representation = (PrototypesRepresentation)((ExtResponseRepresentation) response.getEntity()).getData();
        assertEquals(0, representation.getPrototypes().size());
    }

    @Test
    public void test_prototype_pages() throws Exception {
        mockNewRequest(session, "localhost", "/home");
        PrototypesRepresentation representation = (PrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();
        assertEquals(1, representation.getPrototypes().size());
        assertEquals("prototype-page", representation.getPrototypes().get(0).getName());
    }


    @Test
    public void test_prototype_no_containers() throws Exception {
        final Node prototypeNode = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page");
        prototypeNode.getNode("main/container1").remove();
        prototypeNode.getNode("main/container2").remove();
        session.save();
        mockNewRequest(session, "localhost", "/home");
     }


    @Test
    public void test_prototype_pages_included_from_inherited_config() throws Exception {
        // make a common config page prototype : inherited config pages should not be available as prototype
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestcommon/hst:prototypepages/common-prototype-page");
        session.save();
        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PrototypesRepresentation representation = (PrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();
        assertEquals(2, representation.getPrototypes().size());
    }

    @Test
    public void test_prototype_pages_are_sorted_on_displayName() throws Exception {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/aaa-page");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/ccc-page");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/bbb-page");

        final Node aaa = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:prototypepages/aaa-page");
        final Node bbb = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:prototypepages/bbb-page");
        final Node ccc = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:prototypepages/ccc-page");

        aaa.addMixin(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META);
        bbb.addMixin(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META);
        ccc.addMixin(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META);

        aaa.setProperty(HstNodeTypes.PROTOTYPE_META_PROPERTY_DISPLAY_NAME, "zzz");
        bbb.setProperty(HstNodeTypes.PROTOTYPE_META_PROPERTY_DISPLAY_NAME, "yyy");
        ccc.setProperty(HstNodeTypes.PROTOTYPE_META_PROPERTY_DISPLAY_NAME, "xxx");

        session.removeItem("/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page");
        session.save();

        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PrototypesRepresentation representation = (PrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();

        PrototypeRepresentation prev = null;
        for (PrototypeRepresentation prototypeRepresentation : representation.getPrototypes()) {
            if (prev == null) {
                prev = prototypeRepresentation;
                continue;
            }

            assertTrue(prototypeRepresentation.getName().compareTo(prev.getName()) <= 0);
            assertTrue(prototypeRepresentation.getDisplayName().compareTo(prev.getDisplayName()) >= 0);

            assertNotSame(prototypeRepresentation.getName(), prototypeRepresentation.getDisplayName());
            prev = prototypeRepresentation;
        }
    }

    @Test
    public void test_prototype_pages_are_displayName_equals_name_when_missing() throws Exception {

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/aaa-page");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/ccc-page");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/prototype-page",
                "/hst:hst/hst:configurations/unittestproject/hst:prototypepages/bbb-page");
        session.save();

        // give time for jcr events to evict model
        Thread.sleep(200);
        mockNewRequest(session, "localhost", "/home");
        PrototypesRepresentation representation = (PrototypesRepresentation)((ExtResponseRepresentation) mountResource.getPrototypePages().getEntity()).getData();

        PrototypeRepresentation prev = null;
        for (PrototypeRepresentation prototypeRepresentation : representation.getPrototypes()) {
            if (prev == null) {
                prev = prototypeRepresentation;
                continue;
            }
            assertTrue(prototypeRepresentation.getName().compareTo(prev.getName()) >= 0);
            assertTrue(prototypeRepresentation.getDisplayName().compareTo(prev.getDisplayName()) >= 0);
            assertEquals(prototypeRepresentation.getName(), prototypeRepresentation.getDisplayName());
            prev = prototypeRepresentation;
        }

    }

}
