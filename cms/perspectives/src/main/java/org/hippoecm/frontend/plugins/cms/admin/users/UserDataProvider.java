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

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.cms.admin.SearchableDataProvider;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

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

    private static final Collator collator = Collator.getInstance(UserSession.get().getLocale());
    private static final Comparator<String> propertyComparator = new PropertyComparator(collator);
    private static final Comparator<User> firstNameComparator = comparing(User::getFirstName, nullsLast(propertyComparator));
    private static final Comparator<User> lastNameComparator = comparing(User::getLastName, nullsLast(propertyComparator));
    private static final Comparator<User> usernameComparator = comparing(User::getUsername, propertyComparator);

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
        final List<User> userList = new ArrayList<>(getList());

        userList.sort((user1, user2) -> {
            final int direction = getSort().isAscending() ? 1 : -1;
            switch (getSort().getProperty()) {
                case "frontend:firstname":
                    return direction * firstNameComparator.compare(user1, user2);
                case "frontend:lastname":
                    return direction * lastNameComparator.compare(user1, user2);
                default:
                    return direction * usernameComparator.compare(user1, user2);
            }
        });

        return userList.subList((int) first, (int) Math.min(first + count, userList.size())).iterator();
    }

    private static class PropertyComparator implements Comparator<String> {
        private final Collator collator;

        PropertyComparator(Collator collator) {
            this.collator = collator;
        }

        @Override
        public int compare(final String property1, final String property2) {
            final CollationKey key1 = collator.getCollationKey(property1);
            final CollationKey key2 = collator.getCollationKey(property2);
            return key1.compareTo(key2);
        }
    }

}
