package org.onehippo.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * DO NOT USE, THIS INTERFACE IS NOT YET PART OF THE PUBLIC API.
 * @exclude
 */
public class ManagerServiceFactory {
    static WeakHashMap<Session, ManagerService> services = new WeakHashMap<Session, ManagerService>();
    public static ManagerService getManagerService(Session jcrSession) throws RepositoryException {
        ManagerService service = services.get(jcrSession);
        if(service == null) {
            try {
                Class<? extends ManagerService> managerServiceFactory = Class.forName("org.onehippo.repository.ManagerServiceImpl").asSubclass(ManagerService.class);
                service = managerServiceFactory.getConstructor(new Class[] { Session.class }).newInstance(new Object[] { jcrSession });
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
