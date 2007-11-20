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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;

public class SortableJcrDataProvider extends SortableDataProvider {

    private static final long serialVersionUID = 1L;
    
    JcrNodeModel model;
    
    public SortableJcrDataProvider(JcrNodeModel model) {
        this.model = model;
    }

    public Iterator iterator(int first, int count) {
        List list = new ArrayList();
        
        if (model != null) {
            HippoNode node = model.getNode();
            try {
                int i = 0;
                for (NodeIterator ni = node.getNodes(); ni.hasNext(); i++) {
                    HippoNode child = (HippoNode) ni.next();
                    if (i >= first && i < (first + count)) {
                        list.add(child);
                    }
                }
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
        
        return list.iterator();
    }

    public IModel model(Object object) {
        return new JcrNodeModel(model, (HippoNode) object);
    }

    public int size() {
        int size = 0;
        if (model != null) {
            HippoNode node = model.getNode();
            try {
                size = (int) node.getNodes().getSize();
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return size;
    }

}
