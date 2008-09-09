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

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageItemFactory implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(ImageItemFactory.class);
    
    final static String BINARIES_PREFIX = "binaries";
    final static String DROP_PREFIX = "drop-on-xinha";
    
    private JcrNodeModel nodeModel;
    private EnumMap<XinhaImage, String> values;

    public ImageItemFactory(JcrNodeModel nodeModel, EnumMap<XinhaImage, String> values) {
        this.nodeModel = nodeModel;
        this.values = values;
    }

    public ImageItem createImageItem() {
        String urlValue = values.get(XinhaImage.URL);
        if (urlValue != null) {
            if(urlValue.startsWith(BINARIES_PREFIX)) {
                // find the nodename of the facetselect
                String resourcePath = urlValue.substring(BINARIES_PREFIX.length());
                Item resourceItem;
                try {
                    resourceItem = nodeModel.getNode().getSession().getItem(resourcePath);
                    if (resourceItem.isNode() && ((Node) resourceItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        return createImageItem(resourceItem.getParent());
                    }
                } catch (PathNotFoundException e) {
                    log.warn("resourcePath not found: " + resourcePath);
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            } else if(urlValue.startsWith(DROP_PREFIX)) {
                //reset width and height
                values.put(XinhaImage.WIDTH, null);
                values.put(XinhaImage.HEIGHT, null);
                
                String resourcePath = urlValue.substring(DROP_PREFIX.length());
                Item resourceItem;
                try {
                    resourceItem = nodeModel.getNode().getSession().getItem(resourcePath);
                    JcrNodeModel mod = new JcrNodeModel(resourcePath);
                    //if (resourceItem.isNode() && ((Node) resourceItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                        return createImageItem(mod.getNode());
                    //}
                } catch (PathNotFoundException e) {
                    log.warn("resourcePath not found: " + resourcePath);
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return ImageItem.DEFAULT;
    }

    public ImageItem createImageItem(Node node) throws UnsupportedRepositoryOperationException, ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        List<String> resourceDefinitions = new ArrayList<String>();
        for (NodeDefinition nd : node.getPrimaryNodeType().getChildNodeDefinitions()) {
            if (!nd.getName().equals(node.getPrimaryItem().getName()) && nd.getDefaultPrimaryType() != null
                    && nd.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                resourceDefinitions.add(nd.getName());
            }
        }
        return new ImageItem(node.getPath(), node.getParent().getUUID(), node.getPrimaryItem().getName(), node
                .getName(), resourceDefinitions, nodeModel.getNode().getPath());
    }

    public static class ImageItem implements IClusterable {
        private static final long serialVersionUID = 1L;

        public static final String DEFAULT_EMPTY_THUMBNAIL = "skin/images/empty.gif";
        public static final ImageItem DEFAULT = new ImageItem() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getPrimaryUrl() {
                return DEFAULT_EMPTY_THUMBNAIL;
            }
        };

        private String parentPath;
        private String path;
        private String uuid;
        private String nodeName;
        private String primaryItemName;
        private List<String> resourceDefinitions;
        private String selectedResourceDefinition;

        private ImageItem() {
            this.resourceDefinitions = new ArrayList<String>();
        }

        public ImageItem(String path, String uuid, String primaryItemName, String nodeName,
                List<String> resourceDefinitions, String nodePath) {
            this.path = path;
            this.uuid = uuid;
            this.primaryItemName = primaryItemName;
            this.nodeName = nodeName;
            this.parentPath = nodePath;
            this.resourceDefinitions = resourceDefinitions != null ? resourceDefinitions : new ArrayList<String>();
            if (this.resourceDefinitions.size() == 1) {
                selectedResourceDefinition = this.resourceDefinitions.get(0);
            }
        }

        public String getUuid() {
            return uuid;
        }

        public String getPath() {
            return path;
        }

        public String getPrimaryUrl() {
            return "binaries" + path + "/" + primaryItemName;
        }

        public List<String> getResourceDefinitions() {
            return resourceDefinitions;
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getSelectedResourceDefinition() {
            return selectedResourceDefinition;
        }

        public void setSelectedResourceDefinition(String selectedResourceDefinition) {
            this.selectedResourceDefinition = selectedResourceDefinition;
        }

        public String getUrl() {
            String url = "binaries" + parentPath + "/" + nodeName;
            if (selectedResourceDefinition != null) {
                return url + "/" + nodeName + "/" + selectedResourceDefinition;
            }
            return url;
        }

        public boolean isValid() {
            return path != null && uuid != null
                    && !(resourceDefinitions.size() > 1 && selectedResourceDefinition == null);
        }

    }
}
