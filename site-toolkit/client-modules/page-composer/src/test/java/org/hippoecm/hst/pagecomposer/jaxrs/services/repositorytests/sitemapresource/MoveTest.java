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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.PreviewWorkspaceNodeValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.Validator;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MoveTest extends AbstractSiteMapResourceTest {


    @Test
    public void test_update_move() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation("home");
        Node homeNode = session.getNodeByIdentifier(home.getId());
        String parentPath = homeNode.getParent().getPath();
        home.setName("renamedHome");

        SiteMapResource siteMapResource = new SiteMapResource();
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("renamedHome", homeNode.getName());
        assertEquals(parentPath + "/renamedHome", homeNode.getPath());

        assertTrue(session.nodeExists(parentPath + "/home"));
        Node deletedMarkerNode = session.getNode(parentPath + "/home");
        assertEquals("admin", deletedMarkerNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());

    }

    @Test
    public void test_update_move_and_back_again() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation("home");
        Node homeNode = session.getNodeByIdentifier(home.getId());
        String parentPath = homeNode.getParent().getPath();
        home.setName("renamedHome");

        SiteMapResource siteMapResource = new SiteMapResource();
        siteMapResource.update(home);

        home.setName("home");
        Response response = siteMapResource.update(home);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("home", homeNode.getName());
        assertEquals("admin", homeNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString());
        assertFalse(session.nodeExists(parentPath + "/renamedHome"));
    }

    @Test
    public void test_update_move_and_move_back_again() throws Exception {

    }


}
