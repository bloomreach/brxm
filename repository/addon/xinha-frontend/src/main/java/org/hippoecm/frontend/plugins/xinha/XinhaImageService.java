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

package org.hippoecm.frontend.plugins.xinha;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin.Configuration;
import org.hippoecm.frontend.plugins.xinha.dialog.images.ImageItemFactory;
import org.hippoecm.frontend.plugins.xinha.dialog.images.XinhaImage;
import org.hippoecm.frontend.plugins.xinha.dialog.images.ImageItemFactory.ImageItem;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaImageService implements IClusterable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(XinhaImageService.class);

    final static String BINARIES_PREFIX = "binaries";

    private ImageItemFactory factory;
    private JcrNodeModel nodeModel;
    private Configuration configuration;

    public XinhaImageService(Configuration configuration, JcrNodeModel nodeModel) {
        this.configuration = configuration;
        this.nodeModel = nodeModel;
        factory = new ImageItemFactory(nodeModel);
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

    private boolean attachImageItem(ImageItem imageItem) {
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
                log
                        .error("An error occured while trying to save new image facetSelect[" + imageItem.getUuid()
                                + "]", e);
            }
        }
        return false;
    }
    
    private boolean detachImageItem(ImageItem item) {
        if (item.getUuid() != null) {
            Node node = nodeModel.getNode();
            String nodeName = item.getNodeName();
            try {
                if (node.hasNode(nodeName)) {
                    Node imgNode = node.getNode(nodeName);
                    imgNode.remove();
                    node.save();
                    return true;
                }
            } catch (RepositoryException e) {
                log
                        .error("An error occured while trying to save new image facetSelect[" + item.getNodeName()
                                + "]", e);
            }
        }
        return false;
    }


    //Attach an image with only a JcrNodeModel. Method return json object wich 
    public String attach(JcrNodeModel model) {
        ImageItem item = createImageItem(model);
        if (attachImageItem(item)) {
            StringBuilder sb = new StringBuilder();
            sb.append("xinha_editors.").append(configuration.getName()).append(
                    ".plugins.InsertImage.instance.insertImage(").append('{').append(XinhaImage.URL).append(": '")
                    .append(item.getUrl()).append("'}, false)");
            return sb.toString();
        }
        return null;
    }

    public String attach(XinhaImage xi) {
        ImageItem item = createImageItem(xi.getNodeModel());
        if (attachImageItem(item)) {
            return item.getUrl();
        }
        return null;
    }
    
    public boolean detach(XinhaImage img) {
        ImageItem item = createImageItem(img.getNodeModel());
        if (detachImageItem(item)) {
            return true;
        }
        return false;
    }

    public XinhaImage createXinhaImage(HashMap<String, String> p) {
        XinhaImage img = new XinhaImage(p);
        img.setNodeModel(findNodeModel(img.getUrl()));
        return img;
    }

    private JcrNodeModel findNodeModel(String url) {
        if (url != null && url.startsWith(BINARIES_PREFIX)) {

            // find the nodename of the facetselect
            String path = XinhaUtil.decode(url.substring(BINARIES_PREFIX.length()));
            UserSession session = (UserSession) Session.get();
            try {
                HippoNode node = (HippoNode) session.getJcrSession().getRootNode().getNode(path).getParent();
                Node imageNode = node.getCanonicalNode();
                if (imageNode != null) {
                    String path2 = imageNode.getPath();
                    NodeType type = imageNode.getPrimaryNodeType();
                    String name = type.getName();
                    Node node2 = imageNode.getParent();
                    type = node2.getPrimaryNodeType();
                    String name2 = type.getName();
                    return new JcrNodeModel(imageNode.getPath());
                }
            } catch (PathNotFoundException e) {
                log.error("Error retrieving canonical node for imageNode[" + path + "]", e);
            } catch (RepositoryException e) {
                log.error("Error retrieving canonical node for imageNode[" + path + "]", e);
            }
        }
        return null;
    }
}
