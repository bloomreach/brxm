package org.onehippo.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.hippoecm.repository.api.HippoSession;

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
