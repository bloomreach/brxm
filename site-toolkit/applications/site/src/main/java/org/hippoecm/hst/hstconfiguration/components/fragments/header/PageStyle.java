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
package org.hippoecm.hst.hstconfiguration.components.fragments.header;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.hstconfiguration.components.HstComponentBase;
import org.hippoecm.hst.pagetypes.PageStyleType;
import org.hippoecm.hst.service.ServiceFactory;

public class PageStyle extends HstComponentBase {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        super.doBeforeRender(request, response);
        
        HstRequestContext hrc = request.getRequestContext();
        
//        String componentContentPath =  this.hstComponentConfigurationBean.getComponentContentBasePath();
//        if(componentContentPath != null) {
//            try {
//                Session session = hrc.getSession();
//                Node contentNode = (Node)session.getItem(hrc.getMatchedSiteMapItem().getSiteMapItem().getHstSiteMap().getSite().getContentPath());
//                Node componentContent = contentNode.getNode(componentContentPath);
//                if(componentContent.hasNode(componentContent.getName())) {
//                    Node n = componentContent.getNode(componentContent.getName());
//                    PageStyleType stylePage = ServiceFactory.create(n, PageStyleType.class);
//                    
//                    request.setAttribute("style", stylePage); 
//                }
//                
//            } catch (LoginException e) {
//                e.printStackTrace();
//            } catch (RepositoryException e) {
//                e.printStackTrace();
//            }
//        }
    }


    
}
