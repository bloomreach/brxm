/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSubNavigation;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Faceted extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Faceted.class);
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
       
        HippoBean currentBean = this.getContentBean(request);
        
        if(currentBean instanceof HippoFacetChildNavigationBean) {
            HippoFacetChildNavigationBean facetNav = (HippoFacetChildNavigationBean)currentBean;
            
            
            List<ProductBean> resultset = new ArrayList<ProductBean>();
         
            HippoDocumentIterator<ProductBean> it = facetNav.getResultSet().getDocumentIterator(ProductBean.class);
            int skip = 0;
            it.skip(skip);
            while(it.hasNext() && it.getPosition() < 10 + (skip - 1)) {
                // the it.getPosition gets increased on it.next() call, hence above, skip - 1
                resultset.add(it.next());
            }
            
            request.setAttribute("resultset", resultset);
        }
        
        if(currentBean instanceof HippoFacetSubNavigation) {
            request.setAttribute("subnavigation", currentBean);
        }
    }

    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
       
        DummyCarDocsCreator carCreator = new DummyCarDocsCreator();
        
        Session writableSession = null;
        try {
            writableSession = this.getPersistableSession(request);
            String rootByPath = this.getHstSite(request).getCanonicalContentPath();
            rootByPath = PathUtils.normalizePath(rootByPath);
            String numberStr = request.getParameter("number");
            int number = Integer.parseInt(numberStr);
            
            carCreator.createCars(writableSession, rootByPath, number);
            
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
        } finally {
            if(writableSession != null) {
                writableSession.logout();
            }
        }
        
        
        
        
    }
       
}