/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository;

import java.util.Map;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.XASessionImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.authentication.AuthContext;

/**
 *
 * @author berry
 */
public class JackrabbitRepository extends RepositoryImpl {
    private final Map<SessionImpl, SessionImpl> sessions = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    public JackrabbitRepository(RepositoryConfig config, String managerAddress) throws RepositoryException {
        super(config);
        super.setDescriptor("manager", managerAddress);
    }

    @Override
    protected void onSessionCreated(SessionImpl session) {
        synchronized (sessions) {
            session.addListener(this);
            sessions.put(session, session);
        }
    }

    protected SessionImpl createSessionInstance(AuthContext loginContext,
                                                WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        return new JackrabbitSession(context, loginContext, wspConfig);
    }

    protected SessionImpl createSessionInstance(Subject subject,
                                                WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        return new JackrabbitSession(context, subject, wspConfig);
    }

    public Session getSession(String sessionName) {
        for(Session session : sessions.values()) {
            if(session.toString().equals(sessionName))
                return session;
        }
        return null;
    }
    
    class JackrabbitSession extends XASessionImpl {
        public JackrabbitSession(RepositoryContext repositoryContext, AuthContext loginContext, WorkspaceConfig wspConfig) throws AccessDeniedException, RepositoryException {
            super(repositoryContext, loginContext, wspConfig);
            setAttribute("sessionName", super.toString());
        }
        public JackrabbitSession(RepositoryContext repositoryContext, Subject subject, WorkspaceConfig wspConfig) throws AccessDeniedException, RepositoryException {
            super(repositoryContext, subject, wspConfig);
            setAttribute("sessionName", super.toString());
        }
    }
}
