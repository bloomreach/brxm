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

package org.hippoecm.frontend.plugins.xinha.modal.imagepicker;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.modal.imagepicker.ImageItemFactory.ImageItem;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageItemDAO implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(ImageItemDAO.class);

    private final static String GALLERY_SEARCH_PATH = "/content/gallery-search";
    
    private ImageItemFactory imageItemFactory;
    private JcrNodeModel nodeModel;

    public ImageItemDAO(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        imageItemFactory = new ImageItemFactory(nodeModel);
    }
    
    public ImageItem attach(JcrNodeModel imageModel) {
        try {
            ImageItem item = imageItemFactory.createImageItem(imageModel.getNode());
            if(saveOrUpdate(item)) {
                return item;
            }
        } catch (RepositoryException e) {
            log.error("An error occured while trying to create new ImageItem for node[" + nodeModel.getItemModel().getPath() + "]", e);
        }
        return null;
    }

    public boolean saveOrUpdate(ImageItem imageItem) {
        if (imageItem.getUuid() != null) {
            Node node = nodeModel.getNode();
            String nodeName = imageItem.getNodeName();
            try {
                if (!node.hasNode(nodeName)) {
                    Node facetselect = node.addNode(nodeName, HippoNodeType.NT_FACETSELECT);
                    //todo fetch corresponding uuid of the chosen imageset
                    facetselect.setProperty(HippoNodeType.HIPPO_DOCBASE, imageItem.getUuid());
                    facetselect.setProperty(HippoNodeType.HIPPO_FACETS, new String[] {});
                    facetselect.setProperty(HippoNodeType.HIPPO_MODES, new String[] {});
                    facetselect.setProperty(HippoNodeType.HIPPO_VALUES, new String[] {});
                    // need a node save (the draft so no problem) to visualize images
                    node.save();
                }
                return true;
            } catch (RepositoryException e) {
                log.error("An error occured while trying to save new image facetSelect[" + imageItem.getUuid() + "]", e);
            }
        }
        return false;
    }

    public ImageItem create(EnumMap<XinhaImage, String> enums) {
        return imageItemFactory.createImageItem(enums);
    }

    public List<ImageItem> getItems() {
        List<ImageItem> items = new ArrayList<ImageItem>();
        
        try {
            Session session = nodeModel.getNode().getSession();
            Node gallerySearchNode = (Node) session.getItem(GALLERY_SEARCH_PATH);
            Node resultset = gallerySearchNode.getNode(HippoNodeType.HIPPO_RESULTSET);
            NodeIterator imageNodesIt = resultset.getNodes();
            while (imageNodesIt.hasNext()) {
                HippoNode imageNode = (HippoNode) imageNodesIt.nextNode();
                // nextNode can return null
                if (imageNode == null) {
                    continue;
                }
                Node canonical = imageNode.getCanonicalNode();
                if (canonical != null && canonical.getParent().isNodeType("mix:referenceable")) {
                    try { //test if canonical node has a primaryItem
                        canonical.getPrimaryItem().getName();
                        items.add(imageItemFactory.createImageItem(canonical));
                    } catch (ItemNotFoundException e) {
                        log.error("gallery node does not have a primary item: skipping node: " + canonical.getPath());
                    }
                }
            }
        } catch (PathNotFoundException e) {
            log.error("Gallery Search node missing: " + GALLERY_SEARCH_PATH);
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage(), e);
        }
        return items;
    }

    public void detach() {
        this.nodeModel.detach();
    }

}