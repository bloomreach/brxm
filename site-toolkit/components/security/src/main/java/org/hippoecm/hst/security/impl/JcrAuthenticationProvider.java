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
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientRole;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JcrAuthenticationProvider
 * <P>
 * Basic authentication provider implementation which queries role nodes by the specified <CODE>rolesOfUserQuery</CODE>.
 * The default configuration is to query group nodes from the Hippo Repository.
 * </P>
 * @version $Id$
 */
public class JcrAuthenticationProvider implements AuthenticationProvider {
    
    static final Logger log = LoggerFactory.getLogger(JcrAuthenticationProvider.class);
    
    public static final String DEFAULT_ROLES_OF_USER_QUERY = 
        "//element(*, hipposys:group)[(@hipposys:members = ''{0}'' or @hipposys:members = ''*'') and @hipposys:securityprovider = ''internal'']";
    
    private Repository systemRepository;
    private Credentials systemCreds;
    private Repository userAuthRepository;
    
    private String rolesOfUserQuery = DEFAULT_ROLES_OF_USER_QUERY;
    private String queryLanguage = Query.XPATH;
    
    private String defaultRoleName;
    
    public JcrAuthenticationProvider(Repository systemRepository, Credentials systemCreds, Repository userAuthRepository) {
        this.systemRepository = systemRepository;
        this.systemCreds = systemCreds;
        this.userAuthRepository = userAuthRepository;
    }
    
    public Repository getSystemRepository() {
        return systemRepository;
    }
    
    public Credentials getSystemCredentials() {
        return systemCreds;
    }
    
    public Repository getUserAuthRepository() {
        return userAuthRepository;
    }

    public void setRolesOfUserQuery(String rolesOfUserQuery) {
        this.rolesOfUserQuery = rolesOfUserQuery;
    }
    
    public String getRolesOfUserQuery() {
        return rolesOfUserQuery;
    }
    
    public void setQueryLanguage(String queryLanguage) {
        this.queryLanguage = queryLanguage;
    }
    
    public String getQueryLanguage() {
        return queryLanguage;
    }
    
    public void setDefaultRoleName(String defaultRoleName) {
        this.defaultRoleName = defaultRoleName;
    }
    
    public String getDefaultRoleName() {
        return defaultRoleName;
    }
    
    public User authenticate(String userName, char[] password) throws SecurityException {
        Session session = null;
        SimpleCredentials creds = new SimpleCredentials(userName, password);
        
        try {
            session = userAuthRepository.login(new SimpleCredentials(userName, password));
        } catch (LoginException e) {
            throw new SecurityException(e.getLocalizedMessage(), e);
        } catch (RepositoryException e) {
            throw new SecurityException(e.getLocalizedMessage(), e);
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                }
            }
        }
        
        return new TransientUser(creds.getUserID());
    }
    
    public Set<Role> getRolesByUsername(String username) throws SecurityException {
        Set<Role> roleSet = null;
        
        try {
            Set<String> roleNameSet = getRoleNamesOfUser(username);
            roleSet = new HashSet<Role>();
            
            if (defaultRoleName == null) {
                for (String roleName : roleNameSet) {
                    roleSet.add(new TransientRole(roleName));
                }
            } else {
                boolean defaultRoleAdded = false;
                
                for (String roleName : roleNameSet) {
                    roleSet.add(new TransientRole(roleName));
                    
                    if (defaultRoleName.equals(roleName)) {
                        defaultRoleAdded = true;
                    }
                }
                
                if (!defaultRoleAdded) {
                    roleSet.add(new TransientRole(defaultRoleName));
                }
            }
        } catch (LoginException e) {
            throw new SecurityException("System repository throws LoginException: " + e, e);
        } catch (RepositoryException e) {
            throw new SecurityException("System repository throws RepositoryException: " + e, e);
        }
        
        if (roleSet == null) {
            roleSet = Collections.emptySet();
        }
        
        return roleSet;
    }
    
    protected Set<String> getRoleNamesOfUser(String username) throws LoginException, RepositoryException {
        Set<String> roleNameSet = null;
        Session session = null;
        
        try {
            if (getSystemCredentials() != null) {
                session = getSystemRepository().login(getSystemCredentials());
            } else {
                session = getSystemRepository().login();
            }

            String statement = MessageFormat.format(getRolesOfUserQuery(), username);

            log.debug("Searching roles of user with query: " + statement);

            Query q = session.getWorkspace().getQueryManager().createQuery(statement, getQueryLanguage());
            QueryResult result = q.execute();
            NodeIterator nodeIt = result.getNodes();
            
            roleNameSet = new HashSet<String>();
            
            while (nodeIt.hasNext()) {
                String roleName = nodeIt.nextNode().getName();
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
