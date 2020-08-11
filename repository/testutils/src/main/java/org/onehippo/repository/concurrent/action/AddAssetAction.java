/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

//import org.hippoecm.frontend.plugins.gallery.model.DefaultGalleryProcessor;
//import org.hippoecm.frontend.plugins.gallery.model.GalleryProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.gallery.GalleryWorkflow;

public class AddAssetAction extends AbstractGalleryWorkflowAction {

    public AddAssetAction(ActionContext context) {
        super(context);
    }

    @Override
    protected String getWorkflowMethodName() {
        return "createGalleryItem";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        GalleryWorkflow workflow = getGalleryWorkflow(node);
        String nodeName = "asset";
        do {
            nodeName +=  random.nextInt(10);
        } while (node.hasNode(nodeName));
        Document document = workflow.createGalleryItem(nodeName, "hippogallery:exampleAssetSet");
        node.getSession().refresh(false);
        Node assetNode = document.getNode(node.getSession());
        InputStream istream = getClass().getClassLoader().getResourceAsStream("org/onehippo/repository/concurrent/action/Hippo.pdf");
        makeImage(assetNode, istream, "application/pdf");
        node.getSession().save();
        return assetNode;
    }

    private void makeImage(Node node, InputStream istream, String mimeType) throws Exception {
        Node primaryChild;
        try {
            Item item = getPrimaryItem(node);
            if (!item.isNode()) {
                throw new Exception("Primary item is not a node");
            }
            primaryChild = (Node) item;
            if (primaryChild.isNodeType("hippo:resource")) {
                setDefaultResourceProperties(primaryChild, mimeType, istream);
            }
            validateResource(primaryChild);
            for (NodeDefinition childDef : node.getPrimaryNodeType().getChildNodeDefinitions()) {
                if (childDef.getDefaultPrimaryType() != null
                        && childDef.getDefaultPrimaryType().isNodeType("hippo:resource")) {
                    makeRegularImage(node, childDef.getName(), primaryChild.getProperty(JcrConstants.JCR_DATA).getStream(),
                            primaryChild.getProperty(JcrConstants.JCR_MIMETYPE).getString(), primaryChild.getProperty(
                            JcrConstants.JCR_LASTMODIFIED).getDate());
                }
            }
            makeThumbnailImage(primaryChild, primaryChild.getProperty(JcrConstants.JCR_DATA).getStream(), primaryChild
                    .getProperty(JcrConstants.JCR_MIMETYPE).getString());
        } catch (ItemNotFoundException ignore) {
        }
    }

    private Item getPrimaryItem(Node node) throws Exception {
        NodeType primaryType = node.getPrimaryNodeType();
        String primaryItemName = primaryType.getPrimaryItemName();
        while (primaryItemName == null && !"nt:base".equals(primaryType.getName())) {
            for (NodeType nt : primaryType.getSupertypes()) {
                if (nt.getPrimaryItemName() != null) {
                    primaryItemName = nt.getPrimaryItemName();
                    break;
                }
                if (nt.isNodeType("nt:base")) {
                    primaryType = nt;
                }
            }
        }
        if (primaryItemName == null) {
            throw new ItemNotFoundException("No primary item definition found in type hierarchy");
        }
        return node.getSession().getItem(node.getPath() + "/" + primaryItemName);
    }

    private void setDefaultResourceProperties(Node node, String mimeType, InputStream inputStream) throws RepositoryException {
        try{
            setDefaultResourceProperties(node, mimeType, node.getSession().getValueFactory().createBinary(inputStream));
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void makeRegularImage(Node node, String name, InputStream istream, String mimeType, Calendar lastModified)
            throws RepositoryException {
        if (!node.hasNode(name)) {
            Node child = node.addNode(name);
            child.setProperty(JcrConstants.JCR_DATA, node.getSession().getValueFactory().createBinary(istream));
            child.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
            child.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);
        }
    }

    private void makeThumbnailImage(Node node, InputStream resourceData, String mimeType) throws RepositoryException {
        node.setProperty(JcrConstants.JCR_DATA, node.getSession().getValueFactory().createBinary(resourceData));
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
    }

    private void setDefaultResourceProperties(final Node node, final String mimeType, final Binary binary) throws RepositoryException {
        node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        node.setProperty(JcrConstants.JCR_DATA, binary);
        node.setProperty(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
    }

    private void validateResource(Node resource) throws Exception {
        try {
            String mimeType = (resource.hasProperty(JcrConstants.JCR_MIMETYPE) ?
                    resource.getProperty(JcrConstants.JCR_MIMETYPE).getString() : "");

            if (mimeType.equals("application/pdf")) {
                String line;
                line = new BufferedReader(new InputStreamReader(resource.getProperty(JcrConstants.JCR_DATA).getBinary().getStream()))
                        .readLine().toUpperCase();
                if (!line.startsWith("%PDF-")) {
                    throw new Exception("impermissible pdf type content");
                }
            }
        } catch (IOException ex) {
            throw new Exception("impermissible unknown type content");
        }
    }

}
