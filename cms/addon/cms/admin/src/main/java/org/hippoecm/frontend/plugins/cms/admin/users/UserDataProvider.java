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
import java.util.Collections;
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

    private static final String QUERY_USER_LIST = "SELECT * FROM hipposys:user where hipposys:system <> 'true' or hipposys:system IS NULL";
    private static transient List<User> userList = new ArrayList<User>();
    private static volatile boolean dirty = true;
    
    private static String sessionId = "none";

    public UserDataProvider() {
    }

    public Iterator<User> iterator(int first, int count) {
        List<User> users = new ArrayList<User>();
        for (int i = first; i < (count + first); i++) {
            users.add(userList.get(i));
        }
        return users.iterator();
    }

    public IModel model(Object object) {
        return new DetachableUser((User) object);
    }

    public int size() {
        populateUserList();
        return userList.size();
    }

    /**
     * Actively invalidate cached list
     */
    public static void setDirty() {
        dirty = true;
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     */
    private static void populateUserList() {
        synchronized (UserDataProvider.class) {
            if (!dirty && sessionId.equals(Session.get().getId())) {
                return;
            }
            userList.clear();
            NodeIterator iter;
            try {
                Query listQuery = ((UserSession) Session.get()).getQueryManager().createQuery(QUERY_USER_LIST, Query.SQL);
                iter = listQuery.execute().getNodes();
                while (iter.hasNext()) {
                    Node node = iter.nextNode();
                    if (node != null) {
                        try {
                            userList.add(new User(node));
                        } catch (RepositoryException e) {
                            log.warn("Unable to instantiate new user.", e);
                        }
                    }
                }
                Collections.sort(userList);
                sessionId = Session.get().getId();
                dirty = false;
            } catch (RepositoryException e) {
                log.error("Error while trying to query user nodes.", e);
            }   
        }
    }

}
