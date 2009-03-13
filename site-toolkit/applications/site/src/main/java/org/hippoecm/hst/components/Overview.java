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

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.ocm.HippoStdCollection;
import org.hippoecm.hst.ocm.HippoStdDocument;
import org.hippoecm.hst.ocm.HippoStdNode;

public class Overview extends GenericResourceServingHstComponent {
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        HippoStdNode  n = this.getContentNode(request);
        
        if(n == null) {
            return;
        }
        
        request.setAttribute("parent", n.getParentCollection());
        request.setAttribute("current",(n));
        
        if(n instanceof HippoStdCollection) {
            request.setAttribute("collections",((HippoStdCollection)n).getCollections());
            request.setAttribute("documents",setDocuments((HippoStdCollection)n, 0 , Integer.MAX_VALUE));
        }
        
    }

    public List<HippoStdDocument> setDocuments(HippoStdCollection hippoStdCollection, int from, int to){
        return hippoStdCollection.getDocuments(from, to);
    }
   
}


  
