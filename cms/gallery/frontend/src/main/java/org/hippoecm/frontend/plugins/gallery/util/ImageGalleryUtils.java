/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.util;

import java.awt.Dimension;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.repository.gallery.HippoGalleryNodeType;

public class ImageGalleryUtils {

    private ImageGalleryUtils() {}

    public static Node getOriginalGalleryNode(final Node variantNode) throws RepositoryException {
        return variantNode.getParent().getNode(HippoGalleryNodeType.IMAGE_SET_ORIGINAL);
    }

    public static Dimension getDimension(final Node imageNode) throws RepositoryException {
        return new Dimension(
                (int) imageNode.getProperty(HippoGalleryNodeType.IMAGE_WIDTH).getLong(),
                (int) imageNode.getProperty(HippoGalleryNodeType.IMAGE_HEIGHT).getLong()
        );
    }

    public static boolean isOriginalImage(final Node imageNode) throws RepositoryException {
        return HippoGalleryNodeType.IMAGE_SET_ORIGINAL.equals(imageNode.getName());
    }

    public static void setImageFileName(final Node node, final String fileName) throws RepositoryException {
        node.setProperty(HippoGalleryNodeType.IMAGE_SET_FILE_NAME, fileName);
    }

    public static void saveImageNode(final Node node, final InputStream data, final int width, final int height)
            throws RepositoryException {
        node.setProperty(JcrConstants.JCR_DATA, ResourceHelper.getValueFactory(node).createBinary(data));
        node.setProperty(HippoGalleryNodeType.IMAGE_WIDTH, width);
        node.setProperty(HippoGalleryNodeType.IMAGE_HEIGHT, height);
    }
}
