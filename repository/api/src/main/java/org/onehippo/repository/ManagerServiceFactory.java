/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.hippoecm.repository.api.HippoSession;

/**
 * DO NOT USE, THIS INTERFACE IS NOT YET PART OF THE PUBLIC API.
 * Factory class to obtain a {@link ManagerService} based on a JCR session.  The factory
 * will provide the right implementation for the ManagerService depending on whether the
 * JCR session is a {@link HippoSession}, a plain Jackrabbit JCR session or a Jackrabbit
 * RMI based JCR session.
 */
@Deprecated
public class ManagerServiceFactory {

    static WeakHashMap<Session, ManagerService> services = new WeakHashMap<Session, ManagerService>();

    /**
     * Creates or re-retrieves a managerService factory based on the JCR session passed as argument.
     * Only one ManagerService will be instantiated per session.
     * @param jcrSession the session for which to create a ManagerService
     * @return a ManagerService factory for the JCR session
     * @throws RepositoryException in case a suitable ManagerService factory could not be instantiated
     */
    public static ManagerService getManagerService(Session jcrSession) throws RepositoryException {
        ManagerService service = services.get(jcrSession);
        if(service == null) {
            try {
                if(jcrSession instanceof HippoSession) {
                    Class<? extends ManagerService> managerServiceFactory = Class.forName("org.onehippo.repository.remote.HippoManagerServiceImpl").asSubclass(ManagerService.class);
                    service = managerServiceFactory.getConstructor(new Class[] {HippoSession.class}).newInstance(new Object[] {jcrSession});
                } else if (Class.forName("org.apache.jackrabbit.rmi.client.ClientSession").isInstance(jcrSession)) {
                    String managerAddress = jcrSession.getRepository().getDescriptor("manager");
                    Class<? extends ManagerService> managerServiceFactory = Class.forName("org.onehippo.repository.embeddedrmi.ClientManagerService").asSubclass(ManagerService.class);
                    service = managerServiceFactory.getConstructor(new Class[] {String.class, Session.class}).newInstance(new Object[] {managerAddress, jcrSession});
                } else {
                    Class<? extends ManagerService> managerServiceFactory = Class.forName("org.onehippo.repository.impl.ManagerServiceImpl").asSubclass(ManagerService.class);
                    service = managerServiceFactory.getConstructor(new Class[] {Session.class}).newInstance(new Object[] {jcrSession});
                }
                services.put(jcrSession, service);
            } catch(ClassNotFoundException ex) {
                throw new RepositoryException("bad or missing dependency to ManagerService implementation", ex);
            } catch(NoSuchMethodException ex) {
                throw new RepositoryException("bad or missing dependency to ManagerService implementation", ex);
            } catch(InstantiationException ex) {
                throw new RepositoryException("bad or missing dependency to ManagerService implementation", ex);
            } catch(IllegalAccessException ex) {
                throw new RepositoryException("bad or missing dependency to ManagerService implementation", ex);
            } catch(InvocationTargetException ex) {
                throw new RepositoryException("bad or missing dependency to ManagerService implementation", ex.getCause());
            }
        }
        return service;
    }
}
