/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.proxyrepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryImpl;

public class ProxyHippoRepository extends HippoRepositoryImpl {
    Map<Session, ProxyHandler> sessions;

    public ProxyHippoRepository(Repository upstream) throws RepositoryException, IOException {
        repository = upstream;
        sessions = new HashMap<Session, ProxyHandler>();
    }

    public static HippoRepository create(String location) throws RepositoryException, IOException {
        if (location.startsWith("proxy:")) {
            location = location.substring("proxy:".length());
            while (location.startsWith("/")) {
                location = location.substring(1);
            }
        }
        HippoRepository upstream = HippoRepositoryFactory.getHippoRepository(location);
        return new ProxyHippoRepository(upstream.getRepository());
    }

    public void logout(Session session) throws IOException {
        ProxyHandler proxy = sessions.get(session);
        proxy.close();
        session.logout();
    }

    public Session login(String username, char[] password, OutputStream dump) throws LoginException, RepositoryException {
        Session session = login(username, password);
        ProxyHandler proxy;
        try {
            proxy = new ProxyHandler(dump);
            session = (Session)proxy.register(session);
            sessions.put(session, proxy);
        } catch (IOException ex) {
            throw new LoginException("cannot open dump file", ex);
        }
        return session;
    }

    public Session login(String username, char[] password, InputStream dump) throws LoginException, RepositoryException {
        Session session = login(username, password);
        ProxyHandler proxy = new ProxyHandler();
        session = (Session)proxy.register(session);
        try {
            proxy.play(dump);
            sessions.put(session, proxy);
        } catch (IOException ex) {
            session.logout();
            throw new LoginException("cannot open dump file", ex);
        }

        return session;
    }
}
