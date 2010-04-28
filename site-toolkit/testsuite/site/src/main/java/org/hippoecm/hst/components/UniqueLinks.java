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

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;

public class UniqueLinks extends BaseHstComponent {
    
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        int maxDepth = 4;
        HstRequestContext ctx = request.getRequestContext();
        HstLinkCreator hstLinkCreator = ctx.getHstLinkCreator();
        HstSite site = ctx.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite();
       
        String crUniqueLinkPart = this.getParameter("uniquelinkpart", request);
        if(crUniqueLinkPart != null && crUniqueLinkPart.split("/").length > maxDepth) {
            return;
        }
        
        List<UniqueLink> uniqueLinks = new ArrayList<UniqueLink>();
        for(int i = 0; i < 10; i++) {
            uniqueLinks.add(new UniqueLink(hstLinkCreator, site, crUniqueLinkPart));
        }
        request.setAttribute("uniquelinks", uniqueLinks);
       
    }


    public class UniqueLink {
        
        private HstLink link;
        private String name;
        
        public UniqueLink(HstLinkCreator hstLinkCreator, HstSite site, String crUniqueLinkPart){
            long creationTick = System.nanoTime();
            String url;
            if(crUniqueLinkPart !=  null) {
                url = crUniqueLinkPart+"/"+creationTick;
            } else {
                url = ""+creationTick;
            }
            
            this.link = hstLinkCreator.create("uniquelinks/"+url, site);
            this.name = "name-"+creationTick;
        }

        public HstLink getLink() {
            return link;
        }

        public String getName() {
            return name;
        }
        
        
    }
    
}


  
