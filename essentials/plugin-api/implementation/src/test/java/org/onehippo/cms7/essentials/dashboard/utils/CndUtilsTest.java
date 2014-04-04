/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

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

        Session session = getSession();
        session.getRootNode().addNode(HippoNodeType.NAMESPACES_PATH);
        session.save();
        CndUtils.registerNamespace(getContext(), TEST_PREFIX, TEST_URI);
        assertTrue("CndUtils.registerNamespaceUri", true);
        CndUtils.createHippoNamespace(getContext(), TEST_PREFIX);
        assertTrue("CndUtils.createHippoNamespace", true);
        boolean exists = CndUtils.namespaceUriExists(getContext(), TEST_URI);
        assertTrue(exists);
        CndUtils.registerDocumentType(getContext(), TEST_PREFIX, "myname", false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);
        assertTrue("CndUtils.registerDocumentType", true);
        // check if exists:
        final boolean prefixExists = CndUtils.namespacePrefixExists(getContext(), TEST_PREFIX);
        assertTrue("Expected to find namespace prefix: " + TEST_PREFIX, prefixExists);
        final boolean nodeTypeExists = CndUtils.nodeTypeExists(getContext(), NODE_TYPE);
        assertTrue("Expected registered nodetype, found nothing", nodeTypeExists);
        final boolean isMySupertype = CndUtils.isNodeOfSuperType(getContext(), NODE_TYPE, GalleryUtils.HIPPOGALLERY_IMAGE_SET);
        assertTrue("Expected to be of supertype:" + GalleryUtils.HIPPOGALLERY_IMAGE_SET, isMySupertype);
        // fetch supertype nodes
        final List<String> superTypes = CndUtils.getNodeTypesOfType(getContext(), GalleryUtils.HIPPOGALLERY_IMAGE_SET);
        assertEquals("Expected 2 gallery types", 2, superTypes.size());
        // test un-register type
        boolean removed = CndUtils.unRegisterDocumentType(getContext(), TEST_PREFIX, "myname");
        assertTrue("Expected type to be removed", removed);


    }

    @Test
    public void testNamespaceNoneExists() throws Exception {
        assertFalse(CndUtils.namespaceUriExists(getContext(), TEST_URI + "/none"));
        assertFalse(CndUtils.namespacePrefixExists(getContext(), TEST_PREFIX + "none"));
        assertFalse(CndUtils.nodeTypeExists(getContext(), NODE_TYPE + "none"));

    }
}
