/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.importer.gallery.conversion;


import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.configuration.Configuration;
import org.hippoecm.frontend.plugins.gallery.ImageUtils;
import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.Context;
import org.hippoecm.tools.importer.api.ImportException;
import org.hippoecm.tools.importer.conversion.AbstractConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalleryConverter extends AbstractConverter {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(GalleryConverter.class);

    private int thumbnailSize;

    @Override
    public void setup(Configuration config) throws ImportException {
        super.setup(config);
        thumbnailSize = config.getInt("thumbnailsize");
    }

    @Override
    public Node convert(Context context, Content content) throws IOException, RepositoryException {
        if (content.isFolder()) {
            return context.createFolder(content);
        } else {
            Node node = context.createDocument(content);

            Item item = node.getPrimaryItem();
            if (item.isNode()) {
                Node primaryChild = (Node) item;
                if (primaryChild.isNodeType("hippo:resource")) {
                    primaryChild.setProperty("jcr:mimeType", content.getMimeType());
                    primaryChild.setProperty("jcr:data", content.getInputStream());
                    primaryChild.setProperty("jcr:lastModified", content.lastModified());
                }
                NodeDefinition[] childDefs = node.getPrimaryNodeType().getChildNodeDefinitions();
                for (int i = 0; i < childDefs.length; i++) {
                    if (childDefs[i].getDefaultPrimaryType() != null
                            && childDefs[i].getDefaultPrimaryType().isNodeType("hippo:resource")) {
                        if (!node.hasNode(childDefs[i].getName())) {
                            Node child = node.addNode(childDefs[i].getName());
                            child.setProperty("jcr:data", primaryChild.getProperty("jcr:data").getStream());
                            child.setProperty("jcr:mimeType", primaryChild.getProperty("jcr:mimeType").getString());
                            child.setProperty("jcr:lastModified", primaryChild.getProperty("jcr:lastModified")
                                    .getDate());
                        }
                    }
                }

                makeThumbnail(primaryChild, primaryChild.getProperty("jcr:data").getStream(), primaryChild.getProperty(
                        "jcr:mimeType").getString());
            }

            return node;
        }
    }

    private void makeThumbnail(Node node, InputStream resourceData, String mimeType) throws RepositoryException {
        if (mimeType.startsWith("image")) {
            InputStream thumbNail = new ImageUtils().createThumbnail(resourceData, thumbnailSize, mimeType);
            node.setProperty("jcr:data", thumbNail);
        } else {
            node.setProperty("jcr:data", resourceData);
        }
        node.setProperty("jcr:mimeType", mimeType);
    }

    public String[] getNodeTypes() {
        return new String[] { "hippogallery:exampleImageSet", "hippogallery:stdImageGallery" };
    }

}
