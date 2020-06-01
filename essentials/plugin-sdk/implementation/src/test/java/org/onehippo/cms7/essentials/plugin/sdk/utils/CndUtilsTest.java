/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.util.List;

import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class CndUtilsTest extends BaseRepositoryTest {

    public static final String TEST_URI = "http://www.test.com";
    public static final String TEST_PREFIX = "test";
    private static final String NODE_TYPE = TEST_PREFIX + ':' + "myname";

    @Test
    public void testRegisterNamespaceUri() throws Exception {

        jcrService.reset();
        Session session = jcrService.createSession();
        if (!session.nodeExists("/"+HippoNodeType.NAMESPACES_PATH)) {
            session.getRootNode().addNode(HippoNodeType.NAMESPACES_PATH);
            session.save();
        }
        CndUtils.registerNamespace(jcrService, TEST_PREFIX, TEST_URI);
        assertTrue("CndUtils.registerNamespaceUri", true);
        CndUtils.createHippoNamespace(jcrService, TEST_PREFIX);
        assertTrue("CndUtils.createHippoNamespace", true);
        boolean exists = CndUtils.namespaceUriExists(jcrService, TEST_URI);
        assertTrue(exists);
        CndUtils.registerDocumentType(jcrService, TEST_PREFIX, "myname", false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);
        assertTrue("CndUtils.registerDocumentType", true);
        // check if exists:
        final boolean prefixExists = CndUtils.namespacePrefixExists(jcrService, TEST_PREFIX);
        assertTrue("Expected to find namespace prefix: " + TEST_PREFIX, prefixExists);
        final boolean nodeTypeExists = CndUtils.nodeTypeExists(jcrService, NODE_TYPE);
        assertTrue("Expected registered nodetype, found nothing", nodeTypeExists);
        final boolean isMySupertype = CndUtils.isNodeOfSuperType(jcrService, NODE_TYPE, GalleryUtils.HIPPOGALLERY_IMAGE_SET);
        assertTrue("Expected to be of supertype:" + GalleryUtils.HIPPOGALLERY_IMAGE_SET, isMySupertype);
        // fetch supertype nodes
        final List<String> superTypes = CndUtils.getNodeTypesOfType(jcrService, GalleryUtils.HIPPOGALLERY_IMAGE_SET);
        assertEquals("Expected 2 gallery types", 2, superTypes.size());
        jcrService.destroySession(session);

    }

    @Test
    public void testNamespaceNoneExists() throws Exception {
        assertFalse(CndUtils.namespaceUriExists(jcrService, TEST_URI + "/none"));
        assertFalse(CndUtils.namespacePrefixExists(jcrService, TEST_PREFIX + "none"));
        assertFalse(CndUtils.nodeTypeExists(jcrService, NODE_TYPE + "none"));
    }
}
