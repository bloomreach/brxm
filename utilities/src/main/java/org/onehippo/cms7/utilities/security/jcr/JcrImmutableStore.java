/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.cms7.utilities.security.jcr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.onehippo.cms7.utilities.security.ImmutableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JCR based implementation of {@link org.onehippo.cms7.utilities.security.ImmutableStore} in which information about users and groups are stored in a
 * certain structure in a JCR repository
 * <P>
 * Callers of objects of this class should not forget to call release
 * </P>
 */
public class JcrImmutableStore implements ImmutableStore {

    private static final Logger log = LoggerFactory.getLogger(JcrImmutableStore.class);

    private final Session session;

    // Only available for inheriting classes
    protected JcrImmutableStore() {
        this.session = null;
    }

    /**
     * One argument constructor which initializes a new
     *
     * @param session {@link Session} object to use for users and groups information retrieval
     */
    public JcrImmutableStore(Session session) {
        this.session = session;
    }

    @Override
    public List<String> getUsers() {
        try {
            QueryManager queryManager = this.session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(Queries.SQL.SELECT_ALL_USERS, Query.SQL);
            NodeIterator iterator = query.execute().getNodes();
            List<String> users = new ArrayList<String>();

            while (iterator.hasNext()) {
                users.add(iterator.nextNode().getName());
            }

            return users;
        } catch (RepositoryException rex) {
            log.error("Unable to read users information. Returning an empty list of users.", rex);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getGroups() {
        try {
            QueryManager queryManager = this.session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(Queries.SQL.SELECT_ALL_GROUPS, Query.SQL);
            NodeIterator iterator = query.execute().getNodes();
            List<String> groups = new ArrayList<String>();

            while (iterator.hasNext()) {
                groups.add(iterator.nextNode().getName());
            }

            return groups;
        } catch (RepositoryException rex) {
            log.error("Unable to read groups information. Returning an empty list of groups.", rex);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getGroupUsers(final String groupName) {
        validateUserName(groupName);

        try {
            QueryManager queryManager = this.session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(Queries.SQL.SELECT_GROUP_BY_NAME.replace("{}", groupName), Query.SQL);
            NodeIterator iterator = query.execute().getNodes();
            List<String> users = new ArrayList<String>();

            if (iterator.hasNext()) {
                Value[] members = iterator.nextNode().getProperty("hipposys:members").getValues();

                for (Value member : members) {
                    users.add(member.getString());
                }
            }

            return users;
        } catch (RepositoryException rex) {
            log.error("Unable to read group's users information for group: '" + groupName + "'. Returning an empty list of users.", rex);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getUserGroups(final String userName) {
        validateGroupName(userName);

        try {
            QueryManager queryManager = this.session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(Queries.SQL.SELECT_ALL_USER_GROUPS.replace("{}", userName), Query.SQL);
            NodeIterator iterator = query.execute().getNodes();
            List<String> groups = new ArrayList<String>();

            while (iterator.hasNext()) {
                groups.add(iterator.nextNode().getName());
            }

            return groups;
        } catch (RepositoryException rex) {
            log.error("Unable to read user's groups information for user: '" + userName + "'. Returning an empty list of groups.", rex);
        }

        return Collections.emptyList();
    }

    /**
     * Check whether a user is a member of a certain group
     *
     * @param userName  The user name
     * @param groupName The group name
     * @return <code>true</code> if user is a member of that group. <code>false</code> otherwise
     */
    @Override
    public boolean isMemberOf(final String userName, final String groupName) {
        validateUserName(userName);
        validateGroupName(groupName);

        return getUserGroups(userName).contains(groupName);
    }

    /**
     * This method should be called to allow the {@link JcrImmutableStore} to release any resources which might have been
     * opened and used to retrieve the required information of both users and groups
     * <P>
     * It is the responsibility of the caller to call this method for resources cleanup
     * </P>
     */
    @Override
    public void release() {
        if (session != null) {
            session.logout();
        }
    }

    protected void validateUserName(final String userName) {
        validateNotNull(userName, "User name can not be null");
        validateNotEmpty(userName, "User name can not be empty");
    }

    protected void validateGroupName(final String groupName) {
        validateNotNull(groupName, "Group name can not be null");
        validateNotEmpty(groupName, "Group name can not be empty");
    }

    protected void validateNotNull(final Object object, final String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    protected void validateNotEmpty(final String value, final String message) {
        if ("".equals(value)) {
            throw new IllegalArgumentException(message);
        }
    }

}
