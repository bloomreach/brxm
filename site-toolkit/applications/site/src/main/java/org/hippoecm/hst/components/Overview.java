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

import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdDocument;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdFolder;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNode;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNodeIterator;
import org.hippoecm.hst.jackrabbit.ocm.query.HippoStdFilter;
import org.hippoecm.hst.jackrabbit.ocm.query.HstOCMQuery;
import org.hippoecm.hst.ocm.NewsPage;

public class Overview extends GenericResourceServingHstComponent {
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
       // System.out.println(this.getParameters(request));
        
        HippoStdNode hippoStdNode = this.getContentNode(request);
        
        if(hippoStdNode == null) {
            hippoStdNode = this.getSiteContentBaseNode(request);
        }
        //  
        
        HstOCMQuery query = this.getHstQuery(request);
        HippoStdNode hippoStdNode2 = this.getSiteContentBaseNode(request);
            try {
                query.setScope(hippoStdNode2);
                
                HippoStdFilter filter = query.createFilter(NewsPage.class);
                //filter.addContains("title", "News");
                query.setFilter(filter);
                //query.addOrderByDescending("date");
                
                HippoStdNodeIterator it = query.execute();
                
                while(it.hasNext()) {
                    NewsPage p = (NewsPage)it.nextHippoStdNode();
                    System.out.println(p);
                }
            }catch (QueryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           
       
      
        if(hippoStdNode == null) {
            return;
        }
        
        request.setAttribute("parent", hippoStdNode.getParentFolder());
        request.setAttribute("current",hippoStdNode);
        
        if(hippoStdNode instanceof HippoStdFolder) {
            request.setAttribute("collections",((HippoStdFolder)hippoStdNode).getFolders());
            request.setAttribute("documents",setDocuments((HippoStdFolder)hippoStdNode, 0 , Integer.MAX_VALUE));
        }
        
    }

 
 
    public List<HippoStdDocument> setDocuments(HippoStdFolder hippoStdCollection, int from, int to){
        return hippoStdCollection.getDocuments(from, to);
    }
   
}


  
