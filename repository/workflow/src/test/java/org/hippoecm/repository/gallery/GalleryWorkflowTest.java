/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.gallery;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.gallery.impl.GalleryWorkflowImpl;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;


public class GalleryWorkflowTest extends RepositoryTestCase {

    private Node gallery;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        gallery = session.getRootNode().addNode("test", "hippogallery:stdImageGallery");
        session.save();
    }

    @Test
    public void testCreateGalleryItem() throws Exception {
        final GalleryWorkflow wf = new GalleryWorkflowImpl(null, session, gallery);
        final Node galleryItem = wf.createGalleryItem("foo.jpg", "hippogallery:imageset").getNode(session);

        assertTrue(isAvailable(galleryItem, "live"));
        assertTrue(isAvailable(galleryItem, "preview"));

        assertTrue(galleryItem.hasNode("hippogallery:thumbnail"));
        assertEquals("hippogallery:image", galleryItem.getNode("hippogallery:thumbnail").getPrimaryNodeType().getName());

    }

    @Test(expected = WorkflowException.class)
    public void testAttemptToCreateSNSThrowsException() throws Exception {
        final GalleryWorkflow wf = new GalleryWorkflowImpl(null, session, gallery);
        wf.createGalleryItem("contrail.jpg", "hippogallery:imageset");
        wf.createGalleryItem("contrail.jpg", "hippogallery:imageset");
        final NodeIterator nodes = gallery.getNodes("contrail.jpg");
        assertEquals("Not allowed to create same-node siblings", 1L, nodes.getSize());
    }

    private static boolean isAvailable(final Node node, final String availability) throws RepositoryException {
        if (node.hasProperty(HIPPO_AVAILABILITY)) {
            for (Value value : node.getProperty(HIPPO_AVAILABILITY).getValues()) {
                if (value.getString().equals(availability)) {
                    return true;
                }
            }
        }
        return false;
    }

}
