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
package org.hippoecm.hst.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.util.HSTNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Site map item component used by the site map component.
 */
public class SiteMapItem {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final List<SiteMapItem> documents = new ArrayList<SiteMapItem>();
    private final List<SiteMapItem> folders = new ArrayList<SiteMapItem>();

    private final int level;
    private final String path;
    private final String label;

    /**
     * Constructor.
     */
    public SiteMapItem(final Node node, final int level,
            final String[] excludedDocumentNames,  final String[] documentLabelProperties) {
        super();

        this.level = level;
        try {
            this.label = getLabel(node, documentLabelProperties);
            this.path = node.getPath();
        }
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }

        createSubItems(node, excludedDocumentNames, documentLabelProperties);
    }

    public List<SiteMapItem> getDocuments() {
        return documents;
    }

    public List<SiteMapItem> getFolders() {
        return folders;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }

    public String getPath() {
        return path;
    }

    // for debugging
    public String toString() {
        return super.toString() + "[level=" + level
                + ", path=" + path + ", name=" + label
                + ", documentItems=" + documents
                + ", folderItems=" + folders+ "]";
    }

    private String getLabel(Node node, String[] documentLabelProperties) throws RepositoryException {

        // an actively set label property
        if (node.hasProperty(HSTNodeTypes.HST_LABEL)) {
            return node.getProperty(HSTNodeTypes.HST_LABEL).getString();
        }

        // for a document handle, get the document and try the label properties
        if (documentLabelProperties != null) {
            for (int i = 0; i < documentLabelProperties.length; i++) {
                String property = documentLabelProperties[i];
                if (node.hasProperty(property.trim())) {
                    return node.getProperty(property).getString();
                }
            }
        }

        // capitalized node name as label
        String label = node.getName().substring(0, 1).toUpperCase();
        if (node.getName().length() > 1) {
            label += node.getName().substring(1);
        }
        return label;
    }

    private void createSubItems(final Node node, final String[] excludedDocumentNames,
            final String[] documentLabelProperties) {
        try {
            // creating site map sub items may be specifically turned off
            if (node.isNodeType(HSTNodeTypes.HST_CHILDLESS)) {
                return;
            }

            // loop the subnodes and create items from them if applicable
            NodeIterator subNodes =  node.getNodes();
            while (subNodes.hasNext()) {

                Node subNode = (Node) subNodes.next();

                // skip explicitly excluded items
                if (subNode.isNodeType(HSTNodeTypes.HST_NON_SITE_MAP_ITEM)) {
                    continue;
                }

                // always create an item, except document excludes

                // when a handle, get document and check excludes
                if (subNode.isNodeType(HippoNodeType.NT_HANDLE)) {

                    if (!subNode.hasNode(subNode.getName())) {
                        continue;
                    }

                    Node documentNode = subNode.getNode(subNode.getName());

                    // hide document nodes by default names
                    if (excludedDocumentNames != null) {
                        boolean skip = false;
                        int i = 0;
                        while ((i < excludedDocumentNames.length) && !skip) {
                            skip = excludedDocumentNames[i].trim().equals(documentNode.getName());
                            i++;
                        }
                        if (skip) {
                            continue;
                        }
                    }

                    documents.add(new SiteMapItem(documentNode, this.getLevel() + 1,
                            excludedDocumentNames, documentLabelProperties));
                }

                // other nodes (folders)
                else  {
                    folders.add(new SiteMapItem(subNode, this.getLevel() + 1,
                            excludedDocumentNames, documentLabelProperties));
                }
            }
        }
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }
}
