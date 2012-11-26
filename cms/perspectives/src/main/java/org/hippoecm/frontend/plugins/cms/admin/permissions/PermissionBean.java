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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.hippoecm.frontend.plugins.cms.admin.domains.DetachableDomain;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Holds a role - domain - group combination
 */
public class PermissionBean implements Serializable {
    
    private static final String DOMAINS_BASE_PATH = 
            "/jcr:root/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.DOMAINS_PATH; 
    private static final String ALL_AUTHROLES_FOR_GROUP_QUERY = DOMAINS_BASE_PATH + 
            "/*/element(*, hipposys:authrole)[@hipposys:groups='{}']";

    public static final PermissionsBeanByDomainNameComparator COMPARATOR_BY_DOMAIN_NAME =
            new PermissionsBeanByDomainNameComparator();

    private final DetachableGroup group;
    private final DetachableDomain domain;
    private final Domain.AuthRole authRole;

    public PermissionBean(final DetachableGroup group, final DetachableDomain domain, final Domain.AuthRole authRole) {
        this.group = group;
        this.domain = domain;
        this.authRole = authRole;
    }

    public DetachableGroup getGroup() {
        return group;
    }

    public DetachableDomain getDomain() {
        return domain;
    }

    public Domain.AuthRole getAuthRole() {
        return authRole;
    }

    /**
     * Returns all permissions for a group. If you already have a {@link Group} object, this is faster than calling
     * forGroup(String).
     * @param group the {@link Group} to return all permissions for
     * @return a {@link List} of {@link PermissionBean}s containing all permissions for this group
     */
    public static List<PermissionBean> forGroup(Group group) {
        String queryString = ALL_AUTHROLES_FOR_GROUP_QUERY.replace("{}", group.getGroupname());
        NodeIterator nodeIterator = obtainNodeIteratorForQueryString(queryString);

        DetachableGroup detachableGroup = new DetachableGroup(group);
        List<PermissionBean> permissionBeans = new ArrayList<PermissionBean>();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            Domain.AuthRole authRole = new Domain.AuthRole(node);

            DetachableDomain detachableDomain;
            String domainNodePath = getDomainNodePathForAuthRole(node);
            detachableDomain = new DetachableDomain(domainNodePath);

            PermissionBean permissionBean = new PermissionBean(detachableGroup, detachableDomain, authRole);
            permissionBeans.add(permissionBean);
        }

        return permissionBeans;
    }

    /**
     * Returns all permissions for a group. If you already have a {@link Group} object, call forGroup(Group) instead,
     * it will be faster.
     * @param groupName the name of the {@link Group} to return all permissions for
     * @return a {@link List} of {@link PermissionBean}s containing all permissions for this group
     */
    public static List<PermissionBean> forGroup(String groupName) {
        return forGroup(Group.forName(groupName));
    }

    private static NodeIterator obtainNodeIteratorForQueryString(final String queryString) {
        QueryManager queryManager = UserSession.get().getQueryManager();
        try {
            @SuppressWarnings("deprecation") Query query = queryManager.createQuery(queryString, Query.XPATH);
            QueryResult queryResult = query.execute();
           return queryResult.getNodes();
        } catch (InvalidQueryException e) {
            throw new IllegalArgumentException("Query is malformed, cannot create query.", e);
        } catch (RepositoryException e) {
            throw new IllegalStateException("Repository error occured, cannot create query.", e);
        }
    }

    private static String getDomainNodePathForAuthRole(Node authRoleNode) {
        try {
            Node domainNodeCandidate = authRoleNode.getParent();
            if (!domainNodeCandidate.isNodeType(HippoNodeType.NT_DOMAIN)) {
                throw new IllegalStateException("Parent of authrole node is not a domain node");
            }
            return domainNodeCandidate.getPath();
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot obtain the domain node for authrole node.");
        }
    }
    
    public static class PermissionsBeanByDomainNameComparator implements Comparator<PermissionBean>, Serializable {
        @Override
        public int compare(final PermissionBean o1, final PermissionBean o2) {
            String domainName1 = o1.getDomain().getObject().getName();
            String domainName2 = o2.getDomain().getObject().getName();
            return domainName1.compareTo(domainName2);
        }
    }
}
