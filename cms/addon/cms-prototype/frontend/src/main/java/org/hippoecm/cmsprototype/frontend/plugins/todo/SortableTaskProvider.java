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

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;

public class SortableTaskProvider extends SortableDataProvider{

    private static final long serialVersionUID = 1L;
    
    Folder folder;
    List<NodeModelWrapper> resources;
    
    public SortableTaskProvider(JcrNodeModel model) {
        
    	if (model != null){
        	this.folder = new Folder(model);
        }
        
    }

    public Iterator<NodeModelWrapper> iterator(int first, int count) {

    	
    	if (this.folder != null) {
	        List<NodeModelWrapper> list = new ArrayList<NodeModelWrapper>();
	        resources = new ArrayList<NodeModelWrapper>();
	        resources.addAll(folder.getSubFoldersAndDocuments());
	        //sortResources();
	        int i = 0;
	        for (Iterator<NodeModelWrapper> iterator = resources.iterator(); iterator.hasNext(); i++) {
	            NodeModelWrapper doc = iterator.next();
	            if (i >= first && i < (first + count)) {
	                list.add(doc);
	            }
	        }
	        return list.iterator();
    	}
/*    	if (this.folder != null)
    	{
	    	List<Node> list = new ArrayList<Node>();
	        int i = 0;
	        for (Iterator<Document> documents = folder.getDocuments().iterator(); documents.hasNext(); i++) {
	            Document doc = documents.next();
	            if (i >= first && i < (first + count)) {
	                list.add(doc.getNodeModel().getNode());
	            }
	        }
	        return list.iterator();
    	}*/
    	else
    	{
    		return null;
    	}
    }

    public IModel model(Object object) {
        if (folder != null)
        {
        	return (NodeModelWrapper) object;
//        	//return new JcrNodeModel((Node) folder);
        }
        else
        {
        	return null;
        }
    }

    public int size() {
    	if (folder != null)
    	{
            return folder.getSubFoldersAndDocuments().size();
    	}
    	else
    	{
    		return 0;
    	}
    }

	public void detach() {
		// TODO Auto-generated method stub
		
	}

}
