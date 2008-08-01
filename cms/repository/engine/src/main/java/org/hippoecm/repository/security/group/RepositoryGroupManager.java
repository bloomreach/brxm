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
package org.hippoecm.repository.security.group;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The GroupManager that stores the groups in the JCR Repository
 */
public class RepositoryGroupManager extends AbstractGroupManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void initManager(ManagerContext context) throws RepositoryException {
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> listGroups() throws RepositoryException {
        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized.");
        }

        // find users managed by this provider as sub nodes off the users path
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_GROUP);
        statement.append(" WHERE ");
        statement.append("  jcr:path LIKE '").append(groupsPath).append("/%").append("'");
        statement.append(" AND jcr:PrimaryType = '").append(HippoNodeType.NT_GROUP).append("')");

        //log.debug("Searching for groups: {}", statement);

        Set<String> groupIds = new HashSet<String>();

        // find users managed by this provider
        QueryManager qm;
        try {
            qm = session.getWorkspace().getQueryManager();
            Query q = qm.createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator iter = result.getNodes();
            while (iter.hasNext()) {
                groupIds.add(iter.nextNode().getName());
            }
        } catch (RepositoryException e) {
            log.warn("Exception while parsing groups from path: {}", groupsPath);
        }
        return Collections.unmodifiableSet(groupIds);
    }
}
