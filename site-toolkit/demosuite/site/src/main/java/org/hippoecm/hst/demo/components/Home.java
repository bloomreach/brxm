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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoRequest;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Home extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Home.class);

 
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        super.doBeforeRender(request, response);
        HippoBean n = this.getContentBean(request);
        
        if(n == null) {
            return;
        }
        request.setAttribute("document",n);
        
        
        try {
            Session s = request.getRequestContext().getSession();
            QueryManager mng = s.getWorkspace().getQueryManager();
            //Query q = mng.createQuery( "//element(*,"+HippoNodeType.NT_REQUEST+")", "xpath");

            Query q = mng.createQuery( "//*[jcr:primaryType='"+HippoNodeType.NT_REQUEST+"']", "xpath");
            ((HippoQuery)q).setLimit(10);
            QueryResult result = q.execute();
            NodeIterator nodes = result.getNodes();
            List<HippoRequest> todoList = new ArrayList<HippoRequest>();
           
            while(nodes.hasNext()) {
                Node requestNode = nodes.nextNode();
                if(request == null) {
                    continue;
                }
                try {
                   HippoBean bean = (HippoBean) this.getObjectConverter().getObject(requestNode);
                   
                   if(bean instanceof HippoRequest) {
                       todoList.add((HippoRequest)bean);
                   }
                   
                   
                }  catch (ObjectBeanManagerException e) {
                    e.printStackTrace();
                }
            }
            request.setAttribute("todoList", todoList);
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}