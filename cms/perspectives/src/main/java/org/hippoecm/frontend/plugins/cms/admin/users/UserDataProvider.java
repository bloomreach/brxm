/*
 *  Copyright 2008-2012 Hippo.
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.comparators.NullComparator;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;
import org.hippoecm.repository.api.HippoNodeType;


public class UserDataProvider extends SearchableDataProvider<User> {

    private static final long serialVersionUID = 1L;
    private static final String QUERY_USER_LIST = "SELECT * FROM " + HippoNodeType.NT_USER
            + " where (hipposys:system <> 'true' or hipposys:system IS NULL)";

    public UserDataProvider() {
        super(QUERY_USER_LIST, "/hippo:configuration/hippo:users", HippoNodeType.NT_USER, HippoNodeType.NT_USERFOLDER);
        setSort("username", true);
    }

    @Override
    public IModel<User> model(final User user) {
        return new DetachableUser(user);
    }

    @Override
    protected User createBean(final Node node) throws RepositoryException {
        return new User(node);
    }

    @Override
    public Iterator<User> iterator(int first, int count) {
        List<User> userList = new ArrayList<User>(getList());

        final Comparator<String> nullSafeComparator = new NullComparator(false);

        Collections.sort(userList, new Comparator<User>() {
            public int compare(User user1, User user2) {
                int direction = getSort().isAscending() ? 1 : -1;

                if ("frontend:firstname".equals(getSort().getProperty())) {
                    return direction * (nullSafeComparator.compare(user1.getFirstName(), user2.getFirstName()));
                } else if ("frontend:lastname".equals(getSort().getProperty())) {
                    return direction * (nullSafeComparator.compare(user1.getLastName(), user2.getLastName()));
                } else {
                    return direction * (nullSafeComparator.compare(user1.getUsername(), user2.getUsername()));
                }
            }
        });

        return userList.subList(first, Math.min(first + count, userList.size())).iterator();
    }
}
