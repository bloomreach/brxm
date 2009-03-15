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

import java.util.Collection;
import java.util.List;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.ocm.HippoStdFolder;
import org.hippoecm.hst.ocm.HippoStdDocument;
import org.hippoecm.hst.ocm.HippoStdNode;
import org.hippoecm.hst.ocm.HippoStdSearcher;
import org.hippoecm.hst.ocm.NewsPage;

public class Overview extends GenericResourceServingHstComponent {
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        System.out.println(this.getParameter("year" , request));
        
        
        HippoStdNode hippoStdNode = this.getContentNode(request);
//        
//        HippoStdSearcher searcher = this.getHippoStdSearcher(hippoStdNode, request);
//        
//        
//        String s = request.getRequestContext().getHstCtxWhereClauseComputer().getCtxWhereClause(hippoStdNode.getNode(), request.getRequestContext());
//        
//        System.out.println(s);
//        
//        ObjectContentManager ocm = this.getObjectContentManager(request);
//      
//        QueryManager queryManager = ocm.getQueryManager();
//       
//        Filter filter = queryManager.createFilter(NewsPage.class);
//        
//        
//        System.out.println(filter.toString());
//        //filter.setScope("/testcontent//");
//        
//        Query query = queryManager.createQuery(filter);
//        
//        long start = System.currentTimeMillis();
//        
//        Collection result = ocm.getObjects(query);
//       
//        
//        System.out.println("took " + (System.currentTimeMillis() - start));
//        
//        if(hippoStdNode == null) {
//            return;
//        }
//        
        request.setAttribute("parent", hippoStdNode.getParentFolder());
        request.setAttribute("current",hippoStdNode);
        
        if(hippoStdNode instanceof HippoStdFolder) {
            request.setAttribute("collections",((HippoStdFolder)hippoStdNode).getFolders());
            request.setAttribute("documents",setDocuments((HippoStdFolder)hippoStdNode, 0 , Integer.MAX_VALUE));
        }
        
    }

    
    private HippoStdSearcher getHippoStdSearcher(HippoStdNode hippoStdNode, HstRequest request) {
        
        return null;
    }


 
    public List<HippoStdDocument> setDocuments(HippoStdFolder hippoStdCollection, int from, int to){
        return hippoStdCollection.getDocuments(from, to);
    }
   
}


  
