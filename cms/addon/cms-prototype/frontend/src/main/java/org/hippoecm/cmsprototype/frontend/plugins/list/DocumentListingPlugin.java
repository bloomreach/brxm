/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.list;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.hippoecm.cmsprototype.frontend.plugins.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

public class DocumentListingPlugin extends AbstractListingPlugin {

    private static final long serialVersionUID = 1L;

    public static final String USER_PREF_NODENAME = "hippo:browseperspective-listingview";

    public DocumentListingPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    /**
     * Finds the first parent node that is a collection of handles 
     * @param nodeModel
     * @return the JcrNodeModel of the matching parent node, or null
     */
    protected JcrNodeModel findCollectionModel(JcrNodeModel nodeModel) {
        
        JcrNodeModel result = nodeModel;
        String docUUID = "";
        
        Node resultNode = result.getNode();
            
        try {
            if (resultNode.hasProperty("document")) {
                
                try {
                    docUUID = resultNode.getProperty("document").getString();
                } catch (ValueFormatException e) {
                    // docUUID property has incorrect formatting
                    result = null;
                    return result;
                }
                
                if (!"".equals(docUUID)) {
                    
                    // This is a referencing Node. Maybe a request node? Find the document the nodeModel is referring at.
    
                    javax.jcr.Session session = (javax.jcr.Session)(((UserSession)Session.get()).getJcrSession());
                    result = findCollectionModel(new JcrNodeModel(session.getNodeByUUID(docUUID)));
                    return result;
                }
            }
        } catch (RepositoryException e2) {
            return null;
        }
        
        try {
            if (resultNode.getPrimaryNodeType().isNodeType(HippoNodeType.NT_DOCUMENT)) {
                // This is a document. We need the parent (or maybe even the parent's parent).
                result = findCollectionModel(new JcrNodeModel(resultNode.getParent()));
                return result;
            }
        } catch (ItemNotFoundException e2) {
            return null;
        } catch (AccessDeniedException e2) {
            return null;
        } catch (RepositoryException e2) {
            return null;
        }
        
            try {
                if (resultNode.getPrimaryNodeType().isNodeType(HippoNodeType.NT_HANDLE)){
                    // This is a handle. We need the parent.
                    result = findCollectionModel(new JcrNodeModel(nodeModel.getNode().getParent()));
                    return result;
                }
            } catch (ItemNotFoundException e1) {
                return null;
            } catch (AccessDeniedException e1) {
                return null;
            } catch (RepositoryException e1) {
                return null;
            }
        
       return result;
    }

    @Override
    protected void addTable(JcrNodeModel nodeModel, int pageSize, int viewSize) {

        dataTable = new CustomizableDocumentListingDataTable("table", columns, new SortableDocumentsProvider(
                findCollectionModel(nodeModel)), pageSize, false);

        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        add((Component)dataTable);

    }

    @Override
    protected String getPluginUserPrefNodeName() {
        return USER_PREF_NODENAME;
    }








}
