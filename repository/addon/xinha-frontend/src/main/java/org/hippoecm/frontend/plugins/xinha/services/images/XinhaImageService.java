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

package org.hippoecm.frontend.plugins.xinha.services.images;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.services.XinhaFacetHelper;
import org.hippoecm.frontend.plugins.xinha.services.images.ImageItemFactory.ImageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaImageService implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaImageService.class);

    private ImageItemFactory factory;
    private JcrNodeModel nodeModel;

    public XinhaImageService(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        factory = new ImageItemFactory(nodeModel);
    }

    //Attach an image with only a JcrNodeModel. Method return json object wich 
    public String attach(JcrNodeModel model) {
        //TODO: fix drag-drop replacing
        ImageItem item = createImageItem(model);
        if (attachImageItem(item)) {
            StringBuilder sb = new StringBuilder();
            sb.append("xinha_editors.").append(getXinhaName()).append(
                    ".plugins.InsertImage.instance.insertImage(").append('{').append(XinhaImage.URL).append(": '")
                    .append(item.getUrl()).append("'}, false)");
            return sb.toString();
        }
        return null;
    }
    
    protected abstract String getXinhaName();

    public void attach(XinhaImage xi) {
        if (xi.isAttacheable()) {
            if (xi.isReplacing()) {
                ImageItem remove = createImageItem(xi.getInitialValues());
                detachImageItem(remove);
            }
            ImageItem item = createImageItem(xi.getNodeModel());
            if (attachImageItem(item)) {
                xi.setUrl(item.getUrl());
            }
        }
    }
    
    public boolean detach(XinhaImage xi) {
        Map<String, String> values = (Map<String, String>) xi.getObject();
        ImageItem item = createImageItem(values);
        if (detachImageItem(item)) {
            return true;
        }
        return false;
    }

    public XinhaImage createXinhaImage(HashMap<String, String> p) {
        return new XinhaImage(p, nodeModel);
    }

    private ImageItem createImageItem(Map<String, String> values) {
        return factory.createImageItem(values);
    }

    private ImageItem createImageItem(JcrNodeModel nodeModel) {
        try {
            return factory.createImageItem(nodeModel.getNode());
        } catch (UnsupportedRepositoryOperationException e) {
            log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
        } catch (ItemNotFoundException e) {
            log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
        } catch (AccessDeniedException e) {
            log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
        } catch (RepositoryException e) {
            log.error("Error creating ImageItem for model[" + nodeModel.getItemModel().getPath() + "]", e);
        }
        return null;
    }

    private boolean attachImageItem(ImageItem item) {
        XinhaFacetHelper helper = new XinhaFacetHelper(false);
        Node node = nodeModel.getNode();
        try {
            String facet = helper.createFacet(node, item.getNodeName(), item.getUuid());
            if(facet != null && !facet.equals("")) {
                item.setFacetName(facet);
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Failed to create facet for " + item.getNodeName(), e);
        }
        return false;
    }

    private boolean detachImageItem(ImageItem item) {
        if (item.getUuid() != null) {
            Node node = nodeModel.getNode();
            String facet = item.getFacetName();
            try {
                if (node.hasNode(facet)) {
                    Node imgNode = node.getNode(facet);
                    imgNode.remove();
                    node.save();
                    return true;
                }
            } catch (RepositoryException e) {
                log.error("An error occured while trying to save new image facetSelect[" + item.getNodeName() + "]", e);
            }
        }
        return false;
    }
}
