/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.security.impl;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HippoAuthenticationProvider
 * <P>
 * Hippo Repository based authentication provider implementation 
 * which queries <CODE>hipposys:authrole</CODE> nodes by the specified <CODE>rolesOfUserAndGroupQuery</CODE>.
 * So, it queries the groups of user first by the specified <CODE>groupsOfUserQuery</CODE>.
 * Also, by default, the <CODE>hipposys:authrole</CODE> nodes are retrieved from the <CODE>everywhere</CODE> domain.
 * You can change the domain name by setting <CODE>roleDomainName</CODE> property though.
 * </P>
 * @version $Id$
 */
public class HippoAuthenticationProvider extends JcrAuthenticationProvider {
    
    static final Logger log = LoggerFactory.getLogger(HippoAuthenticationProvider.class);
    
    public static final String DEFAULT_GROUPS_OF_USER_QUERY = 
        "//element(*, hipposys:group)[(@hipposys:members = ''{0}'' or @hipposys:members = ''*'') and @hipposys:securityprovider = ''internal'']";
    
    public static final String DEFAULT_ROLES_OF_USER_AND_GROUP_QUERY =
        "//hippo:configuration/hippo:domains/{0}/element(*, hipposys:authrole)[ @hipposys:users = ''{1}'' {2}]";
    
    private String groupsOfUserQuery = DEFAULT_GROUPS_OF_USER_QUERY;
    
    private String roleDomainName = "everywhere";
    
    private String rolesOfUserAndGroupQuery = DEFAULT_ROLES_OF_USER_AND_GROUP_QUERY;
    
    public HippoAuthenticationProvider(Repository systemRepository, Credentials systemCreds, Repository userAuthRepository) {
        super(systemRepository, systemCreds, userAuthRepository);
    }
    
    public void setGroupsOfUserQuery(String groupsOfUserQuery) {
        this.groupsOfUserQuery = groupsOfUserQuery;
    }
    
    public String getGroupsOfUserQuery() {
        return groupsOfUserQuery;
    }
    
    public void setRoleDomainName(String roleDomainName) {
        this.roleDomainName = roleDomainName;
    }
    
    public String getRoleDomainName() {
        return roleDomainName;
    }
    
    public void setRolesOfUserAndGroupQuery(String rolesOfUserAndGroupQuery) {
        this.rolesOfUserAndGroupQuery = rolesOfUserAndGroupQuery;
    }
    
    public String getRolesOfUserAndGroupQuery() {
        return rolesOfUserAndGroupQuery;
    }
    
    @Override
    protected Set<String> getRoleNamesOfUser(String username) throws LoginException, RepositoryException {
        Set<String> roleNameSet = null;
        Session session = null;
        
        try {
            if (getSystemCredentials() != null) {
                session = getSystemRepository().login(getSystemCredentials());
            } else {
                session = getSystemRepository().login();
            }

            String statement = MessageFormat.format(getGroupsOfUserQuery(), username);

            log.debug("Searching groups of user with query: " + statement);

            Query q = session.getWorkspace().getQueryManager().createQuery(statement, getQueryLanguage());
            QueryResult result = q.execute();
            NodeIterator nodeIt = result.getNodes();
            
            StringBuilder groupsConstraintsBuilder = new StringBuilder(100);
            
            while (nodeIt.hasNext()) {
                String groupName = nodeIt.nextNode().getName();
                groupsConstraintsBuilder.append("or @hipposys:groups = '").append(groupName).append("' ");
            }
            
            statement = MessageFormat.format(getRolesOfUserAndGroupQuery(), getRoleDomainName(), username, groupsConstraintsBuilder.toString());
            
            q = session.getWorkspace().getQueryManager().createQuery(statement, getQueryLanguage());
            result = q.execute();
            nodeIt = result.getNodes();
            
            roleNameSet = new HashSet<String>();
            
            while (nodeIt.hasNext()) {
                String roleName = nodeIt.nextNode().getProperty("hipposys:role").getString();
                roleNameSet.add(roleName);
            }
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                }
            }
        }
        
        if (roleNameSet == null) {
            roleNameSet = Collections.emptySet();
        }
        
        return roleNameSet;
    }
    
}
