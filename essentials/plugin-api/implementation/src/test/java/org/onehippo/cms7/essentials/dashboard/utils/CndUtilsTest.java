/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: CndUtilsTest.java 176263 2013-09-06 09:31:16Z dvandiepen $"
 */
public class CndUtilsTest extends BaseRepositoryTest {

    public static final String TEST_URI = "http://www.test.com";
    public static final String TEST_PREFX = "test";

    @Test
    public void testRegisterNamespaceUri() throws Exception {

        session.getRootNode().addNode(HippoNodeType.NAMESPACES_PATH);
        session.save();
        CndUtils.registerNamespace(getContext(), TEST_PREFX, TEST_URI);
        assertTrue("CndUtils.registerNamespaceUri", true);
        CndUtils.createHippoNamespace(getContext(), TEST_PREFX);
        assertTrue("CndUtils.createHippoNamespace", true);
        boolean exists = CndUtils.existsNamespaceUri(getContext(), TEST_URI);
        assertTrue(exists);
        CndUtils.registerDocumentType(getContext(), TEST_PREFX, "myname", false, false, GalleryUtils.HIPPOGALLERY_IMAGE_SET, GalleryUtils.HIPPOGALLERY_RELAXED);
        assertTrue("CndUtils.registerDocumentType", true);
    }
}
