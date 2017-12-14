/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;
import org.hippoecm.repository.api.HippoNodeType;

public class UserDataProvider extends SearchableDataProvider<User> {

    private static final long serialVersionUID = 1L;
    private static final String QUERY_USER_LIST = "SELECT * " +
                " FROM " + HippoNodeType.NT_USER
               +" WHERE (hipposys:system <> 'true' OR hipposys:system IS NULL)";

    private static final String QUERY_USER_LIST_TEMPLATE = "SELECT * " +
                " FROM " + HippoNodeType.NT_USER +
                " WHERE (hipposys:system <> 'true' OR hipposys:system IS NULL) AND " +
                            "(" +
                                " contains(hippo:_localname, '{}') OR " +
                                " contains(hipposys:firstname, '{}') OR " +
                                " contains(hipposys:lastname, '{}') OR " +
                                " contains(hipposys:email, '{}')" +
                            ")";

    private static final String HIPPO_USERS_NODE_PATH = "/hippo:configuration/hippo:users";

    public UserDataProvider() {
        super(QUERY_USER_LIST, QUERY_USER_LIST_TEMPLATE, HIPPO_USERS_NODE_PATH, HippoNodeType.NT_USER, HippoNodeType.NT_USERFOLDER);
        setSort("username", SortOrder.ASCENDING);
    }

    /**
     * Support overriding the query statements in instantiation by subclasses.
     */
    protected UserDataProvider(String searchAllSqlStatement, String searchTermSqlStatementTemplate) {
        super(searchAllSqlStatement, searchTermSqlStatementTemplate, HIPPO_USERS_NODE_PATH, HippoNodeType.NT_USER, HippoNodeType.NT_USERFOLDER);
        setSort("username", SortOrder.ASCENDING);
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
    public Iterator<User> iterator(long first, long count) {
        List<User> userList = new ArrayList<>(getList());

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

        return userList.subList((int) first, (int) Math.min(first + count, userList.size())).iterator();
    }
}
