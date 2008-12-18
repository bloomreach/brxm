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
package org.hippoecm.frontend.plugins.cms.admin.users;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Remove primitive total count accounting when it's 
 * possible to get the site of the resultset without going
 * through the accessmanager.
 */
public class UserDataProvider extends SortableDataProvider {

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DetachableUser.class);

    private static final String QUERY_USER_LIST = "SELECT * FROM hippo:user WHERE jcr:primaryType<>'hippo:user'";

    private static int totalCount = -1;
    private static String sessionId = "none";

    public UserDataProvider() {
        setSort("frontend:lastname", true);
    }

    protected QueryManager getQueryManager() throws RepositoryException {
        return ((UserSession) Session.get()).getQueryManager();
    }

    public Iterator<User> iterator(int first, int count) {
        List<User> users = new ArrayList<User>();
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
                        users.add(new User(node));
                    } catch (RepositoryException e) {
                        log.warn("Unable to instantiate new user.", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while trying to query user nodes.", e);
        }
        return users.iterator();
    }

    public IModel model(Object object) {
        return new DetachableUser((User) object);
    }

    public int size() {
        // just count once, until there is an option to do authorized queries
        if (totalCount > -1 && sessionId.equals(Session.get().getId())) {
            return totalCount;
        }
        try {
            HippoQuery countQuery = (HippoQuery) getQueryManager().createQuery(QUERY_USER_LIST, Query.SQL);
            // must return int instead of long
            totalCount = (int) countQuery.execute().getNodes().getSize();
            sessionId = Session.get().getId();
            return totalCount;
        } catch (RepositoryException e) {
            log.error("Unable to count the total number of users, returning 0", e);
            return 0;
        }
    }

    public static void countMinusOne() {
        totalCount--;
    }

    public static void countPlusOne() {
        totalCount++;
    }

    @Override
    public void detach() {
    }

    private String buildListQuery() {
        SortParam sortParam = getSort();
        sortParam.getProperty();
        StringBuilder sb = new StringBuilder();
        sb.append(QUERY_USER_LIST).append(" ");
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
