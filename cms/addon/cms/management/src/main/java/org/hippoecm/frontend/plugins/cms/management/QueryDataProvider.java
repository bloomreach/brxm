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
package org.hippoecm.frontend.plugins.cms.management;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryDataProvider extends SortableNodesDataProvider {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(QueryDataProvider.class);

    private String queryString;
    private String queryType;

    public QueryDataProvider(String queryString, String queryType, String defaultSort) {
        super(defaultSort);
        this.queryString = queryString;
        this.queryType = queryType;
    }

    @Override
    protected List<JcrNodeModel> createNodes() {
        List<JcrNodeModel> list = new ArrayList<JcrNodeModel>();
        try {
            QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
            HippoQuery query = (HippoQuery) queryManager.createQuery(queryString, queryType);
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            session.refresh(true);
            QueryResult result;
            result = query.execute();

            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                JcrNodeModel modcheck = new JcrNodeModel(it.nextNode());
                list.add(modcheck);
            }
        } catch (RepositoryException e) {
            log.error("Error executing query[" + queryString + "]", e);
        }
        return list;
    }

}
