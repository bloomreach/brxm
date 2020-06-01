/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.solr.client.solrj.SolrServerException;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.solr.HippoSolrClient;

public class SolrIndexer extends BaseHstComponent{


    public static final String SOLR_MODULE_NAME = "org.hippoecm.hst.solr";

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        String uuid = request.getParameter("indexed");
        final HstRequestContext requestContext = request.getRequestContext();
        if (uuid != null) {
            try {
                String path = requestContext.getSession().getNodeByIdentifier(uuid).getPath();
                request.setAttribute("message", "indexed succesfully : " +  path);
            } catch (RepositoryException e) {
                throw new HstComponentException(e);
            }
        }

        try {
            final HstQuery query = requestContext.getQueryManager().createQuery(requestContext.getSiteContentBaseBean());
            query.setLimit(1000);
            request.setAttribute("result", query.execute());
        } catch (QueryException e) {
            throw new HstComponentException(e);
        }
    }

    @Override
    public void doAction(final HstRequest request, final HstResponse response) throws HstComponentException {
        String uuid = request.getParameter("uuid");
        try {
            HippoBean bean = (HippoBean) getObjectConverter().getObject(uuid, request.getRequestContext().getSession());
            HippoSolrClient solrClient = HstServices.getComponentManager().getComponent(HippoSolrClient.class.getName(), SOLR_MODULE_NAME);
            solrClient.getSolrServer().addBean(bean);
            solrClient.getSolrServer().commit();
            response.setRenderParameter("indexed", uuid);
            
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException(e);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        } catch (IOException e) {
            throw new HstComponentException(e);
        } catch (SolrServerException e) {
            throw new HstComponentException(e);
        }
    }
}
