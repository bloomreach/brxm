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
package org.hippoecm.cmsprototype.frontend.plugins.todo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class SortableTaskProvider extends SortableDataProvider{

    private static final long serialVersionUID = 1L;
    
    JcrNodeModel model = null;
    
    public SortableTaskProvider(JcrNodeModel model) {
        
        if (model != null){
                this.model = model;
        }
        
    }

    public Iterator<NodeModelWrapper> iterator(int first, int count) {

        NodeIterator children = null;
        
        if (this.model != null) {
                List<NodeModelWrapper> list = new ArrayList<NodeModelWrapper>();
                
                int i = 0;
                        
                try {
                                children = model.getNode().getNodes();
                        } catch (RepositoryException e) {
                                return null;
                        }
                                        
                        while(children.hasNext()) {

                                HippoNode jcrChild = (HippoNode) children.nextNode();
                        
                                try {
                                        if (jcrChild.isNodeType(HippoNodeType.NT_REQUEST)) {

                                                i++;

                                                if (i >= first && i < (first + count)) {
                                                        list.add(new JcrTreeNode(new JcrNodeModel(jcrChild)));
                                            }
                                        }
                                } catch (RepositoryException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                }
                return list.iterator();
        }
        else
        {
                return null;
        }
        
    }
    
    public IModel model(Object object) {
        if (model != null)
        {
                return (NodeModelWrapper) object;
        }
        else
        {
                return null;
        }
    }

    public int size() {
        if(model == null ) { return 0; }
        try {
                        if (model.getNode().getNodes() != null)
                        {
                            
                                //TODO: filter out non-request items
                                return (int) model.getNode().getNodes().getSize();
                                
                        }
                        else
                        {
                                return 0;
                        }
                } catch (RepositoryException e) {
                        return 0;
                }
    }


}
