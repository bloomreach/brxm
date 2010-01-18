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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.XinhaUtil;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageItemFactory implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(ImageItemFactory.class);

    final static String BINARIES_PREFIX = "binaries";

    private JcrNodeModel nodeModel;

    public ImageItemFactory(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }
    
    //TODO: Logic here is same as XinhaImage.createInitialModel, should only be here in factory. Remove from XinhaImage. 
    public ImageItem createImageItem(Map<String, String> values) {
        //TODO: handle facetselect value
        String url = values.get(XinhaImage.URL);
        if (url != null) {
            url = XinhaUtil.decode(url);
            if (url.startsWith(BINARIES_PREFIX)) {
                
                // find the nodename of the facetselect
                url = XinhaUtil.decode(url.substring(BINARIES_PREFIX.length()));
                if(!Strings.isEmpty(url)) {
                    try {
                        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                        HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
                        Node root = session.getRootNode();
                        Node node = ((HippoNode) workspace.getHierarchyResolver().getNode(root, url)).getCanonicalNode();
                        if(node != null) {
                            while(!node.equals(root) && !node.isNodeType(HippoNodeType.NT_HANDLE)) {
                                node = node.getParent();
                            }
                            return createImageItem(node);
                        }
                    } catch (PathNotFoundException e) {
                        log.error("Error retrieving canonical node for imageNode[" + url + "]", e);
                    } catch (RepositoryException e) {
                        log.error("Error retrieving canonical node for imageNode[" + url + "]", e);
                    }
                }
            }
        }
        return null;
    }

    public ImageItem createImageItem(Node node) throws UnsupportedRepositoryOperationException, ItemNotFoundException,
            AccessDeniedException, RepositoryException {

        NodeType nodetype = node.getPrimaryNodeType();
        if (nodetype.getName().equals("hippo:handle")) {
            node = node.getNode(node.getName());
        }

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
      
        private String parentPath;
        private String path;
        private String uuid;
        private String nodeName;
        private String facetName;
        private String primaryItemName;
        private List<String> resourceDefinitions;
        private String selectedResourceDefinition;

        public ImageItem(String path, String uuid, String primaryItemName, String nodeName,
                List<String> resourceDefinitions, String nodePath) {
            this.path = path;
            this.uuid = uuid;
            this.primaryItemName = primaryItemName;
            this.facetName = nodeName;
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

        public String getPrimaryUrl() {
            return XinhaUtil.encode("binaries" + path + "/" + primaryItemName);
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
        
        public void setFacetName(String facet) {
            this.facetName = facet;
        }
        
        public String getFacetName() {
            return facetName;
        }
        
        public String getFacetSelectPath() {
            if (selectedResourceDefinition != null) {
                return facetName + "/{_document}/" + selectedResourceDefinition;
            } else {
                return facetName;
            }
        }
        
        public String getUrl() {
            String url = null;
            String parentUrl = "binaries" + parentPath + "/";

            if (!XinhaUtil.isPortletContext()) {
                if (selectedResourceDefinition != null) {
                    url = XinhaUtil.encode(parentUrl + facetName + "/{_document}/" + selectedResourceDefinition);
                } else {
                    url = XinhaUtil.encode(parentUrl + facetName);
                }
            } else {
                parentUrl = XinhaUtil.encodeResourceURL(XinhaUtil.encode(parentUrl));
                url = 
                    new StringBuilder(80).append(parentUrl)
                    .append(parentUrl.indexOf('?') == -1 ? '?' : '&')
                    .append("_path=")
                    .append(getFacetSelectPath())
                    .toString();
            }
            
            return url;
        }

        public boolean isValid() {
            return path != null && uuid != null
                    && !(resourceDefinitions.size() > 1 && selectedResourceDefinition == null);
        }

        public JcrNodeModel getNodeModel() {
            return new JcrNodeModel(path);
        }

    }
}
