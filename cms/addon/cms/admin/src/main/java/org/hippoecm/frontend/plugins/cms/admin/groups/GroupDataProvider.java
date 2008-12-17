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
package org.hippoecm.frontend.plugins.cms.admin.groups;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;

public class GroupDataProvider extends SortableDataProvider {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;
    
    private static final String QUERY_GROUP_LIST = "SELECT * FROM hippo:group";

    private static int totalCount = -1;

    public GroupDataProvider() {
        setSort("nodename", true);
    }

    protected Node getRootNode() throws RepositoryException {
        return ((UserSession) Session.get()).getJcrSession().getRootNode();
    }

    protected QueryManager getQueryManager() throws RepositoryException {
        return getRootNode().getSession().getWorkspace().getQueryManager();
    }

    public Iterator<Group> iterator(int first, int count) {

        List<Group> groups = new ArrayList<Group>();
        NodeIterator iter;
        try {
            HippoQuery listQuery = (HippoQuery) getQueryManager().createQuery(buildListQuery(), Query.SQL);
            listQuery.setOffset(first);
            listQuery.setLimit(count);
            iter = listQuery.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        groups.add(new Group(node));
                    } catch (RepositoryException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return groups.iterator();
    }

    public IModel model(Object object) {
        return new DetachableGroup((Group) object);
    }

    public int size() {
        // just count once, until there is an option to do authorized queries
        if (totalCount > -1) {
            return totalCount;
        }
        try {
            HippoQuery countQuery = (HippoQuery) getQueryManager().createQuery(QUERY_GROUP_LIST, Query.SQL);
            // must return int instead of long
            totalCount = (int) countQuery.execute().getNodes().getSize();
            return totalCount;
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return 0;
        }
    }

    public void detach() {
    }

    private String buildListQuery() {
        SortParam sortParam = getSort();
        sortParam.getProperty();
        StringBuilder sb = new StringBuilder();
        sb.append(QUERY_GROUP_LIST).append(" ");
        sb.append("ORDER BY ");
        sb.append(sortParam.getProperty()).append(" ");
        if (sortParam.isAscending()) {
            sb.append("ASC");
        } else {
            sb.append("DESC");
        }
        return sb.toString();
    }

}
