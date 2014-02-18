/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @version "$Id: GalleryUtilsTest.java 172469 2013-08-01 12:02:04Z mmilicevic $"
 */
public class GalleryUtilsTest {

    public static final String SOME_NAMESPACE_PREFIX = "someprefix";

    @Test
    public void testGetNamespacePathForImageset() throws Exception {
        String expectedNamespace = "/hippo:namespaces/someprefix/imageset";
        String namespacePathForImageset = GalleryUtils.getNamespacePathForImageset(SOME_NAMESPACE_PREFIX, "imageset");
        assertEquals(expectedNamespace, namespacePathForImageset);
    }

    @Test
    public void testGetGalleryUriWhenPrefixIsBlank() throws Exception {
        assertNull(GalleryUtils.getGalleryURI(""));
    }

    @Test
    public void testGetGalleryUriWhenPrefixIsNotBlank() throws Exception {
        assertEquals("http://www.onehippo.org/gallery/test/nt/2.0", GalleryUtils.getGalleryURI("test"));
    }

    @Test
    public void testGetImagesetName() throws Exception {
        assertNotNull(GalleryUtils.getImagesetName(SOME_NAMESPACE_PREFIX, "name"));
        assertEquals("someprefix:name", GalleryUtils.getImagesetName(SOME_NAMESPACE_PREFIX, "name"));
    }
}
